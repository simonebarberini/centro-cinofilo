package it.cinofilo.bookings.calendar.dto;

import it.cinofilo.bookings.availability.DailyAvailability;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
public class CalendarResponse {
    private LocalDate start;
    private LocalDate end;
    private int capacity;
    private List<DailyAvailability> days;
    private List<CalendarBookingItem> bookings;
}
