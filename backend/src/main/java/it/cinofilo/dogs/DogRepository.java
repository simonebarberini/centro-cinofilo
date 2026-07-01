package it.cinofilo.dogs;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Dog entity with tenant-isolated queries.
 */
@Repository
public interface DogRepository extends JpaRepository<Dog, UUID> {

    /**
     * Find all dogs for a specific tenant.
     */
    List<Dog> findAllByTenantId(UUID tenantId);

    /**
     * Find all dogs for a specific customer within a tenant.
     */
    List<Dog> findAllByTenantIdAndCustomerId(UUID tenantId, UUID customerId);

    /**
     * Find a dog by ID within a specific tenant.
     */
    Optional<Dog> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Check if a dog exists by ID within a specific tenant.
     */
    boolean existsByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Count all dogs for a specific tenant. Used for quota enforcement.
     */
    long countByTenantId(UUID tenantId);
}
