package it.cinofilo.customer;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Create a new customer for the current tenant.
     */
    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        Customer customer = Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .notes(request.getNotes())
                .build();

        Customer created = customerService.create(customer);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CustomerResponse.fromEntity(created));
    }

    /**
     * Get all customers for the current tenant.
     */
    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAll() {
        List<Customer> customers = customerService.getAllForCurrentTenant();
        List<CustomerResponse> responses = customers.stream()
                .map(CustomerResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Get a customer by ID (tenant-scoped).
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getById(@PathVariable UUID id) {
        Customer customer = customerService.getById(id);
        return ResponseEntity.ok(CustomerResponse.fromEntity(customer));
    }

    /**
     * Update a customer (tenant-scoped).
     */
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCustomerRequest request) {
        Customer customerUpdate = Customer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .notes(request.getNotes())
                .build();

        Customer updated = customerService.update(id, customerUpdate);
        return ResponseEntity.ok(CustomerResponse.fromEntity(updated));
    }

    /**
     * Delete a customer (tenant-scoped).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
