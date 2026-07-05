package it.cinofilo.config;

import it.cinofilo.tenancy.Role;
import it.cinofilo.tenancy.Tenant;
import it.cinofilo.tenancy.TenantRepository;
import it.cinofilo.users.AppUser;
import it.cinofilo.users.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bootstrap idempotente dell'unico utente amministratore di piattaforma (ADMIN_APP)
 * necessario per accedere al Platform Backoffice.
 *
 * Crea, solo se non esiste già, un tenant TECNICO di sistema (nessun dato applicativo:
 * nessun cliente, cane, prenotazione o modulo) a cui agganciare l'utente ADMIN_APP —
 * necessario perché {@code AppUser.tenant} è oggi una relazione obbligatoria (vedi ADR-017).
 *
 * Non crea nessun altro dato: né tenant demo, né clienti, né cani, né prenotazioni.
 * Riusa {@link TenantRepository} e {@link AppUserRepository} così come sono, senza
 * bypassare la logica di dominio esistente.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class PlatformAdminBootstrap {

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;
    private final PlatformAdminProperties platformAdminProperties;

    @Bean
    public ApplicationRunner platformAdminBootstrapRunner(PasswordEncoder passwordEncoder) {
        return (ApplicationArguments args) -> bootstrap(passwordEncoder);
    }

    @Transactional
    void bootstrap(PasswordEncoder passwordEncoder) {
        String tenantSlug = platformAdminProperties.getTenant().getSlug();

        Tenant platformTenant = tenantRepository.findBySlug(tenantSlug)
                .orElseGet(() -> {
                    Tenant created = tenantRepository.save(Tenant.builder()
                            .name(platformAdminProperties.getTenant().getName())
                            .type("PLATFORM")
                            .slug(tenantSlug)
                            .capacityBoxes(0)
                            .build());
                    log.info("Created technical platform tenant '{}' (slug={})",
                            created.getName(), created.getSlug());
                    return created;
                });

        String username = platformAdminProperties.getUsername();

        if (appUserRepository.existsByTenantIdAndUsername(platformTenant.getId(), username)) {
            log.info("Platform admin user '{}' already exists. Skipping bootstrap.", username);
            return;
        }

        AppUser admin = appUserRepository.save(AppUser.builder()
                .tenant(platformTenant)
                .username(username)
                .passwordHash(passwordEncoder.encode(platformAdminProperties.getPassword()))
                .email(platformAdminProperties.getEmail())
                .role(Role.ADMIN_APP)
                .enabled(true)
                .emailVerified(true)
                .build());

        log.info("Created platform admin user '{}' (role={})", admin.getUsername(), admin.getRole());
    }
}
