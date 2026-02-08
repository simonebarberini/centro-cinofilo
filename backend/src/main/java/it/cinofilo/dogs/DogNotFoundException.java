package it.cinofilo.dogs;

/**
 * Exception thrown when a dog is not found.
 */
public class DogNotFoundException extends RuntimeException {
    public DogNotFoundException(String message) {
        super(message);
    }
}
