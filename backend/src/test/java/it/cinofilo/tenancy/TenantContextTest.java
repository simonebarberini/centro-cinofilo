package it.cinofilo.tenancy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        // Ensure clean state after each test
        TenantContext.clear();
    }

    @Test
    void shouldSetAndGetTenantId() {
        // Given
        UUID tenantId = UUID.randomUUID();

        // When
        TenantContext.setTenantId(tenantId);

        // Then
        assertThat(TenantContext.getTenantId()).isEqualTo(tenantId);
    }

    @Test
    void shouldReturnNullWhenNotSet() {
        // When/Then
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void shouldClearTenantId() {
        // Given
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        // When
        TenantContext.clear();

        // Then
        assertThat(TenantContext.getTenantId()).isNull();
    }

    @Test
    void shouldAllowOverwritingTenantId() {
        // Given
        UUID tenantId1 = UUID.randomUUID();
        UUID tenantId2 = UUID.randomUUID();

        // When
        TenantContext.setTenantId(tenantId1);
        assertThat(TenantContext.getTenantId()).isEqualTo(tenantId1);

        TenantContext.setTenantId(tenantId2);

        // Then
        assertThat(TenantContext.getTenantId()).isEqualTo(tenantId2);
    }
}
