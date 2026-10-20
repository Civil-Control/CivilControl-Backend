package PSG.backEnd.service.implementation;

import PSG.backEnd.model.entity.TenantEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service for handling JWT token operations.
 * Generates, validates, and extracts information from JWT tokens.
 */
@Service
@Slf4j
@Getter
public class JwtService {

    private static final String TENANT_CLAIM = "tenantId";

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token.expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration}")
    private Long refreshTokenExpiration;

    /**
     * Generates an access token for the user.
     */
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();

        // Add roles and permissions to token
        extraClaims.put("authorities", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));

        extraClaims.put(TENANT_CLAIM, requireTenantId(userDetails));

        return generateToken(extraClaims, userDetails, accessTokenExpiration);
    }

    /**
     * Generates a refresh token for the user.
     */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put(TENANT_CLAIM, requireTenantId(userDetails));
        return generateToken(extraClaims, userDetails, refreshTokenExpiration);
    }

    private Long requireTenantId(UserDetails userDetails) {
        if (userDetails instanceof TenantEntity tenantEntity && tenantEntity.getTenantId() != null) {
            return tenantEntity.getTenantId();
        }
        throw new IllegalStateException("Cannot issue a token for a user without tenant");
    }

    /**
     * Generates a JWT token with custom claims.
     * Every issued token gets a unique {@code jti} so it can be individually
     * revoked by the {@link PSG.backEnd.service.security.JwtTokenBlacklist}.
     */
    private String generateToken(Map<String, Object> extraClaims, UserDetails userDetails, Long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .id(UUID.randomUUID().toString())
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    /** Extracts the JWT id (jti) used for revocation tracking. May be null for legacy tokens. */
    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    /**
     * Extracts username from token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts tenant ID from token.
     * Returns null if the tenantId claim is not present or invalid.
     */
    public Long extractTenantId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object tenantId = claims.get(TENANT_CLAIM);
            if (tenantId instanceof Number number) {
                return number.longValue();
            }
            return tenantId != null ? Long.parseLong(tenantId.toString()) : null;
        } catch (Exception e) {
            log.warn("Could not extract tenantId from token");
            return null;
        }
    }

    /**
     * Extracts expiration date from token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts a specific claim from token.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extracts all claims from token.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Validates if token is valid for the user.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        final Long tenantId = extractTenantId(token);
        return username.equals(userDetails.getUsername())
                && userDetails instanceof TenantEntity tenantEntity
                && tenantId != null
                && tenantId.equals(tenantEntity.getTenantId())
                && !isTokenExpired(token);
    }

    /**
     * Checks if token is expired.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Gets the signing key for JWT.
     */
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

