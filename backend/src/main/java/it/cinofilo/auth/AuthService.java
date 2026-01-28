package it.cinofilo.auth;

import it.cinofilo.security.JwtService;
import it.cinofilo.tenancy.Tenant;
import it.cinofilo.tenancy.TenantRepository;
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
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Value("${jwt.expiration-ms:28800000}")
    private long expirationMs;

    public LoginResponse login(LoginRequest request) {
        // Resolve tenant by slug
        Tenant tenant = tenantRepository.findBySlug(request.getTenantSlug())
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + request.getTenantSlug()));

        // Find user by tenantId and username
        AppUser user = userRepository.findByTenantIdAndUsername(tenant.getId(), request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        // Verify user is enabled
        if (!user.getEnabled()) {
            throw new UnauthorizedException("User is disabled");
        }

        // Verify password with BCrypt
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        // Generate JWT with tenant context
        String token = jwtService.generateToken(
                user.getId(),
                user.getTenant().getId(),
                user.getRole().name(),
                user.getUsername()
        );

        return new LoginResponse(token, expirationMs / 1000); // Return seconds
    }
}
