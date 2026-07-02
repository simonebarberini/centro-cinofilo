package it.cinofilo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import it.cinofilo.config.JwtProperties;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(JwtProperties jwtProperties) {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 64) {
            throw new IllegalArgumentException(
                "JWT secret must be at least 64 bytes (512 bits) for HS512. " +
                "Current length: " + keyBytes.length + " bytes. " +
                "Set a stronger JWT_SECRET environment variable."
            );
        }
        // The signing algorithm is an explicit application choice (HS512); it is
        // NEVER derived from the key length. The key is bound to HmacSHA512 to
        // match the JwtDecoder configured in SecurityConfig.
        this.secretKey = new SecretKeySpec(keyBytes, "HmacSHA512");
        this.expirationMs = jwtProperties.getExpirationMs();
    }

    public String generateToken(UUID userId, UUID tenantId, String role, String username) {
        Instant now = Instant.now();
        Instant expiration = now.plusMillis(expirationMs);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("role", role)
                .claim("username", username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    public Claims validateAndExtractClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            validateAndExtractClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public UUID extractUserId(String token) {
        Claims claims = validateAndExtractClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    public UUID extractTenantId(String token) {
        Claims claims = validateAndExtractClaims(token);
        return UUID.fromString(claims.get("tenantId", String.class));
    }

    public String extractRole(String token) {
        Claims claims = validateAndExtractClaims(token);
        return claims.get("role", String.class);
    }

    public String extractUsername(String token) {
        Claims claims = validateAndExtractClaims(token);
        return claims.get("username", String.class);
    }
}
