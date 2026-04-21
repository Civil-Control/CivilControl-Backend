package PSG.backEnd.service.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory rate limiter and account-lockout tracker for authentication endpoints.
 *
 * <p>Two complementary controls:
 * <ul>
 *   <li><b>IP-based token bucket</b> on every login/refresh attempt. Default:
 *       {@code 5 requests / minute} per remote address. Mitigates fast brute-force.</li>
 *   <li><b>Per-username failed-attempt counter</b> with temporary lockout. Default:
 *       {@code 10 consecutive failures → 15 min lockout}. Mitigates targeted accounts.</li>
 * </ul>
 *
 * <p>State is held in-process. Acceptable for a single-instance deployment;
 * for multi-replica deployments switch to a Bucket4j Redis backend (the API
 * stays identical).
 */
@Service
@Slf4j
public class AuthRateLimiter {

    @Value("${app.security.ratelimit.ip.requests:5}")
    private int ipRequestsPerMinute;

    @Value("${app.security.ratelimit.lockout.threshold:10}")
    private int lockoutThreshold;

    @Value("${app.security.ratelimit.lockout.duration-minutes:15}")
    private int lockoutDurationMinutes;

    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();
    private final Map<String, FailedAttempts> failedByUsername = new ConcurrentHashMap<>();

    /** @return the bucket for the given key, creating it lazily. */
    public Bucket bucketForIp(String ip) {
        return ipBuckets.computeIfAbsent(ip, k -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(ipRequestsPerMinute)
                        .refillIntervally(ipRequestsPerMinute, Duration.ofMinutes(1))
                        .build())
                .build());
    }

    /** Returns true if the username is currently locked out. */
    public boolean isLockedOut(String username) {
        if (username == null) return false;
        FailedAttempts fa = failedByUsername.get(username.toLowerCase());
        if (fa == null) return false;
        if (fa.lockedUntil != null && Instant.now().isBefore(fa.lockedUntil)) {
            return true;
        }
        // expired lockout — reset
        if (fa.lockedUntil != null && !Instant.now().isBefore(fa.lockedUntil)) {
            failedByUsername.remove(username.toLowerCase());
        }
        return false;
    }

    /** Returns remaining seconds until the lockout expires (0 if not locked). */
    public long lockoutRemainingSeconds(String username) {
        if (username == null) return 0;
        FailedAttempts fa = failedByUsername.get(username.toLowerCase());
        if (fa == null || fa.lockedUntil == null) return 0;
        long secs = Duration.between(Instant.now(), fa.lockedUntil).getSeconds();
        return Math.max(0, secs);
    }

    /** Records a failed attempt and engages lockout once the threshold is hit. */
    public void recordFailure(String username) {
        if (username == null || username.isBlank()) return;
        String key = username.toLowerCase();
        FailedAttempts fa = failedByUsername.computeIfAbsent(key, k -> new FailedAttempts());
        synchronized (fa) {
            fa.count++;
            if (fa.count >= lockoutThreshold) {
                fa.lockedUntil = Instant.now().plus(Duration.ofMinutes(lockoutDurationMinutes));
                log.warn("Account locked out for {} minutes after {} failed attempts: {}",
                        lockoutDurationMinutes, fa.count, key);
            }
        }
    }

    /** Resets failed-attempt state on a successful authentication. */
    public void recordSuccess(String username) {
        if (username == null) return;
        failedByUsername.remove(username.toLowerCase());
    }

    private static final class FailedAttempts {
        int count;
        Instant lockedUntil;
    }
}
