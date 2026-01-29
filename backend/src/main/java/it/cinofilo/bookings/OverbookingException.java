package it.cinofilo.bookings;

import java.time.LocalDate;

/**
 * Exception thrown when a booking would exceed the tenant's box capacity.
 */
public class OverbookingException extends RuntimeException {
    private final LocalDate overCapacityDate;

    public OverbookingException(String message, LocalDate overCapacityDate) {
        super(message);
        this.overCapacityDate = overCapacityDate;
    }

    public LocalDate getOverCapacityDate() {
        return overCapacityDate;
    }
}
