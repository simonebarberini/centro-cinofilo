package it.cinofilo.bookings;

import it.cinofilo.bookings.dto.BookingResponse;
import it.cinofilo.bookings.dto.CreateBookingRequest;
import it.cinofilo.bookings.dto.UpdateBookingRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class BookingController {

    private final BookingService bookingService;

    /**
     * Create a new booking.
     *
     * @param request the booking creation request
     * @return the created booking
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody CreateBookingRequest request) {
        return bookingService.create(request);
    }

    /**
     * Get all bookings for the current tenant.
     *
     * @return list of bookings
     */
    @GetMapping
    public List<BookingResponse> getAll() {
        return bookingService.getAllForCurrentTenant();
    }

    /**
     * Get a specific booking by ID.
     *
     * @param id the booking ID
     * @return the booking
     */
    @GetMapping("/{id}")
    public BookingResponse getById(@PathVariable UUID id) {
        return bookingService.getById(id);
    }

    /**
     * Update an existing booking.
     *
     * @param id the booking ID
     * @param request the update request
     * @return the updated booking
     */
    @PutMapping("/{id}")
    public BookingResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateBookingRequest request) {
        return bookingService.update(id, request);
    }

    /**
     * Cancel a booking.
     *
     * @param id the booking ID
     * @return the cancelled booking
     */
    @PostMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable UUID id) {
        return bookingService.cancel(id);
    }
}
