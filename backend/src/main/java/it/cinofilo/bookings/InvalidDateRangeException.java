package it.cinofilo.bookings;

/**
 * Exception thrown when a booking date range is invalid.
 */
public class InvalidDateRangeException extends RuntimeException {

    public InvalidDateRangeException(String message) {
        super(message);
    }
}
