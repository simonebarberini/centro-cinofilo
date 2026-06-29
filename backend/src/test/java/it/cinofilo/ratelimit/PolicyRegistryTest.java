package it.cinofilo.ratelimit;

import it.cinofilo.ratelimit.config.PolicyRegistry;
import it.cinofilo.ratelimit.config.RateLimitProperties;
import it.cinofilo.ratelimit.core.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PolicyRegistry — no Spring context, pure Java.
 *
 * Verifies:
 * - Correct policy lookup by servlet path.
 * - Namespaced bucket keys prevent cross-policy collisions.
 * - Unknown paths return Optional.empty().
 * - isEnabled() reflects the properties flag.
 */
class PolicyRegistryTest {

    private PolicyRegistry registry;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = buildProperties();
        registry = new PolicyRegistry(properties);
    }

    @Test
    void shouldFindLoginPolicyByPath() {
        Optional<RateLimitPolicy> policy = registry.findByPath("/auth/login");

        assertThat(policy).isPresent();
        assertThat(policy.get().type()).isEqualTo(RateLimitPolicyType.LOGIN);
    }

    @Test
    void shouldFindRegisterPolicyByPath() {
        Optional<RateLimitPolicy> policy = registry.findByPath("/auth/register");

        assertThat(policy).isPresent();
        assertThat(policy.get().type()).isEqualTo(RateLimitPolicyType.REGISTER);
    }

    @Test
    void shouldReturnEmptyForUnknownPath() {
        assertThat(registry.findByPath("/auth/unknown")).isEmpty();
        assertThat(registry.findByPath("/api/auth/login")).isEmpty();
        assertThat(registry.findByPath("")).isEmpty();
    }

    @Test
    void shouldBuildLoginPolicyWithTwoBuckets() {
        RateLimitPolicy policy = registry.findByPath("/auth/login").orElseThrow();

        assertThat(policy.buckets()).hasSize(2);
    }

    @Test
    void shouldGenerateNamespacedKeysForLoginBuckets() {
        RateLimitPolicy policy = registry.findByPath("/auth/login").orElseThrow();
        RequestContext ctx = RequestContext.of("1.2.3.4", "demo", "owner", null, null, null, "/auth/login");

        String key0 = policy.buckets().get(0).keyExtractor().extract(ctx);
        String key1 = policy.buckets().get(1).keyExtractor().extract(ctx);

        assertThat(key0).startsWith("LOGIN:IP_TENANT:");
        assertThat(key1).startsWith("LOGIN:TENANT_USERNAME:");
        assertThat(key0).contains("1.2.3.4").contains("demo");
        assertThat(key1).contains("demo").contains("owner");
    }

    @Test
    void shouldGenerateDifferentKeysForDifferentPoliciesWithSameIp() {
        RateLimitPolicy login = registry.findByPath("/auth/login").orElseThrow();
        RateLimitPolicy register = registry.findByPath("/auth/register").orElseThrow();
        RequestContext ctx = RequestContext.of("1.2.3.4", null, null, null, null, null, "/auth/register");

        String loginKey = login.buckets().get(0).keyExtractor().extract(ctx);
        String registerKey = register.buckets().get(0).keyExtractor().extract(ctx);

        assertThat(loginKey).isNotEqualTo(registerKey);
        assertThat(loginKey).startsWith("LOGIN:");
        assertThat(registerKey).startsWith("REGISTER:");
    }

    @Test
    void shouldReflectEnabledFlag() {
        assertThat(registry.isEnabled()).isTrue();
    }

    @Test
    void shouldBeDisabledWhenPropertyIsFalse() {
        RateLimitProperties disabled = buildProperties();
        disabled.setEnabled(false);
        PolicyRegistry disabledRegistry = new PolicyRegistry(disabled);

        assertThat(disabledRegistry.isEnabled()).isFalse();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private RateLimitProperties buildProperties() {
        RateLimitProperties props = new RateLimitProperties();
        props.setEnabled(true);
        props.setEndpointPolicies(Map.of(
                "/auth/login", RateLimitPolicyType.LOGIN,
                "/auth/register", RateLimitPolicyType.REGISTER
        ));

        RateLimitProperties.BucketDefinitionConfig ipTenant = new RateLimitProperties.BucketDefinitionConfig();
        ipTenant.setKeyType(KeyExtractorType.IP_TENANT);
        ipTenant.setCapacity(20);
        ipTenant.setRefillTokens(20);
        ipTenant.setRefillPeriodMinutes(15);

        RateLimitProperties.BucketDefinitionConfig tenantUser = new RateLimitProperties.BucketDefinitionConfig();
        tenantUser.setKeyType(KeyExtractorType.TENANT_USERNAME);
        tenantUser.setCapacity(10);
        tenantUser.setRefillTokens(10);
        tenantUser.setRefillPeriodMinutes(15);

        RateLimitProperties.PolicyConfig loginConfig = new RateLimitProperties.PolicyConfig();
        loginConfig.setBuckets(List.of(ipTenant, tenantUser));

        RateLimitProperties.BucketDefinitionConfig ip = new RateLimitProperties.BucketDefinitionConfig();
        ip.setKeyType(KeyExtractorType.IP);
        ip.setCapacity(3);
        ip.setRefillTokens(3);
        ip.setRefillPeriodMinutes(60);

        RateLimitProperties.PolicyConfig registerConfig = new RateLimitProperties.PolicyConfig();
        registerConfig.setBuckets(List.of(ip));

        props.setPolicies(Map.of(
                RateLimitPolicyType.LOGIN, loginConfig,
                RateLimitPolicyType.REGISTER, registerConfig
        ));

        return props;
    }
}
