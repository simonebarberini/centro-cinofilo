package it.cinofilo.auth;

import it.cinofilo.security.JwtService;
import it.cinofilo.users.AppUser;
import it.cinofilo.users.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${jwt.expiration-ms:28800000}")
    private long expirationMs;

    public LoginResponse login(LoginRequest request) {
        // Trova user per username (query su tutte le tenant)
        AppUser user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        // Verifica che sia abilitato
        if (!user.getEnabled()) {
            throw new UnauthorizedException("User is disabled");
        }

        // Verifica password con BCrypt
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        // Genera JWT
        String token = jwtService.generateToken(
                user.getId(),
                user.getTenant().getId(),
                user.getRole().name(),
                user.getUsername()
        );

        return new LoginResponse(token, expirationMs / 1000); // Ritorna secondi
    }
}
