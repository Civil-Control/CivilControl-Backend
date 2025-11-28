package PSG.backEnd.model.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for Permission.
 * Used to send permission information to the frontend.
 */
@Schema(description = "Represents an individual system permission")
public record PermissionDTO(

        @Schema(description = "Unique permission ID", example = "1")
        Long id,

        @Schema(description = "Technical permission name (used in code)",
                example = "PROJECT_READ",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String name,

        @Schema(description = "Module to which the permission belongs (for UI grouping)",
                example = "Projects",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String module,

        @Schema(description = "Human-readable permission description",
                example = "Allows viewing projects and construction sites")
        String description
) {}

