package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.permission.PermissionNotFoundException;
import PSG.backEnd.exception.role.RoleAlreadyExistsException;
import PSG.backEnd.exception.role.RoleDataConflictException;
import PSG.backEnd.exception.role.RoleNotFoundException;
import PSG.backEnd.exception.role.RoleNotValidException;
import PSG.backEnd.model.dto.security.*;
import PSG.backEnd.model.entity.security.Permission;
import PSG.backEnd.model.entity.security.Role;
import PSG.backEnd.model.mapper.PermissionMapper;
import PSG.backEnd.model.mapper.RoleMapper;
import PSG.backEnd.repository.PermissionRepository;
import PSG.backEnd.repository.RoleRepository;
import PSG.backEnd.service.port.IRoleService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the role management service.
 * Contains all business logic and validations before reaching the DB.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService implements IRoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    @Transactional
    public RoleResponseDTO createRole(RoleRequestDTO requestDTO) {
        log.info("Creating new role: {}", requestDTO.name());

        // Validar datos del rol
        validateNewRole(requestDTO);

        // Check if there's a deleted role with the same name
        Optional<Role> deletedRole = roleRepository.findByName(requestDTO.name())
                .filter(Role::getDeleted);

        if (deletedRole.isPresent()) {
            return reactivateRole(deletedRole.get(), requestDTO);
        }

        return createNewRole(requestDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RoleResponseDTO> getAllRoles(RoleFilterDTO filterDTO, Pageable pageable) {
        log.debug("Fetching roles with filters: {}", filterDTO);

        return roleRepository.findAllWithFilters(
                filterDTO.name(),
                filterDTO.active(),
                pageable
        ).map(roleMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponseDTO getRoleById(Long id) {
        log.debug("Fetching role by id: {}", id);

        return roleRepository.findByIdAndDeletedFalse(id)
                .map(roleMapper::toResponseDto)
                .orElseThrow(() -> new RoleNotFoundException(id));
    }

    @Override
    @Transactional
    public RoleResponseDTO updateRole(Long id, RoleRequestDTO requestDTO) {
        log.info("Updating role with id: {}", id);

        Role existingRole = roleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RoleNotFoundException(id));

        // Validar actualización
        validateRoleUpdate(id, requestDTO);

        // Partial update: only apply non-null fields from DTO
        // Use mapper to update basic fields, but mapper ignores active and permissions
        roleMapper.partialUpdate(requestDTO, existingRole);

        // Explicitly handle fields that mapper ignores or need sanitization
        if (requestDTO.name() != null) {
            existingRole.setName(requestDTO.name().trim());
        }

        if (requestDTO.description() != null) {
            existingRole.setDescription(requestDTO.description().trim());
        }

        // Allow toggling active flag via patch if provided
        if (requestDTO.active() != null) {
            existingRole.setActive(requestDTO.active());
        }

        // Update permissions if provided
        if (requestDTO.permissionIds() != null) {
            if (requestDTO.permissionIds().isEmpty()) {
                throw new RoleNotValidException(messageSourceHelper.getMessage("role.permissions.required"));
            }
            Set<Permission> permissions = validateAndGetPermissions(requestDTO.permissionIds());
            existingRole.setPermissions(permissions);
        }

        try {
            Role updatedRole = roleRepository.save(existingRole);
            log.info("Role updated successfully: {}", updatedRole.getName());
            return roleMapper.toResponseDto(updatedRole);
        } catch (DataIntegrityViolationException e) {
            handleDataIntegrityViolation(e, requestDTO);
            throw e;
        }
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        log.info("Deleting role with id: {}", id);

        Role role = roleRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new RoleNotFoundException(id));

        // Validate that the role can be deleted
        validateRoleDeletion(role);

        role.setDeleted(true);
        role.setActive(false);
        roleRepository.save(role);

        log.info("Role deleted successfully: {}", role.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public GroupedPermissionsDTO getAllPermissionsGrouped() {
        log.debug("Fetching all permissions grouped by module");

        List<Permission> allPermissions = permissionRepository.findAll();

        // Agrupar permisos por módulo, excluyendo permisos internos del sistema (módulo "System")
        Map<String, List<PermissionDTO>> groupedMap = allPermissions.stream()
                .filter(p -> !"System".equals(p.getModule()))
                .map(permissionMapper::toDto)
                .collect(Collectors.groupingBy(PermissionDTO::module));

        return new GroupedPermissionsDTO(groupedMap);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return roleRepository.existsByIdAndDeletedFalse(id);
    }

    // ==================== Métodos privados de validación ====================

    /**
     * Validates data for a new role.
     */
    private void validateNewRole(RoleRequestDTO requestDTO) {
        // Validate name
        if (requestDTO.name() == null || requestDTO.name().trim().isEmpty()) {
            throw new RoleNotValidException(messageSourceHelper.getMessage("role.name.empty"));
        }
        if (requestDTO.name().length() > 100) {
            throw new RoleNotValidException(messageSourceHelper.getMessage("role.name.tooLong"));
        }

        // Validate that the name doesn't exist
        if (roleRepository.existsByNameAndDeletedFalse(requestDTO.name())) {
            throw new RoleAlreadyExistsException(requestDTO.name());
        }

        // Validate description length if provided
        if (requestDTO.description() != null && requestDTO.description().length() > 500) {
            throw new RoleNotValidException(messageSourceHelper.getMessage("role.description.tooLong"));
        }

        // Validate that permissions are provided
        if (requestDTO.permissionIds() == null || requestDTO.permissionIds().isEmpty()) {
            throw new RoleNotValidException(messageSourceHelper.getMessage("role.permissions.required"));
        }

        // Validate that permissions exist
        validatePermissionIds(requestDTO.permissionIds());
    }

    /**
     * Validates role update data.
     */
    private void validateRoleUpdate(Long id, RoleRequestDTO requestDTO) {
        // Validate name if provided
        if (requestDTO.name() != null) {
            if (requestDTO.name().trim().isEmpty()) {
                throw new RoleNotValidException(messageSourceHelper.getMessage("role.name.empty"));
            }
            if (requestDTO.name().length() > 100) {
                throw new RoleNotValidException(messageSourceHelper.getMessage("role.name.tooLong"));
            }

            // Verify that the name is not in use by another role
            roleRepository.findByName(requestDTO.name())
                    .ifPresent(existing -> {
                        if (!existing.getId().equals(id)) {
                            if (existing.getDeleted()) {
                                throw new RoleAlreadyExistsException(
                                        messageSourceHelper.getMessage("role.name.deletedRole", requestDTO.name(), existing.getId()));
                            } else {
                                throw new RoleAlreadyExistsException(requestDTO.name());
                            }
                        }
                    });
        }

        // Validate description length if provided
        if (requestDTO.description() != null && requestDTO.description().length() > 500) {
            throw new RoleNotValidException(messageSourceHelper.getMessage("role.description.tooLong"));
        }

        // Validate permissions if provided
        if (requestDTO.permissionIds() != null) {
            if (requestDTO.permissionIds().isEmpty()) {
                throw new RoleNotValidException(messageSourceHelper.getMessage("role.permissions.required"));
            }
            validatePermissionIds(requestDTO.permissionIds());
        }
    }

    /**
     * Validates that all permission IDs exist in the database.
     */
    private void validatePermissionIds(Set<Long> permissionIds) {
        for (Long permissionId : permissionIds) {
            if (!permissionRepository.existsById(permissionId)) {
                throw new PermissionNotFoundException(permissionId);
            }
        }
    }

    /**
     * Validates and retrieves permissions by their IDs.
     */
    private Set<Permission> validateAndGetPermissions(Set<Long> permissionIds) {
        Set<Permission> permissions = new HashSet<>();

        for (Long permissionId : permissionIds) {
            Permission permission = permissionRepository.findById(permissionId)
                    .orElseThrow(() -> new PermissionNotFoundException(permissionId));
            permissions.add(permission);
        }

        return permissions;
    }

    /**
     * Validates that a role can be deleted.
     */
    private void validateRoleDeletion(Role role) {
        // Additional validations can be added here, for example:
        // - Don't allow deletion of system roles (ROOT, ADMIN)
        // - Verify that there are no active users with this role

        // Protect system roles
        if ("ROOT".equalsIgnoreCase(role.getName()) ||
            "ADMIN".equalsIgnoreCase(role.getName()) ||
            "ROLE_ROOT".equalsIgnoreCase(role.getName()) ||
            "ROLE_ADMIN".equalsIgnoreCase(role.getName())) {
            throw new RoleNotValidException(messageSourceHelper.getMessage("role.cannotDeleteSystem", role.getName()));
        }
    }

    /**
     * Creates a new role in the database.
     */
    private RoleResponseDTO createNewRole(RoleRequestDTO requestDTO) {
        Role newRole = roleMapper.toEntity(requestDTO);

        // Ensure defaults are set in the service (business rule):
        // - newly created roles are active by default
        // - newly created roles are not deleted
        newRole.setActive(true);
        newRole.setDeleted(false);

        // Ensure name is trimmed
        newRole.setName(requestDTO.name().trim());

        // Ensure description not null
        newRole.setDescription(requestDTO.description() != null ? requestDTO.description().trim() : null);

        // Assign permissions (already validated)
        Set<Permission> permissions = validateAndGetPermissions(requestDTO.permissionIds());
        newRole.setPermissions(permissions);

        Role savedRole = roleRepository.save(newRole);
        log.info("New role created successfully: {} with {} permissions",
                savedRole.getName(), savedRole.getPermissions().size());

        return roleMapper.toResponseDto(savedRole);
    }

    /**
     * Reactivates a deleted role.
     */
    private RoleResponseDTO reactivateRole(Role deletedRole, RoleRequestDTO requestDTO) {
        log.info("Reactivating deleted role: {}", deletedRole.getName());

        // Ensure provided data is valid for reactivation
        if (requestDTO.permissionIds() == null || requestDTO.permissionIds().isEmpty()) {
            throw new RoleNotValidException(messageSourceHelper.getMessage("role.permissions.atLeastOne"));
        }
        if (requestDTO.description() != null && requestDTO.description().length() > 500) {
            throw new RoleNotValidException(messageSourceHelper.getMessage("role.description.tooLong"));
        }

        deletedRole.setDeleted(false);
        // When reactivating, enforce active = true by default in the service
        deletedRole.setActive(true);
        deletedRole.setDescription(requestDTO.description() != null ? requestDTO.description().trim() : deletedRole.getDescription());

        // Update permissions
        Set<Permission> permissions = validateAndGetPermissions(requestDTO.permissionIds());
        deletedRole.setPermissions(permissions);

        Role reactivatedRole = roleRepository.save(deletedRole);
        log.info("Role reactivated successfully: {}", reactivatedRole.getName());

        return roleMapper.toResponseDto(reactivatedRole);
    }

    private void handleDataIntegrityViolation(DataIntegrityViolationException e, RoleRequestDTO requestDTO) {
        String errorMessage = e.getMessage().toLowerCase();

        // Detectar violación de constraint de name
        if (errorMessage.contains("name") || errorMessage.contains("uk_") && errorMessage.contains("name")) {
            throw new RoleDataConflictException(
                messageSourceHelper.getMessage("role.update.conflict.name", requestDTO.name()),
                e
            );
        }

        // Si es una violación de integridad pero no podemos determinar el campo específico
        throw new RoleDataConflictException(
            messageSourceHelper.getMessage("role.update.conflict.generic"),
            e
        );
    }
}
