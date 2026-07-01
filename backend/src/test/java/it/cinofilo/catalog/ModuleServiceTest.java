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
    void findAll_returnsMappedResponses() {
        when(moduleRepository.findAll()).thenReturn(List.of(
                module("base",    ModuleActivationStatus.ACTIVE),
                module("staff",   ModuleActivationStatus.INACTIVE),
                module("reports", ModuleActivationStatus.DEPRECATED)
        ));

        List<ModuleResponse> result = moduleService.findAll();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(ModuleResponse::moduleKey)
                .containsExactly("base", "staff", "reports");
        verify(moduleRepository).findAll();
    }

    @Test
    void findAll_returnsAllStatuses() {
        when(moduleRepository.findAll()).thenReturn(List.of(
                module("a", ModuleActivationStatus.ACTIVE),
                module("b", ModuleActivationStatus.INACTIVE),
                module("c", ModuleActivationStatus.DEPRECATED)
        ));

        List<ModuleResponse> result = moduleService.findAll();

        assertThat(result).extracting(ModuleResponse::activationStatus)
                .containsExactlyInAnyOrder("ACTIVE", "INACTIVE", "DEPRECATED");
    }

    @Test
    void findActive_returnsOnlyActiveMappedResponses() {
        when(moduleRepository.findByActivationStatus(ModuleActivationStatus.ACTIVE))
                .thenReturn(List.of(module("base", ModuleActivationStatus.ACTIVE)));

        List<ModuleResponse> result = moduleService.findActive();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).activationStatus()).isEqualTo("ACTIVE");
        verify(moduleRepository).findByActivationStatus(ModuleActivationStatus.ACTIVE);
    }

    @Test
    void findActive_returnsEmptyListWhenNoneActive() {
        when(moduleRepository.findByActivationStatus(ModuleActivationStatus.ACTIVE))
                .thenReturn(List.of());

        assertThat(moduleService.findActive()).isEmpty();
    }

    @Test
    void findByKey_returnsMappedResponse() {
        when(moduleRepository.findById("base"))
                .thenReturn(Optional.of(module("base", ModuleActivationStatus.ACTIVE)));

        ModuleResponse result = moduleService.findByKey("base");

        assertThat(result.moduleKey()).isEqualTo("base");
    }

    @Test
    void findByKey_throwsModuleNotFoundException_whenAbsent() {
        when(moduleRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> moduleService.findByKey("unknown"))
                .isInstanceOf(ModuleNotFoundException.class)
                .hasMessageContaining("unknown");
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
