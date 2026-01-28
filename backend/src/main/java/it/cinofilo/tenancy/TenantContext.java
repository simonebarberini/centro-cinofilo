package it.cinofilo.tenancy;

import java.util.UUID;

/**
 * ThreadLocal-based context for storing the current tenant ID during request processing.
 * Ensures each request is isolated and tenant information is accessible throughout the request lifecycle.
 */
public class TenantContext {

    private static final ThreadLocal<UUID> tenantIdHolder = new ThreadLocal<>();

    /**
     * Set the tenant ID for the current thread.
     *
     * @param tenantId the UUID of the current tenant
     */
    public static void setTenantId(UUID tenantId) {
        tenantIdHolder.set(tenantId);
    }

    /**
     * Get the tenant ID for the current thread.
     *
     * @return the UUID of the current tenant, or null if not set
     */
    public static UUID getTenantId() {
        return tenantIdHolder.get();
    }

    /**
     * Clear the tenant ID from the current thread.
     * Should be called in a finally block to prevent memory leaks.
     */
    public static void clear() {
        tenantIdHolder.remove();
    }
}
