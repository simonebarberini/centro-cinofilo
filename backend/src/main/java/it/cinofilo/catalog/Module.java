package it.cinofilo.catalog;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "module")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Module {

    @Id
    @Column(name = "module_key", nullable = false, length = 50)
    private String moduleKey;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ModuleType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "activation_status", nullable = false, length = 20)
    @Builder.Default
    private ModuleActivationStatus activationStatus = ModuleActivationStatus.INACTIVE;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "module_key", updatable = false)
    @Builder.Default
    private List<ModuleEntitlement> entitlements = new ArrayList<>();
}
