package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.security.GroupedPermissionsDTO;
import PSG.backEnd.model.dto.security.RoleFilterDTO;
import PSG.backEnd.model.dto.security.RoleRequestDTO;
import PSG.backEnd.model.dto.security.RoleResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for role management.
 */
public interface IRoleService {

    /**
     * Creates a new role in the system.
     * @param requestDTO DTO with the role data to create
     * @return DTO of the created role
     */
    RoleResponseDTO createRole(RoleRequestDTO requestDTO);

    /**
     * Gets all roles with filters and pagination.
     * @param filterDTO Optional filters
     * @param pageable Pagination configuration
     * @return Page with the found roles
     */
    Page<RoleResponseDTO> getAllRoles(RoleFilterDTO filterDTO, Pageable pageable);

    /**
     * Gets a role by its ID.
     * @param id Role ID
     * @return DTO of the found role
     */
    RoleResponseDTO getRoleById(Long id);

    /**
     * Updates an existing role.
     * @param id ID of the role to update
     * @param requestDTO DTO with the updated data
     * @return DTO of the updated role
     */
    RoleResponseDTO updateRole(Long id, RoleRequestDTO requestDTO);

    /**
     * Deletes (soft delete) a role from the system.
     * @param id ID of the role to delete
     */
    void deleteRole(Long id);

    /**
     * Gets all available permissions grouped by module.
     * @return DTO with the grouped permissions
     */
    GroupedPermissionsDTO getAllPermissionsGrouped();

    /**
     * Checks if a role exists with the given ID.
     * @param id Role ID
     * @return true if exists, false otherwise
     */
    boolean existsById(Long id);
}

