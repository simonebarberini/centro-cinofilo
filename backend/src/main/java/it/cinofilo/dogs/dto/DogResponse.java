package it.cinofilo.dogs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for dog data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DogResponse {

    private UUID id;
    private UUID customerId;
    private String name;
    private String breed;
    private LocalDate birthDate;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
