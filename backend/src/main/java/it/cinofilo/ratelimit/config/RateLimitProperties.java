package it.cinofilo.ratelimit.config;

import it.cinofilo.ratelimit.core.KeyExtractorType;
import it.cinofilo.ratelimit.core.RateLimitPolicyType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Binds the "rate-limit" section of application.yml.
 *
 * Example YAML structure:
 *
 *   rate-limit:
 *     enabled: true
 *     endpoint-policies:
 *       "/auth/login": LOGIN
 *     policies:
 *       LOGIN:
 *         buckets:
 *           - key-type: IP_TENANT
 *             capacity: 20
 *             refill-tokens: 20
 *             refill-period-minutes: 15
 *
 * Setting "enabled: false" disables all rate limiting globally (useful for
 * integration tests and local development when needed).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;

    /** Maps servlet paths (e.g. "/auth/login") to their policy type. */
    private Map<String, RateLimitPolicyType> endpointPolicies = new LinkedHashMap<>();

    /** Maps policy types to their bucket configurations. */
    private Map<RateLimitPolicyType, PolicyConfig> policies = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class PolicyConfig {
        private List<BucketDefinitionConfig> buckets = List.of();
    }

    @Getter
    @Setter
    public static class BucketDefinitionConfig {
        private KeyExtractorType keyType;
        private long capacity;
        private long refillTokens;
        private int refillPeriodMinutes;
    }
}
