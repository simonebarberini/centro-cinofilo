package it.cinofilo.ratelimit.core;

/**
 * Pure configuration for a single rate limit bucket.
 *
 * Completely independent of Bucket4j or any other backend library.
 * BucketProvider implementations translate this into their own bucket type.
 */
public record BucketConfig(long capacity, long refillTokens, int refillPeriodMinutes) {}
