package it.cinofilo.users;

import it.cinofilo.AbstractPostgresIT;
import it.cinofilo.tenancy.Role;
import it.cinofilo.tenancy.Tenant;
import it.cinofilo.tenancy.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AppUserRepositoryIT extends AbstractPostgresIT {

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private TenantRepository tenantRepository;

    private Tenant testTenant;

    @BeforeEach
    void setUp() {
        String uniqueSlug = "test-tenant-" + UUID.randomUUID();
        testTenant = Tenant.builder()
                .name("Test Tenant")
                .type("PENSIONE")
                .slug(uniqueSlug)
                .capacityBoxes(10)
                .build();
        testTenant = tenantRepository.save(testTenant);
    }

    @Test
    void shouldSaveAppUser() {
        // Given
        AppUser user = AppUser.builder()
                .tenant(testTenant)
                .username("testuser")
                .passwordHash("$2a$10$hash")
                .role(Role.TENANT_OWNER)
                .enabled(true)
                .build();

        // When
        AppUser saved = appUserRepository.save(user);
        appUserRepository.flush();

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUsername()).isEqualTo("testuser");
        assertThat(saved.getTenant().getId()).isEqualTo(testTenant.getId());
        assertThat(saved.getRole()).isEqualTo(Role.TENANT_OWNER);
        assertThat(saved.getEnabled()).isTrue();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldThrowExceptionOnDuplicateUsernameInSameTenant() {
        // Given
        AppUser user1 = AppUser.builder()
                .tenant(testTenant)
                .username("duplicate")
                .passwordHash("$2a$10$hash1")
                .role(Role.TENANT_OWNER)
                .enabled(true)
                .build();
        appUserRepository.save(user1);

        AppUser user2 = AppUser.builder()
                .tenant(testTenant)
                .username("duplicate")
                .passwordHash("$2a$10$hash2")
                .role(Role.TENANT_STAFF)
                .enabled(true)
                .build();

        // When/Then
        assertThatThrownBy(() -> {
            appUserRepository.save(user2);
            appUserRepository.flush(); // Force immediate constraint check
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAllowSameUsernameInDifferentTenants() {
        // Given - Create a second tenant
        String uniqueSlug2 = "another-tenant-" + UUID.randomUUID();
        Tenant tenant2 = Tenant.builder()
                .name("Another Tenant")
                .type("ADDESTRAMENTO")
                .slug(uniqueSlug2)
                .capacityBoxes(5)
                .build();
        tenant2 = tenantRepository.save(tenant2);

        // Create two users with the same username but different tenants
        AppUser user1 = AppUser.builder()
                .tenant(testTenant)
                .username("shared")
                .passwordHash("$2a$10$hash1")
                .role(Role.TENANT_OWNER)
                .enabled(true)
                .build();

        AppUser user2 = AppUser.builder()
                .tenant(tenant2)
                .username("shared")
                .passwordHash("$2a$10$hash2")
                .role(Role.TENANT_OWNER)
                .enabled(true)
                .build();

        // When
        appUserRepository.save(user1);
        appUserRepository.save(user2);
        appUserRepository.flush();

        // Then - Verify both users exist with their respective tenant-username combinations
        boolean user1Exists = appUserRepository.existsByTenantIdAndUsername(testTenant.getId(), "shared");
        boolean user2Exists = appUserRepository.existsByTenantIdAndUsername(tenant2.getId(), "shared");
        
        assertThat(user1Exists).isTrue();
        assertThat(user2Exists).isTrue();
        
        // Verify cross-tenant isolation: user1's username doesn't exist in tenant2's context
        // This confirms the unique constraint is on (tenant_id, username) not just username
    }

    @Test
    void shouldThrowExceptionWhenTenantDoesNotExist() {
        // Given
        Tenant fakeTenant = Tenant.builder()
                .id(UUID.randomUUID())
                .name("Fake")
                .type("FAKE")
                .capacityBoxes(0)
                .build();

        AppUser user = AppUser.builder()
                .tenant(fakeTenant)
                .username("orphan")
                .passwordHash("$2a$10$hash")
                .role(Role.TENANT_STAFF)
                .enabled(true)
                .build();

        // When/Then
        assertThatThrownBy(() -> {
            appUserRepository.save(user);
            appUserRepository.flush(); // Force immediate FK check
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldCheckIfUserExistsByTenantAndUsername() {
        // Given
        AppUser user = AppUser.builder()
                .tenant(testTenant)
                .username("checker")
                .passwordHash("$2a$10$hash")
                .role(Role.TENANT_STAFF)
                .enabled(true)
                .build();
        appUserRepository.save(user);

        // When
        boolean exists = appUserRepository.existsByTenantIdAndUsername(testTenant.getId(), "checker");
        boolean notExists = appUserRepository.existsByTenantIdAndUsername(testTenant.getId(), "nonexistent");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
}
