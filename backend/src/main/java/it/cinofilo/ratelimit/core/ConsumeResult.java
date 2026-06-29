package it.cinofilo.ratelimit.core;

/**
 * Result of a single bucket token consumption attempt.
 *
 * Returned by BucketHandle.tryConsume(). Contains no library-specific types.
 */
public record ConsumeResult(boolean allowed, long remainingTokens, long waitMillis) {

    public static ConsumeResult allowed(long remainingTokens) {
        return new ConsumeResult(true, remainingTokens, 0L);
    }

    public static ConsumeResult denied(long waitMillis) {
        return new ConsumeResult(false, 0L, waitMillis);
    }
}
