package it.cinofilo.subscription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.*;

class SubscriptionAdminControllerTest {

    private SubscriptionService subscriptionService;
    private SubscriptionAdminController controller;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final String MODULE_KEY = "staff";

    @BeforeEach
    void setUp() {
        subscriptionService = mock(SubscriptionService.class);
        controller = new SubscriptionAdminController(subscriptionService);
    }

    @Test
    void cancel_delegatesToSubscriptionService() {
        controller.cancel(TENANT_ID, MODULE_KEY);

        verify(subscriptionService).cancel(TENANT_ID, MODULE_KEY);
    }

    @Test
    void cancel_alreadyCancelled_stillDelegatesWithoutError() {
        doNothing().when(subscriptionService).cancel(any(), any());

        controller.cancel(TENANT_ID, MODULE_KEY);

        verify(subscriptionService).cancel(TENANT_ID, MODULE_KEY);
    }

    @Test
    void cancel_unknownTenant_stillDelegatesWithoutError() {
        UUID unknownTenant = UUID.randomUUID();
        doNothing().when(subscriptionService).cancel(any(), any());

        controller.cancel(unknownTenant, MODULE_KEY);

        verify(subscriptionService).cancel(unknownTenant, MODULE_KEY);
    }
}
