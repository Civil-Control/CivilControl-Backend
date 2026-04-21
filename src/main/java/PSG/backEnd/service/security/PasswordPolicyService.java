package PSG.backEnd.service.security;

import PSG.backEnd.exception.user.UserNotValidException;
import PSG.backEnd.service.util.MessageSourceHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Centralized password policy enforcement.
 *
 * <p>Defense-in-depth strategy:
 * <ol>
 *   <li>Strong structural rules (length, character classes).</li>
 *   <li>Context-aware checks (rejects passwords that contain the username/email).</li>
 *   <li>Embedded denylist of the most common breached passwords.</li>
 *   <li>Online check against HaveIBeenPwned (k-anonymity API: only the first 5
 *       characters of the SHA-1 hash leave the server, the password itself never
 *       does). Fail-open with a logged warning if the service is unreachable.</li>
 * </ol>
 *
 * <p>The HIBP check can be disabled by setting {@code app.security.password.hibp-enabled=false}
 * (useful for offline/dev environments and tests).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordPolicyService {

    /**
     * At least 12 characters, must contain lower-case, upper-case, digit and a symbol.
     */
    private static final Pattern STRONG_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{12,}$");

    private static final int MIN_LENGTH = 12;
    private static final int MAX_LENGTH = 100;

    /**
     * Top-N most common breached passwords. Anything that gets past this is still
     * subject to the live HIBP check, but this guarantees an offline floor.
     */
    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password", "password1", "password123", "password12", "password!",
            "passw0rd", "p@ssw0rd", "p@ssword", "p@ssword1", "p@ssword123",
            "qwerty", "qwerty123", "qwerty12345", "qwertyuiop",
            "12345678", "123456789", "1234567890", "12345678910",
            "11111111", "00000000", "01234567", "abcdefgh",
            "iloveyou", "letmein", "welcome", "welcome1", "welcome123",
            "admin", "admin123", "administrator", "root", "toor",
            "monkey", "dragon", "master", "shadow", "superman",
            "football", "baseball", "michael", "jennifer",
            "trustno1", "starwars", "sunshine", "princess",
            "abc123", "abc12345", "aaaaaaaa", "11111aaaa",
            "civilcontrol", "civilcontrol1", "civilcontrol123",
            "controlcivil", "esea", "esea123", "esea1234",
            "argentina", "argentina123", "buenosaires"
    );

    private final MessageSourceHelper messageSourceHelper;

    @Value("${app.security.password.hibp-enabled:true}")
    private boolean hibpEnabled;

    @Value("${app.security.password.hibp-timeout-ms:2500}")
    private long hibpTimeoutMs;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    /**
     * Validates a password against the full policy.
     *
     * @param rawPassword the plain-text password to validate
     * @param username    the user's username (used for context-aware checks; may be {@code null})
     * @param email       the user's email (used for context-aware checks; may be {@code null})
     * @throws UserNotValidException if the password is not acceptable
     */
    public void validate(String rawPassword, String username, String email) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.password.required"));
        }

        if (rawPassword.length() < MIN_LENGTH) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.password.minLength"));
        }

        if (rawPassword.length() > MAX_LENGTH) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.password.maxLength"));
        }

        if (!STRONG_PATTERN.matcher(rawPassword).matches()) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.password.complexity"));
        }

        // Context-aware checks
        String lower = rawPassword.toLowerCase();
        if (username != null && !username.isBlank() && lower.contains(username.toLowerCase())) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.password.containsUsername"));
        }
        if (email != null && !email.isBlank()) {
            String localPart = email.split("@")[0].toLowerCase();
            if (localPart.length() >= 3 && lower.contains(localPart)) {
                throw new UserNotValidException(messageSourceHelper.getMessage("user.password.containsEmail"));
            }
        }

        // Embedded denylist
        if (COMMON_PASSWORDS.contains(lower)) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.password.commonlyUsed"));
        }

        // Live HIBP check (k-anonymity, fail-open)
        if (hibpEnabled && isPwned(rawPassword)) {
            throw new UserNotValidException(messageSourceHelper.getMessage("user.password.compromised"));
        }
    }

    /**
     * Queries HaveIBeenPwned with the first 5 characters of the password's SHA-1 hash.
     * The full password never leaves the server.
     * Fail-open: returns {@code false} on any I/O error.
     */
    private boolean isPwned(String rawPassword) {
        try {
            String hash = sha1Hex(rawPassword).toUpperCase();
            String prefix = hash.substring(0, 5);
            String suffix = hash.substring(5);

            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.pwnedpasswords.com/range/" + prefix))
                    .timeout(Duration.ofMillis(hibpTimeoutMs))
                    .header("Add-Padding", "true")
                    .header("User-Agent", "CivilControl-PasswordPolicy/1.0")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (response.statusCode() != 200) {
                log.warn("HIBP returned non-200 status {}, allowing password (fail-open).", response.statusCode());
                return false;
            }

            // Response is a list of "SUFFIX:count" lines. We match the suffix exactly.
            for (String line : response.body().split("\\R")) {
                int sep = line.indexOf(':');
                if (sep <= 0) continue;
                String lineSuffix = line.substring(0, sep);
                if (lineSuffix.equalsIgnoreCase(suffix)) {
                    long count = parseCount(line.substring(sep + 1));
                    if (count > 0) {
                        log.warn("Password rejected: appears in HIBP breach corpus ({} times).", count);
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.warn("HIBP lookup failed ({}); allowing password (fail-open).", e.getClass().getSimpleName());
            return false;
        }
    }

    private static long parseCount(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static String sha1Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 not available", e);
        }
    }
}
