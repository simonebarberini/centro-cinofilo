package it.cinofilo.catalog;

public class ModuleNotFoundException extends RuntimeException {

    public ModuleNotFoundException(String moduleKey) {
        super("Module not found: " + moduleKey);
    }
}
