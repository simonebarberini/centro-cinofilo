package it.cinofilo.dogs;

import it.cinofilo.customer.Customer;
import it.cinofilo.customer.CustomerNotFoundException;
import it.cinofilo.customer.CustomerRepository;
import it.cinofilo.dogs.dto.CreateDogRequest;
import it.cinofilo.dogs.dto.DogResponse;
import it.cinofilo.dogs.dto.UpdateDogRequest;
import it.cinofilo.domain.entitlement.Entitlements;
import it.cinofilo.entitlements.EntitlementService;
import it.cinofilo.entitlements.EntitlementViolationException;
import it.cinofilo.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing dogs with multi-tenant isolation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DogService {

    private final DogRepository dogRepository;
    private final CustomerRepository customerRepository;
    private final EntitlementService entitlementService;

    /**
     * Create a new dog for a customer.
     * Verifies that the customer belongs to the current tenant.
     */
    @Transactional
    public DogResponse create(CreateDogRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        log.debug("Creating dog for customer {} in tenant {}", request.getCustomerId(), tenantId);

        if (!entitlementService.isEnabled(tenantId, Entitlements.DOG_MANAGEMENT.key())) {
            throw new EntitlementViolationException("Dog management is not enabled for this tenant");
        }
        int quota = entitlementService.getQuota(tenantId, Entitlements.MAX_DOGS_PER_TENANT.key());
        if (quota > 0 && dogRepository.countByTenantId(tenantId) >= quota) {
            throw new EntitlementViolationException(
                    "Dog quota exceeded: maximum " + quota + " dogs allowed");
        }

        // Verify customer exists and belongs to current tenant
        Customer customer = customerRepository.findByIdAndTenantId(request.getCustomerId(), tenantId)
                .orElseThrow(() -> new CustomerNotFoundException(
                        "Customer not found or does not belong to current tenant"));

        Dog dog = Dog.builder()
                .tenantId(tenantId)
                .customer(customer)
                .name(request.getName())
                .breed(request.getBreed())
                .birthDate(request.getBirthDate())
                .notes(request.getNotes())
                .build();

        dog = dogRepository.save(dog);
        log.info("Created dog {} for customer {} in tenant {}", dog.getId(), customer.getId(), tenantId);

        return mapToResponse(dog);
    }

    /**
     * List all dogs for the current tenant.
     */
    @Transactional(readOnly = true)
    public List<DogResponse> listAllForTenant() {
        UUID tenantId = TenantContext.getTenantId();
        log.debug("Listing all dogs for tenant {}", tenantId);

        return dogRepository.findAllByTenantId(tenantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * List all dogs for a specific customer.
     * Returns 404 if customer doesn't exist in current tenant.
     */
    @Transactional(readOnly = true)
    public List<DogResponse> listByCustomer(UUID customerId) {
        UUID tenantId = TenantContext.getTenantId();
        log.debug("Listing dogs for customer {} in tenant {}", customerId, tenantId);

        // Verify customer exists in current tenant
        if (!customerRepository.existsByIdAndTenantId(customerId, tenantId)) {
            throw new CustomerNotFoundException("Customer not found in current tenant");
        }

        return dogRepository.findAllByTenantIdAndCustomerId(tenantId, customerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a dog by ID.
     * Returns 404 if not found or belongs to different tenant.
     */
    @Transactional(readOnly = true)
    public DogResponse getById(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        log.debug("Getting dog {} in tenant {}", id, tenantId);

        Dog dog = dogRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new DogNotFoundException("Dog not found"));

        return mapToResponse(dog);
    }

    /**
     * Update a dog.
     * Returns 404 if not found or belongs to different tenant.
     */
    @Transactional
    public DogResponse update(UUID id, UpdateDogRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        log.debug("Updating dog {} in tenant {}", id, tenantId);

        Dog dog = dogRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new DogNotFoundException("Dog not found"));

        dog.setName(request.getName());
        dog.setBreed(request.getBreed());
        dog.setBirthDate(request.getBirthDate());
        dog.setNotes(request.getNotes());

        dog = dogRepository.save(dog);
        log.info("Updated dog {} in tenant {}", id, tenantId);

        return mapToResponse(dog);
    }

    /**
     * Delete a dog.
     * Returns 404 if not found or belongs to different tenant.
     */
    @Transactional
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        log.debug("Deleting dog {} in tenant {}", id, tenantId);

        if (!dogRepository.existsByIdAndTenantId(id, tenantId)) {
            throw new DogNotFoundException("Dog not found");
        }

        dogRepository.deleteById(id);
        log.info("Deleted dog {} in tenant {}", id, tenantId);
    }

    private DogResponse mapToResponse(Dog dog) {
        return DogResponse.builder()
                .id(dog.getId())
                .customerId(dog.getCustomer().getId())
                .name(dog.getName())
                .breed(dog.getBreed())
                .birthDate(dog.getBirthDate())
                .notes(dog.getNotes())
                .createdAt(dog.getCreatedAt())
                .updatedAt(dog.getUpdatedAt())
                .build();
    }
}
