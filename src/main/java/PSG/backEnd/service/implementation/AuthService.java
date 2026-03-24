package PSG.backEnd.service.implementation;

import PSG.backEnd.exception.auth.InvalidCredentialsException;
import PSG.backEnd.exception.auth.InvalidTokenException;
import PSG.backEnd.model.dto.auth.AuthResponseDTO;
import PSG.backEnd.model.dto.auth.LoginRequestDTO;
import PSG.backEnd.model.dto.auth.RefreshTokenRequestDTO;
import PSG.backEnd.model.dto.auth.UserProfileDTO;
import PSG.backEnd.model.dto.security.RoleResponseDTO;
import PSG.backEnd.model.dto.security.UserLocationDTO;
import PSG.backEnd.model.entity.security.User;
import PSG.backEnd.model.mapper.RoleMapper;
import PSG.backEnd.model.mapper.UserLocationMapper;
import PSG.backEnd.repository.UserRepository;
import PSG.backEnd.service.port.IAuthService;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of authentication service.
 * Handles login, token refresh, and logout operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements IAuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final RoleMapper roleMapper;
    private final UserLocationMapper userLocationMapper;
    private final MessageSourceHelper messageSourceHelper;

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO loginRequest) {
        log.info("Login attempt for user: {}", loginRequest.credentials().username());

        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.credentials().username(),
                            loginRequest.credentials().password()
                    )
            );

            // Load user details
            User user = userRepository.findByCredentialsUsernameAndDeletedFalse(loginRequest.credentials().username())
                    .orElseThrow(() -> new InvalidCredentialsException(messageSourceHelper.getMessage("auth.invalidCredentials")));

            // Verify user is enabled
            if (!user.isEnabled()) {
                throw new InvalidCredentialsException(messageSourceHelper.getMessage("auth.accountDisabled"));
            }

            // Generate tokens
            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = jwtService.generateRefreshToken(user);

            log.info("User logged in successfully: {}", loginRequest.credentials().username());

            return buildAuthResponse(user, accessToken, refreshToken);

        } catch (BadCredentialsException e) {
            log.error("Invalid credentials for user: {}", loginRequest.credentials().username());
            throw new InvalidCredentialsException(messageSourceHelper.getMessage("auth.invalidCredentials"));
        }
    }


    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO refreshToken(RefreshTokenRequestDTO refreshTokenRequest) {
        log.info("Token refresh attempt");

        try {
            // Extract username from refresh token
            String username = jwtService.extractUsername(refreshTokenRequest.refreshToken());

            if (username == null) {
                throw new InvalidTokenException(messageSourceHelper.getMessage("auth.invalidRefreshToken"));
            }

            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Validate refresh token
            if (!jwtService.isTokenValid(refreshTokenRequest.refreshToken(), userDetails)) {
                throw new InvalidTokenException(messageSourceHelper.getMessage("auth.refreshTokenExpired"));
            }

            // Load full user entity
            User user = userRepository.findByCredentialsUsernameAndDeletedFalse(username)
                    .orElseThrow(() -> new InvalidTokenException(messageSourceHelper.getMessage("auth.userNotFound")));

            // Generate new tokens
            String newAccessToken = jwtService.generateAccessToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user);

            log.info("Token refreshed successfully for user: {}", username);

            return buildAuthResponse(user, newAccessToken, newRefreshToken);

        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
            throw new InvalidTokenException(messageSourceHelper.getMessage("auth.invalidOrExpiredRefreshToken"));
        }
    }

    @Override
    public void logout(String username) {
        log.info("Logout request for user: {}", username);

        // Note: With JWT, tokens cannot be invalidated server-side unless we implement a blacklist
        // For now, we just log the logout action
        // In production, you might want to:
        // 1. Store tokens in Redis with TTL
        // 2. Maintain a blacklist of invalidated tokens
        // 3. Use short-lived access tokens and rely on refresh token rotation

        log.info("User logged out: {}", username);
    }

    /**
     * Builds the authentication response DTO.
     */
    private AuthResponseDTO buildAuthResponse(User user, String accessToken, String refreshToken) {
        Set<RoleResponseDTO> roleDTOs = buildRoleDTOs(user);

        return new AuthResponseDTO(
                user.getId(),
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessTokenExpiration(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                roleDTOs,
                user.getLocation() != null ? userLocationMapper.toDto(user.getLocation()) : null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(String username) {
        User user = userRepository.findByCredentialsUsernameAndDeletedFalse(username)
                .orElseThrow(() -> new InvalidTokenException(
                        messageSourceHelper.getMessage("auth.userNotFound")));

        return new UserProfileDTO(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                buildRoleDTOs(user),
                user.getLocation() != null ? userLocationMapper.toDto(user.getLocation()) : null
        );
    }

    private Set<RoleResponseDTO> buildRoleDTOs(User user) {
        return user.getRoles().stream()
                .filter(role -> role.getActive() && !role.getDeleted())
                .map(roleMapper::toResponseDto)
                .collect(Collectors.toSet());
    }
}

