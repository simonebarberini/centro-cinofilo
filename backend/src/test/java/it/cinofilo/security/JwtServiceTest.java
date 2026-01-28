package it.cinofilo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET = "dev-secret-key-minimum-256-bits-for-hs256-algorithm-change-in-production";
    private static final long EXPIRATION_MS = 3600000; // 1 hour

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, EXPIRATION_MS);
    }

    @Test
    void shouldGenerateValidToken() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String role = "TENANT_OWNER";
        String username = "testuser";

        // When
        String token = jwtService.generateToken(userId, tenantId, role, username);

        // Then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // Header.Payload.Signature
    }

    @Test
    void shouldExtractCorrectClaims() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String role = "TENANT_OWNER";
        String username = "testuser";
        String token = jwtService.generateToken(userId, tenantId, role, username);

        // When
        Claims claims = jwtService.validateAndExtractClaims(token);

        // Then
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("tenantId", String.class)).isEqualTo(tenantId.toString());
        assertThat(claims.get("role", String.class)).isEqualTo(role);
        assertThat(claims.get("username", String.class)).isEqualTo(username);
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
    }

    @Test
    void shouldValidateCorrectToken() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, tenantId, "ADMIN_APP", "admin");

        // When
        boolean isValid = jwtService.isTokenValid(token);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldRejectInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When
        boolean isValid = jwtService.isTokenValid(invalidToken);

        // Then
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldRejectExpiredToken() {
        // Given - Create token with immediate expiration
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("tenantId", UUID.randomUUID().toString())
                .claim("role", "TENANT_OWNER")
                .claim("username", "test")
                .issuedAt(Date.from(Instant.now().minusSeconds(7200)))
                .expiration(Date.from(Instant.now().minusSeconds(3600)))
                .signWith(key)
                .compact();

        // When/Then
        assertThatThrownBy(() -> jwtService.validateAndExtractClaims(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void shouldExtractUserId() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, tenantId, "TENANT_STAFF", "staff");

        // When
        UUID extractedUserId = jwtService.extractUserId(token);

        // Then
        assertThat(extractedUserId).isEqualTo(userId);
    }

    @Test
    void shouldExtractTenantId() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, tenantId, "TENANT_OWNER", "owner");

        // When
        UUID extractedTenantId = jwtService.extractTenantId(token);

        // Then
        assertThat(extractedTenantId).isEqualTo(tenantId);
    }

    @Test
    void shouldExtractRole() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String role = "ADMIN_APP";
        String token = jwtService.generateToken(userId, tenantId, role, "admin");

        // When
        String extractedRole = jwtService.extractRole(token);

        // Then
        assertThat(extractedRole).isEqualTo(role);
    }

    @Test
    void shouldExtractUsername() {
        // Given
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String username = "myusername";
        String token = jwtService.generateToken(userId, tenantId, "TENANT_OWNER", username);

        // When
        String extractedUsername = jwtService.extractUsername(token);

        // Then
        assertThat(extractedUsername).isEqualTo(username);
    }
}
