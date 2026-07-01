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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AdminGrantRepositoryIT extends AbstractPostgresIT {

    @Autowired private AdminGrantRepository adminGrantRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private ModuleRepository moduleRepository;
    @Autowired private TestEntityManager em;

    private static final String MODULE_KEY = "ag-test";

    private UUID tenantId;

    @BeforeEach
    void setup() {
        moduleRepository.deleteAllById(java.util.List.of(MODULE_KEY));
        moduleRepository.save(Module.builder()
                .moduleKey(MODULE_KEY)
                .name("AdminGrant Test Module")
                .type(ModuleType.OPTIONAL)
                .build());
        Tenant tenant = tenantRepository.save(Tenant.builder()
                .name("Test Kennel AG")
                .type("KENNEL")
                .slug("test-kennel-ag")
                .build());
        tenantId = tenant.getId();
    }

    @Test
    void save_andRetrieveById() {
        AdminGrant saved = adminGrantRepository.save(AdminGrant.builder()
                .tenantId(tenantId)
                .moduleKey(MODULE_KEY)
                .grantedBy("admin@cinofilo.it")
                .note("Manual grant for beta")
                .build());
        em.flush();

        AdminGrant found = adminGrantRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getTenantId()).isEqualTo(tenantId);
        assertThat(found.getModuleKey()).isEqualTo(MODULE_KEY);
        assertThat(found.getGrantedBy()).isEqualTo("admin@cinofilo.it");
        assertThat(found.getNote()).isEqualTo("Manual grant for beta");
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void save_withNullNote_isAllowed() {
        AdminGrant saved = adminGrantRepository.save(AdminGrant.builder()
                .tenantId(tenantId)
                .moduleKey(MODULE_KEY)
                .grantedBy("admin@cinofilo.it")
                .build());
        em.flush();

        AdminGrant found = adminGrantRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getNote()).isNull();
    }

    @Test
    void multipleGrants_allowedForSameTenantAndModule() {
        adminGrantRepository.save(AdminGrant.builder()
                .tenantId(tenantId).moduleKey(MODULE_KEY).grantedBy("admin1@cinofilo.it").build());
        adminGrantRepository.save(AdminGrant.builder()
                .tenantId(tenantId).moduleKey(MODULE_KEY).grantedBy("admin2@cinofilo.it").build());
        em.flush();

        assertThat(adminGrantRepository.count()).isEqualTo(2);
    }

    @Test
    void save_withNullGrantedBy_fails() {
        assertThatThrownBy(() -> {
            adminGrantRepository.save(AdminGrant.builder()
                    .tenantId(tenantId)
                    .moduleKey(MODULE_KEY)
                    .grantedBy(null)
                    .build());
            em.flush();
        }).isInstanceOf(Exception.class);
    }

    @Test
    void save_withUnknownTenantId_fails() {
        assertThatThrownBy(() -> {
            adminGrantRepository.save(AdminGrant.builder()
                    .tenantId(UUID.randomUUID())
                    .moduleKey(MODULE_KEY)
                    .grantedBy("admin@cinofilo.it")
                    .build());
            em.flush();
        }).isInstanceOf(Exception.class);
    }
}
