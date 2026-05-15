package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.role.RoleNotFoundException;
import PSG.backEnd.exception.user.UserAlreadyExistsException;
import PSG.backEnd.exception.user.UserDataConflictException;
import PSG.backEnd.exception.user.UserNotFoundException;
import PSG.backEnd.exception.user.UserNotValidException;
import PSG.backEnd.model.dto.security.UserFilterDTO;
import PSG.backEnd.model.dto.security.UserRequestDTO;
import PSG.backEnd.model.dto.security.UserResponseDTO;
import PSG.backEnd.model.dto.security.UserUpdateOwnProfileResponseDTO;
import PSG.backEnd.model.entity.security.Credentials;
import PSG.backEnd.model.entity.security.Role;
import PSG.backEnd.model.entity.security.User;
import PSG.backEnd.model.mapper.CredentialsMapper;
import PSG.backEnd.model.mapper.UserMapper;
import PSG.backEnd.repository.CredentialsRepository;
import PSG.backEnd.repository.RoleRepository;
import PSG.backEnd.repository.UserRepository;
import PSG.backEnd.model.entity.security.UserDeletedEvent;
import PSG.backEnd.service.port.IUserService;
import org.springframework.context.ApplicationEventPublisher;
import PSG.backEnd.service.security.PasswordPolicyService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of the user management service.
 * Includes password encryption with BCrypt and business validations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements IUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CredentialsRepository credentialsRepository;
    private final UserMapper userMapper;
    private final CredentialsMapper credentialsMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyService passwordPolicyService;
    private final MessageSourceHelper messageSourceHelper;
    private final JwtService jwtService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public UserResponseDTO createUser(UserRequestDTO requestDTO) {
        log.info("Creating new user: {}", requestDTO.credentials().username());

        // Validar datos del usuario
        validateNewUser(requestDTO);

        // Check if there's a deleted user with the same username
        Optional<User> deletedUser = userRepository.findByCredentialsUsername(requestDTO.credentials().username())
                .filter(User::getDeleted);

        if (deletedUser.isPresent()) {
            return reactivateUser(deletedUser.get(), requestDTO);
        }

        return createNewUser(requestDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> getAllUsers(UserFilterDTO filterDTO, Pageable pageable) {
        log.debug("Fetching users with filters: {}", filterDTO);

        return userRepository.findAllWithFilters(
                filterDTO.username(),
                filterDTO.email(),
                filterDTO.firstName(),
                filterDTO.lastName(),
                filterDTO.enabled(),
                filterDTO.search(),
                pageable
        ).map(userMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(Long id) {
        log.debug("Fetching user by id: {}", id);

        return userRepository.findByIdAndDeletedFalse(id)
                .map(userMapper::toResponseDto)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    @Transactional
    public UserResponseDTO updateUser(Long id, UserRequestDTO requestDTO) {
        log.info("Updating user with id: {}", id);

        User existingUser = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        // Validar inmunidad jerárquica
        validateHierarchyImmunity(existingUser);

        // Validar actualización
        validateUserUpdate(id, requestDTO);

        // Capture sensitive values before partial update to detect changes
        String oldEmail         = existingUser.getEmail();
        String oldWhatsappNumber = existingUser.getWhatsappNumber();

        // Partial update: map non-null basic fields (mapper uses IGNORE for nulls)
        userMapper.partialUpdate(requestDTO, existingUser);

        // Explicit sanitization and partial updates for fields that need trimming or special handling
        if (requestDTO.email() != null) {
            String trimmedEmail = requestDTO.email().trim();
            if (!trimmedEmail.equals(oldEmail)) {
                existingUser.setEmailVerified(false);
            }
            existingUser.setEmail(trimmedEmail);
        }

        if (requestDTO.whatsappNumber() != null && !requestDTO.whatsappNumber().equals(oldWhatsappNumber)) {
            existingUser.setWhatsappVerified(false);
        }
        if (requestDTO.firstName() != null) {
            existingUser.setFirstName(requestDTO.firstName().trim());
        }
        if (requestDTO.lastName() != null) {
            existingUser.setLastName(requestDTO.lastName().trim());
        }
        if (requestDTO.jobTitle() != null) {
            existingUser.setJobTitle(requestDTO.jobTitle().trim());
        }

        // Allow toggling enabled flag via patch if provided
        if (requestDTO.enabled() != null) {
            existingUser.setEnabled(requestDTO.enabled());
        }

        // Update credentials if provided (partial)
        if (requestDTO.credentials() != null) {
            Credentials credentials = existingUser.getCredentials();
            if (credentials == null) {
                credentials = new Credentials();
                existingUser.setCredentials(credentials);
            }

            // Update username if provided
            if (requestDTO.credentials().username() != null && !requestDTO.credentials().username().trim().isEmpty()) {
                credentials.setUsername(requestDTO.credentials().username().trim());
            }

            // Update password if provided
            if (requestDTO.credentials().password() != null && !requestDTO.credentials().password().trim().isEmpty()) {
                String newUsername = credentials.getUsername();
                String newEmail = existingUser.getEmail();
                passwordPolicyService.validate(requestDTO.credentials().password(), newUsername, newEmail);
                String encryptedPassword = passwordEncoder.encode(requestDTO.credentials().password());
                credentials.setPassword(encryptedPassword);
            }

            credentials.setUser(existingUser);
        }

        // Update roles if provided
        if (requestDTO.roleIds() != null) {
            if (requestDTO.roleIds().isEmpty()) {
                throw new UserNotValidException(messageSourceHelper.getMessage("user.roles.required"));
            }
            Set<Role> roles = validateAndGetRoles(requestDTO.roleIds());
            existingUser.setRoles(roles);
        }

        try {
            User updatedUser = userRepository.save(existingUser);
            log.info("User updated successfully: {}", updatedUser.getUsername());
            return userMapper.toResponseDto(updatedUser);
        } catch (DataIntegrityViolationException e) {
            handleDataIntegrityViolation(e, requestDTO);
            throw e;
        }
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);

        User user = userRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        // Validar inmunidad jerárquica
        validateHierarchyImmunity(user);

        // Validar que el usuario pueda ser eliminado
        validateUserDeletion(user);

        user.setDeleted(true);
        user.setEnabled(false);

        // Also soft-delete the credentials to free up the username
        if (user.getCredentials() != null) {
            user.getCredentials().setDeleted(true);
        }

        eventPublisher.publishEvent(new UserDeletedEvent(user.getId()));
        userRepository.save(user);

        log.info("User deleted successfully: {}", user.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return userRepository.existsByIdAndDeletedFalse(id);
    }

    // ==================== Métodos privados de validación ====================

    /**
     * Verifica si el usuario autenticado tiene God Mode (ROLE_OWNER o ROLE_ADMIN).
     */
    private boolean isAuthenticatedUserGodMode() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) return false;
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> "ROLE_OWNER".equals(a) || "ROLE_ADMIN".equals(a));
    }

    /**
     * Obtiene la posición jerárquica más alta (número más bajo) del usuario autenticado.
     */
    private int getAuthenticatedUserHighestPosition() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User user)) {
            return Integer.MAX_VALUE;
        }
        return user.getRoles().stream()
                .filter(r -> !r.getDeleted() && r.getActive())
                .mapToInt(Role::getPosition)
                .min()
                .orElse(Integer.MAX_VALUE);
    }

    /**
     * Valida inmunidad jerárquica: un usuario no puede modificar/eliminar a otro usuario
     * que tenga un rol con posición igual o superior (número menor o igual).
     * God Mode (OWNER/ADMIN) está exento.
     */
    private void validateHierarchyImmunity(User targetUser) {
        if (isAuthenticatedUserGodMode()) return;

        int callerPosition = getAuthenticatedUserHighestPosition();
        int targetPosition = targetUser.getRoles().stream()
                .filter(r -> !r.getDeleted() && r.getActive())
                .mapToInt(Role::getPosition)
                .min()
                .orElse(Integer.MAX_VALUE);

        if (callerPosition >= targetPosition) {
            throw new UserNotValidException(
                    messageSourceHelper.getMessage("user.hierarchy.cannotManage",
                            targetUser.getFirstName() + " " + targetUser.getLastName()));
        }
    }

    /**
     * Validates data for a new user.
     */
    private void validateNewUser(UserRequestDTO requestDTO) {
        // Validate credentials
        if (requestDTO.credentials() == null) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.credentials.required"));
        }

        // Validate username
        if (requestDTO.credentials().username() == null || requestDTO.credentials().username().trim().isEmpty()) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.username.empty"));
        }
        if (requestDTO.credentials().username().length() > 50) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.username.tooLong"));
        }

        // Validate that username doesn't exist (excluding deleted credentials)
        if (credentialsRepository.existsByUsernameAndDeletedFalse(requestDTO.credentials().username())) {
            throw new UserAlreadyExistsException(messageSourceHelper.getMessage("user.username.exists"));
        }

        // Validate email
        if (requestDTO.email() == null || requestDTO.email().trim().isEmpty()) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.email.empty"));
        }
        if (requestDTO.email().length() > 100) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.email.tooLong"));
        }
        // Basic email pattern
        if (!requestDTO.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.email.invalid"));
        }

        // Validate that email doesn't exist
        if (userRepository.existsByEmailAndDeletedFalse(requestDTO.email())) {
            throw new UserAlreadyExistsException(messageSourceHelper.getMessage("user.email.exists"));
        }

        // Validate password
        if (requestDTO.credentials().password() == null || requestDTO.credentials().password().trim().isEmpty()) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.password.empty"));
        }
        passwordPolicyService.validate(
                requestDTO.credentials().password(),
                requestDTO.credentials().username(),
                requestDTO.email()
        );

        // Validate names
        if (requestDTO.firstName() == null || requestDTO.firstName().trim().isEmpty()) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.firstName.empty"));
        }
        if (requestDTO.firstName().length() > 50) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.firstName.tooLong"));
        }

        if (requestDTO.lastName() == null || requestDTO.lastName().trim().isEmpty()) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.lastName.empty"));
        }
        if (requestDTO.lastName().length() > 50) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.lastName.tooLong"));
        }

        if (requestDTO.jobTitle() != null && requestDTO.jobTitle().length() > 100) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.jobTitle.tooLong"));
        }

        // Validate that roles are provided
        if (requestDTO.roleIds() == null || requestDTO.roleIds().isEmpty()) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.roles.required"));
        }

        // Validate that roles exist
        validateRoleIds(requestDTO.roleIds());
    }

    /**
     * Validates user update data.
     */
    private void validateUserUpdate(Long id, UserRequestDTO requestDTO) {
        // Validate username if provided
        if (requestDTO.credentials() != null && requestDTO.credentials().username() != null) {
            if (requestDTO.credentials().username().trim().isEmpty()) {
                throw new UserNotValidException(messageSourceHelper.getMessage("user.username.empty"));
            }
            if (requestDTO.credentials().username().length() > 50) {
                throw new UserNotValidException(messageSourceHelper.getMessage("user.username.tooLong"));
            }

            // Verify that username is not in use by another user (check only non-deleted credentials)
            credentialsRepository.findByUsernameAndDeletedFalse(requestDTO.credentials().username())
                    .ifPresent(existing -> {
                        if (existing.getUser() != null && !existing.getUser().getId().equals(id)) {
                            throw new UserAlreadyExistsException(messageSourceHelper.getMessage("user.username.exists"));
                        }
                    });
        }

        // Validate email if provided
        if (requestDTO.email() != null) {
            if (requestDTO.email().trim().isEmpty()) {
                throw new UserNotValidException(messageSourceHelper.getMessage("user.email.empty"));
            }
            if (requestDTO.email().length() > 100) {
                throw new UserNotValidException(messageSourceHelper.getMessage("user.email.tooLong"));
            }
            if (!requestDTO.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new UserNotValidException(messageSourceHelper.getMessage("user.email.invalid"));
            }

            // Verify that email is not in use by another user
            userRepository.findByEmail(requestDTO.email())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(id)) {
                            if (existing.getDeleted()) {
                                throw new UserAlreadyExistsException(
                                        messageSourceHelper.getMessage("user.email.deletedUser", requestDTO.email()));
                            } else {
                                throw new UserAlreadyExistsException(messageSourceHelper.getMessage("user.email.exists"));
                            }
                        }
                    });
        }

        // Validate password if provided
        if (requestDTO.credentials() != null && requestDTO.credentials().password() != null) {
            if (requestDTO.credentials().password().trim().isEmpty()) {
                throw new UserNotValidException(messageSourceHelper.getMessage("user.password.empty"));
            }
            passwordPolicyService.validate(
                    requestDTO.credentials().password(),
                    requestDTO.credentials().username(),
                    requestDTO.email()
            );
        }

        // Validate roles if provided
        if (requestDTO.roleIds() != null) {
            if (requestDTO.roleIds().isEmpty()) {
                throw new UserNotValidException(messageSourceHelper.getMessage("user.roles.required"));
            }
            validateRoleIds(requestDTO.roleIds());
        }
    }

    /**
     * Validates password complexity. Delegates to {@link PasswordPolicyService}
     * which enforces length, character classes, denylist and HIBP breach lookup.
     * Kept for callers that don't have user context handy.
     */
    private void validatePasswordComplexity(String password) {
        passwordPolicyService.validate(password, null, null);
    }

    /**
     * Reactivates a deleted user.
     */
    private UserResponseDTO reactivateUser(User deletedUser, UserRequestDTO requestDTO) {
        log.info("Reactivating deleted user: {}", deletedUser.getUsername());

        // Validate that all required fields are present for reactivation
        if (requestDTO.credentials() == null || requestDTO.credentials().password() == null || requestDTO.credentials().password().trim().isEmpty()) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.reactivate.passwordRequired"));
        }

        if (requestDTO.email() == null || requestDTO.email().trim().isEmpty()) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.reactivate.emailRequired"));
        }

        if (requestDTO.firstName() == null || requestDTO.firstName().trim().isEmpty()) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.reactivate.firstNameRequired"));
        }

        if (requestDTO.lastName() == null || requestDTO.lastName().trim().isEmpty()) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.reactivate.lastNameRequired"));
        }

        if (requestDTO.roleIds() == null || requestDTO.roleIds().isEmpty()) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.reactivate.rolesRequired"));
        }

        // Validate the data (lengths, format, complexity, uniqueness)
        if (requestDTO.credentials().username().length() > 50) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.username.tooLong"));
        }

        if (requestDTO.email().length() > 100) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.email.tooLong"));
        }
        if (!requestDTO.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.email.invalid"));
        }
        if (requestDTO.firstName().length() > 50) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.firstName.tooLong"));
        }
        if (requestDTO.lastName().length() > 50) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.lastName.tooLong"));
        }
        if (requestDTO.jobTitle() != null && requestDTO.jobTitle().length() > 100) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.jobTitle.tooLong"));
        }

        validatePasswordComplexity(requestDTO.credentials().password());
        validateRoleIds(requestDTO.roleIds());

        // Check username uniqueness (excluding this user's credentials and deleted credentials)
        credentialsRepository.findByUsernameAndDeletedFalse(requestDTO.credentials().username()).ifPresent(existing -> {
            if (existing.getUser() != null && !existing.getUser().getId().equals(deletedUser.getId())) {
                throw new UserAlreadyExistsException(messageSourceHelper.getMessage("user.username.exists"));
            }
        });

        // Check email uniqueness (excluding this user and deleted users)
        userRepository.findByEmail(requestDTO.email()).ifPresent(existing -> {
            if (!existing.getId().equals(deletedUser.getId()) && !existing.getDeleted()) {
                throw new UserAlreadyExistsException(messageSourceHelper.getMessage("user.email.exists"));
            }
        });

        // Reactivate user
        deletedUser.setDeleted(false);
        deletedUser.setEnabled(requestDTO.enabled() != null ? requestDTO.enabled() : true);

        // Update data
        deletedUser.setEmail(requestDTO.email().trim());
        deletedUser.setFirstName(requestDTO.firstName().trim());
        deletedUser.setLastName(requestDTO.lastName().trim());
        deletedUser.setJobTitle(requestDTO.jobTitle() != null ? requestDTO.jobTitle().trim() : null);

        // Update credentials and reactivate them
        Credentials credentials = deletedUser.getCredentials();
        if (credentials == null) {
            credentials = new Credentials();
            credentials.setUser(deletedUser);
            deletedUser.setCredentials(credentials);
        }

        // Ensure username is set (allow same username for reactivation)
        credentials.setUsername(requestDTO.credentials().username().trim());
        // Reactivate credentials
        credentials.setDeleted(false);

        // Update password
        String encryptedPassword = passwordEncoder.encode(requestDTO.credentials().password());
        credentials.setPassword(encryptedPassword);

        // Update roles
        Set<Role> roles = validateAndGetRoles(requestDTO.roleIds());
        deletedUser.setRoles(roles);

        User reactivatedUser = userRepository.save(deletedUser);
        log.info("User reactivated successfully: {}", reactivatedUser.getUsername());

        return userMapper.toResponseDto(reactivatedUser);
    }

    /**
     * Validates that all role IDs exist in the database.
     */
    private void validateRoleIds(Set<Long> roleIds) {
        for (Long roleId : roleIds) {
            if (!roleRepository.existsByIdAndDeletedFalse(roleId)) {
                throw new RoleNotFoundException(roleId);
            }
        }
    }

    /**
     * Validates and retrieves roles by their IDs.
     * Enforces role assignment hierarchy rules:
     * - OWNER role (position 1) is NEVER assignable via API.
     * - Non-God-Mode users can only assign roles at their position or below,
     *   except LECTOR which anyone can assign.
     */
    private Set<Role> validateAndGetRoles(Set<Long> roleIds) {
        Set<Role> roles = new HashSet<>();
        int callerPosition = getAuthenticatedUserHighestPosition();
        boolean godMode = isAuthenticatedUserGodMode();

        for (Long roleId : roleIds) {
            Role role = roleRepository.findByIdAndDeletedFalse(roleId)
                    .orElseThrow(() -> new RoleNotFoundException(roleId));

            // OWNER (position 1) is NEVER assignable — only the tenant creator has it
            if (role.getPosition() != null && role.getPosition() == 1) {
                throw new UserNotValidException(
                        messageSourceHelper.getMessage("user.role.ownerNotAssignable"));
            }

            // God mode users (OWNER/ADMIN) can assign any non-OWNER role
            if (!godMode) {
                // LECTOR exception: anyone with user management can assign it
                boolean isLector = "LECTOR".equalsIgnoreCase(role.getName());
                if (!isLector && role.getPosition() != null && role.getPosition() < callerPosition) {
                    throw new UserNotValidException(
                            messageSourceHelper.getMessage("user.role.cannotAssignAbove", role.getName()));
                }
            }

            roles.add(role);
        }

        return roles;
    }

    /**
     * Validates that a user can be deleted.
     */
    private void validateUserDeletion(User user) {
        // Additional validations can be added here, for example:
        // - Don't allow deleting the last administrator user
        // - Verify the user has no pending operations

        // Protect ROOT user
        String username = user.getUsername();
        if (username != null && ("root".equalsIgnoreCase(username) || "admin".equalsIgnoreCase(username))) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.cannotDeleteSystem", username));
        }
    }

    /**
     * Creates a new user in the database.
     */
    private UserResponseDTO createNewUser(UserRequestDTO requestDTO) {
        User newUser = userMapper.toEntity(requestDTO);

        // Create and set credentials
        Credentials credentials = credentialsMapper.toEntity(requestDTO.credentials());
        credentials.setUsername(requestDTO.credentials().username());
        credentials.setDeleted(false); // Set default for new credentials

        // Encrypt password
        String encryptedPassword = passwordEncoder.encode(requestDTO.credentials().password());
        credentials.setPassword(encryptedPassword);

        credentials.setUser(newUser);
        newUser.setCredentials(credentials);

        // Set default values
        newUser.setEnabled(requestDTO.enabled() != null ? requestDTO.enabled() : true);
        newUser.setDeleted(false);

        // Assign roles
        Set<Role> roles = validateAndGetRoles(requestDTO.roleIds());
        newUser.setRoles(roles);

        User savedUser = userRepository.save(newUser);
        log.info("New user created successfully: {} with {} roles",
                savedUser.getUsername(), savedUser.getRoles().size());

        return userMapper.toResponseDto(savedUser);
    }

    private void handleDataIntegrityViolation(DataIntegrityViolationException e, UserRequestDTO requestDTO) {
        String errorMessage = e.getMessage().toLowerCase();

        // Detectar violación de constraint de username (en Credentials)
        if (errorMessage.contains("username") || errorMessage.contains("uk_") && errorMessage.contains("username")) {
            String username = requestDTO.credentials() != null ? requestDTO.credentials().username() : "unknown";
            throw new UserDataConflictException(
                messageSourceHelper.getMessage("user.update.conflict.username", username),
                e
            );
        }

        // Detectar violación de constraint de email
        if (errorMessage.contains("email") || errorMessage.contains("uk_") && errorMessage.contains("email")) {
            throw new UserDataConflictException(
                messageSourceHelper.getMessage("user.update.conflict.email", requestDTO.email()),
                e
            );
        }

        // Si es una violación de integridad pero no podemos determinar el campo específico
        throw new UserDataConflictException(
            messageSourceHelper.getMessage("user.update.conflict.generic"),
            e
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> getAllUsersExcludingCurrent(UserFilterDTO filterDTO, Pageable pageable, String excludeUsername) {
        log.debug("Fetching users with filters: {} (excluding current user: {})", filterDTO, excludeUsername);

        return userRepository.findAllWithFiltersExcludingUsername(
                excludeUsername,
                filterDTO.username(),
                filterDTO.email(),
                filterDTO.firstName(),
                filterDTO.lastName(),
                filterDTO.enabled(),
                filterDTO.search(),
                pageable
        ).map(userMapper::toResponseDto);
    }

    @Override
    @Transactional
    public UserUpdateOwnProfileResponseDTO updateOwnProfile(String username, UserRequestDTO requestDTO) {
        log.info("User {} is updating their own profile", username);

        // Find user by username
        User existingUser = userRepository.findByCredentialsUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        if (existingUser.getDeleted()) {
            throw new UserNotFoundException(username);
        }

        // Update user data
        if (requestDTO.firstName() != null) {
            existingUser.setFirstName(requestDTO.firstName());
        }
        if (requestDTO.lastName() != null) {
            existingUser.setLastName(requestDTO.lastName());
        }
        if (requestDTO.email() != null) {
            if (!requestDTO.email().equals(existingUser.getEmail())) {
                existingUser.setEmailVerified(false);
            }
            existingUser.setEmail(requestDTO.email());
        }
        if (requestDTO.whatsappNumber() != null) {
            if (!requestDTO.whatsappNumber().equals(existingUser.getWhatsappNumber())) {
                existingUser.setWhatsappVerified(false);
            }
            existingUser.setWhatsappNumber(requestDTO.whatsappNumber());
        }

        // Update credentials if provided
        if (requestDTO.credentials() != null) {
            Credentials credentials = existingUser.getCredentials();

            if (requestDTO.credentials().username() != null &&
                !requestDTO.credentials().username().equals(credentials.getUsername())) {
                // Validate new username is available
                Optional<User> userWithSameUsername = userRepository.findByCredentialsUsername(requestDTO.credentials().username());
                if (userWithSameUsername.isPresent() && !userWithSameUsername.get().getId().equals(existingUser.getId())) {
                    throw new UserAlreadyExistsException(
                        messageSourceHelper.getMessage("user.username.exists", requestDTO.credentials().username()));
                }
                credentials.setUsername(requestDTO.credentials().username());
            }

            if (requestDTO.credentials().password() != null && !requestDTO.credentials().password().isBlank()) {
                String encryptedPassword = passwordEncoder.encode(requestDTO.credentials().password());
                credentials.setPassword(encryptedPassword);
            }

            credentialsRepository.save(credentials);
        }

        // Update roles if provided
        if (requestDTO.roleIds() != null && !requestDTO.roleIds().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (Long roleId : requestDTO.roleIds()) {
                Role role = roleRepository.findByIdAndDeletedFalse(roleId)
                        .orElseThrow(() -> new RoleNotFoundException(roleId));
                roles.add(role);
            }
            existingUser.setRoles(roles);
        }

        try {
            User updatedUser = userRepository.save(existingUser);

            // Generate new tokens
            String accessToken = jwtService.generateAccessToken(updatedUser);
            String refreshToken = jwtService.generateRefreshToken(updatedUser);

            UserResponseDTO userResponse = userMapper.toResponseDto(updatedUser);

            log.info("User profile updated successfully: {}", username);

            return new UserUpdateOwnProfileResponseDTO(userResponse, accessToken, refreshToken);

        } catch (DataIntegrityViolationException e) {
            handleDataIntegrityViolation(e, requestDTO);
            return null; // Never reached, handleDataIntegrityViolation throws
        }
    }
}

