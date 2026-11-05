package PSG.backEnd.model.dto.auth;

import PSG.backEnd.model.dto.security.RoleResponseDTO;
import PSG.backEnd.model.dto.security.UserLocationDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

/**
 * DTO returned by GET /auth/me with the authenticated user's profile and current roles/permissions.
 * Used by the frontend to keep the cached user object up-to-date without requiring a full re-login.
 */
@Schema(description = "Current user profile including up-to-date roles and permissions")
public record UserProfileDTO(

        @Schema(description = "User ID", example = "1")
        Long userId,

        @Schema(description = "Username", example = "jgarcia")
        String username,

        @Schema(description = "User first name", example = "Juan")
        String firstName,

        @Schema(description = "User last name", example = "García")
        String lastName,

        @Schema(description = "User email", example = "juan.garcia@example.com")
        String email,

        @Schema(description = "User roles with their current permissions")
        Set<RoleResponseDTO> roles,

        @Schema(description = "User's default location")
        UserLocationDTO location,

        @Schema(description = "Whether the user's email channel has been verified")
        Boolean emailVerified
) {}
