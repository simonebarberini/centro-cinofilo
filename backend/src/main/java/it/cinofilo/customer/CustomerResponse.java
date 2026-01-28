package it.cinofilo.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerResponse {

    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    public static CustomerResponse fromEntity(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .notes(customer.getNotes())
                .createdAt(customer.getCreatedAt())
                .updatedAt(customer.getUpdatedAt())
                .build();
    }
}
