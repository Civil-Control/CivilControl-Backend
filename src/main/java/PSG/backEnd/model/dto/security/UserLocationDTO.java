package PSG.backEnd.model.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * DTO for a user's default geographic location.
 * Used to pre-fill address fields in forms.
 */
@Schema(description = "User's default location used for address autofill in forms")
public record UserLocationDTO(

    @Schema(description = "City or municipality", example = "Córdoba")
    @Size(min = 2, max = 100, message = "{validation.size}")
    String city,

    @Schema(description = "State or province", example = "Córdoba")
    @Size(min = 2, max = 100, message = "{validation.size}")
    String state,

    @Schema(description = "Country", example = "Argentina")
    @Size(min = 2, max = 100, message = "{validation.size}")
    String country,

    @Schema(description = "Postal / ZIP code", example = "5000")
    @Pattern(regexp = "^[A-Z0-9]{4,20}$", message = "{validation.pattern}")
    String zipCode
) {}
