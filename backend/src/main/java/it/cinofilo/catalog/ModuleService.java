package it.cinofilo.catalog;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ModuleService {

    private final ModuleRepository moduleRepository;

    public ModuleService(ModuleRepository moduleRepository) {
        this.moduleRepository = moduleRepository;
    }

    public List<ModuleResponse> findAll() {
        return moduleRepository.findAll().stream()
                .map(ModuleResponse::from)
                .toList();
    }

    public List<ModuleResponse> findActive() {
        return moduleRepository.findByActivationStatus(ModuleActivationStatus.ACTIVE).stream()
                .map(ModuleResponse::from)
                .toList();
    }

    public ModuleResponse findByKey(String moduleKey) {
        return moduleRepository.findById(moduleKey)
                .map(ModuleResponse::from)
                .orElseThrow(() -> new ModuleNotFoundException(moduleKey));
    }
}
