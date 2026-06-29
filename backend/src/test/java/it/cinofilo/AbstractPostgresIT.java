package it.cinofilo;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests using Testcontainers PostgreSQL.
 * Provides a shared PostgreSQL container for all integration tests.
 */
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractPostgresIT {

    protected static final PostgreSQLContainer<?> postgresContainer;

    static {
        postgresContainer = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
        postgresContainer.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);

        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.clean-disabled", () -> "false");

        // JWT secret required — application refuses to start without it.
        // This value is only used in tests; it must be at least 32 bytes.
        registry.add("jwt.secret", () -> "test-only-secret-key-for-integration-tests-only");

        // CORS — test suite calls the API directly, any origin is acceptable.
        registry.add("app.cors.allowed-origins", () -> "http://localhost:4200");

        // Rate limiting — disabled in integration tests to prevent bucket state
        // from interfering between test methods (shared in-memory buckets).
        registry.add("rate-limit.enabled", () -> "false");
    }
}
