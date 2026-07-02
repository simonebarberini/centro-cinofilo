package it.cinofilo;

import it.cinofilo.auth.EmailTokenRepository;
import it.cinofilo.bookings.BookingRepository;
import it.cinofilo.customer.CustomerRepository;
import it.cinofilo.dogs.DogRepository;
import it.cinofilo.subscription.AdminGrantRepository;
import it.cinofilo.subscription.TenantModule;
import it.cinofilo.subscription.TenantModuleRepository;
import it.cinofilo.tenancy.TenantRepository;
import it.cinofilo.users.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests using Testcontainers PostgreSQL.
 *
 * <p>The PostgreSQL container is {@code static}, so it is shared by every
 * integration test in the suite regardless of which Spring context is active.
 * Because the {@code @SpringBootTest} tests commit their data (they are not
 * transactional), rows written by one test class are visible to the next.
 * To keep every test deterministic, this base class wipes all tables before
 * each test in foreign-key-safe order — this is the single, uniform teardown
 * strategy for the whole suite, so individual tests must NOT clean up
 * themselves.
 *
 * <p><strong>The integration tests must run sequentially.</strong> A shared
 * static database combined with per-test wiping is not safe under JUnit/Maven
 * parallel execution, where one test could wipe another test's data mid-run.
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

    @Autowired(required = false)
    private BookingRepository bookingRepository;

    @Autowired(required = false)
    private DogRepository dogRepository;

    @Autowired(required = false)
    private CustomerRepository customerRepository;

    @Autowired(required = false)
    private EmailTokenRepository emailTokenRepository;

    @Autowired(required = false)
    private AppUserRepository appUserRepository;

    @Autowired(required = false)
    private TenantModuleRepository tenantModuleRepository;

    @Autowired(required = false)
    private AdminGrantRepository adminGrantRepository;

    @Autowired(required = false)
    private TenantRepository tenantRepository;

    /**
     * Wipes all domain tables before each test, in child-to-parent order so that
     * every foreign key is satisfied:
     * email_token → tenant_module → booking → dog → customer → app_user → tenant.
     *
     * <p>Repositories are injected with {@code required = false} because slice
     * tests (e.g. {@code @DataJpaTest}) may not expose every repository bean;
     * each delete is guarded so the cleanup degrades gracefully.
     *
     * <p>Runs as a superclass {@code @BeforeEach}, which JUnit 5 guarantees to
     * execute before each subclass {@code @BeforeEach}, so subclasses always
     * start from an empty database.
     */
    @BeforeEach
    protected void cleanDatabase() {
        if (emailTokenRepository != null) emailTokenRepository.deleteAllInBatch();
        if (tenantModuleRepository != null) tenantModuleRepository.deleteAllInBatch();
        if (adminGrantRepository != null) adminGrantRepository.deleteAllInBatch();
        if (bookingRepository != null) bookingRepository.deleteAllInBatch();
        if (dogRepository != null) dogRepository.deleteAllInBatch();
        if (customerRepository != null) customerRepository.deleteAllInBatch();
        if (appUserRepository != null) appUserRepository.deleteAllInBatch();
        if (tenantRepository != null) tenantRepository.deleteAllInBatch();
    }

    /**
     * Activates the 'base' module for one or more test tenants.
     * Must be called after test tenants are persisted, since cleanDatabase()
     * wipes tenant_module before each test.
     */
    protected void activateBaseModule(java.util.UUID... tenantIds) {
        if (tenantModuleRepository == null) return;
        for (java.util.UUID tenantId : tenantIds) {
            tenantModuleRepository.save(TenantModule.forActivation(tenantId, "base"));
        }
    }

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
        // This value is only used in tests; it must be at least 64 bytes (HS512).
        registry.add("jwt.secret", () -> "test-only-secret-key-for-integration-tests-only-hs512-minimum-64bytes");

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
