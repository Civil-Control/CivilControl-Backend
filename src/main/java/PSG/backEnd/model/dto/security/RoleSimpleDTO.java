package PSG.backEnd.model.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Simple Role DTO to include in User responses (avoids circular references).
 */
@Schema(description = "Simplified role information (without permissions)")
public record RoleSimpleDTO(
        
        @Schema(description = "Unique role ID", example = "1")
        Long id,
        
        @Schema(description = "Role name", example = "Senior Architect")
        String name,
        
        @Schema(description = "Role description",
                example = "Experienced architect who supervises projects")
        String description,
        
        @Schema(description = "Indicates if the role is active", example = "true")
        Boolean active,

        @Schema(description = "Indicates if this is a system-defined role (immutable)", example = "false")
        Boolean systemRole
) {}

