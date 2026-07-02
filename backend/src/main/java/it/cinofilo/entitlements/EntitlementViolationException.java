package it.cinofilo.entitlements;

public class EntitlementViolationException extends RuntimeException {

    public EntitlementViolationException(String message) {
        super(message);
    }
}
