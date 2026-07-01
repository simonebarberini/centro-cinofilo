package it.cinofilo.catalog;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "module_entitlement")
@IdClass(ModuleEntitlementId.class)
@Getter
@NoArgsConstructor
public class ModuleEntitlement {

    @Id
    @Column(name = "module_key", nullable = false, length = 50)
    private String moduleKey;

    @Id
    @Column(name = "entitlement_key", nullable = false, length = 100)
    private String entitlementKey;

    @Column(name = "bool_value")
    private Boolean boolValue;

    @Column(name = "quota_value")
    private Integer quotaValue;

    public static ModuleEntitlement ofBoolean(String moduleKey, String entitlementKey, boolean value) {
        ModuleEntitlement e = new ModuleEntitlement();
        e.moduleKey = moduleKey;
        e.entitlementKey = entitlementKey;
        e.boolValue = value;
        e.quotaValue = null;
        return e;
    }

    public static ModuleEntitlement ofQuota(String moduleKey, String entitlementKey, int value) {
        ModuleEntitlement e = new ModuleEntitlement();
        e.moduleKey = moduleKey;
        e.entitlementKey = entitlementKey;
        e.boolValue = null;
        e.quotaValue = value;
        return e;
    }
}
