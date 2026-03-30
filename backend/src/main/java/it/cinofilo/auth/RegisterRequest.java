package it.cinofilo.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Il nome del centro è obbligatorio")
    @Size(max = 255, message = "Il nome del centro non può superare 255 caratteri")
    private String tenantName;

    @NotBlank(message = "Il tipo di centro è obbligatorio")
    @Size(max = 100, message = "Il tipo non può superare 100 caratteri")
    private String tenantType;

    @NotBlank(message = "Lo slug del centro è obbligatorio")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Lo slug può contenere solo lettere minuscole, numeri e trattini")
    @Size(max = 100, message = "Lo slug non può superare 100 caratteri")
    private String tenantSlug;

    @NotBlank(message = "Lo username è obbligatorio")
    @Size(max = 100, message = "Lo username non può superare 100 caratteri")
    private String username;

    @NotBlank(message = "La password è obbligatoria")
    @Size(min = 8, message = "La password deve essere di almeno 8 caratteri")
    private String password;

    @NotBlank(message = "L'email è obbligatoria")
    @Email(message = "Email non valida")
    @Size(max = 255, message = "L'email non può superare 255 caratteri")
    private String email;
}
