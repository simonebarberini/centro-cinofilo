package it.cinofilo.tenancy.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenantSettingsRequest {

    @Min(value = 0, message = "capacityBoxes deve essere >= 0")
    private Integer capacityBoxes;

    private Map<String, Object> preferences;
}
