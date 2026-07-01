package it.cinofilo.catalog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ModuleServiceTest {

    private ModuleRepository moduleRepository;
    private ModuleService moduleService;

    @BeforeEach
    void setUp() {
        moduleRepository = mock(ModuleRepository.class);
        moduleService = new ModuleService(moduleRepository);
    }

    @Test
    void findActive_delegatesToRepository() {
        List<Module> expected = List.of(activeModule("base"), activeModule("reports"));
        when(moduleRepository.findByActivationStatus(ModuleActivationStatus.ACTIVE)).thenReturn(expected);

        List<Module> result = moduleService.findActive();

        assertThat(result).isEqualTo(expected);
        verify(moduleRepository).findByActivationStatus(ModuleActivationStatus.ACTIVE);
    }

    @Test
    void findActive_returnsEmptyListWhenNoneActive() {
        when(moduleRepository.findByActivationStatus(ModuleActivationStatus.ACTIVE)).thenReturn(List.of());

        assertThat(moduleService.findActive()).isEmpty();
    }

    @Test
    void findByKey_returnsModuleWhenFound() {
        Module base = activeModule("base");
        when(moduleRepository.findById("base")).thenReturn(Optional.of(base));

        Module result = moduleService.findByKey("base");

        assertThat(result).isEqualTo(base);
    }

    @Test
    void findByKey_throwsModuleNotFoundException_whenAbsent() {
        when(moduleRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> moduleService.findByKey("unknown"))
                .isInstanceOf(ModuleNotFoundException.class)
                .hasMessageContaining("unknown");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Module activeModule(String key) {
        return Module.builder()
                .moduleKey(key)
                .name(key)
                .type(ModuleType.OPTIONAL)
                .activationStatus(ModuleActivationStatus.ACTIVE)
                .build();
    }
}
