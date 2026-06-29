package it.cinofilo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import it.cinofilo.config.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    // >= 64 bytes — the minimum required for HS512.
    private static final String VALID_SECRET =
        "valid-test-secret-key-that-is-at-least-64-bytes-long-for-the-hs512-algo";
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
            .hasMessageContaining("at least 64 bytes");
    }

    @Test
    void shouldRejectSecretShorterThan64Bytes() {
        // 15 chars = 15 bytes — below the 64-byte minimum
        assertThatThrownBy(() -> new JwtService(propertiesWith("tooshort1234567", EXPIRATION_MS)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least 64 bytes")
            .hasMessageContaining("15 bytes");
    }

    @Test
    void shouldRejectSecretOfExactly32Bytes() {
        // 32 bytes was valid for HS256 but is below the 64-byte HS512 minimum.
        // This is the fail-fast guard that blocks application startup.
        String exactly32 = "exactly-32-bytes-long-secret-key";
        assertThat(exactly32.getBytes(StandardCharsets.UTF_8)).hasSize(32);

        assertThatThrownBy(() -> new JwtService(propertiesWith(exactly32, EXPIRATION_MS)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least 64 bytes")
            .hasMessageContaining("32 bytes");
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
    void shouldAlwaysSignWithHs512Algorithm() {
        // Regression guard: the algorithm must be an explicit HS512 choice,
        // independent of the secret length, so the decoder can always validate it.
        String token = jwtService.generateToken(
            UUID.randomUUID(), UUID.randomUUID(), "TENANT_OWNER", "testuser");

        String headerJson = new String(
            Base64.getUrlDecoder().decode(token.split("\\.")[0]), StandardCharsets.UTF_8);

        assertThat(headerJson).contains("\"alg\":\"HS512\"");
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
        SecretKey key = new SecretKeySpec(VALID_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        String expiredToken = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .claim("tenantId", UUID.randomUUID().toString())
            .claim("role", "TENANT_OWNER")
            .claim("username", "test")
            .issuedAt(Date.from(Instant.now().minusSeconds(7200)))
            .expiration(Date.from(Instant.now().minusSeconds(3600)))
            .signWith(key, Jwts.SIG.HS512)
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
