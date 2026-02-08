package it.cinofilo.bookings.dto;

import it.cinofilo.bookings.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for Booking entities.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private UUID id;
    private UUID customerId;
    private UUID dogId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String notes;
    private BookingStatus status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
