package it.cinofilo.subscription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GrantRequest(
        @NotNull UUID tenantId,
        @NotBlank String moduleKey,
        String note
) {}
