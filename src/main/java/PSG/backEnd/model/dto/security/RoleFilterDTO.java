package PSG.backEnd.model.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO to filter roles in GET queries.
 */
@Schema(description = "Optional filters for role search")
public record RoleFilterDTO(
        
        @Schema(description = "Filter by role name (partial search, case-insensitive)",
                example = "Architect")
        String name,
        
        @Schema(description = "Filter by active/inactive status",
                example = "true")
        Boolean active
) {}

