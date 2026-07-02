package it.cinofilo.subscription;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "tenant_module")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantModule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "module_key", nullable = false, length = 50)
    private String moduleKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenantModuleStatus status;

    @Column(name = "trial_ends_at")
    private Instant trialEndsAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ── Static factories ──────────────────────────────────────────────────────

    public static TenantModule forActivation(UUID tenantId, String moduleKey) {
        return TenantModule.builder()
                .tenantId(tenantId)
                .moduleKey(moduleKey)
                .status(TenantModuleStatus.ACTIVE)
                .build();
    }

    public static TenantModule forTrial(UUID tenantId, String moduleKey, Instant trialEndsAt) {
        Objects.requireNonNull(trialEndsAt, "trialEndsAt must not be null for TRIAL status");
        return TenantModule.builder()
                .tenantId(tenantId)
                .moduleKey(moduleKey)
                .status(TenantModuleStatus.TRIAL)
                .trialEndsAt(trialEndsAt)
                .build();
    }

    // ── State transitions ─────────────────────────────────────────────────────

    public void activate() {
        this.status = TenantModuleStatus.ACTIVE;
        this.trialEndsAt = null;
    }

    public void startTrial(Instant trialEndsAt) {
        Objects.requireNonNull(trialEndsAt, "trialEndsAt must not be null for TRIAL status");
        this.status = TenantModuleStatus.TRIAL;
        this.trialEndsAt = trialEndsAt;
    }

    public void cancel() {
        this.status = TenantModuleStatus.CANCELLED;
        this.trialEndsAt = null;
    }
}
