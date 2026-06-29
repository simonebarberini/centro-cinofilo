package it.cinofilo.ratelimit.core;

/**
 * Strategy for computing a rate limit bucket key from a RequestContext.
 *
 * Implementations are pure functions: same context → same key.
 * Keys must be globally unique across all policies and bucket types —
 * PolicyRegistry adds a namespace prefix automatically.
 */
@FunctionalInterface
public interface RateLimitKeyExtractor {
    String extract(RequestContext context);
}
