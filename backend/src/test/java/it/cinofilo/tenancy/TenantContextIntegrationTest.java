package it.cinofilo.tenancy;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.cinofilo.AbstractPostgresIT;
import it.cinofilo.auth.LoginRequest;
import it.cinofilo.auth.LoginResponse;
import it.cinofilo.customer.CustomerRepository;
import it.cinofilo.security.JwtService;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.profiles.active=test")
class TenantContextIntegrationTest extends AbstractPostgresIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private Tenant testTenant;
    private AppUser testUser;
    private static final String TEST_PASSWORD = "Password123!";
    private String uniqueSlug;

    @BeforeEach
    void setUp() {
        // Database is wiped by AbstractPostgresIT#cleanDatabase before each test.

        // Create test tenant with unique slug
        uniqueSlug = "test-tenant-" + UUID.randomUUID();
        testTenant = Tenant.builder()
                .name("Test Tenant for Context")
                .type("PENSIONE")
                .slug(uniqueSlug)
                .capacityBoxes(10)
                .build();
        testTenant = tenantRepository.save(testTenant);

        // Create test user with BCrypt password
        testUser = AppUser.builder()
                .tenant(testTenant)
                .username("contextuser")
                .passwordHash(passwordEncoder.encode(TEST_PASSWORD))
                .role(Role.TENANT_OWNER)
                .enabled(true)
                .emailVerified(true)
                .build();
        testUser = appUserRepository.save(testUser);
    }

    @Test
    void shouldPopulateTenantContextWhenCallingProtectedEndpointWithValidJwt() throws Exception {
        // Given - login to get JWT token
        LoginRequest loginRequest = new LoginRequest(uniqueSlug, "contextuser", TEST_PASSWORD);
        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        LoginResponse response = objectMapper.readValue(loginResponse, LoginResponse.class);
        String token = response.getToken();

        // When - call protected endpoint with valid JWT
        String contextResponse = mockMvc.perform(get("/_test/tenant-context/current")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        TenantContextTestController.TenantContextResponse contextData = 
            objectMapper.readValue(contextResponse, TenantContextTestController.TenantContextResponse.class);

        // Then - verify TenantContext contains the tenant ID
        assertThat(contextData.tenantId).isEqualTo(testTenant.getId());
    }

    @Test
    void shouldNotPopulateTenantContextWhenNotAuthenticated() throws Exception {
        // When - call protected endpoint without token
        mockMvc.perform(get("/_test/tenant-context/current")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldNotPopulateTenantContextWithInvalidToken() throws Exception {
        // When - call protected endpoint with invalid token
        mockMvc.perform(get("/_test/tenant-context/current")
                        .header("Authorization", "Bearer invalid.token.here")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
