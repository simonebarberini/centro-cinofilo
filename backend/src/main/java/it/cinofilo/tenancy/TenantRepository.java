package it.cinofilo.tenancy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    
    Optional<Tenant> findByName(String name);
    
    Optional<Tenant> findBySlug(String slug);
    
    boolean existsByName(String name);
}
