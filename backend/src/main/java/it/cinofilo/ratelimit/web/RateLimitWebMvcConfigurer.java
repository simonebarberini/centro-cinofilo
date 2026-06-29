package it.cinofilo.ratelimit.web;

import it.cinofilo.ratelimit.config.PolicyRegistry;
import it.cinofilo.ratelimit.config.RateLimitProperties;
import it.cinofilo.ratelimit.factory.RequestContextFactory;
import it.cinofilo.ratelimit.provider.BucketProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers RateLimitInterceptor for all paths declared in rate-limit.endpoint-policies.
 *
 * The interceptor is instantiated here (not as a @Component) so it is NOT
 * subject to Spring's bean post-processing, avoiding circular dependency
 * issues between HandlerInterceptor and WebMvcConfigurer.
 *
 * Path patterns are derived directly from the YAML configuration — adding a
 * new endpoint to rate-limit.endpoint-policies automatically registers it here
 * with zero code changes.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RateLimitWebMvcConfigurer implements WebMvcConfigurer {

    private final PolicyRegistry policyRegistry;
    private final RateLimitProperties properties;
    private final RequestContextFactory contextFactory;
    private final BucketProvider bucketProvider;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (properties.getEndpointPolicies().isEmpty()) {
            log.debug("Rate limit: no endpoint-policies configured, interceptor not registered.");
            return;
        }

        String[] paths = properties.getEndpointPolicies().keySet().toArray(String[]::new);

        registry.addInterceptor(
                new RateLimitInterceptor(policyRegistry, contextFactory, bucketProvider)
        ).addPathPatterns(paths);

        log.info("Rate limit: interceptor registered for {} path(s): {}",
                paths.length, String.join(", ", paths));
    }
}
