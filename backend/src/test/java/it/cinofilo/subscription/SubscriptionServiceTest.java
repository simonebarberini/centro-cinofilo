package it.cinofilo.subscription;

import it.cinofilo.catalog.ModuleNotFoundException;
import it.cinofilo.catalog.ModuleResponse;
import it.cinofilo.catalog.ModuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SubscriptionServiceTest {

    private TenantModuleRepository repository;
    private ModuleService moduleService;
    private SubscriptionService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final String MODULE_KEY = "reports";
    private static final Instant TRIAL_ENDS = Instant.now().plusSeconds(86_400 * 14);

    @BeforeEach
    void setUp() {
        repository = mock(TenantModuleRepository.class);
        moduleService = mock(ModuleService.class);
        service = new SubscriptionService(repository, moduleService);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── activate ─────────────────────────────────────────────────────────────

    @Test
    void activate_createsNewActiveSubscription_whenNoneExists() {
        givenModuleActive();
        when(repository.findByTenantIdAndModuleKey(TENANT_ID, MODULE_KEY)).thenReturn(Optional.empty());

        TenantModule result = service.activate(TENANT_ID, MODULE_KEY);

        assertThat(result.getStatus()).isEqualTo(TenantModuleStatus.ACTIVE);
        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(result.getModuleKey()).isEqualTo(MODULE_KEY);
        assertThat(result.getTrialEndsAt()).isNull();
        verify(repository).save(any(TenantModule.class));
    }

    @Test
    void activate_reactivatesCancelledSubscription() {
        givenModuleActive();
        TenantModule cancelled = existingWith(TenantModuleStatus.CANCELLED);
        when(repository.findByTenantIdAndModuleKey(TENANT_ID, MODULE_KEY)).thenReturn(Optional.of(cancelled));

        TenantModule result = service.activate(TENANT_ID, MODULE_KEY);

        assertThat(result.getStatus()).isEqualTo(TenantModuleStatus.ACTIVE);
        assertThat(result.getTrialEndsAt()).isNull();
        verify(repository).save(cancelled);
    }

    @Test
    void activate_throwsConflict_whenAlreadyActive() {
        givenModuleActive();
        when(repository.findByTenantIdAndModuleKey(TENANT_ID, MODULE_KEY))
                .thenReturn(Optional.of(existingWith(TenantModuleStatus.ACTIVE)));

        assertThatThrownBy(() -> service.activate(TENANT_ID, MODULE_KEY))
                .isInstanceOf(SubscriptionConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void activate_throwsConflict_whenAlreadyTrial() {
        givenModuleActive();
        when(repository.findByTenantIdAndModuleKey(TENANT_ID, MODULE_KEY))
                .thenReturn(Optional.of(existingWith(TenantModuleStatus.TRIAL)));

        assertThatThrownBy(() -> service.activate(TENANT_ID, MODULE_KEY))
                .isInstanceOf(SubscriptionConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void activate_throwsModuleNotActivatable_whenModuleInactive() {
        givenModuleWithStatus("INACTIVE");
        when(repository.findByTenantIdAndModuleKey(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activate(TENANT_ID, MODULE_KEY))
                .isInstanceOf(ModuleNotActivatableException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void activate_propagatesModuleNotFoundException_whenModuleAbsent() {
        when(moduleService.findByKey(MODULE_KEY)).thenThrow(new ModuleNotFoundException(MODULE_KEY));

        assertThatThrownBy(() -> service.activate(TENANT_ID, MODULE_KEY))
                .isInstanceOf(ModuleNotFoundException.class);
        verify(repository, never()).save(any());
    }

    // ── startTrial ────────────────────────────────────────────────────────────

    @Test
    void startTrial_createsNewTrialSubscription_whenNoneExists() {
        givenModuleActive();
        when(repository.findByTenantIdAndModuleKey(TENANT_ID, MODULE_KEY)).thenReturn(Optional.empty());

        TenantModule result = service.startTrial(TENANT_ID, MODULE_KEY, TRIAL_ENDS);

        assertThat(result.getStatus()).isEqualTo(TenantModuleStatus.TRIAL);
        assertThat(result.getTrialEndsAt()).isEqualTo(TRIAL_ENDS);
        verify(repository).save(any(TenantModule.class));
    }

    @Test
    void startTrial_restartsTrialFromCancelled() {
        givenModuleActive();
        TenantModule cancelled = existingWith(TenantModuleStatus.CANCELLED);
        when(repository.findByTenantIdAndModuleKey(TENANT_ID, MODULE_KEY)).thenReturn(Optional.of(cancelled));

        TenantModule result = service.startTrial(TENANT_ID, MODULE_KEY, TRIAL_ENDS);

        assertThat(result.getStatus()).isEqualTo(TenantModuleStatus.TRIAL);
        assertThat(result.getTrialEndsAt()).isEqualTo(TRIAL_ENDS);
        verify(repository).save(cancelled);
    }

    @Test
    void startTrial_throwsConflict_whenAlreadyActive() {
        givenModuleActive();
        when(repository.findByTenantIdAndModuleKey(TENANT_ID, MODULE_KEY))
                .thenReturn(Optional.of(existingWith(TenantModuleStatus.ACTIVE)));

        assertThatThrownBy(() -> service.startTrial(TENANT_ID, MODULE_KEY, TRIAL_ENDS))
                .isInstanceOf(SubscriptionConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void startTrial_throwsModuleNotActivatable_whenModuleDeprecated() {
        givenModuleWithStatus("DEPRECATED");

        assertThatThrownBy(() -> service.startTrial(TENANT_ID, MODULE_KEY, TRIAL_ENDS))
                .isInstanceOf(ModuleNotActivatableException.class);
        verify(repository, never()).save(any());
    }

    // ── cancel ────────────────────────────────────────────────────────────────

    @Test
    void cancel_setsStatusCancelled_whenActive() {
        TenantModule active = existingWith(TenantModuleStatus.ACTIVE);
        when(repository.findByTenantIdAndModuleKey(TENANT_ID, MODULE_KEY)).thenReturn(Optional.of(active));

        service.cancel(TENANT_ID, MODULE_KEY);

        assertThat(active.getStatus()).isEqualTo(TenantModuleStatus.CANCELLED);
        assertThat(active.getTrialEndsAt()).isNull();
        verify(repository).save(active);
    }

    @Test
    void cancel_setsStatusCancelled_whenTrial() {
        TenantModule trial = existingWith(TenantModuleStatus.TRIAL);
        trial.setTrialEndsAt(TRIAL_ENDS);
        when(repository.findByTenantIdAndModuleKey(TENANT_ID, MODULE_KEY)).thenReturn(Optional.of(trial));

        service.cancel(TENANT_ID, MODULE_KEY);

        assertThat(trial.getStatus()).isEqualTo(TenantModuleStatus.CANCELLED);
        assertThat(trial.getTrialEndsAt()).isNull();
        verify(repository).save(trial);
    }

    @Test
    void cancel_isIdempotent_whenAlreadyCancelled() {
        when(repository.findByTenantIdAndModuleKey(TENANT_ID, MODULE_KEY))
                .thenReturn(Optional.of(existingWith(TenantModuleStatus.CANCELLED)));

        service.cancel(TENANT_ID, MODULE_KEY);

        verify(repository, never()).save(any());
    }

    @Test
    void cancel_isIdempotent_whenSubscriptionNotFound() {
        when(repository.findByTenantIdAndModuleKey(TENANT_ID, MODULE_KEY)).thenReturn(Optional.empty());

        service.cancel(TENANT_ID, MODULE_KEY);

        verify(repository, never()).save(any());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void givenModuleActive() {
        givenModuleWithStatus("ACTIVE");
    }

    private void givenModuleWithStatus(String activationStatus) {
        when(moduleService.findByKey(MODULE_KEY)).thenReturn(
                new ModuleResponse(MODULE_KEY, "Reports", null, "OPTIONAL", activationStatus, List.of())
        );
    }

    private TenantModule existingWith(TenantModuleStatus status) {
        return TenantModule.builder()
                .id(UUID.randomUUID())
                .tenantId(TENANT_ID)
                .moduleKey(MODULE_KEY)
                .status(status)
                .build();
    }
}
