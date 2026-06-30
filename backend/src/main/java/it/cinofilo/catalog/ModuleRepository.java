package it.cinofilo.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModuleRepository extends JpaRepository<Module, String> {

    List<Module> findByActivationStatus(ModuleActivationStatus status);

    List<Module> findByModuleKeyIn(List<String> moduleKeys);
}
