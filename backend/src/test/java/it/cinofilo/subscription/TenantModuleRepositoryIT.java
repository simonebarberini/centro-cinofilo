package it.cinofilo.subscription;

import it.cinofilo.AbstractPostgresIT;
import it.cinofilo.catalog.Module;
import it.cinofilo.catalog.ModuleRepository;
import it.cinofilo.catalog.ModuleType;
import it.cinofilo.tenancy.Tenant;
import it.cinofilo.tenancy.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TenantModuleRepositoryIT extends AbstractPostgresIT {

    @Autowired private TenantModuleRepository tenantModuleRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private ModuleRepository moduleRepository;
    @Autowired private TestEntityManager em;

    private static final String MODULE_KEY = "tm-test";

    private UUID tenantId;

    @BeforeEach
    void setup() {
        moduleRepository.deleteAllById(java.util.List.of(MODULE_KEY));
        moduleRepository.save(Module.builder()
                .moduleKey(MODULE_KEY)
                .name("TenantModule Test Module")
                .type(ModuleType.OPTIONAL)
                .build());
        Tenant tenant = tenantRepository.save(Tenant.builder()
                .name("Test Kennel")
                .type("KENNEL")
                .slug("test-kennel-tm")
                .build());
        tenantId = tenant.getId();
    }

    @Test
    void save_andRetrieveById_activeModule() {
        TenantModule saved = tenantModuleRepository.save(TenantModule.builder()
                .tenantId(tenantId)
                .moduleKey(MODULE_KEY)
                .status(TenantModuleStatus.ACTIVE)
                .build());

        em.flush();
        em.clear();
        TenantModule found = tenantModuleRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getTenantId()).isEqualTo(tenantId);
        assertThat(found.getModuleKey()).isEqualTo(MODULE_KEY);
        assertThat(found.getStatus()).isEqualTo(TenantModuleStatus.ACTIVE);
        assertThat(found.getTrialEndsAt()).isNull();
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void findByTenantIdAndModuleKey_returnsPresent() {
        tenantModuleRepository.save(TenantModule.builder()
                .tenantId(tenantId)
                .moduleKey(MODULE_KEY)
                .status(TenantModuleStatus.ACTIVE)
                .build());

        Optional<TenantModule> result = tenantModuleRepository
                .findByTenantIdAndModuleKey(tenantId, MODULE_KEY);

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(TenantModuleStatus.ACTIVE);
    }

    @Test
    void findByTenantIdAndModuleKey_returnsEmpty_whenNotFound() {
        Optional<TenantModule> result = tenantModuleRepository
                .findByTenantIdAndModuleKey(UUID.randomUUID(), MODULE_KEY);

        assertThat(result).isEmpty();
    }

    @Test
    void save_trialModule_withTrialEndsAt() {
        Instant trialEnd = Instant.now().plusSeconds(86_400 * 14);

        TenantModule saved = tenantModuleRepository.save(TenantModule.builder()
                .tenantId(tenantId)
                .moduleKey(MODULE_KEY)
                .status(TenantModuleStatus.TRIAL)
                .trialEndsAt(trialEnd)
                .build());
        em.flush();

        TenantModule found = tenantModuleRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(TenantModuleStatus.TRIAL);
        assertThat(found.getTrialEndsAt()).isNotNull();
    }

    @Test
    void uniqueConstraint_rejectsDuplicateTenantModule() {
        tenantModuleRepository.save(TenantModule.builder()
                .tenantId(tenantId)
                .moduleKey(MODULE_KEY)
                .status(TenantModuleStatus.ACTIVE)
                .build());
        em.flush();

        assertThatThrownBy(() -> {
            tenantModuleRepository.save(TenantModule.builder()
                    .tenantId(tenantId)
                    .moduleKey(MODULE_KEY)
                    .status(TenantModuleStatus.TRIAL)
                    .trialEndsAt(Instant.now().plusSeconds(86_400))
                    .build());
            em.flush();
        }).isInstanceOf(Exception.class);
    }

    @Test
    void checkConstraint_trialEndsAt_rejectsActiveModuleWithDate() {
        assertThatThrownBy(() -> {
            tenantModuleRepository.save(TenantModule.builder()
                    .tenantId(tenantId)
                    .moduleKey(MODULE_KEY)
                    .status(TenantModuleStatus.ACTIVE)
                    .trialEndsAt(Instant.now().plusSeconds(86_400))
                    .build());
            em.flush();
        }).isInstanceOf(Exception.class);
    }
}
