package it.cinofilo.entitlements;

import it.cinofilo.catalog.Module;
import it.cinofilo.catalog.ModuleEntitlement;
import it.cinofilo.catalog.ModuleRepository;
import it.cinofilo.catalog.ModuleType;
import it.cinofilo.subscription.TenantModule;
import it.cinofilo.subscription.TenantModuleRepository;
import it.cinofilo.subscription.TenantModuleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EntitlementServiceTest {

    private TenantModuleRepository tenantModuleRepository;
    private ModuleRepository moduleRepository;
    private EntitlementService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final String BOOL_KEY = "dogs.enabled";
    private static final String QUOTA_KEY = "dogs.max_count";

    @BeforeEach
    void setUp() {
        tenantModuleRepository = mock(TenantModuleRepository.class);
        moduleRepository = mock(ModuleRepository.class);
        service = new EntitlementService(tenantModuleRepository, moduleRepository);
    }

    // ── isEnabled ─────────────────────────────────────────────────────────────

    @Test
    void isEnabled_returnsTrue_whenModuleHasBoolEntitlementTrue() {
        givenSingleActiveModule("base", ModuleEntitlement.ofBoolean("base", BOOL_KEY, true));

        assertThat(service.isEnabled(TENANT_ID, BOOL_KEY)).isTrue();
    }

    @Test
    void isEnabled_returnsFalse_whenModuleHasBoolEntitlementFalse() {
        givenSingleActiveModule("base", ModuleEntitlement.ofBoolean("base", BOOL_KEY, false));

        assertThat(service.isEnabled(TENANT_ID, BOOL_KEY)).isFalse();
    }

    @Test
    void isEnabled_returnsFalse_whenNoActiveSubscriptions() {
        when(tenantModuleRepository.findByTenantIdAndStatusIn(eq(TENANT_ID), any()))
                .thenReturn(List.of());

        assertThat(service.isEnabled(TENANT_ID, BOOL_KEY)).isFalse();
    }

    @Test
    void isEnabled_returnsFalse_whenEntitlementKeyNotFound() {
        givenSingleActiveModule("base", ModuleEntitlement.ofBoolean("base", "other.key", true));

        assertThat(service.isEnabled(TENANT_ID, BOOL_KEY)).isFalse();
    }

    @Test
    void isEnabled_returnsTrue_whenTrialModuleEnablesIt() {
        givenSingleModule("reports", TenantModuleStatus.TRIAL,
                ModuleEntitlement.ofBoolean("reports", BOOL_KEY, true));

        assertThat(service.isEnabled(TENANT_ID, BOOL_KEY)).isTrue();
    }

    // ── getQuota ──────────────────────────────────────────────────────────────

    @Test
    void getQuota_returnsValue_whenSingleModuleDefinesIt() {
        givenSingleActiveModule("base", ModuleEntitlement.ofQuota("base", QUOTA_KEY, 10));

        assertThat(service.getQuota(TENANT_ID, QUOTA_KEY)).isEqualTo(10);
    }

    @Test
    void getQuota_sumsAcrossMultipleActiveModules() {
        when(tenantModuleRepository.findByTenantIdAndStatusIn(eq(TENANT_ID), any()))
                .thenReturn(List.of(
                        tenantModule("base",  TenantModuleStatus.ACTIVE),
                        tenantModule("staff", TenantModuleStatus.ACTIVE)
                ));
        when(moduleRepository.findByModuleKeyIn(any()))
                .thenReturn(List.of(
                        moduleWith("base",  ModuleEntitlement.ofQuota("base",  QUOTA_KEY, 10)),
                        moduleWith("staff", ModuleEntitlement.ofQuota("staff", QUOTA_KEY, 5))
                ));

        assertThat(service.getQuota(TENANT_ID, QUOTA_KEY)).isEqualTo(15);
    }

    @Test
    void getQuota_returnsZero_whenNoActiveSubscriptions() {
        when(tenantModuleRepository.findByTenantIdAndStatusIn(eq(TENANT_ID), any()))
                .thenReturn(List.of());

        assertThat(service.getQuota(TENANT_ID, QUOTA_KEY)).isZero();
    }

    @Test
    void getQuota_returnsZero_whenEntitlementKeyNotFound() {
        givenSingleActiveModule("base", ModuleEntitlement.ofQuota("base", "other.quota", 99));

        assertThat(service.getQuota(TENANT_ID, QUOTA_KEY)).isZero();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void givenSingleActiveModule(String moduleKey, ModuleEntitlement entitlement) {
        givenSingleModule(moduleKey, TenantModuleStatus.ACTIVE, entitlement);
    }

    private void givenSingleModule(String moduleKey, TenantModuleStatus status,
                                   ModuleEntitlement entitlement) {
        when(tenantModuleRepository.findByTenantIdAndStatusIn(eq(TENANT_ID), any()))
                .thenReturn(List.of(tenantModule(moduleKey, status)));
        when(moduleRepository.findByModuleKeyIn(any()))
                .thenReturn(List.of(moduleWith(moduleKey, entitlement)));
    }

    private Module moduleWith(String moduleKey, ModuleEntitlement entitlement) {
        return Module.builder()
                .moduleKey(moduleKey)
                .name(moduleKey)
                .type(ModuleType.OPTIONAL)
                .entitlements(new ArrayList<>(List.of(entitlement)))
                .build();
    }

    private TenantModule tenantModule(String moduleKey, TenantModuleStatus status) {
        return TenantModule.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .moduleKey(moduleKey)
                .status(status)
                .build();
    }
}
