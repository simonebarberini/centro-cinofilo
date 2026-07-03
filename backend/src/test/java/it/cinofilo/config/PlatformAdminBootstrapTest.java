package it.cinofilo.config;

import it.cinofilo.tenancy.Role;
import it.cinofilo.tenancy.Tenant;
import it.cinofilo.tenancy.TenantRepository;
import it.cinofilo.users.AppUser;
import it.cinofilo.users.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PlatformAdminBootstrapTest {

    private TenantRepository tenantRepository;
    private AppUserRepository appUserRepository;
    private PasswordEncoder passwordEncoder;
    private PlatformAdminProperties properties;
    private PlatformAdminBootstrap bootstrap;

    private static final UUID PLATFORM_TENANT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        tenantRepository = mock(TenantRepository.class);
        appUserRepository = mock(AppUserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);

        properties = new PlatformAdminProperties();
        properties.getTenant().setName("Piattaforma");
        properties.getTenant().setSlug("platform");
        properties.setUsername("admin");
        properties.setEmail("admin@platform.local");
        properties.setPassword("SuperSecret1!");

        bootstrap = new PlatformAdminBootstrap(tenantRepository, appUserRepository, properties);

        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void bootstrap_whenTenantAndAdminDoNotExist_createsBoth() {
        when(tenantRepository.findBySlug("platform")).thenReturn(Optional.empty());
        Tenant savedTenant = Tenant.builder().id(PLATFORM_TENANT_ID).name("Piattaforma").slug("platform").build();
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);
        when(appUserRepository.existsByTenantIdAndUsername(PLATFORM_TENANT_ID, "admin")).thenReturn(false);
        when(passwordEncoder.encode("SuperSecret1!")).thenReturn("hashed-password");

        bootstrap.bootstrap(passwordEncoder);

        verify(tenantRepository).save(argThat(t ->
                t.getName().equals("Piattaforma")
                        && t.getSlug().equals("platform")
                        && t.getType().equals("PLATFORM")
                        && t.getCapacityBoxes() == 0));

        verify(appUserRepository).save(argThat((AppUser u) ->
                u.getUsername().equals("admin")
                        && u.getEmail().equals("admin@platform.local")
                        && u.getPasswordHash().equals("hashed-password")
                        && u.getRole() == Role.ADMIN_APP
                        && u.getEnabled()
                        && u.getEmailVerified()
                        && u.getTenant().getId().equals(PLATFORM_TENANT_ID)));
    }

    @Test
    void bootstrap_whenTenantExistsButAdminDoesNot_reusesTenantAndCreatesAdmin() {
        Tenant existingTenant = Tenant.builder().id(PLATFORM_TENANT_ID).name("Piattaforma").slug("platform").build();
        when(tenantRepository.findBySlug("platform")).thenReturn(Optional.of(existingTenant));
        when(appUserRepository.existsByTenantIdAndUsername(PLATFORM_TENANT_ID, "admin")).thenReturn(false);
        when(passwordEncoder.encode("SuperSecret1!")).thenReturn("hashed-password");

        bootstrap.bootstrap(passwordEncoder);

        verify(tenantRepository, never()).save(any(Tenant.class));
        verify(appUserRepository).save(any(AppUser.class));
    }

    @Test
    void bootstrap_whenAdminAlreadyExists_isNoop() {
        Tenant existingTenant = Tenant.builder().id(PLATFORM_TENANT_ID).name("Piattaforma").slug("platform").build();
        when(tenantRepository.findBySlug("platform")).thenReturn(Optional.of(existingTenant));
        when(appUserRepository.existsByTenantIdAndUsername(PLATFORM_TENANT_ID, "admin")).thenReturn(true);

        bootstrap.bootstrap(passwordEncoder);

        verify(tenantRepository, never()).save(any(Tenant.class));
        verify(appUserRepository, never()).save(any(AppUser.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void bootstrap_isIdempotent_runningTwiceOnlyCreatesOnce() {
        when(tenantRepository.findBySlug("platform")).thenReturn(Optional.empty());
        Tenant savedTenant = Tenant.builder().id(PLATFORM_TENANT_ID).name("Piattaforma").slug("platform").build();
        when(tenantRepository.save(any(Tenant.class))).thenReturn(savedTenant);
        when(appUserRepository.existsByTenantIdAndUsername(PLATFORM_TENANT_ID, "admin"))
                .thenReturn(false)
                .thenReturn(true);
        when(passwordEncoder.encode("SuperSecret1!")).thenReturn("hashed-password");

        bootstrap.bootstrap(passwordEncoder);
        bootstrap.bootstrap(passwordEncoder);

        verify(appUserRepository, times(1)).save(any(AppUser.class));
    }
}
