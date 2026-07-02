package it.cinofilo.tenancy;

import java.time.Instant;
import java.util.UUID;

public record TenantSummaryResponse(
        UUID id,
        String name,
        String slug,
        String type,
        Integer capacityBoxes,
        Instant createdAt
) {
    public static TenantSummaryResponse from(Tenant tenant) {
        return new TenantSummaryResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getType(),
                tenant.getCapacityBoxes(),
                tenant.getCreatedAt()
        );
    }
}
