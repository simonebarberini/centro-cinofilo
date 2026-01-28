package it.cinofilo.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.cinofilo.AbstractPostgresIT;
import it.cinofilo.auth.LoginRequest;
import it.cinofilo.auth.LoginResponse;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomerControllerIT extends AbstractPostgresIT {

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

    private Tenant tenantA;
    private Tenant tenantB;
    private String tokenA;
    private String tokenB;
    private static final String TEST_PASSWORD = "Password123!";

    @BeforeEach
    void setUp() throws Exception {
        // Clean database
        customerRepository.deleteAll();
        appUserRepository.deleteAll();
        tenantRepository.deleteAll();

        // Create tenant A
        tenantA = Tenant.builder()
                .name("Tenant A")
                .type("PENSIONE")
                .slug("tenant-a-" + UUID.randomUUID())
                .capacityBoxes(10)
                .build();
        tenantA = tenantRepository.save(tenantA);

        // Create tenant B
        tenantB = Tenant.builder()
                .name("Tenant B")
                .type("ADDESTRAMENTO")
                .slug("tenant-b-" + UUID.randomUUID())
                .capacityBoxes(5)
                .build();
        tenantB = tenantRepository.save(tenantB);

        // Create user for tenant A
        AppUser userA = AppUser.builder()
                .tenant(tenantA)
                .username("userA")
                .passwordHash(passwordEncoder.encode(TEST_PASSWORD))
                .role(Role.TENANT_OWNER)
                .enabled(true)
                .build();
        appUserRepository.save(userA);

        // Create user for tenant B
        AppUser userB = AppUser.builder()
                .tenant(tenantB)
                .username("userB")
                .passwordHash(passwordEncoder.encode(TEST_PASSWORD))
                .role(Role.TENANT_OWNER)
                .enabled(true)
                .build();
        appUserRepository.save(userB);

        // Get JWT token for tenant A
        LoginRequest loginA = new LoginRequest(tenantA.getSlug(), "userA", TEST_PASSWORD);
        String loginResponseA = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginA)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        LoginResponse responseA = objectMapper.readValue(loginResponseA, LoginResponse.class);
        tokenA = responseA.getToken();

        // Get JWT token for tenant B
        LoginRequest loginB = new LoginRequest(tenantB.getSlug(), "userB", TEST_PASSWORD);
        String loginResponseB = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginB)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        LoginResponse responseB = objectMapper.readValue(loginResponseB, LoginResponse.class);
        tokenB = responseB.getToken();
    }

    @Test
    void shouldCreateCustomerForCurrentTenant() throws Exception {
        // Given
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phone("123456789")
                .notes("Test customer")
                .build();

        // When/Then
        mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void shouldListCustomersForCurrentTenant() throws Exception {
        // Given - create 2 customers in tenant A
        createCustomerInTenantA("John", "Doe");
        createCustomerInTenantA("Jane", "Smith");

        // When/Then
        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void shouldGetCustomerById() throws Exception {
        // Given
        Customer customer = createCustomerInTenantA("John", "Doe");

        // When/Then
        mockMvc.perform(get("/api/customers/" + customer.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"));
    }

    @Test
    void shouldUpdateCustomer() throws Exception {
        // Given
        Customer customer = createCustomerInTenantA("John", "Doe");

        CreateCustomerRequest updateRequest = CreateCustomerRequest.builder()
                .firstName("John")
                .lastName("Smith")
                .email("john.smith@example.com")
                .phone("987654321")
                .notes("Updated")
                .build();

        // When/Then
        mockMvc.perform(put("/api/customers/" + customer.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastName").value("Smith"))
                .andExpect(jsonPath("$.email").value("john.smith@example.com"));
    }

    @Test
    void shouldDeleteCustomer() throws Exception {
        // Given
        Customer customer = createCustomerInTenantA("John", "Doe");

        // When/Then
        mockMvc.perform(delete("/api/customers/" + customer.getId())
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        // Verify deletion
        assertThat(customerRepository.findById(customer.getId())).isEmpty();
    }

    @Test
    void shouldReturn404WhenTenantBTriesToAccessTenantACustomer() throws Exception {
        // Given - create customer in tenant A
        Customer customer = createCustomerInTenantA("John", "Doe");

        // When/Then - tenant B tries to access it
        mockMvc.perform(get("/api/customers/" + customer.getId())
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldIsolateTenantCustomers() throws Exception {
        // Given
        Customer customerA = createCustomerInTenantA("John", "A");
        createCustomerInTenantB("Jane", "B");

        // When/Then - tenant A lists customers, should only see their own
        mockMvc.perform(get("/api/customers")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstName").value("John"));
    }

    @Test
    void shouldReturn401WhenNoToken() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // Helper methods
    private Customer createCustomerInTenantA(String firstName, String lastName) {
        Customer customer = Customer.builder()
                .tenant(tenantA)
                .firstName(firstName)
                .lastName(lastName)
                .email(firstName.toLowerCase() + "@example.com")
                .build();
        return customerRepository.save(customer);
    }

    private Customer createCustomerInTenantB(String firstName, String lastName) {
        Customer customer = Customer.builder()
                .tenant(tenantB)
                .firstName(firstName)
                .lastName(lastName)
                .email(firstName.toLowerCase() + "@example.com")
                .build();
        return customerRepository.save(customer);
    }
}
