package it.cinofilo.ratelimit.web;

import it.cinofilo.ratelimit.config.PolicyRegistry;
import it.cinofilo.ratelimit.core.*;
import it.cinofilo.ratelimit.factory.RequestContextFactory;
import it.cinofilo.ratelimit.provider.BucketProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * HTTP interceptor that enforces rate limit policies.
 *
 * This is the only class in the framework that knows about HTTP (HttpServletRequest,
 * HttpServletResponse). All domain logic lives in core/, provider/, factory/, config/.
 *
 * Processing per request:
 * 1. If rate limiting is disabled globally → pass through.
 * 2. Look up the policy for the servlet path via PolicyRegistry.
 * 3. If no policy configured → pass through.
 * 4. Build RequestContext (abstracts IP, body fields, JWT claims).
 * 5. For each bucket in the policy:
 *    a. Compute the namespaced key.
 *    b. Try to consume a token from the bucket.
 *    c. If denied → send 429 + standard headers and stop.
 * 6. If all buckets pass → return true (continue to controller).
 *
 * HTTP response on rate limit exceeded:
 *   429 Too Many Requests
 *   Retry-After: <seconds>
 *   X-RateLimit-Remaining: 0
 *   X-RateLimit-Policy: <POLICY_NAME>
 */
@Slf4j
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final PolicyRegistry policyRegistry;
    private final RequestContextFactory contextFactory;
    private final BucketProvider bucketProvider;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {
        if (!policyRegistry.isEnabled()) {
            return true;
        }

        String servletPath = request.getServletPath();
        Optional<RateLimitPolicy> policyOpt = policyRegistry.findByPath(servletPath);
        if (policyOpt.isEmpty()) {
            return true;
        }

        RateLimitPolicy policy = policyOpt.get();
        RequestContext context = contextFactory.build(request);

        for (RateLimitBucketDefinition bucketDef : policy.buckets()) {
            String key = bucketDef.keyExtractor().extract(context);
            BucketHandle handle = bucketProvider.getBucket(key, bucketDef.config());
            ConsumeResult consume = handle.tryConsume();

            if (!consume.allowed()) {
                RateLimitResult result = RateLimitResult.denied(policy.type(), consume.waitMillis());
                applyDeniedHeaders(response, result);
                response.sendError(
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        "Too many requests. Retry after " + result.resetAfterSeconds() + " seconds."
                );
                log.warn("Rate limit exceeded: policy={} path={} key={}",
                        policy.type(), servletPath, key);
                return false;
            }

            response.setHeader("X-RateLimit-Remaining", String.valueOf(consume.remainingTokens()));
        }

        return true;
    }

    private void applyDeniedHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setHeader("Retry-After", String.valueOf(result.resetAfterSeconds()));
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setHeader("X-RateLimit-Policy", result.policyType().name());
    }
}
