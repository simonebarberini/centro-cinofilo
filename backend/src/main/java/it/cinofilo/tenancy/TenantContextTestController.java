package it.cinofilo.tenancy;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Test-only controller for verifying TenantContext is populated during authenticated requests.
 * This controller is used exclusively in integration tests and is not available in production.
 */
@RestController
@RequestMapping("/_test/tenant-context")
@Profile("test")
public class TenantContextTestController {

    /**
     * Returns the current tenant ID from TenantContext.
     * Requires authentication.
     *
     * @return the tenant ID if set, or null if not set
     */
    @GetMapping("/current")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TenantContextResponse> getCurrentTenantId() {
        UUID tenantId = TenantContext.getTenantId();
        return ResponseEntity.ok(new TenantContextResponse(tenantId));
    }

    /**
     * Simple response DTO for tenant context information.
     */
    public static class TenantContextResponse {
        @JsonProperty("tenantId")
        public UUID tenantId;

        public TenantContextResponse() {
            this.tenantId = null;
        }

        public TenantContextResponse(UUID tenantId) {
            this.tenantId = tenantId;
        }
    }
}
