package PSG.backEnd.model.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/**
 * DTO for bulk role reorder operations.
 * Contains the complete ordered list of role positions.
 */
@Schema(description = "DTO to reorder roles by specifying new positions")
public record RoleReorderDTO(

        @Schema(description = "Ordered list of role-position assignments")
        @NotEmpty(message = "{validation.required}")
        @Valid
        List<RolePositionEntry> entries
) {
    @Schema(description = "A single role-position assignment")
    public record RolePositionEntry(

            @Schema(description = "Role ID", example = "5")
            @NotNull(message = "{validation.required}")
            Long roleId,

            @Schema(description = "New position (1 = highest authority)", example = "3")
            @NotNull(message = "{validation.required}")
            @Positive(message = "{validation.positive}")
            Integer position
    ) {}
}
