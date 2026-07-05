package it.cinofilo.tenancy;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/tenants")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN_APP')")
public class TenantAdminController {

    private final TenantAdminService tenantAdminService;

    @GetMapping
    public ResponseEntity<List<TenantSummaryResponse>> search(
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(tenantAdminService.search(q));
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantDetailResponse> getById(@PathVariable UUID tenantId) {
        return ResponseEntity.ok(tenantAdminService.findById(tenantId));
    }

    @GetMapping("/{tenantId}/modules")
    public ResponseEntity<List<TenantModuleStatusResponse>> getModules(
            @PathVariable UUID tenantId) {
        return ResponseEntity.ok(tenantAdminService.findModules(tenantId));
    }
}
