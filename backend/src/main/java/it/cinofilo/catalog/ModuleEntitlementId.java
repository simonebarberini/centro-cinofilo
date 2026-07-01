package it.cinofilo.catalog;

import java.io.Serializable;
import java.util.Objects;

public class ModuleEntitlementId implements Serializable {

    private String moduleKey;
    private String entitlementKey;

    public ModuleEntitlementId() {}

    public ModuleEntitlementId(String moduleKey, String entitlementKey) {
        this.moduleKey = moduleKey;
        this.entitlementKey = entitlementKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModuleEntitlementId that)) return false;
        return Objects.equals(moduleKey, that.moduleKey)
                && Objects.equals(entitlementKey, that.entitlementKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(moduleKey, entitlementKey);
    }
}
