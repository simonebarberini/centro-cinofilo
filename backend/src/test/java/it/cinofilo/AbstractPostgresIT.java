package it.cinofilo;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests using Testcontainers PostgreSQL.
 * Provides a shared PostgreSQL container for all integration tests.
 */
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractPostgresIT {

    /**
     * Mocks the mail sender for all integration tests.
     * Prevents real SMTP connections and avoids the MailHealthContributorAutoConfiguration
     * failure ("Beans must not be empty") that occurs when @MockBean replaces the
     * auto-configured JavaMailSenderImpl at the individual test-class level.
     */
    @MockBean
    protected JavaMailSender mailSender;

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

        // Mail health indicator — disabled because SMTP reachability is
        // infrastructure, not business logic.  Without this, Actuator tries to
        // build a MailHealthContributor from the mocked JavaMailSender bean map,
        // which Spring resolves as empty at context-load time, causing
        // "Beans must not be empty" and a context startup failure.
        registry.add("management.health.mail.enabled", () -> "false");
    }
}
