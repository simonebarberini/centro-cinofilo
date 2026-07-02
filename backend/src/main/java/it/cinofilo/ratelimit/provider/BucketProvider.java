package it.cinofilo.ratelimit.provider;

import it.cinofilo.ratelimit.core.BucketConfig;
import it.cinofilo.ratelimit.core.BucketHandle;

/**
 * Factory for rate limit bucket handles.
 *
 * This is the single seam between the framework and the underlying
 * rate limiting library. Implementations are free to use Bucket4j,
 * Redis, Caffeine, or any other backend — nothing outside this package
 * is aware of the library choice.
 *
 * Contract:
 * - The same (key + config) pair must return the same stateful bucket.
 * - Implementations are responsible for caching/lookup by key.
 * - Config changes for an existing key are NOT guaranteed to take effect
 *   until the application restarts (acceptable for in-memory implementations).
 */
public interface BucketProvider {
    BucketHandle getBucket(String key, BucketConfig config);
}
