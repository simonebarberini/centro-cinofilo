package it.cinofilo.tenancy;

import java.time.Instant;
import java.util.UUID;

public record TenantDetailResponse(
        UUID id,
        String name,
        String slug,
        String type,
        Integer capacityBoxes,
        String billingEmail,
        Instant createdAt
) {
    public static TenantDetailResponse from(Tenant tenant) {
        return new TenantDetailResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getType(),
                tenant.getCapacityBoxes(),
                tenant.getBillingEmail(),
                tenant.getCreatedAt()
        );
    }
}
