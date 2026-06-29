package it.cinofilo.security;

import it.cinofilo.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Regression test for the JWT algorithm contract.
 *
 * <p>Guards against the HS256/HS512 mismatch bug: a token produced by
 * {@link JwtService} must always be accepted by the production {@link JwtDecoder}
 * bean wired in SecurityConfig, and must carry an {@code alg = HS512} header.
 *
 * <p>This test wires the REAL production beans, so any future refactoring that
 * makes the signing and verification algorithms diverge will fail here.
 */
class JwtDecoderRoundTripIT extends AbstractPostgresIT {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void tokenFromJwtServiceIsValidatedByProductionDecoder() {
        UUID userId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        String token = jwtService.generateToken(userId, tenantId, "TENANT_OWNER", "owner");

        assertThatCode(() -> jwtDecoder.decode(token)).doesNotThrowAnyException();

        Jwt decoded = jwtDecoder.decode(token);
        assertThat(decoded.getHeaders().get("alg")).isEqualTo("HS512");
        assertThat(decoded.getSubject()).isEqualTo(userId.toString());
        assertThat(decoded.getClaimAsString("tenantId")).isEqualTo(tenantId.toString());
    }
}
