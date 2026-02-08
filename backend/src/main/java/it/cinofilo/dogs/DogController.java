package it.cinofilo.dogs;

import it.cinofilo.dogs.dto.CreateDogRequest;
import it.cinofilo.dogs.dto.DogResponse;
import it.cinofilo.dogs.dto.UpdateDogRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for dog management.
 * All endpoints require authentication and are tenant-isolated.
 */
@RestController
@RequestMapping("/dogs")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DogController {

    private final DogService dogService;

    /**
     * Create a new dog for a customer.
     */
    @PostMapping
    public ResponseEntity<DogResponse> createDog(@Valid @RequestBody CreateDogRequest request) {
        DogResponse dog = dogService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dog);
    }

    /**
     * List all dogs in the current tenant.
     */
    @GetMapping
    public ResponseEntity<List<DogResponse>> listDogs() {
        List<DogResponse> dogs = dogService.listAllForTenant();
        return ResponseEntity.ok(dogs);
    }

    /**
     * List all dogs for a specific customer.
     */
    @GetMapping("/by-customer/{customerId}")
    public ResponseEntity<List<DogResponse>> listDogsByCustomer(@PathVariable UUID customerId) {
        List<DogResponse> dogs = dogService.listByCustomer(customerId);
        return ResponseEntity.ok(dogs);
    }

    /**
     * Get a specific dog by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DogResponse> getDog(@PathVariable UUID id) {
        DogResponse dog = dogService.getById(id);
        return ResponseEntity.ok(dog);
    }

    /**
     * Update an existing dog.
     */
    @PutMapping("/{id}")
    public ResponseEntity<DogResponse> updateDog(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDogRequest request) {
        DogResponse dog = dogService.update(id, request);
        return ResponseEntity.ok(dog);
    }

    /**
     * Delete a dog.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDog(@PathVariable UUID id) {
        dogService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
