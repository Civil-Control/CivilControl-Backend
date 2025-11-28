package PSG.backEnd.model.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

/**
 * Response DTO for Role.
 * Includes all role information including its permissions.
 */
@Schema(description = "Represents a system role with its assigned permissions")
public record RoleResponseDTO(

        @Schema(description = "Unique role ID", example = "1")
        Long id,

        @Schema(description = "Role name",
                example = "Senior Architect",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "Role description",
                example = "Experienced architect who supervises projects")
        String description,

        @Schema(description = "List of permissions assigned to the role")
        Set<PermissionDTO> permissions,

        @Schema(description = "Indicates if the role is active", example = "true")
        Boolean active
) {}

