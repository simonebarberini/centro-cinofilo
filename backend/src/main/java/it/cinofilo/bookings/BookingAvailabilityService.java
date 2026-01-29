package it.cinofilo.bookings;

import it.cinofilo.tenancy.Tenant;
import it.cinofilo.tenancy.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for checking booking availability and preventing overbooking.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingAvailabilityService {

    private final BookingRepository bookingRepository;
    private final TenantRepository tenantRepository;

    /**
     * Check if a booking can be made without exceeding capacity.
     * 
     * @param tenantId the tenant ID
     * @param startDate the requested start date (inclusive)
     * @param endDate the requested end date (exclusive)
     * @param excludeBookingId optional booking ID to exclude from check (for updates)
     * @throws OverbookingException if capacity would be exceeded on any date
     */
    public void checkAvailability(UUID tenantId, LocalDate startDate, LocalDate endDate, UUID excludeBookingId) {
        log.debug("Checking availability for tenant {} from {} to {} (excluding booking {})", 
                tenantId, startDate, endDate, excludeBookingId);

        // Get tenant capacity
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found: " + tenantId));
        
        int capacity = tenant.getCapacityBoxes();
        log.debug("Tenant capacity: {} boxes", capacity);

        // Find all confirmed bookings that overlap with requested range
        List<Booking> overlappingBookings = bookingRepository
                .findAllByTenantIdAndStartDateLessThanAndEndDateGreaterThan(tenantId, endDate, startDate);

        // Filter only CONFIRMED bookings and exclude the booking being updated
        List<Booking> confirmedBookings = overlappingBookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .filter(b -> excludeBookingId == null || !b.getId().equals(excludeBookingId))
                .toList();

        log.debug("Found {} confirmed overlapping bookings", confirmedBookings.size());

        // Build occupancy map for each day in the requested range
        Map<LocalDate, Integer> occupancyByDate = new HashMap<>();
        
        // Initialize all days in range with 0
        LocalDate currentDate = startDate;
        while (currentDate.isBefore(endDate)) {
            occupancyByDate.put(currentDate, 0);
            currentDate = currentDate.plusDays(1);
        }

        // Count occupancy for each day
        for (Booking booking : confirmedBookings) {
            // Find intersection of booking with requested range
            LocalDate intersectionStart = booking.getStartDate().isAfter(startDate) 
                    ? booking.getStartDate() : startDate;
            LocalDate intersectionEnd = booking.getEndDate().isBefore(endDate) 
                    ? booking.getEndDate() : endDate;

            // Increment count for each day in the intersection
            LocalDate date = intersectionStart;
            while (date.isBefore(intersectionEnd)) {
                occupancyByDate.merge(date, 1, Integer::sum);
                date = date.plusDays(1);
            }
        }

        // Check if adding one more booking would exceed capacity on any day
        for (Map.Entry<LocalDate, Integer> entry : occupancyByDate.entrySet()) {
            int currentOccupancy = entry.getValue();
            int newOccupancy = currentOccupancy + 1;
            
            log.debug("Date {}: current occupancy {}, after new booking {}, capacity {}", 
                    entry.getKey(), currentOccupancy, newOccupancy, capacity);

            if (newOccupancy > capacity) {
                String message = String.format(
                        "Capacity exceeded on %s: %d bookings would exceed capacity of %d boxes",
                        entry.getKey(), newOccupancy, capacity);
                log.warn(message);
                throw new OverbookingException(message, entry.getKey());
            }
        }

        log.debug("Availability check passed for tenant {} from {} to {}", tenantId, startDate, endDate);
    }

    /**
     * Check availability for a new booking.
     */
    public void checkAvailability(UUID tenantId, LocalDate startDate, LocalDate endDate) {
        checkAvailability(tenantId, startDate, endDate, null);
    }
}
