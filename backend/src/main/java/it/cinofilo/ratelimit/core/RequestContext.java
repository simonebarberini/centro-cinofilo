package it.cinofilo.ratelimit.core;

import java.util.Optional;
import java.util.UUID;

/**
 * Immutable view of the caller's identity extracted from an HTTP request.
 *
 * Built by RequestContextFactory, which abstracts the source of each field:
 * - ip           → X-Forwarded-For / X-Real-IP / remoteAddr
 * - tenantSlug   → JSON request body
 * - username     → JSON request body
 * - email        → JSON request body
 * - userId       → JWT claims (authenticated endpoints)
 * - tenantId     → JWT claims (authenticated endpoints)
 * - path         → servlet path (without context prefix)
 *
 * Rate limit key extractors receive a RequestContext and are completely
 * unaware of where each field originated.
 */
public record RequestContext(
        String ip,
        Optional<String> tenantSlug,
        Optional<String> username,
        Optional<String> email,
        Optional<UUID> userId,
        Optional<UUID> tenantId,
        String path
) {
    public static RequestContext of(
            String ip,
            String tenantSlug,
            String username,
            String email,
            UUID userId,
            UUID tenantId,
            String path
    ) {
        return new RequestContext(
                ip,
                Optional.ofNullable(tenantSlug),
                Optional.ofNullable(username),
                Optional.ofNullable(email),
                Optional.ofNullable(userId),
                Optional.ofNullable(tenantId),
                path
        );
    }
}
