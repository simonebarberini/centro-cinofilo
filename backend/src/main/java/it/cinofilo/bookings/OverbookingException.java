package it.cinofilo.bookings;

import java.time.LocalDate;

/**
 * Exception thrown when a booking would exceed the tenant's box capacity.
 */
public class OverbookingException extends RuntimeException {
    private final LocalDate date;
    private final int capacity;
    private final int booked;

    public OverbookingException(String message, LocalDate date, int capacity, int booked) {
        super(message);
        this.date = date;
        this.capacity = capacity;
        this.booked = booked;
    }

    public LocalDate getDate() {
        return date;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getBooked() {
        return booked;
    }
}
