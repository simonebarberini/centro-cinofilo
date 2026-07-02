package it.cinofilo.domain.entitlement;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EntitlementTypeTest {

    @Test
    void booleanEntitlement_implementsEntitlement() {
        Entitlement e = new BooleanEntitlement("TEST");
        assertThat(e).isInstanceOf(BooleanEntitlement.class);
    }

    @Test
    void quotaEntitlement_implementsEntitlement() {
        Entitlement e = new QuotaEntitlement("TEST");
        assertThat(e).isInstanceOf(QuotaEntitlement.class);
    }

    @Test
    void booleanEntitlement_isNotQuotaEntitlement() {
        Entitlement e = new BooleanEntitlement("TEST");
        assertThat(e).isNotInstanceOf(QuotaEntitlement.class);
    }

    @Test
    void quotaEntitlement_isNotBooleanEntitlement() {
        Entitlement e = new QuotaEntitlement("TEST");
        assertThat(e).isNotInstanceOf(BooleanEntitlement.class);
    }

    @Test
    void sealedSwitch_exhaustsAllPermittedTypes() {
        Entitlement bool  = new BooleanEntitlement("B");
        Entitlement quota = new QuotaEntitlement("Q");

        assertThat(describe(bool)).isEqualTo("boolean");
        assertThat(describe(quota)).isEqualTo("quota");
    }

    @Test
    void records_withSameKey_areEqual() {
        assertThat(new BooleanEntitlement("X")).isEqualTo(new BooleanEntitlement("X"));
        assertThat(new QuotaEntitlement("X")).isEqualTo(new QuotaEntitlement("X"));
    }

    @Test
    void records_withDifferentTypes_areNotEqual() {
        assertThat(new BooleanEntitlement("X")).isNotEqualTo(new QuotaEntitlement("X"));
    }

    private String describe(Entitlement e) {
        return switch (e) {
            case BooleanEntitlement b -> "boolean";
            case QuotaEntitlement   q -> "quota";
        };
    }
}
