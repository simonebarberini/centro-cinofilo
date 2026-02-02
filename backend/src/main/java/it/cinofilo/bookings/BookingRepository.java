package it.cinofilo.bookings;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
         * Find confirmed bookings that overlap with a given date range.
         * Overlap condition: booking.startDate < requestedEndDate AND booking.endDate > requestedStartDate
         */
        @Query("SELECT b FROM Booking b "
            + "WHERE b.tenantId = :tenantId "
            + "AND b.status = it.cinofilo.bookings.BookingStatus.CONFIRMED "
            + "AND b.startDate < :endDateExclusive "
            + "AND b.endDate > :startDateInclusive")
        List<Booking> findConfirmedOverlapping(
            @Param("tenantId") UUID tenantId,
            @Param("startDateInclusive") LocalDate startDateInclusive,
            @Param("endDateExclusive") LocalDate endDateExclusive);

        /**
         * Find confirmed bookings that overlap with a given date range, excluding a booking ID.
         */
        @Query("SELECT b FROM Booking b "
            + "WHERE b.tenantId = :tenantId "
            + "AND b.status = it.cinofilo.bookings.BookingStatus.CONFIRMED "
            + "AND b.startDate < :endDateExclusive "
            + "AND b.endDate > :startDateInclusive "
            + "AND b.id <> :excludeId")
        List<Booking> findConfirmedOverlappingExcludingId(
            @Param("tenantId") UUID tenantId,
            @Param("startDateInclusive") LocalDate startDateInclusive,
            @Param("endDateExclusive") LocalDate endDateExclusive,
            @Param("excludeId") UUID excludeId);

    /**
     * Find all bookings for a specific dog within a tenant.
     */
    List<Booking> findAllByTenantIdAndDogId(UUID tenantId, UUID dogId);

    /**
     * Find all bookings for a specific customer within a tenant.
     */
    List<Booking> findAllByTenantIdAndCustomerId(UUID tenantId, UUID customerId);
}
