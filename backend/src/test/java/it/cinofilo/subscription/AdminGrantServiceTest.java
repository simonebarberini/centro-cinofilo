package it.cinofilo.subscription;

import it.cinofilo.catalog.ModuleNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AdminGrantServiceTest {

    private SubscriptionService subscriptionService;
    private AdminGrantRepository adminGrantRepository;
    private AdminGrantService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final String MODULE_KEY = "reports";
    private static final String GRANTED_BY = "admin@cinofilo.it";

    @BeforeEach
    void setUp() {
        subscriptionService = mock(SubscriptionService.class);
        adminGrantRepository = mock(AdminGrantRepository.class);
        service = new AdminGrantService(subscriptionService, adminGrantRepository);
        when(adminGrantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void grant_activatesSubscriptionAndSavesGrant() {
        when(subscriptionService.activate(TENANT_ID, MODULE_KEY))
                .thenReturn(TenantModule.forActivation(TENANT_ID, MODULE_KEY));

        AdminGrant result = service.grant(TENANT_ID, MODULE_KEY, GRANTED_BY, "beta access");

        verify(subscriptionService).activate(TENANT_ID, MODULE_KEY);
        verify(adminGrantRepository).save(any(AdminGrant.class));
        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(result.getModuleKey()).isEqualTo(MODULE_KEY);
        assertThat(result.getGrantedBy()).isEqualTo(GRANTED_BY);
        assertThat(result.getNote()).isEqualTo("beta access");
    }

    @Test
    void grant_withNullNote_savesGrantWithoutNote() {
        when(subscriptionService.activate(TENANT_ID, MODULE_KEY))
                .thenReturn(TenantModule.forActivation(TENANT_ID, MODULE_KEY));

        AdminGrant result = service.grant(TENANT_ID, MODULE_KEY, GRANTED_BY, null);

        assertThat(result.getNote()).isNull();
    }

    @Test
    void grant_propagatesModuleNotFoundException_andDoesNotSaveGrant() {
        doThrow(new ModuleNotFoundException(MODULE_KEY))
                .when(subscriptionService).activate(TENANT_ID, MODULE_KEY);

        assertThatThrownBy(() -> service.grant(TENANT_ID, MODULE_KEY, GRANTED_BY, null))
                .isInstanceOf(ModuleNotFoundException.class);
        verify(adminGrantRepository, never()).save(any());
    }

    @Test
    void grant_propagatesModuleNotActivatableException_andDoesNotSaveGrant() {
        doThrow(new ModuleNotActivatableException(MODULE_KEY))
                .when(subscriptionService).activate(TENANT_ID, MODULE_KEY);

        assertThatThrownBy(() -> service.grant(TENANT_ID, MODULE_KEY, GRANTED_BY, null))
                .isInstanceOf(ModuleNotActivatableException.class);
        verify(adminGrantRepository, never()).save(any());
    }

    @Test
    void grant_propagatesSubscriptionConflictException_andDoesNotSaveGrant() {
        doThrow(new SubscriptionConflictException(TENANT_ID, MODULE_KEY))
                .when(subscriptionService).activate(TENANT_ID, MODULE_KEY);

        assertThatThrownBy(() -> service.grant(TENANT_ID, MODULE_KEY, GRANTED_BY, null))
                .isInstanceOf(SubscriptionConflictException.class);
        verify(adminGrantRepository, never()).save(any());
    }

    @Test
    void findByTenant_delegatesToRepository() {
        List<AdminGrant> grants = List.of(
                AdminGrant.builder().tenantId(TENANT_ID).moduleKey(MODULE_KEY)
                        .grantedBy(GRANTED_BY).build()
        );
        when(adminGrantRepository.findByTenantId(TENANT_ID)).thenReturn(grants);

        List<AdminGrant> result = service.findByTenant(TENANT_ID);

        assertThat(result).isEqualTo(grants);
        verify(adminGrantRepository).findByTenantId(TENANT_ID);
    }
}
