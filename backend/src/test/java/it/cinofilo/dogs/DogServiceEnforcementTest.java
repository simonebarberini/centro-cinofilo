package it.cinofilo.dogs;

import it.cinofilo.domain.entitlement.Entitlements;
import it.cinofilo.entitlements.EntitlementService;
import it.cinofilo.entitlements.EntitlementViolationException;
import it.cinofilo.customer.CustomerRepository;
import it.cinofilo.tenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DogServiceEnforcementTest {

    private DogRepository dogRepository;
    private CustomerRepository customerRepository;
    private EntitlementService entitlementService;
    private DogService service;

    private MockedStatic<TenantContext> tenantContextMock;
    private static final UUID TENANT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        dogRepository = mock(DogRepository.class);
        customerRepository = mock(CustomerRepository.class);
        entitlementService = mock(EntitlementService.class);
        service = new DogService(dogRepository, customerRepository, entitlementService);

        tenantContextMock = mockStatic(TenantContext.class);
        tenantContextMock.when(TenantContext::getTenantId).thenReturn(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    @Test
    void create_throwsEntitlementViolation_whenDogManagementDisabled() {
        when(entitlementService.isEnabled(eq(TENANT_ID), eq(Entitlements.DOG_MANAGEMENT.key())))
                .thenReturn(false);

        assertThatThrownBy(() -> service.create(mock(it.cinofilo.dogs.dto.CreateDogRequest.class)))
                .isInstanceOf(EntitlementViolationException.class)
                .hasMessageContaining("not enabled");

        verify(dogRepository, never()).save(any());
        verify(customerRepository, never()).findByIdAndTenantId(any(), any());
    }

    @Test
    void create_throwsEntitlementViolation_whenDogQuotaExceeded() {
        when(entitlementService.isEnabled(eq(TENANT_ID), eq(Entitlements.DOG_MANAGEMENT.key())))
                .thenReturn(true);
        when(entitlementService.getQuota(eq(TENANT_ID), eq(Entitlements.MAX_DOGS_PER_TENANT.key())))
                .thenReturn(10);
        when(dogRepository.countByTenantId(TENANT_ID)).thenReturn(10L);

        assertThatThrownBy(() -> service.create(mock(it.cinofilo.dogs.dto.CreateDogRequest.class)))
                .isInstanceOf(EntitlementViolationException.class)
                .hasMessageContaining("quota exceeded");

        verify(dogRepository, never()).save(any());
        verify(customerRepository, never()).findByIdAndTenantId(any(), any());
    }

    @Test
    void create_proceedsToCustomerLookup_whenEntitlementGrantedAndQuotaNotExceeded() {
        when(entitlementService.isEnabled(eq(TENANT_ID), eq(Entitlements.DOG_MANAGEMENT.key())))
                .thenReturn(true);
        when(entitlementService.getQuota(eq(TENANT_ID), eq(Entitlements.MAX_DOGS_PER_TENANT.key())))
                .thenReturn(500);
        when(dogRepository.countByTenantId(TENANT_ID)).thenReturn(10L);

        var request = mock(it.cinofilo.dogs.dto.CreateDogRequest.class);
        when(request.getCustomerId()).thenReturn(UUID.randomUUID());
        // customer not found → CustomerNotFoundException, but enforcement has passed
        when(customerRepository.findByIdAndTenantId(any(), any()))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(it.cinofilo.customer.CustomerNotFoundException.class);

        // confirms enforcement did NOT block the call
        verify(customerRepository).findByIdAndTenantId(any(), eq(TENANT_ID));
    }
}
