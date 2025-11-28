package PSG.backEnd.model.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO for refresh token request.
 */
@Schema(description = "Request to refresh access token")
public record RefreshTokenRequestDTO(

        @Schema(description = "Refresh token",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {}

