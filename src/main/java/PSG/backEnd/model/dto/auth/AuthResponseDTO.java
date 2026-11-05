package PSG.backEnd.model.dto.auth;

import PSG.backEnd.model.dto.security.RoleResponseDTO;
import PSG.backEnd.model.dto.security.UserLocationDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

/**
 * DTO for authentication response containing JWT tokens.
 */
@Schema(description = "Authentication response with access and refresh tokens")
public record AuthResponseDTO(
        
        @Schema(description = "User ID",
                example = "1")
        Long userId,

        @Schema(description = "JWT access token",
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,
        
        @Schema(description = "JWT refresh token", 
                example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String refreshToken,
        
        @Schema(description = "Token type", 
                example = "Bearer")
        String tokenType,
        
        @Schema(description = "Access token expiration time in milliseconds", 
                example = "3600000")
        Long expiresIn,
        
        @Schema(description = "Username", 
                example = "jgarcia")
        String username,
        
        @Schema(description = "User first name",
                example = "Juan")
        String firstName,

        @Schema(description = "User last name",
                example = "García")
        String lastName,

        @Schema(description = "User email", 
                example = "juan.garcia@esea.com.ar")
        String email,

        @Schema(description = "User roles with their permissions")
        Set<RoleResponseDTO> roles,

        @Schema(description = "User's default location for address autofill")
        UserLocationDTO location,

        @Schema(description = "Whether the user's email channel has been verified")
        Boolean emailVerified
) {}

