package it.cinofilo.bookings.availability;

import it.cinofilo.bookings.Booking;
import it.cinofilo.bookings.BookingRepository;
import it.cinofilo.bookings.BookingStatus;
import it.cinofilo.bookings.InvalidDateRangeException;
import it.cinofilo.bookings.OverbookingException;
import it.cinofilo.tenancy.Tenant;
import it.cinofilo.tenancy.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingAvailabilityService {

    private final TenantRepository tenantRepository;
    private final BookingRepository bookingRepository;

    /**
     * Get daily availability for a tenant in a date range.
     *
     * @param tenantId tenant ID
     * @param start start date (inclusive)
     * @param end end date (exclusive)
     * @return list of daily availability
     */
    public List<DailyAvailability> getDailyAvailability(UUID tenantId, LocalDate start, LocalDate end) {
        validateDateRange(start, end);

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));
        int capacity = tenant.getCapacityBoxes();

        List<Booking> overlapping = bookingRepository.findConfirmedOverlapping(tenantId, start, end);
        Map<LocalDate, Integer> occupancyMap = buildOccupancyMap(overlapping, start, end);

        return start.datesUntil(end)
                .map(date -> new DailyAvailability(
                        date,
                        capacity,
                        occupancyMap.getOrDefault(date, 0),
                        capacity - occupancyMap.getOrDefault(date, 0)
                ))
                .toList();
    }

    /**
     * Check if a booking can be created without exceeding capacity.
     * Throws OverbookingException if capacity would be exceeded.
     *
     * @param tenantId tenant ID
     * @param start start date (inclusive)
     * @param end end date (exclusive)
     * @param excludeId booking ID to exclude from overlap check (for updates)
     */
    public void checkCanBookOrThrow(UUID tenantId, LocalDate start, LocalDate end, UUID excludeId) {
        validateDateRange(start, end);

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));
        int capacity = tenant.getCapacityBoxes();

        List<Booking> overlapping = excludeId == null
                ? bookingRepository.findConfirmedOverlapping(tenantId, start, end)
                : bookingRepository.findConfirmedOverlappingExcludingId(tenantId, start, end, excludeId);

        Map<LocalDate, Integer> occupancyMap = buildOccupancyMap(overlapping, start, end);

        for (LocalDate date = start; date.isBefore(end); date = date.plusDays(1)) {
            int booked = occupancyMap.getOrDefault(date, 0);
            if (booked >= capacity) {
                throw new OverbookingException(
                        "Capacity exceeded for date " + date,
                        date,
                        capacity,
                        booked
                );
            }
        }
    }

    /**
     * Build a map of dates to occupancy count.
     *
     * @param bookings bookings to count
     * @param start start date (inclusive)
     * @param end end date (exclusive)
     * @return map of date to count
     */
    private Map<LocalDate, Integer> buildOccupancyMap(List<Booking> bookings, LocalDate start, LocalDate end) {
        Map<LocalDate, Integer> occupancyMap = new HashMap<>();

        for (Booking booking : bookings) {
            LocalDate bookingStart = booking.getStartDate().isAfter(start) ? booking.getStartDate() : start;
            LocalDate bookingEnd = booking.getEndDate().isBefore(end) ? booking.getEndDate() : end;

            for (LocalDate date = bookingStart; date.isBefore(bookingEnd); date = date.plusDays(1)) {
                occupancyMap.put(date, occupancyMap.getOrDefault(date, 0) + 1);
            }
        }

        return occupancyMap;
    }

    /**
     * Validate that end date is after start date.
     *
     * @param start start date
     * @param end end date
     */
    private void validateDateRange(LocalDate start, LocalDate end) {
        if (!end.isAfter(start)) {
            throw new InvalidDateRangeException("End date must be after start date");
        }
    }
}
