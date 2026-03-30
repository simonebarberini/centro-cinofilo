package it.cinofilo.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import it.cinofilo.AbstractPostgresIT;
import it.cinofilo.security.JwtService;
import it.cinofilo.tenancy.Role;
import it.cinofilo.tenancy.Tenant;
import it.cinofilo.tenancy.TenantRepository;
import it.cinofilo.users.AppUser;
import it.cinofilo.users.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthControllerIT extends AbstractPostgresIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    // Evita connessioni SMTP reali durante i test
    @MockBean
    private JavaMailSender mailSender;

    private Tenant testTenant;
    private AppUser testUser;
    private static final String TEST_PASSWORD = "Password123!";
    private String uniqueSlug;

    @BeforeEach
    void setUp() {
        appUserRepository.deleteAll();
        tenantRepository.deleteAll();

        uniqueSlug = "test-tenant-" + UUID.randomUUID();
        testTenant = Tenant.builder()
                .name("Test Tenant")
                .type("PENSIONE")
                .slug(uniqueSlug)
                .capacityBoxes(10)
                .build();
        testTenant = tenantRepository.save(testTenant);

        testUser = AppUser.builder()
                .tenant(testTenant)
                .username("testowner")
                .passwordHash(passwordEncoder.encode(TEST_PASSWORD))
                .role(Role.TENANT_OWNER)
                .enabled(true)
                .email("testowner@example.com")
                .emailVerified(true)
                .build();
        testUser = appUserRepository.save(testUser);
    }

    @Test
    void shouldLoginSuccessfullyAndReturnToken() throws Exception {
        LoginRequest request = new LoginRequest(uniqueSlug, "testowner", TEST_PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.expiresIn").value(greaterThan(0)));
    }

    @Test
    void shouldReturn401OnInvalidPassword() throws Exception {
        LoginRequest request = new LoginRequest(uniqueSlug, "testowner", "WrongPassword");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401OnNonExistentUser() throws Exception {
        LoginRequest request = new LoginRequest(uniqueSlug, "nonexistent", TEST_PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401OnNonExistentTenant() throws Exception {
        LoginRequest request = new LoginRequest("nonexistent-tenant", "testowner", TEST_PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401OnDisabledUser() throws Exception {
        AppUser disabledUser = AppUser.builder()
                .tenant(testTenant)
                .username("disabled")
                .passwordHash(passwordEncoder.encode(TEST_PASSWORD))
                .role(Role.TENANT_STAFF)
                .enabled(false)
                .email("disabled@example.com")
                .emailVerified(true)
                .build();
        appUserRepository.save(disabledUser);

        LoginRequest request = new LoginRequest(uniqueSlug, "disabled", TEST_PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenEmailNotVerified() throws Exception {
        AppUser unverifiedUser = AppUser.builder()
                .tenant(testTenant)
                .username("unverified")
                .passwordHash(passwordEncoder.encode(TEST_PASSWORD))
                .role(Role.TENANT_STAFF)
                .enabled(true)
                .email("unverified@example.com")
                .emailVerified(false)
                .build();
        appUserRepository.save(unverifiedUser);

        LoginRequest request = new LoginRequest(uniqueSlug, "unverified", TEST_PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("Email non verificata")));
    }

    @Test
    void shouldLoginSuccessfullyAndTokenIsValidated() throws Exception {
        LoginRequest loginRequest = new LoginRequest(uniqueSlug, "testowner", TEST_PASSWORD);
        String loginResponseStr = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        LoginResponse response = objectMapper.readValue(loginResponseStr, LoginResponse.class);
        String token = response.getToken();

        Claims claims = jwtService.validateAndExtractClaims(token);
        assertNotNull(claims);
        assertEquals("testowner", claims.get("username"));
        assertEquals("TENANT_OWNER", claims.get("role"));
    }

    @Test
    void shouldReturn401OnMissingToken() throws Exception {
        mockMvc.perform(get("/some-protected-endpoint"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401OnInvalidToken() throws Exception {
        String invalidToken = "invalid.jwt.token";

        mockMvc.perform(get("/some-protected-endpoint")
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn400OnMissingLoginFields() throws Exception {
        String invalidRequest = "{\"tenantSlug\":\"\",\"username\":\"test\",\"password\":\"test\"}";

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldAllowAnonymousAccessToLoginEndpoint() throws Exception {
        LoginRequest request = new LoginRequest(uniqueSlug, "testowner", TEST_PASSWORD);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void shouldRequireAuthenticationForProtectedEndpoints() throws Exception {
        mockMvc.perform(get("/customers")
                        .header("X-Tenant-Slug", uniqueSlug))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRegisterAndReturnPendingVerificationResponse() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Nuovo Centro", "PENSIONE", "nuovo-centro-" + UUID.randomUUID(),
                "nuovoowner", "Password123!", "nuovoowner@example.com"
        );

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.email").value("nuovoowner@example.com"));
    }
}
