package it.cinofilo.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    
    boolean existsByTenantIdAndUsername(UUID tenantId, String username);
    
    Optional<AppUser> findByTenantIdAndUsername(UUID tenantId, String username);
    
    Optional<AppUser> findByUsername(String username);
}
