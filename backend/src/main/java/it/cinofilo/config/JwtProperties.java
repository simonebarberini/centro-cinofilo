package it.cinofilo.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT configuration properties.
 *
 * Required environment variable: JWT_SECRET (min 64 bytes / 512 bits for HS512).
 * Optional: JWT_EXPIRATION_MS (default: 28800000 = 8 hours).
 *
 * The application will refuse to start if JWT_SECRET is blank or missing.
 * Byte-length validation (>= 64 bytes) is enforced in JwtService constructor.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    @NotBlank(message =
        "JWT_SECRET is required. " +
        "Set the JWT_SECRET environment variable with at least 64 bytes (512 bits) for HS512.")
    private String secret;

    private long expirationMs = 28800000L;
}
