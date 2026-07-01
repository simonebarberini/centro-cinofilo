package it.cinofilo.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TenantModuleRepository extends JpaRepository<TenantModule, UUID> {

    Optional<TenantModule> findByTenantIdAndModuleKey(UUID tenantId, String moduleKey);
}
