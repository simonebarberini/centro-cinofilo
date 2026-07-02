package it.cinofilo.entitlements;

import it.cinofilo.catalog.ModuleEntitlement;
import it.cinofilo.catalog.ModuleRepository;
import it.cinofilo.subscription.TenantModule;
import it.cinofilo.subscription.TenantModuleRepository;
import it.cinofilo.subscription.TenantModuleStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class EntitlementService {

    private final TenantModuleRepository tenantModuleRepository;
    private final ModuleRepository moduleRepository;

    public EntitlementService(TenantModuleRepository tenantModuleRepository,
                              ModuleRepository moduleRepository) {
        this.tenantModuleRepository = tenantModuleRepository;
        this.moduleRepository = moduleRepository;
    }

    /**
     * Returns true if the tenant has at least one active module that enables
     * the given boolean entitlement key with value = true.
     */
    public boolean isEnabled(UUID tenantId, String entitlementKey) {
        List<String> keys = activeModuleKeys(tenantId);
        if (keys.isEmpty()) return false;

        return moduleRepository.findByModuleKeyIn(keys).stream()
                .flatMap(m -> m.getEntitlements().stream())
                .filter(e -> entitlementKey.equals(e.getEntitlementKey()))
                .anyMatch(e -> Boolean.TRUE.equals(e.getBoolValue()));
    }

    /**
     * Returns the sum of quota values for the given entitlement key across all
     * active modules. Returns 0 when no active subscription provides the quota.
     */
    public int getQuota(UUID tenantId, String entitlementKey) {
        List<String> keys = activeModuleKeys(tenantId);
        if (keys.isEmpty()) return 0;

        return moduleRepository.findByModuleKeyIn(keys).stream()
                .flatMap(m -> m.getEntitlements().stream())
                .filter(e -> entitlementKey.equals(e.getEntitlementKey()) && e.getQuotaValue() != null)
                .mapToInt(ModuleEntitlement::getQuotaValue)
                .sum();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<String> activeModuleKeys(UUID tenantId) {
        return tenantModuleRepository
                .findByTenantIdAndStatusIn(tenantId, List.of(TenantModuleStatus.ACTIVE, TenantModuleStatus.TRIAL))
                .stream()
                .map(TenantModule::getModuleKey)
                .toList();
    }
}
