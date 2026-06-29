package it.cinofilo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import it.cinofilo.config.JwtProperties;
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

    // 64-char string = 64 bytes > 32 bytes minimum required for HS256.
    private static final String VALID_SECRET =
        "valid-test-secret-key-that-is-at-least-32-bytes-long-for-hs256";
    private static final long EXPIRATION_MS = 3600000L;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(propertiesWith(VALID_SECRET, EXPIRATION_MS));
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    void shouldRejectBlankSecret() {
        assertThatThrownBy(() -> new JwtService(propertiesWith("", EXPIRATION_MS)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least 32 bytes");
    }

    @Test
    void shouldRejectSecretShorterThan32Bytes() {
        // 15 chars = 15 bytes — below the 32-byte minimum
        assertThatThrownBy(() -> new JwtService(propertiesWith("tooshort1234567", EXPIRATION_MS)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least 32 bytes")
            .hasMessageContaining("15 bytes");
    }

    @Test
    void shouldAcceptSecretOfExactly32Bytes() {
        // 32 ASCII chars = 32 bytes — exactly at the minimum
        String exactly32 = "exactly-32-bytes-long-secret-key";
        assertThat(exactly32.getBytes(StandardCharsets.UTF_8)).hasSize(32);

        JwtService service = new JwtService(propertiesWith(exactly32, EXPIRATION_MS));
        UUID userId = UUID.randomUUID();
        String token = service.generateToken(userId, UUID.randomUUID(), "TENANT_OWNER", "u");
        assertThat(service.extractUserId(token)).isEqualTo(userId);
    }

    // ── Token generation ──────────────────────────────────────────────────────

    @Test
    void shouldGenerateValidToken() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        String token = jwtService.generateToken(userId, tenantId, "TENANT_OWNER", "testuser");

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void shouldExtractCorrectClaims() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String role = "TENANT_OWNER";
        String username = "testuser";

        String token = jwtService.generateToken(userId, tenantId, role, username);
        Claims claims = jwtService.validateAndExtractClaims(token);

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("tenantId", String.class)).isEqualTo(tenantId.toString());
        assertThat(claims.get("role", String.class)).isEqualTo(role);
        assertThat(claims.get("username", String.class)).isEqualTo(username);
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
    }

    // ── Validation helpers ────────────────────────────────────────────────────

    @Test
    void shouldValidateCorrectToken() {
        String token = jwtService.generateToken(
            UUID.randomUUID(), UUID.randomUUID(), "ADMIN_APP", "admin");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void shouldRejectInvalidToken() {
        assertThat(jwtService.isTokenValid("invalid.token.here")).isFalse();
    }

    @Test
    void shouldRejectExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(VALID_SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .claim("tenantId", UUID.randomUUID().toString())
            .claim("role", "TENANT_OWNER")
            .claim("username", "test")
            .issuedAt(Date.from(Instant.now().minusSeconds(7200)))
            .expiration(Date.from(Instant.now().minusSeconds(3600)))
            .signWith(key)
            .compact();

        assertThatThrownBy(() -> jwtService.validateAndExtractClaims(expiredToken))
            .isInstanceOf(ExpiredJwtException.class);
    }

    // ── Claim extraction ──────────────────────────────────────────────────────

    @Test
    void shouldExtractUserId() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, UUID.randomUUID(), "TENANT_STAFF", "staff");
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
    }

    @Test
    void shouldExtractTenantId() {
        UUID tenantId = UUID.randomUUID();
        String token = jwtService.generateToken(UUID.randomUUID(), tenantId, "TENANT_OWNER", "owner");
        assertThat(jwtService.extractTenantId(token)).isEqualTo(tenantId);
    }

    @Test
    void shouldExtractRole() {
        String token = jwtService.generateToken(
            UUID.randomUUID(), UUID.randomUUID(), "ADMIN_APP", "admin");
        assertThat(jwtService.extractRole(token)).isEqualTo("ADMIN_APP");
    }

    @Test
    void shouldExtractUsername() {
        String token = jwtService.generateToken(
            UUID.randomUUID(), UUID.randomUUID(), "TENANT_OWNER", "myusername");
        assertThat(jwtService.extractUsername(token)).isEqualTo("myusername");
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private static JwtProperties propertiesWith(String secret, long expirationMs) {
        JwtProperties props = new JwtProperties();
        props.setSecret(secret);
        props.setExpirationMs(expirationMs);
        return props;
    }
}
