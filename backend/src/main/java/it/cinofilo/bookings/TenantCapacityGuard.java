package it.cinofilo.bookings;

import it.cinofilo.tenancy.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Domain entry point for any operation that changes a tenant's box occupancy.
 *
 * <p>The capacity invariant — for every day, the number of {@code CONFIRMED}
 * bookings overlapping that day must not exceed {@code tenant.capacityBoxes} —
 * can only be upheld if the "read current occupancy, then write" sequence runs
 * atomically per tenant. Otherwise two concurrent requests can both observe
 * free capacity and both persist, overbooking the centre.
 *
 * <p>This component is the <strong>single authorized place</strong> that
 * serializes those sequences: it acquires a pessimistic write lock on the
 * tenant row and executes the supplied operation while holding it, so requests
 * for the same tenant are processed one at a time. Different tenants never
 * contend with each other.
 *
 * <p><strong>Every</strong> operation that may change occupancy — creating a
 * booking, moving its dates, confirming or cancelling it, and any future
 * feature such as group, recurring or waiting-list bookings — MUST run inside
 * {@link #executeForTenant(UUID, Supplier)}. Bypassing this guard re-introduces
 * the race condition it exists to prevent.
 *
 * <p>The method requires an already-active transaction
 * ({@link Propagation#MANDATORY}): the lock is only meaningful until the
 * surrounding transaction commits, so invoking it without a transaction is a
 * programming error and fails fast.
 */
@Component
@RequiredArgsConstructor
public class TenantCapacityGuard {

    private final TenantRepository tenantRepository;

    /**
     * Executes {@code operation} for the given tenant while holding the
     * per-tenant capacity lock, making the enclosed capacity check and write
     * atomic with respect to other capacity-changing operations on the same
     * tenant.
     *
     * @param tenantId  the tenant whose occupancy is being modified
     * @param operation the occupancy-changing operation (typically a capacity
     *                  check followed by a persist)
     * @param <T>       the operation result type
     * @return the value produced by {@code operation}
     * @throws IllegalStateException if the tenant does not exist
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> T executeForTenant(UUID tenantId, Supplier<T> operation) {
        tenantRepository.findByIdForUpdate(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found: " + tenantId));
        return operation.get();
    }
}
