package it.cinofilo.tenancy;

import it.cinofilo.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TenantRepositoryIT extends AbstractPostgresIT {

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void shouldSaveTenant() {
        // Given
        Tenant tenant = Tenant.builder()
                .name("Test Centro")
                .type("PENSIONE")
                .capacityBoxes(15)
                .build();

        // When
        Tenant saved = tenantRepository.save(tenant);
        tenantRepository.flush();

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Test Centro");
        assertThat(saved.getType()).isEqualTo("PENSIONE");
        assertThat(saved.getCapacityBoxes()).isEqualTo(15);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldFindTenantByName() {
        // Given
        Tenant tenant = Tenant.builder()
                .name("Unique Centro")
                .type("ADDESTRAMENTO")
                .capacityBoxes(20)
                .build();
        tenantRepository.save(tenant);

        // When
        var found = tenantRepository.findByName("Unique Centro");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Unique Centro");
    }

    @Test
    void shouldCheckIfTenantExistsByName() {
        // Given
        Tenant tenant = Tenant.builder()
                .name("Existing Centro")
                .type("PENSIONE")
                .capacityBoxes(10)
                .build();
        tenantRepository.save(tenant);

        // When
        boolean exists = tenantRepository.existsByName("Existing Centro");
        boolean notExists = tenantRepository.existsByName("Non Existing");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }
}
