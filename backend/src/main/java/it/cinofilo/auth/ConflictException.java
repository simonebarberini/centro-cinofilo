package it.cinofilo.auth;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
