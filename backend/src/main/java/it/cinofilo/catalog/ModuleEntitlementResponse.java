package it.cinofilo.catalog;

public record ModuleEntitlementResponse(
        String entitlementKey,
        Boolean boolValue,
        Integer quotaValue
) {
    static ModuleEntitlementResponse from(ModuleEntitlement e) {
        return new ModuleEntitlementResponse(e.getEntitlementKey(), e.getBoolValue(), e.getQuotaValue());
    }
}
