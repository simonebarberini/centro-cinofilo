package it.cinofilo.tenancy;

import it.cinofilo.subscription.TenantModuleRepository;
import it.cinofilo.subscription.TenantModuleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantAdminService {

    private final TenantRepository tenantRepository;
    private final TenantModuleRepository tenantModuleRepository;

    public List<TenantSummaryResponse> search(String q) {
        List<Tenant> tenants = (q == null || q.isBlank())
                ? tenantRepository.findAll()
                : tenantRepository.findByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(q, q);
        return tenants.stream().map(TenantSummaryResponse::from).toList();
    }

    public TenantDetailResponse findById(UUID tenantId) {
        return tenantRepository.findById(tenantId)
                .map(TenantDetailResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tenant not found: " + tenantId));
    }

    public List<TenantModuleStatusResponse> findModules(UUID tenantId) {
        if (!tenantRepository.existsById(tenantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Tenant not found: " + tenantId);
        }
        return tenantModuleRepository
                .findByTenantIdAndStatusIn(tenantId,
                        List.of(TenantModuleStatus.ACTIVE, TenantModuleStatus.TRIAL))
                .stream()
                .map(TenantModuleStatusResponse::from)
                .toList();
    }
}
