package it.cinofilo.ratelimit;

import it.cinofilo.ratelimit.config.RateLimitProperties;
import it.cinofilo.ratelimit.core.KeyExtractorType;
import it.cinofilo.ratelimit.core.RateLimitPolicyType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that RateLimitProperties correctly binds the YAML structure,
 * including enum keys and values (RateLimitPolicyType, KeyExtractorType).
 *
 * Uses a lightweight slice (no datasource, JPA, or security).
 */
@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(RateLimitProperties.class)
@TestPropertySource(properties = {
    "rate-limit.enabled=true",
    "rate-limit.endpoint-policies.[/auth/login]=LOGIN",
    "rate-limit.endpoint-policies.[/auth/register]=REGISTER",
    "rate-limit.policies.LOGIN.buckets[0].key-type=IP_TENANT",
    "rate-limit.policies.LOGIN.buckets[0].capacity=20",
    "rate-limit.policies.LOGIN.buckets[0].refill-tokens=20",
    "rate-limit.policies.LOGIN.buckets[0].refill-period-minutes=15",
    "rate-limit.policies.LOGIN.buckets[1].key-type=TENANT_USERNAME",
    "rate-limit.policies.LOGIN.buckets[1].capacity=10",
    "rate-limit.policies.LOGIN.buckets[1].refill-tokens=10",
    "rate-limit.policies.LOGIN.buckets[1].refill-period-minutes=15",
    "rate-limit.policies.REGISTER.buckets[0].key-type=IP",
    "rate-limit.policies.REGISTER.buckets[0].capacity=3",
    "rate-limit.policies.REGISTER.buckets[0].refill-tokens=3",
    "rate-limit.policies.REGISTER.buckets[0].refill-period-minutes=60",
})
class RateLimitPropertiesTest {

    @Autowired
    private RateLimitProperties properties;

    @Test
    void shouldBeEnabled() {
        assertThat(properties.isEnabled()).isTrue();
    }

    @Test
    void shouldBindEndpointPolicies() {
        assertThat(properties.getEndpointPolicies())
                .containsEntry("/auth/login", RateLimitPolicyType.LOGIN)
                .containsEntry("/auth/register", RateLimitPolicyType.REGISTER);
    }

    @Test
    void shouldBindLoginPolicyWithTwoBuckets() {
        RateLimitProperties.PolicyConfig login = properties.getPolicies().get(RateLimitPolicyType.LOGIN);
        assertThat(login).isNotNull();
        assertThat(login.getBuckets()).hasSize(2);

        RateLimitProperties.BucketDefinitionConfig bucket0 = login.getBuckets().get(0);
        assertThat(bucket0.getKeyType()).isEqualTo(KeyExtractorType.IP_TENANT);
        assertThat(bucket0.getCapacity()).isEqualTo(20);
        assertThat(bucket0.getRefillTokens()).isEqualTo(20);
        assertThat(bucket0.getRefillPeriodMinutes()).isEqualTo(15);

        RateLimitProperties.BucketDefinitionConfig bucket1 = login.getBuckets().get(1);
        assertThat(bucket1.getKeyType()).isEqualTo(KeyExtractorType.TENANT_USERNAME);
        assertThat(bucket1.getCapacity()).isEqualTo(10);
    }

    @Test
    void shouldBindRegisterPolicyWithOneBucket() {
        RateLimitProperties.PolicyConfig register = properties.getPolicies().get(RateLimitPolicyType.REGISTER);
        assertThat(register).isNotNull();
        assertThat(register.getBuckets()).hasSize(1);
        assertThat(register.getBuckets().get(0).getKeyType()).isEqualTo(KeyExtractorType.IP);
        assertThat(register.getBuckets().get(0).getCapacity()).isEqualTo(3);
    }
}
