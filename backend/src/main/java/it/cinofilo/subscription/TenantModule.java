package it.cinofilo.subscription;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
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
}
