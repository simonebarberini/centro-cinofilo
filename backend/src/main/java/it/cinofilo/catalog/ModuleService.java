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

    public List<Module> findActive() {
        return moduleRepository.findByActivationStatus(ModuleActivationStatus.ACTIVE);
    }

    public Module findByKey(String moduleKey) {
        return moduleRepository.findById(moduleKey)
                .orElseThrow(() -> new ModuleNotFoundException(moduleKey));
    }
}
