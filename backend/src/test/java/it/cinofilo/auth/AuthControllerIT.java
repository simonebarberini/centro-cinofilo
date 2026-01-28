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
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

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

    private Tenant testTenant;
    private AppUser testUser;
    private static final String TEST_PASSWORD = "Password123!";

    @BeforeEach
    void setUp() {
        // Clean database
        appUserRepository.deleteAll();
        tenantRepository.deleteAll();

        // Create test tenant
        testTenant = Tenant.builder()
                .name("Test Tenant")
                .type("PENSIONE")
                .capacityBoxes(10)
                .build();
        testTenant = tenantRepository.save(testTenant);

        // Create test user with BCrypt password
        testUser = AppUser.builder()
                .tenant(testTenant)
                .username("testowner")
                .passwordHash(passwordEncoder.encode(TEST_PASSWORD))
                .role(Role.TENANT_OWNER)
                .enabled(true)
                .build();
        testUser = appUserRepository.save(testUser);
    }

    @Test
    void shouldLoginSuccessfullyAndReturnToken() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("testowner", TEST_PASSWORD);

        // When/Then
        mockMvc.perform(post("/api/auth/login")
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
        // Given
        LoginRequest request = new LoginRequest("testowner", "WrongPassword");

        // When/Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401OnNonExistentUser() throws Exception {
        // Given
        LoginRequest request = new LoginRequest("nonexistent", TEST_PASSWORD);

        // When/Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401OnDisabledUser() throws Exception {
        // Given - Create disabled user
        AppUser disabledUser = AppUser.builder()
                .tenant(testTenant)
                .username("disabled")
                .passwordHash(passwordEncoder.encode(TEST_PASSWORD))
                .role(Role.TENANT_STAFF)
                .enabled(false)
                .build();
        appUserRepository.save(disabledUser);

        LoginRequest request = new LoginRequest("disabled", TEST_PASSWORD);

        // When/Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldLoginSuccessfullyAndTokenIsValidated() throws Exception {
        // Given - Get token through login
        LoginRequest loginRequest = new LoginRequest("testowner", TEST_PASSWORD);
        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        LoginResponse response = objectMapper.readValue(loginResponse, LoginResponse.class);
        String token = response.getToken();

        // Then - Verify token is valid and contains correct claims
        Claims claims = jwtService.validateAndExtractClaims(token);
        assertNotNull(claims);
        assertEquals("testowner", claims.get("username"));
        assertEquals("TENANT_OWNER", claims.get("role"));
    }

    @Test
    void shouldReturn401OnMissingToken() throws Exception {
        // Given - A protected endpoint that requires authentication
        // Note: Since all endpoints except /api/auth/** and /api/health require auth,
        // we'd need a custom endpoint to test this properly
        // For now, verify that accessing a non-existent protected endpoint without token fails
        
        // When/Then
        mockMvc.perform(get("/api/some-protected-endpoint"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401OnInvalidToken() throws Exception {
        // Given
        String invalidToken = "invalid.jwt.token";

        // When/Then
        mockMvc.perform(get("/api/some-protected-endpoint")
                        .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn400OnMissingLoginFields() throws Exception {
        // Given - Request with empty username
        String invalidRequest = "{\"username\":\"\",\"password\":\"test\"}";

        // When/Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }
}
