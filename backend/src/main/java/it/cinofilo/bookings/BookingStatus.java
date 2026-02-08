package it.cinofilo.bookings;

/**
 * Booking status enum.
 */
public enum BookingStatus {
    /**
     * Booking is confirmed and occupies capacity.
     */
    CONFIRMED,
    
    /**
     * Booking was cancelled and does not occupy capacity.
     */
    CANCELLED
}
