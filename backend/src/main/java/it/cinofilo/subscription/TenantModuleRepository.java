package it.cinofilo.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantModuleRepository extends JpaRepository<TenantModule, UUID> {

    Optional<TenantModule> findByTenantIdAndModuleKey(UUID tenantId, String moduleKey);

    List<TenantModule> findByTenantIdAndStatusIn(UUID tenantId, Collection<TenantModuleStatus> statuses);
}
