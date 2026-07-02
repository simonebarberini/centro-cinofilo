package it.cinofilo.ratelimit.provider;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import it.cinofilo.ratelimit.core.BucketConfig;
import it.cinofilo.ratelimit.core.BucketHandle;
import it.cinofilo.ratelimit.core.ConsumeResult;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * In-memory Bucket4j implementation of BucketProvider.
 *
 * ── Bucket4j is CONFINED to this class. ──────────────────────────────────────
 * No Bucket4j type (Bucket, Bandwidth, ConsumptionProbe, …) is referenced
 * outside this file. The rest of the framework is completely library-agnostic.
 *
 * Buckets are cached in a ConcurrentHashMap keyed by the namespaced string key
 * produced by PolicyRegistry. Thread-safe: computeIfAbsent is atomic.
 *
 * Limitations (acceptable for single-instance deployment):
 * - State is lost on restart.
 * - Not shared across multiple JVM instances.
 * Migration path: replace this bean with a Redis-backed implementation
 * (e.g., using bucket4j-redis + LettuceBasedProxyManager) without touching
 * any other class in the framework.
 */
@Component
public class InMemoryBucketProvider implements BucketProvider {

    private final ConcurrentHashMap<String, BucketHandle> cache = new ConcurrentHashMap<>();

    @Override
    public BucketHandle getBucket(String key, BucketConfig config) {
        return cache.computeIfAbsent(key, k -> createHandle(config));
    }

    private BucketHandle createHandle(BucketConfig config) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(config.capacity())
                .refillGreedy(config.refillTokens(), Duration.ofMinutes(config.refillPeriodMinutes()))
                .build();
        Bucket bucket = Bucket.builder()
                .addLimit(limit)
                .build();
        return new Bucket4jHandle(bucket);
    }

    private record Bucket4jHandle(Bucket bucket) implements BucketHandle {
        @Override
        public ConsumeResult tryConsume() {
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (probe.isConsumed()) {
                return ConsumeResult.allowed(probe.getRemainingTokens());
            }
            long waitMillis = TimeUnit.NANOSECONDS.toMillis(probe.getNanosToWaitForRefill());
            return ConsumeResult.denied(waitMillis);
        }
    }
}
