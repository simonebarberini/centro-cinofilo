package it.cinofilo.dogs.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for updating an existing dog.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDogRequest {

    @NotBlank(message = "Name is required")
    private String name;

    private String breed;

    private LocalDate birthDate;

    private String notes;
}
