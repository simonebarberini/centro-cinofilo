package it.cinofilo.config;

import it.cinofilo.tenancy.Role;
import it.cinofilo.tenancy.Tenant;
import it.cinofilo.tenancy.TenantRepository;
import it.cinofilo.users.AppUser;
import it.cinofilo.users.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder {

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;

    @Bean
    @Profile("dev")
    public CommandLineRunner seedDevData(PasswordEncoder passwordEncoder) {
        return args -> {
            log.info("🌱 Starting DEV data seeding...");

            // Verifica se esistono già tenant
            if (tenantRepository.count() > 0) {
                log.info("✅ Tenants already exist. Skipping seed.");
                return;
            }

            // Crea tenant demo
            Tenant demoTenant = Tenant.builder()
                    .name("Demo Centro")
                    .type("PENSIONE")
                    .capacityBoxes(10)
                    .build();
            
            demoTenant = tenantRepository.save(demoTenant);
            log.info("✅ Created demo tenant: {} (ID: {})", demoTenant.getName(), demoTenant.getId());

            // Crea utente owner
            String rawPassword = "owner123!";
            String hashedPassword = passwordEncoder.encode(rawPassword);

            AppUser ownerUser = AppUser.builder()
                    .tenant(demoTenant)
                    .username("owner")
                    .passwordHash(hashedPassword)
                    .role(Role.TENANT_OWNER)
                    .enabled(true)
                    .build();

            ownerUser = appUserRepository.save(ownerUser);
            log.info("✅ Created owner user: {} (Role: {}, Tenant: {})", 
                    ownerUser.getUsername(), 
                    ownerUser.getRole(), 
                    demoTenant.getName());

            log.info("🎉 DEV data seeding completed successfully!");
            log.info("📝 Login credentials - username: owner, password: owner123!");
        };
    }
}
