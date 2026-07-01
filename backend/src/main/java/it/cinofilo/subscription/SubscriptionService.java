package it.cinofilo.subscription;

import it.cinofilo.catalog.ModuleResponse;
import it.cinofilo.catalog.ModuleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class SubscriptionService {

    private final TenantModuleRepository tenantModuleRepository;
    private final ModuleService moduleService;

    public SubscriptionService(TenantModuleRepository tenantModuleRepository,
                               ModuleService moduleService) {
        this.tenantModuleRepository = tenantModuleRepository;
        this.moduleService = moduleService;
    }

    public TenantModule activate(UUID tenantId, String moduleKey) {
        assertModuleActivatable(moduleKey);

        Optional<TenantModule> existing = tenantModuleRepository.findByTenantIdAndModuleKey(tenantId, moduleKey);
        if (existing.isPresent()) {
            TenantModule tm = existing.get();
            if (tm.getStatus() == TenantModuleStatus.ACTIVE || tm.getStatus() == TenantModuleStatus.TRIAL) {
                throw new SubscriptionConflictException(tenantId, moduleKey);
            }
            tm.activate();
            return tenantModuleRepository.save(tm);
        }

        return tenantModuleRepository.save(TenantModule.forActivation(tenantId, moduleKey));
    }

    public TenantModule startTrial(UUID tenantId, String moduleKey, Instant trialEndsAt) {
        assertModuleActivatable(moduleKey);

        Optional<TenantModule> existing = tenantModuleRepository.findByTenantIdAndModuleKey(tenantId, moduleKey);
        if (existing.isPresent()) {
            TenantModule tm = existing.get();
            if (tm.getStatus() == TenantModuleStatus.ACTIVE || tm.getStatus() == TenantModuleStatus.TRIAL) {
                throw new SubscriptionConflictException(tenantId, moduleKey);
            }
            tm.startTrial(trialEndsAt);
            return tenantModuleRepository.save(tm);
        }

        return tenantModuleRepository.save(TenantModule.forTrial(tenantId, moduleKey, trialEndsAt));
    }

    public void cancel(UUID tenantId, String moduleKey) {
        tenantModuleRepository.findByTenantIdAndModuleKey(tenantId, moduleKey).ifPresent(tm -> {
            if (tm.getStatus() != TenantModuleStatus.CANCELLED) {
                tm.cancel();
                tenantModuleRepository.save(tm);
            }
        });
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void assertModuleActivatable(String moduleKey) {
        ModuleResponse module = moduleService.findByKey(moduleKey);
        if (!"ACTIVE".equals(module.activationStatus())) {
            throw new ModuleNotActivatableException(moduleKey);
        }
    }
}
