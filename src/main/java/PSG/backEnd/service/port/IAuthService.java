package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.auth.AuthResponseDTO;
import PSG.backEnd.model.dto.auth.LoginRequestDTO;
import PSG.backEnd.model.dto.auth.RefreshTokenRequestDTO;
import PSG.backEnd.model.dto.auth.UserProfileDTO;

/**
 * Service interface for authentication operations.
 */
public interface IAuthService {

    /**
     * Authenticates a user and returns JWT tokens.
     * @param loginRequest Login credentials
     * @return Authentication response with tokens
     */
    AuthResponseDTO login(LoginRequestDTO loginRequest);


    /**
     * Refreshes the access token using a refresh token.
     * @param refreshTokenRequest Refresh token
     * @return New authentication response with tokens
     */
    AuthResponseDTO refreshToken(RefreshTokenRequestDTO refreshTokenRequest);

    /**
     * Logs out a user (invalidates tokens - to be implemented with token blacklist if needed).
     * @param username Username to logout
     */
    void logout(String username);

    /**
     * Token-aware logout: also revokes the supplied access/refresh tokens via the
     * blacklist so they cannot be reused before their natural expiration.
     */
    void logout(String username, String bearerToken, String refreshToken);

    /**
     * Returns the current user's profile including up-to-date roles and permissions.
     * @param username Authenticated username (from JWT)
     * @return User profile DTO
     */
    UserProfileDTO getUserProfile(String username);
}

