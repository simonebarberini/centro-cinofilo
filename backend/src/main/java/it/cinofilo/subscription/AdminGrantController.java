package it.cinofilo.subscription;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/grants")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN_APP')")
public class AdminGrantController {

    private final AdminGrantService adminGrantService;

    @PostMapping
    public ResponseEntity<AdminGrantResponse> grant(
            @RequestBody @Valid GrantRequest request,
            Authentication authentication) {
        AdminGrant grant = adminGrantService.grant(
                request.tenantId(),
                request.moduleKey(),
                authentication.getName(),
                request.note());
        return ResponseEntity.status(HttpStatus.CREATED).body(AdminGrantResponse.from(grant));
    }

    @GetMapping("/{tenantId}")
    public ResponseEntity<List<AdminGrantResponse>> listByTenant(@PathVariable UUID tenantId) {
        List<AdminGrantResponse> grants = adminGrantService.findByTenant(tenantId).stream()
                .map(AdminGrantResponse::from)
                .toList();
        return ResponseEntity.ok(grants);
    }
}
