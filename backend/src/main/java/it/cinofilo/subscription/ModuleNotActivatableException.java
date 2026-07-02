package it.cinofilo.subscription;

public class ModuleNotActivatableException extends RuntimeException {

    public ModuleNotActivatableException(String moduleKey) {
        super("Module '" + moduleKey + "' is not available for subscription");
    }
}
