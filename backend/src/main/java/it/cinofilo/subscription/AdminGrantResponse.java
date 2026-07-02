package it.cinofilo.subscription;

import java.time.Instant;
import java.util.UUID;

public record AdminGrantResponse(
        UUID id,
        UUID tenantId,
        String moduleKey,
        String grantedBy,
        String note,
        Instant createdAt
) {
    static AdminGrantResponse from(AdminGrant g) {
        return new AdminGrantResponse(
                g.getId(),
                g.getTenantId(),
                g.getModuleKey(),
                g.getGrantedBy(),
                g.getNote(),
                g.getCreatedAt()
        );
    }
}
