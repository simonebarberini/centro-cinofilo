package it.cinofilo.bookings.calendar;

import it.cinofilo.bookings.Booking;
import it.cinofilo.bookings.BookingRepository;
import it.cinofilo.bookings.InvalidDateRangeException;
import it.cinofilo.bookings.availability.BookingAvailabilityService;
import it.cinofilo.bookings.availability.DailyAvailability;
import it.cinofilo.bookings.calendar.dto.CalendarBookingItem;
import it.cinofilo.bookings.calendar.dto.CalendarResponse;
import it.cinofilo.tenancy.Tenant;
import it.cinofilo.tenancy.TenantContext;
import it.cinofilo.tenancy.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingCalendarService {

    private final BookingAvailabilityService availabilityService;
    private final BookingRepository bookingRepository;
    private final TenantRepository tenantRepository;

    /**
     * Get calendar with days availability and bookings for a date range.
     *
     * @param start start date (inclusive)
     * @param end end date (exclusive)
     * @return calendar response with days and bookings
     */
    public CalendarResponse getCalendar(LocalDate start, LocalDate end) {
        if (!end.isAfter(start)) {
            throw new InvalidDateRangeException("End date must be after start date");
        }

        UUID tenantId = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));

        // Get daily availability
        List<DailyAvailability> days = availabilityService.getDailyAvailability(tenantId, start, end);

        // Get all bookings in range (CONFIRMED, CANCELLED, PENDING - all statuses for calendar view)
        List<Booking> bookingsInRange = bookingRepository.findAllInRangeWithDetails(tenantId, start, end);

        // Map to DTOs
        List<CalendarBookingItem> items = bookingsInRange.stream()
                .map(this::toCalendarBookingItem)
                .toList();

        return new CalendarResponse(start, end, tenant.getCapacityBoxes(), days, items);
    }

    private CalendarBookingItem toCalendarBookingItem(Booking booking) {
        CalendarBookingItem.CustomerSummary customerSummary = null;
        if (booking.getCustomer() != null) {
            customerSummary = new CalendarBookingItem.CustomerSummary(
                    booking.getCustomer().getId(),
                    booking.getCustomer().getFirstName(),
                    booking.getCustomer().getLastName()
            );
        }

        CalendarBookingItem.DogSummary dogSummary = null;
        if (booking.getDog() != null) {
            dogSummary = new CalendarBookingItem.DogSummary(
                    booking.getDog().getId(),
                    booking.getDog().getName()
            );
        }

        return new CalendarBookingItem(
                booking.getId(),
                booking.getStartDate(),
                booking.getEndDate(),
                booking.getStatus(),
                booking.getNotes(),
                customerSummary,
                dogSummary
        );
    }
}
