package it.cinofilo.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ResetPasswordRequest {

    @NotBlank(message = "Il token è obbligatorio")
    private String token;

    @NotBlank(message = "La nuova password è obbligatoria")
    @Size(min = 8, message = "La password deve essere di almeno 8 caratteri")
    private String newPassword;
}
