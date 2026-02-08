package it.cinofilo.tenancy;

import it.cinofilo.tenancy.dto.TenantSettingsResponse;
import it.cinofilo.tenancy.dto.UpdateTenantSettingsRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenant")
@RequiredArgsConstructor
public class TenantSettingsController {

    private final TenantSettingsService settingsService;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_STAFF')")
    public ResponseEntity<TenantSettingsResponse> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings(TenantContext.getTenantId()));
    }

    @PutMapping("/me")
    @PreAuthorize("hasRole('TENANT_OWNER')")
    public ResponseEntity<TenantSettingsResponse> updateSettings(
            @Valid @RequestBody UpdateTenantSettingsRequest request) {
        return ResponseEntity.ok(settingsService.updateSettings(TenantContext.getTenantId(), request));
    }
}
