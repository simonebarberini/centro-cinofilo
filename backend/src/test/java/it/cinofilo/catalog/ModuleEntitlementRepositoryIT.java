package it.cinofilo.catalog;

import it.cinofilo.AbstractPostgresIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModuleEntitlementRepositoryIT extends AbstractPostgresIT {

    @Autowired
    private ModuleRepository moduleRepository;

    @BeforeEach
    void clean() {
        moduleRepository.deleteAll();
    }

    @Test
    void booleanEntitlement_isPersistedAndLoadedViaModule() {
        Module module = savedModule("m1");

        module.getEntitlements().add(ModuleEntitlement.ofBoolean("m1", "BOOKING_MANAGEMENT", true));
        moduleRepository.save(module);

        Module found = moduleRepository.findById("m1").orElseThrow();
        assertThat(found.getEntitlements()).hasSize(1);
        ModuleEntitlement e = found.getEntitlements().get(0);
        assertThat(e.getBoolValue()).isTrue();
        assertThat(e.getQuotaValue()).isNull();
    }

    @Test
    void quotaEntitlement_isPersistedAndLoadedViaModule() {
        Module module = savedModule("m2");

        module.getEntitlements().add(ModuleEntitlement.ofQuota("m2", "MAX_STAFF_USERS", 5));
        moduleRepository.save(module);

        Module found = moduleRepository.findById("m2").orElseThrow();
        ModuleEntitlement e = found.getEntitlements().get(0);
        assertThat(e.getQuotaValue()).isEqualTo(5);
        assertThat(e.getBoolValue()).isNull();
    }

    @Test
    void checkConstraint_rejectsBothValuesNull() {
        savedModule("m3");
        assertThatThrownBy(() -> {
            Module m = moduleRepository.findById("m3").orElseThrow();
            m.getEntitlements().add(badEntitlement("m3", null, null));
            moduleRepository.saveAndFlush(m);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkConstraint_rejectsBothValuesSet() {
        savedModule("m4");
        assertThatThrownBy(() -> {
            Module m = moduleRepository.findById("m4").orElseThrow();
            m.getEntitlements().add(badEntitlement("m4", true, 5));
            moduleRepository.saveAndFlush(m);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void cascade_deletesEntitlementsWhenModuleDeleted() {
        Module module = savedModule("m5");
        module.getEntitlements().add(ModuleEntitlement.ofBoolean("m5", "SMS_NOTIFICATIONS", true));
        moduleRepository.save(module);

        moduleRepository.deleteById("m5");

        assertThat(moduleRepository.findById("m5")).isEmpty();
    }

    @Test
    void multipleEntitlements_onSameModule() {
        Module module = savedModule("m6");
        module.getEntitlements().addAll(List.of(
                ModuleEntitlement.ofBoolean("m6", "BOOKING_MANAGEMENT", true),
                ModuleEntitlement.ofQuota("m6", "MAX_STAFF_USERS", 3)
        ));
        moduleRepository.save(module);

        assertThat(moduleRepository.findById("m6").orElseThrow().getEntitlements()).hasSize(2);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Module savedModule(String key) {
        return moduleRepository.save(Module.builder()
                .moduleKey(key)
                .name(key)
                .type(ModuleType.OPTIONAL)
                .build());
    }

    private ModuleEntitlement badEntitlement(String moduleKey, Boolean boolVal, Integer quotaVal) {
        try {
            var c = ModuleEntitlement.class.getDeclaredConstructor();
            c.setAccessible(true);
            ModuleEntitlement e = c.newInstance();
            var fMk = ModuleEntitlement.class.getDeclaredField("moduleKey");
            var fEk = ModuleEntitlement.class.getDeclaredField("entitlementKey");
            var fBv = ModuleEntitlement.class.getDeclaredField("boolValue");
            var fQv = ModuleEntitlement.class.getDeclaredField("quotaValue");
            fMk.setAccessible(true); fMk.set(e, moduleKey);
            fEk.setAccessible(true); fEk.set(e, "BAD_KEY");
            fBv.setAccessible(true); fBv.set(e, boolVal);
            fQv.setAccessible(true); fQv.set(e, quotaVal);
            return e;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
