package it.cinofilo.catalog;

import it.cinofilo.AbstractPostgresIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModuleRepositoryIT extends AbstractPostgresIT {

    @Autowired
    private ModuleRepository moduleRepository;

    @BeforeEach
    void clean() {
        moduleRepository.deleteAll();
    }

    @Test
    void save_andRetrieve_byKey() {
        Module module = Module.builder()
                .moduleKey("test")
                .name("Test Module")
                .type(ModuleType.OPTIONAL)
                .build();

        moduleRepository.save(module);

        Module found = moduleRepository.findById("test").orElseThrow();
        assertThat(found.getName()).isEqualTo("Test Module");
        assertThat(found.getType()).isEqualTo(ModuleType.OPTIONAL);
        assertThat(found.getActivationStatus()).isEqualTo(ModuleActivationStatus.INACTIVE);
    }

    @Test
    void defaultActivationStatus_isInactive() {
        Module module = Module.builder()
                .moduleKey("defaults")
                .name("Defaults")
                .type(ModuleType.BASE)
                .build();

        moduleRepository.save(module);

        assertThat(moduleRepository.findById("defaults").orElseThrow().getActivationStatus())
                .isEqualTo(ModuleActivationStatus.INACTIVE);
    }

    @Test
    void findByActivationStatus_returnsOnlyMatching() {
        moduleRepository.save(module("m-active",     ModuleActivationStatus.ACTIVE));
        moduleRepository.save(module("m-inactive",   ModuleActivationStatus.INACTIVE));
        moduleRepository.save(module("m-deprecated", ModuleActivationStatus.DEPRECATED));

        List<Module> active = moduleRepository.findByActivationStatus(ModuleActivationStatus.ACTIVE);

        assertThat(active).hasSize(1);
        assertThat(active.get(0).getModuleKey()).isEqualTo("m-active");
    }

    @Test
    void findByModuleKeyIn_returnsSubset() {
        moduleRepository.save(module("alpha", ModuleActivationStatus.ACTIVE));
        moduleRepository.save(module("beta",  ModuleActivationStatus.ACTIVE));
        moduleRepository.save(module("gamma", ModuleActivationStatus.ACTIVE));

        List<Module> result = moduleRepository.findByModuleKeyIn(List.of("alpha", "gamma"));

        assertThat(result).extracting(Module::getModuleKey)
                .containsExactlyInAnyOrder("alpha", "gamma");
    }

    @Test
    void checkConstraint_rejectsInvalidType() {
        assertThatThrownBy(() -> {
            moduleRepository.save(Module.builder()
                    .moduleKey("bad-type")
                    .name("Bad")
                    .type(null)
                    .build());
            moduleRepository.flush();
        }).isInstanceOf(Exception.class);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Module module(String key, ModuleActivationStatus status) {
        return Module.builder()
                .moduleKey(key)
                .name(key)
                .type(ModuleType.OPTIONAL)
                .activationStatus(status)
                .build();
    }
}
