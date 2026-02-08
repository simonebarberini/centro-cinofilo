package it.cinofilo.bookings.calendar.dto;

import it.cinofilo.bookings.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
public class CalendarBookingItem {
    private UUID id;
    private LocalDate startDate;
    private LocalDate endDate;
    private BookingStatus status;
    private String notes;
    private CustomerSummary customer;
    private DogSummary dog;

    @Data
    @AllArgsConstructor
    public static class CustomerSummary {
        private UUID id;
        private String firstName;
        private String lastName;
    }

    @Data
    @AllArgsConstructor
    public static class DogSummary {
        private UUID id;
        private String name;
    }
}
