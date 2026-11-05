package PSG.backEnd.model.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for updating own user profile.
 * Includes updated user data and new authentication tokens to maintain session.
 */
@Schema(description = "Response for own profile update. Includes updated user data and new tokens for auto-login.")
public record UserUpdateOwnProfileResponseDTO(
    @Schema(description = "Updated user information")
    UserResponseDTO user,

    @Schema(description = "New JWT access token (valid for 1 hour)")
    String accessToken,

    @Schema(description = "New JWT refresh token (valid for 7 days)")
    String refreshToken
) {}
