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
     * Throws an exception if the tenant context is not set.
     *
     * @return the UUID of the current tenant
     * @throws IllegalStateException if tenant context is not set
     */
    public static UUID getTenantId() {
        UUID tenantId = tenantIdHolder.get();
        if (tenantId == null) {
            throw new IllegalStateException("TenantContext not set");
        }
        return tenantId;
    }

    /**
     * Get the tenant ID for the current thread, or null if not set.
     * Use this method when you need to handle cases where tenant context may not be available.
     *
     * @return the UUID of the current tenant, or null if not set
     */
    public static UUID getTenantIdOrNull() {
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
