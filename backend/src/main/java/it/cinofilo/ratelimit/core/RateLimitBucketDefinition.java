package it.cinofilo.ratelimit.core;

/**
 * Pairs a key extraction strategy with a bucket configuration.
 *
 * A RateLimitPolicy contains one or more of these. The interceptor checks
 * ALL bucket definitions in sequence — a request is blocked if ANY is exhausted.
 */
public record RateLimitBucketDefinition(RateLimitKeyExtractor keyExtractor, BucketConfig config) {}
