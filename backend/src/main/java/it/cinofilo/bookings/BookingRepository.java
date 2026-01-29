package it.cinofilo.bookings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Booking entity with tenant-isolated queries.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    /**
     * Find all bookings for a specific tenant.
     */
    List<Booking> findAllByTenantId(UUID tenantId);

    /**
     * Find a booking by ID within a specific tenant.
     */
    Optional<Booking> findByIdAndTenantId(UUID id, UUID tenantId);

    /**
     * Find bookings that overlap with a given date range.
     * Overlap condition: booking.startDate < requestedEndDate AND booking.endDate > requestedStartDate
     * 
     * Used for availability checks.
     * 
     * @param tenantId the tenant ID
     * @param endDateExclusive the requested end date (exclusive)
     * @param startDateInclusive the requested start date (inclusive)
     * @return list of overlapping bookings
     */
    List<Booking> findAllByTenantIdAndStartDateLessThanAndEndDateGreaterThan(
            UUID tenantId, 
            LocalDate endDateExclusive, 
            LocalDate startDateInclusive);

    /**
     * Find all bookings for a specific dog within a tenant.
     */
    List<Booking> findAllByTenantIdAndDogId(UUID tenantId, UUID dogId);

    /**
     * Find all bookings for a specific customer within a tenant.
     */
    List<Booking> findAllByTenantIdAndCustomerId(UUID tenantId, UUID customerId);
}
