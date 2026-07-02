package it.cinofilo.domain.entitlement;

public sealed interface Entitlement permits BooleanEntitlement, QuotaEntitlement {
    String key();
}
