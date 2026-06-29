package it.cinofilo.ratelimit.core;

/**
 * Handle to a rate limit bucket.
 *
 * This is the only point of interaction between the framework and the
 * underlying rate limiting library. No Bucket4j (or any other library)
 * type crosses this boundary — only framework-owned ConsumeResult.
 *
 * Implementations are obtained via BucketProvider.getBucket().
 */
public interface BucketHandle {
    ConsumeResult tryConsume();
}
