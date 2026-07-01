package it.cinofilo.subscription;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminGrantRepository extends JpaRepository<AdminGrant, UUID> {
}
