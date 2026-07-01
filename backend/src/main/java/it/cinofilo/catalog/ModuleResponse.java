package it.cinofilo.catalog;

import java.util.List;

public record ModuleResponse(
        String moduleKey,
        String name,
        String description,
        String type,
        String activationStatus,
        List<ModuleEntitlementResponse> entitlements
) {
    static ModuleResponse from(Module m) {
        return new ModuleResponse(
                m.getModuleKey(),
                m.getName(),
                m.getDescription(),
                m.getType().name(),
                m.getActivationStatus().name(),
                m.getEntitlements().stream().map(ModuleEntitlementResponse::from).toList()
        );
    }
}
