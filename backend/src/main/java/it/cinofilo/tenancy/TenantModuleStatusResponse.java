package it.cinofilo.tenancy;

import it.cinofilo.subscription.TenantModule;
import it.cinofilo.subscription.TenantModuleStatus;

import java.time.Instant;

public record TenantModuleStatusResponse(
        String moduleKey,
        TenantModuleStatus status,
        Instant trialEndsAt
) {
    public static TenantModuleStatusResponse from(TenantModule tenantModule) {
        return new TenantModuleStatusResponse(
                tenantModule.getModuleKey(),
                tenantModule.getStatus(),
                tenantModule.getTrialEndsAt()
        );
    }
}
