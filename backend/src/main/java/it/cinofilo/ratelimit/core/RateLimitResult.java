package it.cinofilo.ratelimit.core;

/**
 * Final outcome of evaluating all buckets for a policy against a request.
 *
 * Used by RateLimitInterceptor to build the HTTP response (200 or 429 + headers).
 * Contains no library-specific types.
 */
public record RateLimitResult(
        boolean allowed,
        RateLimitPolicyType policyType,
        long remainingTokens,
        long resetAfterSeconds
) {
    public static RateLimitResult allowed(RateLimitPolicyType type, long remaining) {
        return new RateLimitResult(true, type, remaining, 0L);
    }

    public static RateLimitResult denied(RateLimitPolicyType type, long waitMillis) {
        long secs = waitMillis > 0 ? (waitMillis + 999L) / 1000L : 1L;
        return new RateLimitResult(false, type, 0L, secs);
    }
}
