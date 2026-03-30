package it.cinofilo.auth;

import it.cinofilo.security.JwtService;
import it.cinofilo.tenancy.Role;
import it.cinofilo.tenancy.Tenant;
import it.cinofilo.tenancy.TenantRepository;
import it.cinofilo.users.AppUser;
import it.cinofilo.users.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailTokenRepository emailTokenRepository;
    private final EmailService emailService;

    @Value("${jwt.expiration-ms:28800000}")
    private long expirationMs;

    public LoginResponse login(LoginRequest request) {
        Tenant tenant = tenantRepository.findBySlug(request.getTenantSlug())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        AppUser user = userRepository.findByTenantIdAndUsername(tenant.getId(), request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!user.getEnabled()) {
            throw new UnauthorizedException("User is disabled");
        }

        if (!user.getEmailVerified()) {
            throw new EmailNotVerifiedException("Email non verificata. Controlla la tua casella di posta.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String token = jwtService.generateToken(
                user.getId(),
                user.getTenant().getId(),
                user.getRole().name(),
                user.getUsername()
        );

        return new LoginResponse(token, expirationMs / 1000);
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (tenantRepository.existsBySlug(request.getTenantSlug())) {
            throw new ConflictException("Slug già in uso: " + request.getTenantSlug());
        }

        Tenant tenant = tenantRepository.save(Tenant.builder()
                .name(request.getTenantName())
                .type(request.getTenantType())
                .slug(request.getTenantSlug())
                .build());

        AppUser owner = userRepository.save(AppUser.builder()
                .tenant(tenant)
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(Role.TENANT_OWNER)
                .emailVerified(false)
                .build());

        String tokenValue = generateAndSaveToken(owner, EmailTokenType.EMAIL_VERIFICATION,
                Instant.now().plus(24, ChronoUnit.HOURS));
        emailService.sendVerificationEmail(owner.getEmail(), tokenValue);

        return new RegisterResponse(
                "Registrazione completata. Controlla la tua email per verificare l'account.",
                owner.getEmail()
        );
    }

    @Transactional
    public void verifyEmail(String tokenValue) {
        EmailToken token = emailTokenRepository
                .findByTokenAndType(tokenValue, EmailTokenType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new UnauthorizedException("Token non valido"));

        if (token.getUsed() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Token scaduto o già usato");
        }

        AppUser user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        token.setUsed(true);
        emailTokenRepository.save(token);
    }

    @Transactional
    public void resendVerification(ResendVerificationRequest request) {
        Tenant tenant = tenantRepository.findBySlug(request.getTenantSlug())
                .orElseThrow(() -> new TenantNotFoundException("Centro non trovato"));

        AppUser user = userRepository.findByTenantIdAndEmail(tenant.getId(), request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Nessun account trovato con questa email"));

        if (user.getEmailVerified()) {
            return; // già verificata, risposta silenziosa
        }

        emailTokenRepository.invalidateAllForUser(user.getId(), EmailTokenType.EMAIL_VERIFICATION);
        String tokenValue = generateAndSaveToken(user, EmailTokenType.EMAIL_VERIFICATION,
                Instant.now().plus(24, ChronoUnit.HOURS));
        emailService.sendVerificationEmail(user.getEmail(), tokenValue);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        Tenant tenant = tenantRepository.findBySlug(request.getTenantSlug())
                .orElseThrow(() -> new TenantNotFoundException("Centro non trovato"));

        // Risponde sempre 200 per prevenire email enumeration
        userRepository.findByTenantIdAndEmail(tenant.getId(), request.getEmail())
                .filter(AppUser::getEmailVerified)
                .ifPresent(user -> {
                    emailTokenRepository.invalidateAllForUser(user.getId(), EmailTokenType.PASSWORD_RESET);
                    String tokenValue = generateAndSaveToken(user, EmailTokenType.PASSWORD_RESET,
                            Instant.now().plus(1, ChronoUnit.HOURS));
                    emailService.sendPasswordResetEmail(user.getEmail(), tokenValue);
                });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        EmailToken token = emailTokenRepository
                .findByTokenAndType(request.getToken(), EmailTokenType.PASSWORD_RESET)
                .orElseThrow(() -> new UnauthorizedException("Token non valido o scaduto"));

        if (token.getUsed() || token.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Token scaduto o già usato");
        }

        AppUser user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setUsed(true);
        emailTokenRepository.save(token);
    }

    private String generateAndSaveToken(AppUser user, EmailTokenType type, Instant expiresAt) {
        String tokenValue = UUID.randomUUID().toString().replace("-", "");
        emailTokenRepository.save(EmailToken.builder()
                .user(user)
                .token(tokenValue)
                .type(type)
                .expiresAt(expiresAt)
                .build());
        return tokenValue;
    }
}
