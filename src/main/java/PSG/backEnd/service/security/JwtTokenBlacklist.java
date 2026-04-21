package PSG.backEnd.service.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory JWT blacklist (revocation list).
 *
 * <p>JWTs are stateless, but logout, password change, and refresh-token rotation
 * require the ability to invalidate a token before its natural expiration. This
 * service keeps a {@code jti -> expiresAtEpochMs} map so the
 * {@link PSG.backEnd.config.security.JwtAuthenticationFilter} can reject revoked
 * tokens.
 *
 * <p>Entries are purged once their original expiration has passed (a scheduled
 * job runs every 5 min) so memory usage stays bounded.
 *
 * <p>For multi-replica deployments, swap this with a Redis-backed implementation
 * — the public API would be unchanged.
 */
@Service
@Slf4j
public class JwtTokenBlacklist {

    private final Map<String, Long> revoked = new ConcurrentHashMap<>();

    /** Marks the given JWT id as revoked until its natural expiration. */
    public void revoke(String jti, long expiresAtEpochMs) {
        if (jti == null || jti.isBlank()) return;
        revoked.put(jti, expiresAtEpochMs);
        log.debug("Token revoked: jti={}, expiresAt={}", jti, expiresAtEpochMs);
    }

    /** @return true if the JWT id has been revoked and is still within its lifetime. */
    public boolean isRevoked(String jti) {
        if (jti == null) return false;
        Long expiry = revoked.get(jti);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            revoked.remove(jti);
            return false;
        }
        return true;
    }

    /** Periodic sweep of expired entries (every 5 minutes). */
    @Scheduled(fixedDelay = 5 * 60 * 1000L)
    public void purgeExpired() {
        long now = System.currentTimeMillis();
        int before = revoked.size();
        revoked.entrySet().removeIf(e -> e.getValue() < now);
        int removed = before - revoked.size();
        if (removed > 0) {
            log.debug("Purged {} expired JWT blacklist entries (remaining={})", removed, revoked.size());
        }
    }
}
