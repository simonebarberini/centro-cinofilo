package it.cinofilo.subscription;

import java.util.UUID;

public class SubscriptionConflictException extends RuntimeException {

    public SubscriptionConflictException(UUID tenantId, String moduleKey) {
        super("Tenant '" + tenantId + "' already has an active subscription for module '" + moduleKey + "'");
    }
}
