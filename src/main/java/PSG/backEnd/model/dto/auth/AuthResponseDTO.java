package PSG.backEnd.model.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for authentication response containing JWT tokens.
 */
@Schema(description = "Authentication response with access and refresh tokens")
public record AuthResponseDTO(
        
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
        
        @Schema(description = "User full name", 
                example = "Juan García")
        String fullName,
        
        @Schema(description = "User email", 
                example = "juan.garcia@esea.com.ar")
        String email
) {}

