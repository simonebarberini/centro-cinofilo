package it.cinofilo.bookings.availability;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

/**
 * Daily availability snapshot for bookings.
 */
@Data
@AllArgsConstructor
public class DailyAvailability {

    private LocalDate date;
    private int capacity;
    private int booked;
    private int available;
}
