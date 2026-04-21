package PSG.backEnd.config.security;

import PSG.backEnd.service.security.AuthRateLimiter;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Servlet filter that enforces rate limits on authentication endpoints.
 *
 * <p>Returns {@code 429 Too Many Requests} with a {@code Retry-After} header
 * when the IP-based bucket is exhausted. Does not consult the per-username
 * lockout list here — that's enforced by {@link PSG.backEnd.service.implementation.AuthService}
 * so the same uniform error message can be returned for both "wrong password"
 * and "account locked".
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private final AuthRateLimiter rateLimiter;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        // Apply only to credential-bearing auth endpoints.
        return !("/api/v1/auth/login".equals(path) || "/api/v1/auth/refresh".equals(path));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String clientIp = resolveClientIp(request);
        ConsumptionProbe probe = rateLimiter.bucketForIp(clientIp).tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1;
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"rate_limited\",\"message\":\"Demasiados intentos. Intenta nuevamente en "
                            + retryAfterSeconds + " segundos.\"}"
            );
            log.warn("Auth rate limit hit for IP {} on {}", clientIp, request.getServletPath());
            return;
        }

        filterChain.doFilter(request, response);
    }

    /** Resolves the client IP, honoring X-Forwarded-For when present (single hop). */
    private static String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            String first = comma < 0 ? xff : xff.substring(0, comma);
            return first.trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) return real.trim();
        return request.getRemoteAddr();
    }
}
