package it.cinofilo.customer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    /**
     * Find all customers for a specific tenant.
     *
     * @param tenantId the tenant ID
     * @return list of customers for the tenant
     */
    List<Customer> findAllByTenantId(UUID tenantId);

    /**
     * Find a customer by ID and tenant ID for secure tenant-scoped access.
     *
     * @param id the customer ID
     * @param tenantId the tenant ID
     * @return the customer if found and belongs to the tenant
     */
    Optional<Customer> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Check if a customer exists by ID and tenant ID.
     *
     * @param id the customer ID
     * @param tenantId the tenant ID
     * @return true if customer exists in the tenant
     */
    boolean existsByIdAndTenantId(UUID id, UUID tenantId);
}
