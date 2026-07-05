package it.cinofilo.subscription;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/subscriptions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN_APP')")
public class SubscriptionAdminController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/{tenantId}/{moduleKey}/cancel")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID tenantId,
            @PathVariable String moduleKey) {
        subscriptionService.cancel(tenantId, moduleKey);
        return ResponseEntity.noContent().build();
    }
}
