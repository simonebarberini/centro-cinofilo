package it.cinofilo.tenancy;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    
    Optional<Tenant> findByName(String name);
    
    Optional<Tenant> findBySlug(String slug);
    
    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    /**
     * Full-text search on name and slug (case-insensitive substring match).
     * Used by the platform admin backoffice to filter tenants.
     */
    List<Tenant> findByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(String name, String slug);

    /**
     * Loads a tenant while acquiring a pessimistic write lock
     * ({@code SELECT ... FOR UPDATE}) on its row.
     *
     * <p>Used exclusively by {@link it.cinofilo.bookings.TenantCapacityGuard} to
     * serialize occupancy-changing operations per tenant. Must be called inside
     * an active transaction; the lock is held until that transaction commits.
     *
     * @param id the tenant ID
     * @return the locked tenant, if present
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tenant t where t.id = :id")
    Optional<Tenant> findByIdForUpdate(@Param("id") UUID id);
}
