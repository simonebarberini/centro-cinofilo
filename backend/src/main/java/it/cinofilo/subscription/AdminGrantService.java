package it.cinofilo.subscription;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdminGrantService {

    private final SubscriptionService subscriptionService;
    private final AdminGrantRepository adminGrantRepository;

    public AdminGrantService(SubscriptionService subscriptionService,
                             AdminGrantRepository adminGrantRepository) {
        this.subscriptionService = subscriptionService;
        this.adminGrantRepository = adminGrantRepository;
    }

    @Transactional
    public AdminGrant grant(UUID tenantId, String moduleKey, String grantedBy, String note) {
        subscriptionService.activate(tenantId, moduleKey);
        return adminGrantRepository.save(AdminGrant.builder()
                .tenantId(tenantId)
                .moduleKey(moduleKey)
                .grantedBy(grantedBy)
                .note(note)
                .build());
    }

    @Transactional(readOnly = true)
    public List<AdminGrant> findByTenant(UUID tenantId) {
        return adminGrantRepository.findByTenantId(tenantId);
    }
}
