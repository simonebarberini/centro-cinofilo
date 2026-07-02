package it.cinofilo.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailTokenRepository extends JpaRepository<EmailToken, UUID> {

    Optional<EmailToken> findByTokenAndType(String token, EmailTokenType type);

    @Modifying
    @Query("UPDATE EmailToken t SET t.used = true WHERE t.user.id = :userId AND t.type = :type AND t.used = false")
    void invalidateAllForUser(UUID userId, EmailTokenType type);
}
