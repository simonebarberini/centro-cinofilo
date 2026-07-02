package it.cinofilo.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that CorsProperties correctly binds allowed origins
 * from application properties and comma-separated env var equivalents.
 *
 * Uses a lightweight slice (no datasource, no JPA, no security) — only
 * the ConfigurationProperties binding mechanism is loaded.
 */
@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(CorsProperties.class)
@TestPropertySource(properties = {
    "app.cors.allowed-origins=http://localhost:4200,https://app.example.com"
})
class CorsPropertiesTest {

    @Autowired
    private CorsProperties corsProperties;

    @Test
    void shouldLoadMultipleOriginsFromCommaSeparatedEnvVar() {
        assertThat(corsProperties.getAllowedOrigins())
            .containsExactlyInAnyOrder(
                "http://localhost:4200",
                "https://app.example.com"
            );
    }

    @Test
    void shouldNotBeEmpty() {
        assertThat(corsProperties.getAllowedOrigins()).isNotEmpty();
    }
}
