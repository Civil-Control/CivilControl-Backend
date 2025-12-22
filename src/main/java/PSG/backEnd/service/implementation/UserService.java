package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.role.RoleNotFoundException;
import PSG.backEnd.exception.user.UserAlreadyExistsException;
import PSG.backEnd.exception.user.UserDataConflictException;
import PSG.backEnd.exception.user.UserNotFoundException;
import PSG.backEnd.exception.user.UserNotValidException;
import PSG.backEnd.model.dto.security.UserFilterDTO;
import PSG.backEnd.model.dto.security.UserRequestDTO;
import PSG.backEnd.model.dto.security.UserResponseDTO;
import PSG.backEnd.model.entity.security.Credentials;
import PSG.backEnd.model.entity.security.Role;
import PSG.backEnd.model.entity.security.User;
import PSG.backEnd.model.mapper.CredentialsMapper;
import PSG.backEnd.model.mapper.UserMapper;
import PSG.backEnd.repository.CredentialsRepository;
import PSG.backEnd.repository.RoleRepository;
import PSG.backEnd.repository.UserRepository;
import PSG.backEnd.service.port.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

        // Validar actualización
        validateUserUpdate(id, requestDTO);

        // Partial update: map non-null basic fields (mapper uses IGNORE for nulls)
        userMapper.partialUpdate(requestDTO, existingUser);

        // Explicit sanitization and partial updates for fields that need trimming or special handling
        if (requestDTO.email() != null) {
            existingUser.setEmail(requestDTO.email().trim());
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
                validatePasswordComplexity(requestDTO.credentials().password());
                String encryptedPassword = passwordEncoder.encode(requestDTO.credentials().password());
                credentials.setPassword(encryptedPassword);
            }

            credentials.setUser(existingUser);
        }

        // Update roles if provided
        if (requestDTO.roleIds() != null) {
            if (requestDTO.roleIds().isEmpty()) {
                throw new UserNotValidException("At least one role is required");
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

        // Validar que el usuario pueda ser eliminado
        validateUserDeletion(user);

        user.setDeleted(true);
        user.setEnabled(false);

        // Also soft-delete the credentials to free up the username
        if (user.getCredentials() != null) {
            user.getCredentials().setDeleted(true);
        }

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
     * Validates data for a new user.
     */
    private void validateNewUser(UserRequestDTO requestDTO) {
        // Validate credentials
        if (requestDTO.credentials() == null) {
            throw new UserNotValidException("Credentials cannot be null");
        }

        // Validate username
        if (requestDTO.credentials().username() == null || requestDTO.credentials().username().trim().isEmpty()) {
            throw new UserNotValidException("Username cannot be empty");
        }
        if (requestDTO.credentials().username().length() > 50) {
            throw new UserNotValidException("Username cannot be longer than 50 characters");
        }

        // Validate that username doesn't exist (excluding deleted credentials)
        if (credentialsRepository.existsByUsernameAndDeletedFalse(requestDTO.credentials().username())) {
            throw new UserAlreadyExistsException("Username already exists: " + requestDTO.credentials().username());
        }

        // Validate email
        if (requestDTO.email() == null || requestDTO.email().trim().isEmpty()) {
            throw new UserNotValidException("Email cannot be empty");
        }
        if (requestDTO.email().length() > 100) {
            throw new UserNotValidException("Email cannot be longer than 100 characters");
        }
        // Basic email pattern
        if (!requestDTO.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new UserNotValidException("Email format is invalid");
        }

        // Validate that email doesn't exist
        if (userRepository.existsByEmailAndDeletedFalse(requestDTO.email())) {
            throw new UserAlreadyExistsException("Email already exists: " + requestDTO.email());
        }

        // Validate password
        if (requestDTO.credentials().password() == null || requestDTO.credentials().password().trim().isEmpty()) {
            throw new UserNotValidException("Password cannot be empty");
        }
        validatePasswordComplexity(requestDTO.credentials().password());

        // Validate names
        if (requestDTO.firstName() == null || requestDTO.firstName().trim().isEmpty()) {
            throw new UserNotValidException("First name cannot be empty");
        }
        if (requestDTO.firstName().length() > 50) {
            throw new UserNotValidException("First name cannot be longer than 50 characters");
        }

        if (requestDTO.lastName() == null || requestDTO.lastName().trim().isEmpty()) {
            throw new UserNotValidException("Last name cannot be empty");
        }
        if (requestDTO.lastName().length() > 50) {
            throw new UserNotValidException("Last name cannot be longer than 50 characters");
        }

        if (requestDTO.jobTitle() != null && requestDTO.jobTitle().length() > 100) {
            throw new UserNotValidException("Job title cannot be longer than 100 characters");
        }

        // Validate that roles are provided
        if (requestDTO.roleIds() == null || requestDTO.roleIds().isEmpty()) {
            throw new UserNotValidException("At least one role is required");
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
                throw new UserNotValidException("Username cannot be empty");
            }
            if (requestDTO.credentials().username().length() > 50) {
                throw new UserNotValidException("Username cannot be longer than 50 characters");
            }

            // Verify that username is not in use by another user (check only non-deleted credentials)
            credentialsRepository.findByUsernameAndDeletedFalse(requestDTO.credentials().username())
                    .ifPresent(existing -> {
                        if (existing.getUser() != null && !existing.getUser().getId().equals(id)) {
                            throw new UserAlreadyExistsException("Username already exists: " + requestDTO.credentials().username());
                        }
                    });
        }

        // Validate email if provided
        if (requestDTO.email() != null) {
            if (requestDTO.email().trim().isEmpty()) {
                throw new UserNotValidException("Email cannot be empty");
            }
            if (requestDTO.email().length() > 100) {
                throw new UserNotValidException("Email cannot be longer than 100 characters");
            }
            if (!requestDTO.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new UserNotValidException("Email format is invalid");
            }

            // Verify that email is not in use by another user
            userRepository.findByEmail(requestDTO.email())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(id)) {
                            if (existing.getDeleted()) {
                                throw new UserAlreadyExistsException(
                                        "Cannot use email '" + requestDTO.email() +
                                        "' because it belongs to a deleted user (ID: " + existing.getId() +
                                        "). Please use a different email.");
                            } else {
                                throw new UserAlreadyExistsException("Email already exists: " + requestDTO.email());
                            }
                        }
                    });
        }

        // Validate password if provided
        if (requestDTO.credentials() != null && requestDTO.credentials().password() != null) {
            if (requestDTO.credentials().password().trim().isEmpty()) {
                throw new UserNotValidException("Password cannot be empty");
            }
            validatePasswordComplexity(requestDTO.credentials().password());
        }

        // Validate roles if provided
        if (requestDTO.roleIds() != null) {
            if (requestDTO.roleIds().isEmpty()) {
                throw new UserNotValidException("At least one role is required");
            }
            validateRoleIds(requestDTO.roleIds());
        }
    }

    /**
     * Validates password complexity: at least 8 chars, one uppercase, one lowercase, one digit.
     */
    private void validatePasswordComplexity(String password) {
        if (password.length() < 8) {
            throw new UserNotValidException("Password must be at least 8 characters long");
        }
        String pattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$";
        if (!password.matches(pattern)) {
            throw new UserNotValidException("Password must contain at least one uppercase letter, one lowercase letter and one digit");
        }
    }

    /**
     * Reactivates a deleted user.
     */
    private UserResponseDTO reactivateUser(User deletedUser, UserRequestDTO requestDTO) {
        log.info("Reactivating deleted user: {}", deletedUser.getUsername());

        // Validate that all required fields are present for reactivation
        if (requestDTO.credentials() == null || requestDTO.credentials().password() == null || requestDTO.credentials().password().trim().isEmpty()) {
            throw new UserNotValidException("Password must be provided to reactivate a user");
        }

        if (requestDTO.email() == null || requestDTO.email().trim().isEmpty()) {
            throw new UserNotValidException("Email must be provided to reactivate a user");
        }

        if (requestDTO.firstName() == null || requestDTO.firstName().trim().isEmpty()) {
            throw new UserNotValidException("First name must be provided to reactivate a user");
        }

        if (requestDTO.lastName() == null || requestDTO.lastName().trim().isEmpty()) {
            throw new UserNotValidException("Last name must be provided to reactivate a user");
        }

        if (requestDTO.roleIds() == null || requestDTO.roleIds().isEmpty()) {
            throw new UserNotValidException("At least one role must be provided to reactivate a user");
        }

        // Validate the data (lengths, format, complexity, uniqueness)
        if (requestDTO.credentials().username().length() > 50) {
            throw new UserNotValidException("Username cannot be longer than 50 characters");
        }

        if (requestDTO.email().length() > 100) {
            throw new UserNotValidException("Email cannot be longer than 100 characters");
        }
        if (!requestDTO.email().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new UserNotValidException("Email format is invalid");
        }
        if (requestDTO.firstName().length() > 50) {
            throw new UserNotValidException("First name cannot be longer than 50 characters");
        }
        if (requestDTO.lastName().length() > 50) {
            throw new UserNotValidException("Last name cannot be longer than 50 characters");
        }
        if (requestDTO.jobTitle() != null && requestDTO.jobTitle().length() > 100) {
            throw new UserNotValidException("Job title cannot be longer than 100 characters");
        }

        validatePasswordComplexity(requestDTO.credentials().password());
        validateRoleIds(requestDTO.roleIds());

        // Check username uniqueness (excluding this user's credentials and deleted credentials)
        credentialsRepository.findByUsernameAndDeletedFalse(requestDTO.credentials().username()).ifPresent(existing -> {
            if (existing.getUser() != null && !existing.getUser().getId().equals(deletedUser.getId())) {
                throw new UserAlreadyExistsException("Username already exists: " + requestDTO.credentials().username());
            }
        });

        // Check email uniqueness (excluding this user and deleted users)
        userRepository.findByEmail(requestDTO.email()).ifPresent(existing -> {
            if (!existing.getId().equals(deletedUser.getId()) && !existing.getDeleted()) {
                throw new UserAlreadyExistsException("Email already exists: " + requestDTO.email());
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
     */
    private Set<Role> validateAndGetRoles(Set<Long> roleIds) {
        Set<Role> roles = new HashSet<>();

        for (Long roleId : roleIds) {
            Role role = roleRepository.findByIdAndDeletedFalse(roleId)
                    .orElseThrow(() -> new RoleNotFoundException(roleId));
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
            throw new UserNotValidException("Cannot delete system user: " + username);
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
                "Cannot update user: Username '" + username + "' is already in use by another user",
                e
            );
        }

        // Detectar violación de constraint de email
        if (errorMessage.contains("email") || errorMessage.contains("uk_") && errorMessage.contains("email")) {
            throw new UserDataConflictException(
                "Cannot update user: Email '" + requestDTO.email() + "' is already in use by another user",
                e
            );
        }

        // Si es una violación de integridad pero no podemos determinar el campo específico
        throw new UserDataConflictException(
            "Cannot update user due to a data integrity violation. Please verify that all unique fields (username, email) are not already in use",
            e
        );
    }
}

