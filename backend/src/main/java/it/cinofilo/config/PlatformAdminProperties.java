package it.cinofilo.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.validation.annotation.Validated;

/**
 * Configurazione del bootstrap dell'utente amministratore di piattaforma (ADMIN_APP).
 *
 * Il tenant qui configurato è un tenant TECNICO di sistema, non un tenant demo:
 * esiste esclusivamente per soddisfare il vincolo NOT NULL di {@code AppUser.tenant}
 * e il flusso di login esistente (tenant-scoped), senza alcun dato applicativo
 * (clienti, cani, prenotazioni, moduli) associato.
 *
 * Credenziali (username, email, password) non hanno valore di default in
 * application.yml: come JWT_SECRET, devono essere esplicitamente configurate
 * (env var o profilo), altrimenti l'applicazione non si avvia. Valori di comodo
 * per lo sviluppo locale sono definiti in application-dev.yml.
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "platform-admin")
public class PlatformAdminProperties {

    @Valid
    @NestedConfigurationProperty
    private Tenant tenant = new Tenant();

    @NotBlank(message =
        "PLATFORM_ADMIN_USERNAME is required. " +
        "Set platform-admin.username (env var PLATFORM_ADMIN_USERNAME).")
    private String username;

    @NotBlank(message =
        "PLATFORM_ADMIN_EMAIL is required. " +
        "Set platform-admin.email (env var PLATFORM_ADMIN_EMAIL).")
    @Email(message = "platform-admin.email deve essere un indirizzo email valido")
    private String email;

    @NotBlank(message =
        "PLATFORM_ADMIN_PASSWORD is required. " +
        "Set platform-admin.password (env var PLATFORM_ADMIN_PASSWORD).")
    private String password;

    @Getter
    @Setter
    public static class Tenant {

        @NotBlank(message = "platform-admin.tenant.name è obbligatorio")
        private String name = "Piattaforma";

        @NotBlank(message = "platform-admin.tenant.slug è obbligatorio")
        private String slug = "platform";
    }
}
