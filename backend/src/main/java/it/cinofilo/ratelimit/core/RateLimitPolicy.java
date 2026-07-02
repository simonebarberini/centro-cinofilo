package it.cinofilo.ratelimit.core;

import java.util.List;

/**
 * A named rate limit policy composed of one or more bucket definitions.
 *
 * All buckets are checked on every request. A request is blocked if ANY
 * single bucket is exhausted — this enables the dual-bucket pattern
 * (one per-IP bucket + one per-account bucket) designed in the architecture.
 */
public record RateLimitPolicy(RateLimitPolicyType type, List<RateLimitBucketDefinition> buckets) {}
