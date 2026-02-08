package it.cinofilo.tenancy;

import it.cinofilo.tenancy.dto.TenantSettingsResponse;
import it.cinofilo.tenancy.dto.UpdateTenantSettingsRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantSettingsService {

    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public TenantSettingsResponse getSettings(UUID tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found: " + tenantId));

        return TenantSettingsResponse.builder()
                .name(tenant.getName())
                .slug(tenant.getSlug())
                .capacityBoxes(tenant.getCapacityBoxes())
                .preferences(tenant.getPreferences())
                .build();
    }

    @Transactional
    public TenantSettingsResponse updateSettings(UUID tenantId, UpdateTenantSettingsRequest request) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found: " + tenantId));

        if (request.getCapacityBoxes() != null) {
            tenant.setCapacityBoxes(request.getCapacityBoxes());
        }
        if (request.getPreferences() != null) {
            tenant.setPreferences(request.getPreferences());
        }

        tenant = tenantRepository.save(tenant);

        return TenantSettingsResponse.builder()
                .name(tenant.getName())
                .slug(tenant.getSlug())
                .capacityBoxes(tenant.getCapacityBoxes())
                .preferences(tenant.getPreferences())
                .build();
    }
}
