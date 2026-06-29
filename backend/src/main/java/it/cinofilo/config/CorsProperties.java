package it.cinofilo.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * CORS configuration properties.
 *
 * Configure via YAML:
 *   app:
 *     cors:
 *       allowed-origins:
 *         - https://app.example.com
 *
 * Or via environment variable (comma-separated):
 *   CORS_ALLOWED_ORIGINS=https://app.example.com,https://admin.example.com
 *
 * At least one origin must be configured.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    @NotEmpty(message =
        "At least one CORS allowed origin must be configured. " +
        "Set app.cors.allowed-origins in YAML or the CORS_ALLOWED_ORIGINS environment variable.")
    private List<String> allowedOrigins;
}
