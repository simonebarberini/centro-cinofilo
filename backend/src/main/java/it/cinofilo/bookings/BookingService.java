package it.cinofilo.bookings;

import it.cinofilo.bookings.availability.BookingAvailabilityService;
import it.cinofilo.bookings.dto.BookingResponse;
import it.cinofilo.bookings.dto.CreateBookingRequest;
import it.cinofilo.bookings.dto.UpdateBookingRequest;
import it.cinofilo.customer.Customer;
import it.cinofilo.customer.CustomerNotFoundException;
import it.cinofilo.customer.CustomerRepository;
import it.cinofilo.dogs.Dog;
import it.cinofilo.dogs.DogNotFoundException;
import it.cinofilo.dogs.DogRepository;
import it.cinofilo.domain.entitlement.Entitlements;
import it.cinofilo.entitlements.EntitlementService;
import it.cinofilo.entitlements.EntitlementViolationException;
import it.cinofilo.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CustomerRepository customerRepository;
    private final DogRepository dogRepository;
    private final BookingAvailabilityService availabilityService;
    private final TenantCapacityGuard tenantCapacityGuard;
    private final EntitlementService entitlementService;

    /**
     * Create a new booking for the current tenant.
     * Validates customer and dog belong to the tenant and checks availability.
     *
     * @param request the booking creation request
     * @return the created booking
     * @throws CustomerNotFoundException if customer not found or doesn't belong to tenant
     * @throws DogNotFoundException if dog not found or doesn't belong to tenant
     * @throws OverbookingException if capacity would be exceeded
     */
    public BookingResponse create(CreateBookingRequest request) {
        UUID tenantId = TenantContext.getTenantId();

        if (!entitlementService.isEnabled(tenantId, Entitlements.BOOKING_MANAGEMENT.key())) {
            throw new EntitlementViolationException("Booking management is not enabled for this tenant");
        }

        // Verify customer belongs to tenant
        Customer customer = customerRepository.findByIdAndTenantId(request.getCustomerId(), tenantId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + request.getCustomerId()));

        // Verify dog belongs to tenant
        Dog dog = dogRepository.findByIdAndTenantId(request.getDogId(), tenantId)
                .orElseThrow(() -> new DogNotFoundException("Dog not found with ID: " + request.getDogId()));

        // Validate date range before entering the capacity-critical section
        validateDateRange(request.getStartDate(), request.getEndDate());

        Booking booking = Booking.builder()
                .tenantId(tenantId)
                .customer(customer)
                .dog(dog)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .notes(request.getNotes())
                .status(BookingStatus.CONFIRMED)
                .build();

        // Capacity check + persist run under a per-tenant lock so two concurrent
        // requests cannot both pass the check and overbook the centre.
        return tenantCapacityGuard.executeForTenant(tenantId, () -> {
            availabilityService.checkCanBookOrThrow(tenantId, request.getStartDate(), request.getEndDate(), null);
            Booking saved = bookingRepository.save(booking);
            return toResponse(saved);
        });
    }

    /**
     * Get all bookings for the current tenant.
     *
     * @return list of bookings
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getAllForCurrentTenant() {
        UUID tenantId = TenantContext.getTenantId();
        return bookingRepository.findAllByTenantId(tenantId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a booking by ID, ensuring it belongs to the current tenant.
     *
     * @param id the booking ID
     * @return the booking
     * @throws BookingNotFoundException if booking not found or doesn't belong to tenant
     */
    @Transactional(readOnly = true)
    public BookingResponse getById(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        Booking booking = bookingRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + id));
        return toResponse(booking);
    }

    /**
     * Update a booking, ensuring it belongs to the current tenant.
     * Checks availability if dates are changed.
     *
     * @param id the booking ID
     * @param request the update request
     * @return the updated booking
     * @throws BookingNotFoundException if booking not found or doesn't belong to tenant
     * @throws OverbookingException if date changes would exceed capacity
     */
    public BookingResponse update(UUID id, UpdateBookingRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        Booking booking = bookingRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + id));

        LocalDate newStartDate = request.getStartDate() != null ? request.getStartDate() : booking.getStartDate();
        LocalDate newEndDate = request.getEndDate() != null ? request.getEndDate() : booking.getEndDate();
        BookingStatus newStatus = request.getStatus() != null ? request.getStatus() : booking.getStatus();

        // Validate date range if dates are provided (pure request validation,
        // no occupancy state touched yet).
        if (request.getStartDate() != null || request.getEndDate() != null) {
            validateDateRange(newStartDate, newEndDate);
        }

        // Capacity check (when the result is CONFIRMED), the occupancy-changing
        // mutations and the persist all run under a per-tenant lock so the tenant
        // row is the first occupancy-changing database action and concurrent
        // updates cannot overbook the centre.
        return tenantCapacityGuard.executeForTenant(tenantId, () -> {
            if (newStatus == BookingStatus.CONFIRMED) {
                availabilityService.checkCanBookOrThrow(tenantId, newStartDate, newEndDate, booking.getId());
            }
            if (request.getStartDate() != null) {
                booking.setStartDate(request.getStartDate());
            }
            if (request.getEndDate() != null) {
                booking.setEndDate(request.getEndDate());
            }
            if (request.getNotes() != null) {
                booking.setNotes(request.getNotes());
            }
            if (request.getStatus() != null) {
                booking.setStatus(request.getStatus());
            }
            Booking updated = bookingRepository.save(booking);
            return toResponse(updated);
        });
    }

    /**
     * Cancel a booking by setting its status to CANCELLED.
     *
     * @param id the booking ID
     * @return the cancelled booking
     * @throws BookingNotFoundException if booking not found or doesn't belong to tenant
     */
    public BookingResponse cancel(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        Booking booking = bookingRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with ID: " + id));

        // Cancellation lowers occupancy and cannot overbook, but it still flows
        // through the guard so that every occupancy-changing operation is
        // serialized through a single, explicit path.
        return tenantCapacityGuard.executeForTenant(tenantId, () -> {
            booking.setStatus(BookingStatus.CANCELLED);
            Booking cancelled = bookingRepository.save(booking);
            return toResponse(cancelled);
        });
    }

    /**
     * Convert Booking entity to BookingResponse DTO.
     */
    private BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .customerId(booking.getCustomer().getId())
                .dogId(booking.getDog().getId())
                .startDate(booking.getStartDate())
                .endDate(booking.getEndDate())
                .notes(booking.getNotes())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            throw new InvalidDateRangeException("End date must be after start date");
        }
    }
}
