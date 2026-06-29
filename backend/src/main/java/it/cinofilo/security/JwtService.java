package it.cinofilo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import it.cinofilo.config.JwtProperties;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    public JwtService(JwtProperties jwtProperties) {
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                "JWT secret must be at least 32 bytes (256 bits) for HS256. " +
                "Current length: " + keyBytes.length + " bytes. " +
                "Set a stronger JWT_SECRET environment variable."
            );
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
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
                .signWith(secretKey)
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
