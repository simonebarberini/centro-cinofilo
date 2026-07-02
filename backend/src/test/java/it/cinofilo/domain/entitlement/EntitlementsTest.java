package it.cinofilo.domain.entitlement;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EntitlementsTest {

    private static final List<Entitlement> ALL = allConstants();

    @Test
    void catalogIsNotEmpty() {
        assertThat(ALL).isNotEmpty();
    }

    @Test
    void allConstants_haveNonBlankKey() {
        for (Entitlement e : ALL) {
            assertThat(e.key())
                    .as("key of %s must not be blank", e)
                    .isNotBlank();
        }
    }

    @Test
    void noKeyDuplicates() {
        Set<String> seen = new HashSet<>();
        for (Entitlement e : ALL) {
            assertThat(seen.add(e.key()))
                    .as("duplicate key detected: %s", e.key())
                    .isTrue();
        }
    }

    @Test
    void booleanConstants_areInstancesOfBooleanEntitlement() {
        assertThat(Entitlements.BOOKING_MANAGEMENT).isInstanceOf(BooleanEntitlement.class);
        assertThat(Entitlements.CUSTOMER_MANAGEMENT).isInstanceOf(BooleanEntitlement.class);
        assertThat(Entitlements.DOG_MANAGEMENT).isInstanceOf(BooleanEntitlement.class);
        assertThat(Entitlements.CALENDAR_VIEW).isInstanceOf(BooleanEntitlement.class);
        assertThat(Entitlements.SMS_NOTIFICATIONS).isInstanceOf(BooleanEntitlement.class);
        assertThat(Entitlements.API_ACCESS).isInstanceOf(BooleanEntitlement.class);
        assertThat(Entitlements.ADVANCED_REPORTS).isInstanceOf(BooleanEntitlement.class);
    }

    @Test
    void quotaConstants_areInstancesOfQuotaEntitlement() {
        assertThat(Entitlements.MAX_STAFF_USERS).isInstanceOf(QuotaEntitlement.class);
        assertThat(Entitlements.MAX_DOGS_PER_TENANT).isInstanceOf(QuotaEntitlement.class);
        assertThat(Entitlements.MAX_BOOKINGS_PER_MONTH).isInstanceOf(QuotaEntitlement.class);
    }

    @Test
    void constantKeys_matchFieldNames() {
        for (Field f : Entitlements.class.getDeclaredFields()) {
            if (!Modifier.isPublic(f.getModifiers())) continue;
            try {
                Entitlement e = (Entitlement) f.get(null);
                assertThat(e.key())
                        .as("key of field %s should match field name", f.getName())
                        .isEqualTo(f.getName());
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static List<Entitlement> allConstants() {
        List<Entitlement> result = new ArrayList<>();
        for (Field f : Entitlements.class.getDeclaredFields()) {
            if (!Modifier.isPublic(f.getModifiers())) continue;
            try {
                result.add((Entitlement) f.get(null));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }
}
