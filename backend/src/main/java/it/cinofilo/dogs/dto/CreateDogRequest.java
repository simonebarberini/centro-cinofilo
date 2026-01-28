package it.cinofilo.dogs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating a new dog.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDogRequest {

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotBlank(message = "Name is required")
    private String name;

    private String breed;

    private LocalDate birthDate;

    private String notes;
}
