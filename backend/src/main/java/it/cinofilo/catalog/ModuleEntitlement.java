package it.cinofilo.catalog;

import it.cinofilo.domain.entitlement.BooleanEntitlement;
import it.cinofilo.domain.entitlement.QuotaEntitlement;
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

    /**
     * API raccomandata — accetta un tipo del shared kernel, elimina magic string.
     */
    public static ModuleEntitlement of(String moduleKey, BooleanEntitlement entitlement, boolean value) {
        return ofBoolean(moduleKey, entitlement.key(), value);
    }

    /**
     * API raccomandata — accetta un tipo del shared kernel, elimina magic string.
     */
    public static ModuleEntitlement of(String moduleKey, QuotaEntitlement entitlement, int value) {
        return ofQuota(moduleKey, entitlement.key(), value);
    }

    /**
     * @deprecated Usare {@link #of(String, BooleanEntitlement, boolean)} nel codice applicativo.
     *             Mantenuto per compatibilità con i test di integrazione.
     */
    @Deprecated(forRemoval = false)
    public static ModuleEntitlement ofBoolean(String moduleKey, String entitlementKey, boolean value) {
        ModuleEntitlement e = new ModuleEntitlement();
        e.moduleKey = moduleKey;
        e.entitlementKey = entitlementKey;
        e.boolValue = value;
        e.quotaValue = null;
        return e;
    }

    /**
     * @deprecated Usare {@link #of(String, QuotaEntitlement, int)} nel codice applicativo.
     *             Mantenuto per compatibilità con i test di integrazione.
     */
    @Deprecated(forRemoval = false)
    public static ModuleEntitlement ofQuota(String moduleKey, String entitlementKey, int value) {
        ModuleEntitlement e = new ModuleEntitlement();
        e.moduleKey = moduleKey;
        e.entitlementKey = entitlementKey;
        e.boolValue = null;
        e.quotaValue = value;
        return e;
    }
}
