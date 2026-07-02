package it.cinofilo.ratelimit.config;

import it.cinofilo.ratelimit.core.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Converts RateLimitProperties (raw YAML bindings) into domain RateLimitPolicy objects.
 *
 * Responsibilities:
 * - Translates KeyExtractorType enums into RateLimitKeyExtractor lambdas.
 * - Namespaces every key extractor with "POLICY_TYPE:KEY_TYPE:" prefix to
 *   prevent accidental bucket sharing across policies or bucket slots.
 * - Exposes findByPath() for the interceptor to look up the applicable policy.
 *
 * The namespace prefix design means:
 *   LOGIN:IP_TENANT:ip:1.2.3.4:tenant:demo  ← LOGIN policy, first bucket
 *   FORGOT_PASSWORD:IP_TENANT:ip:1.2.3.4:tenant:demo  ← different bucket, same IP+tenant
 * These are two independent counters, as intended.
 */
@Slf4j
@Component
public class PolicyRegistry {

    private final boolean enabled;
    private final Map<String, RateLimitPolicyType> endpointPolicies;
    private final Map<RateLimitPolicyType, RateLimitPolicy> policies;

    public PolicyRegistry(RateLimitProperties properties) {
        this.enabled = properties.isEnabled();
        this.endpointPolicies = Map.copyOf(properties.getEndpointPolicies());
        this.policies = properties.getPolicies().entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        e -> buildPolicy(e.getKey(), e.getValue())
                ));
        log.info("Rate limit: loaded {} policies, {} endpoint mappings, enabled={}",
                this.policies.size(), this.endpointPolicies.size(), this.enabled);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Optional<RateLimitPolicy> findByPath(String servletPath) {
        RateLimitPolicyType type = endpointPolicies.get(servletPath);
        if (type == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(policies.get(type));
    }

    private RateLimitPolicy buildPolicy(
            RateLimitPolicyType type,
            RateLimitProperties.PolicyConfig config
    ) {
        List<RateLimitBucketDefinition> buckets = config.getBuckets().stream()
                .map(b -> new RateLimitBucketDefinition(
                        namespacedExtractor(type, b.getKeyType()),
                        new BucketConfig(b.getCapacity(), b.getRefillTokens(), b.getRefillPeriodMinutes())
                ))
                .toList();
        return new RateLimitPolicy(type, buckets);
    }

    /**
     * Wraps a base extractor with a "POLICY:KEY_TYPE:" prefix to guarantee
     * global uniqueness of bucket keys across all policies and bucket slots.
     */
    private RateLimitKeyExtractor namespacedExtractor(RateLimitPolicyType type, KeyExtractorType keyType) {
        String prefix = type.name() + ":" + keyType.name() + ":";
        RateLimitKeyExtractor base = buildBaseExtractor(keyType);
        return ctx -> prefix + base.extract(ctx);
    }

    private RateLimitKeyExtractor buildBaseExtractor(KeyExtractorType type) {
        return switch (type) {
            case IP ->
                    ctx -> "ip:" + ctx.ip();
            case IP_TENANT ->
                    ctx -> "ip:" + ctx.ip() + ":tenant:" + ctx.tenantSlug().orElse("unknown");
            case TENANT_USERNAME ->
                    ctx -> "tenant:" + ctx.tenantSlug().orElse("unknown") + ":user:" + ctx.username().orElse("unknown");
            case TENANT_EMAIL ->
                    ctx -> "tenant:" + ctx.tenantSlug().orElse("unknown") + ":email:" + ctx.email().orElse("unknown");
            case USER_ID ->
                    ctx -> "user:" + ctx.userId().map(Object::toString).orElse("anonymous");
            case TENANT_ID ->
                    ctx -> "tenant-id:" + ctx.tenantId().map(Object::toString).orElse("unknown");
        };
    }
}
