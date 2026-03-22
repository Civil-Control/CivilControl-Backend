package PSG.backEnd.controller;

import PSG.backEnd.model.dto.security.GroupedPermissionsDTO;
import PSG.backEnd.model.dto.security.RoleFilterDTO;
import PSG.backEnd.model.dto.security.RoleReorderDTO;
import PSG.backEnd.model.dto.security.RoleRequestDTO;
import PSG.backEnd.model.dto.security.RoleResponseDTO;
import PSG.backEnd.model.dto.security.UserResponseDTO;
import PSG.backEnd.service.port.IRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import PSG.backEnd.model.constants.AppPermissions;
import java.net.URI;
import java.util.List;

/**
 * REST controller for role management.
 * Allows CRUD operations on roles and queries of available permissions.
 *
 * IMPORTANT: This controller should be protected with @PreAuthorize in production.
 * Example: @PreAuthorize("hasAuthority('ROLE_MANAGEMENT')")
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Role Management",
     description = "API for managing roles and permissions. " +
                   "Allows creating dynamic roles by grouping permissions. " +
                   "Users with ROLE_ROOT or ROLE_ADMIN have God Mode (bypass all permission checks).")
public class RoleController {

    private final IRoleService roleService;

    @PreAuthorize("hasAuthority('" + AppPermissions.ROLE_MANAGEMENT + "')")
    @PostMapping
    @Operation(summary = "Create a new role",
            description = "Creates a new role in the system by grouping selected permissions. " +
                         "The role name must be unique. At least one permission is required.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Role successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "One or more permission IDs not found"),
            @ApiResponse(responseCode = "409", description = "Role already exists with the same name")
    })
    public ResponseEntity<RoleResponseDTO> createRole(
            @Valid @RequestBody RoleRequestDTO requestDTO) {
        RoleResponseDTO created = roleService.createRole(requestDTO);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.ROLE_MANAGEMENT + "')")
    @GetMapping
    @Operation(summary = "Get all roles with filters",
            description = "Retrieves a paginated list of roles with optional filtering by name and active status. " +
                         "Supports sorting by any field.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Roles retrieved successfully")
    })
    public ResponseEntity<Page<RoleResponseDTO>> getAllRoles(
            @Parameter(description = "Filter by role name (partial match, case-insensitive)", example = "Arquitecto")
            @RequestParam(required = false) String name,

            @Parameter(description = "Filter by active status", example = "true")
            @RequestParam(required = false) Boolean active,

            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Page size", example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Sort field", example = "name")
            @RequestParam(defaultValue = "name") String sortBy,

            @Parameter(description = "Sort direction (ASC or DESC)", example = "ASC")
            @RequestParam(defaultValue = "ASC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("DESC")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        RoleFilterDTO filterDTO = new RoleFilterDTO(name, active);

        Page<RoleResponseDTO> roles = roleService.getAllRoles(filterDTO, pageable);
        return ResponseEntity.ok(roles);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.ROLE_MANAGEMENT + "')")
    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID",
            description = "Retrieves detailed information about a specific role including all its assigned permissions.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role found"),
            @ApiResponse(responseCode = "404", description = "Role not found")
    })
    public ResponseEntity<RoleResponseDTO> getRoleById(
            @Parameter(description = "Role unique identifier", required = true, example = "1")
            @PathVariable Long id) {
        RoleResponseDTO role = roleService.getRoleById(id);
        return ResponseEntity.ok(role);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.ROLE_MANAGEMENT + "')")
    @PatchMapping("/{id}")
    @Operation(summary = "Update role",
            description = "Updates an existing role. Only provided fields will be updated. " +
                         "Validates that the role exists and isn't marked as deleted. " +
                         "Cannot update system roles (ROOT, ADMIN).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data or validation error"),
            @ApiResponse(responseCode = "404", description = "Role or permission not found"),
            @ApiResponse(responseCode = "409", description = "Update would create a duplicate role name")
    })
    public ResponseEntity<RoleResponseDTO> updateRole(
            @Parameter(description = "Role unique identifier", required = true, example = "1")
            @PathVariable Long id,

            @RequestBody RoleRequestDTO requestDTO) {
        RoleResponseDTO updated = roleService.updateRole(id, requestDTO);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.ROLE_MANAGEMENT + "')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete role",
            description = "Performs a soft delete of a role from the system. " +
                         "If the role has users assigned, a newRoleId must be provided to reassign them. " +
                         "Cannot delete system roles (ROOT, ADMIN).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Role successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Cannot delete system role or role has users without reassignment"),
            @ApiResponse(responseCode = "404", description = "Role not found")
    })
    public ResponseEntity<Void> deleteRole(
            @Parameter(description = "Role unique identifier", required = true, example = "1")
            @PathVariable Long id,
            @Parameter(description = "ID of the role to reassign affected users to")
            @RequestParam(required = false) Long newRoleId) {
        roleService.deleteRole(id, newRoleId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.ROLE_MANAGEMENT + "')")
    @GetMapping("/{id}/users")
    @Operation(summary = "Get users assigned to a role",
            description = "Retrieves all active users that have the specified role assigned.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Role not found")
    })
    public ResponseEntity<List<UserResponseDTO>> getUsersByRole(
            @Parameter(description = "Role unique identifier", required = true, example = "1")
            @PathVariable Long id) {
        List<UserResponseDTO> users = roleService.getUsersByRoleId(id);
        return ResponseEntity.ok(users);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.ROLE_MANAGEMENT + "')")
    @GetMapping("/permissions")
    @Operation(summary = "Get all available permissions",
            description = "Retrieves all permissions available in the system, grouped by module. " +
                         "This endpoint is useful for building the UI when creating or editing roles. " +
                         "Permissions are defined in code (AppPermissions) and synced automatically with the database.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Permissions retrieved successfully")
    })
    public ResponseEntity<GroupedPermissionsDTO> getAllPermissions() {
        GroupedPermissionsDTO permissions = roleService.getAllPermissionsGrouped();
        return ResponseEntity.ok(permissions);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.ROLE_MANAGEMENT + "')")
    @PutMapping("/reorder")
    @Operation(summary = "Reorder roles",
            description = "Applies new hierarchical positions to roles. " +
                         "System roles (OWNER, ADMIN, LECTOR) must keep their original positions. " +
                         "Users can only reorder roles below their own hierarchical position.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Roles reordered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid positions or hierarchy violation")
    })
    public ResponseEntity<List<RoleResponseDTO>> reorderRoles(
            @Valid @RequestBody RoleReorderDTO reorderDTO) {
        roleService.reorderRoles(reorderDTO);
        List<RoleResponseDTO> ordered = roleService.getAllRolesOrdered();
        return ResponseEntity.ok(ordered);
    }

    @PreAuthorize("hasAuthority('" + AppPermissions.ROLE_MANAGEMENT + "')")
    @GetMapping("/ordered")
    @Operation(summary = "Get all roles ordered by hierarchy",
            description = "Retrieves all non-deleted roles ordered by hierarchical position (ascending). " +
                         "Position 1 = highest authority (OWNER).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Ordered roles retrieved successfully")
    })
    public ResponseEntity<List<RoleResponseDTO>> getAllRolesOrdered() {
        List<RoleResponseDTO> roles = roleService.getAllRolesOrdered();
        return ResponseEntity.ok(roles);
    }
}
