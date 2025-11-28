package PSG.backEnd.model.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO to filter users in GET queries.
 */
@Schema(description = "Optional filters for user search")
public record UserFilterDTO(

        @Schema(description = "Filter by username (partial search, case-insensitive)",
                example = "jgarcia")
        String username,

        @Schema(description = "Filter by email (partial search, case-insensitive)",
                example = "garcia@esea.com.ar")
        String email,

        @Schema(description = "Filter by first name (partial search, case-insensitive)",
                example = "Juan")
        String firstName,

        @Schema(description = "Filter by last name (partial search, case-insensitive)",
                example = "García")
        String lastName,

        @Schema(description = "Filter by enabled/disabled status",
                example = "true")
        Boolean enabled
) {}

