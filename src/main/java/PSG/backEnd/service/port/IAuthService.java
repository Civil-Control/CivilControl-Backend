package PSG.backEnd.service.port;

import PSG.backEnd.model.dto.auth.AuthResponseDTO;
import PSG.backEnd.model.dto.auth.LoginRequestDTO;
import PSG.backEnd.model.dto.auth.RefreshTokenRequestDTO;
import PSG.backEnd.model.dto.security.UserRequestDTO;

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
     * Registers a new user in the system.
     * @param userRequest User data
     * @return Authentication response with tokens
     */
    AuthResponseDTO register(UserRequestDTO userRequest);

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
}

