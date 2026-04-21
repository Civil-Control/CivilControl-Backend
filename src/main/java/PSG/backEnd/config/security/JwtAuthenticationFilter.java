package PSG.backEnd.config.security;

import PSG.backEnd.service.implementation.JwtService;
import PSG.backEnd.service.security.JwtTokenBlacklist;
import PSG.backEnd.service.util.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter.
 * Intercepts every request and validates the JWT token if present.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final JwtTokenBlacklist tokenBlacklist;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // Check if Authorization header exists and starts with Bearer
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract JWT token
        jwt = authHeader.substring(7);

        try {
            // Extract username and tenantId from JWT
            username = jwtService.extractUsername(jwt);
            Long tenantId = jwtService.extractTenantId(jwt);

            // Reject revoked tokens (logged-out users, rotated refresh tokens, etc.)
            String jti = jwtService.extractJti(jwt);
            if (jti != null && tokenBlacklist.isRevoked(jti)) {
                log.warn("Rejected revoked JWT (jti={}, user={})", jti, username);
                filterChain.doFilter(request, response);
                return;
            }

            // Set tenant context
            TenantContext.setCurrentTenant(tenantId);

            log.debug("Tenant context set to: {}", tenantId);

            // If username is present and user is not already authenticated
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Load user details
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                // Validate token
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // Create authentication token
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    log.debug("User '{}' authenticated successfully for tenant '{}'", username, tenantId);
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        } finally {
            // Always continue the filter chain
            try {
                filterChain.doFilter(request, response);
            } finally {
                // Clear tenant context after request processing to prevent memory leaks
                TenantContext.clear();
            }
        }
    }
}

