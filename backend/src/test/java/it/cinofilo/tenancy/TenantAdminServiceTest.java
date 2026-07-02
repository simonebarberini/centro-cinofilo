package it.cinofilo.tenancy;

import it.cinofilo.subscription.TenantModule;
import it.cinofilo.subscription.TenantModuleRepository;
import it.cinofilo.subscription.TenantModuleStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;

class TenantAdminServiceTest {

    private TenantRepository tenantRepository;
    private TenantModuleRepository tenantModuleRepository;
    private TenantAdminService service;

    private static final UUID TENANT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        tenantRepository = mock(TenantRepository.class);
        tenantModuleRepository = mock(TenantModuleRepository.class);
        service = new TenantAdminService(tenantRepository, tenantModuleRepository);
    }

    // ── search ────────────────────────────────────────────────────────────────

    @Test
    void search_withNullQ_callsFindAll() {
        when(tenantRepository.findAll()).thenReturn(List.of(tenant("Alpha"), tenant("Beta")));

        List<TenantSummaryResponse> result = service.search(null);

        assertThat(result).hasSize(2);
        verify(tenantRepository).findAll();
        verify(tenantRepository, never())
                .findByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(any(), any());
    }

    @Test
    void search_withBlankQ_callsFindAll() {
        when(tenantRepository.findAll()).thenReturn(List.of());

        service.search("   ");

        verify(tenantRepository).findAll();
        verify(tenantRepository, never())
                .findByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(any(), any());
    }

    @Test
    void search_withQ_callsFilteredQuery() {
        when(tenantRepository.findByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(
                eq("centro"), eq("centro")))
                .thenReturn(List.of(tenant("Centro Cinofilo")));

        List<TenantSummaryResponse> result = service.search("centro");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Centro Cinofilo");
        verify(tenantRepository, never()).findAll();
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_tenantExists_returnsDetail() {
        Tenant t = tenant("Centro Test");
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(t));

        TenantDetailResponse result = service.findById(TENANT_ID);

        assertThat(result.name()).isEqualTo("Centro Test");
        assertThat(result.billingEmail()).isNull();
    }

    @Test
    void findById_tenantNotFound_throws404() {
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(TENANT_ID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(NOT_FOUND));
    }

    // ── findModules ───────────────────────────────────────────────────────────

    @Test
    void findModules_tenantNotFound_throws404() {
        when(tenantRepository.existsById(TENANT_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.findModules(TENANT_ID))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(NOT_FOUND));

        verify(tenantModuleRepository, never()).findByTenantIdAndStatusIn(any(), any());
    }

    @Test
    void findModules_returnsActiveAndTrialModules() {
        when(tenantRepository.existsById(TENANT_ID)).thenReturn(true);
        when(tenantModuleRepository.findByTenantIdAndStatusIn(
                eq(TENANT_ID),
                eq(List.of(TenantModuleStatus.ACTIVE, TenantModuleStatus.TRIAL))))
                .thenReturn(List.of(
                        tenantModule("base", TenantModuleStatus.ACTIVE, null),
                        tenantModule("sms", TenantModuleStatus.TRIAL,
                                Instant.now().plusSeconds(3600))));

        List<TenantModuleStatusResponse> result = service.findModules(TENANT_ID);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TenantModuleStatusResponse::moduleKey)
                .containsExactlyInAnyOrder("base", "sms");
        assertThat(result).extracting(TenantModuleStatusResponse::status)
                .containsExactlyInAnyOrder(TenantModuleStatus.ACTIVE, TenantModuleStatus.TRIAL);
    }

    @Test
    void findModules_tenantWithNoActiveModules_returnsEmptyList() {
        when(tenantRepository.existsById(TENANT_ID)).thenReturn(true);
        when(tenantModuleRepository.findByTenantIdAndStatusIn(any(), any()))
                .thenReturn(List.of());

        List<TenantModuleStatusResponse> result = service.findModules(TENANT_ID);

        assertThat(result).isEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Tenant tenant(String name) {
        return Tenant.builder()
                .id(TENANT_ID)
                .name(name)
                .slug("slug-" + name.toLowerCase().replace(" ", "-"))
                .type("PENSIONE")
                .capacityBoxes(10)
                .build();
    }

    private TenantModule tenantModule(String moduleKey, TenantModuleStatus status,
                                      Instant trialEndsAt) {
        return TenantModule.builder()
                .tenantId(TENANT_ID)
                .moduleKey(moduleKey)
                .status(status)
                .trialEndsAt(trialEndsAt)
                .build();
    }
}
