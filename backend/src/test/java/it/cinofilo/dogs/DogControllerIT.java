package it.cinofilo.dogs;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.cinofilo.AbstractPostgresIT;
import it.cinofilo.auth.LoginRequest;
import it.cinofilo.auth.LoginResponse;
import it.cinofilo.customer.Customer;
import it.cinofilo.customer.CustomerRepository;
import it.cinofilo.dogs.dto.CreateDogRequest;
import it.cinofilo.dogs.dto.DogResponse;
import it.cinofilo.dogs.dto.UpdateDogRequest;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.profiles.active=test")
class DogControllerIT extends AbstractPostgresIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DogRepository dogRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Tenant tenantA;
    private Tenant tenantB;
    private Customer customerA;
    private Customer customerB;
    private String tokenTenantA;
    private String tokenTenantB;

    private static final String TEST_PASSWORD = "Password123!";

    @BeforeEach
    void setUp() throws Exception {
        // Clean database - must delete in order due to foreign key constraints
        dogRepository.deleteAll();
        customerRepository.deleteAll();
        appUserRepository.deleteAll();
        tenantRepository.deleteAll();

        // Create Tenant A
        String slugA = "tenant-a-" + UUID.randomUUID();
        tenantA = Tenant.builder()
                .name("Tenant A")
                .type("PENSIONE")
                .slug(slugA)
                .capacityBoxes(10)
                .build();
        tenantA = tenantRepository.save(tenantA);

        AppUser userA = AppUser.builder()
                .tenant(tenantA)
                .username("user-a")
                .passwordHash(passwordEncoder.encode(TEST_PASSWORD))
                .role(Role.TENANT_OWNER)
                .enabled(true)
                .build();
        appUserRepository.save(userA);

        customerA = Customer.builder()
                .tenant(tenantA)
                .firstName("Mario")
                .lastName("Rossi")
                .email("mario.rossi@example.com")
                .phone("+39 123 456 7890")
                .build();
        customerA = customerRepository.save(customerA);

        // Create Tenant B
        String slugB = "tenant-b-" + UUID.randomUUID();
        tenantB = Tenant.builder()
                .name("Tenant B")
                .type("PENSIONE")
                .slug(slugB)
                .capacityBoxes(10)
                .build();
        tenantB = tenantRepository.save(tenantB);

        AppUser userB = AppUser.builder()
                .tenant(tenantB)
                .username("user-b")
                .passwordHash(passwordEncoder.encode(TEST_PASSWORD))
                .role(Role.TENANT_OWNER)
                .enabled(true)
                .build();
        appUserRepository.save(userB);

        customerB = Customer.builder()
                .tenant(tenantB)
                .firstName("Luigi")
                .lastName("Verdi")
                .email("luigi.verdi@example.com")
                .phone("+39 098 765 4321")
                .build();
        customerB = customerRepository.save(customerB);

        // Login for both tenants
        tokenTenantA = login(slugA, "user-a");
        tokenTenantB = login(slugB, "user-b");
    }

    private String login(String tenantSlug, String username) throws Exception {
        LoginRequest loginRequest = new LoginRequest(tenantSlug, username, TEST_PASSWORD);
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        LoginResponse loginResponse = objectMapper.readValue(response, LoginResponse.class);
        return loginResponse.getToken();
    }

    @Test
    void shouldCreateDogForCustomerInCurrentTenant() throws Exception {
        CreateDogRequest request = CreateDogRequest.builder()
                .customerId(customerA.getId())
                .name("Rex")
                .breed("German Shepherd")
                .birthDate(LocalDate.of(2020, 5, 15))
                .notes("Very friendly")
                .build();

        String responseJson = mockMvc.perform(post("/dogs")
                        .header("Authorization", "Bearer " + tokenTenantA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.customerId").value(customerA.getId().toString()))
                .andExpect(jsonPath("$.name").value("Rex"))
                .andExpect(jsonPath("$.breed").value("German Shepherd"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        DogResponse response = objectMapper.readValue(responseJson, DogResponse.class);
        assertThat(response.getId()).isNotNull();
        assertThat(response.getCustomerId()).isEqualTo(customerA.getId());
    }

    @Test
    void shouldListDogsForTenant() throws Exception {
        // Create two dogs in tenant A
        Dog dog1 = Dog.builder()
                .tenantId(tenantA.getId())
                .customer(customerA)
                .name("Rex")
                .breed("German Shepherd")
                .build();
        dogRepository.save(dog1);

        Dog dog2 = Dog.builder()
                .tenantId(tenantA.getId())
                .customer(customerA)
                .name("Max")
                .breed("Labrador")
                .build();
        dogRepository.save(dog2);

        // Create one dog in tenant B
        Dog dog3 = Dog.builder()
                .tenantId(tenantB.getId())
                .customer(customerB)
                .name("Bella")
                .breed("Beagle")
                .build();
        dogRepository.save(dog3);

        // Tenant A should see only 2 dogs
        mockMvc.perform(get("/dogs")
                        .header("Authorization", "Bearer " + tokenTenantA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Rex"))
                .andExpect(jsonPath("$[1].name").value("Max"));
    }

    @Test
    void shouldListDogsByCustomer() throws Exception {
        // Create dogs for customerA
        Dog dog1 = Dog.builder()
                .tenantId(tenantA.getId())
                .customer(customerA)
                .name("Rex")
                .breed("German Shepherd")
                .build();
        dogRepository.save(dog1);

        Dog dog2 = Dog.builder()
                .tenantId(tenantA.getId())
                .customer(customerA)
                .name("Max")
                .breed("Labrador")
                .build();
        dogRepository.save(dog2);

        mockMvc.perform(get("/dogs/by-customer/{customerId}", customerA.getId())
                        .header("Authorization", "Bearer " + tokenTenantA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldGetDogById() throws Exception {
        Dog dog = Dog.builder()
                .tenantId(tenantA.getId())
                .customer(customerA)
                .name("Rex")
                .breed("German Shepherd")
                .birthDate(LocalDate.of(2020, 5, 15))
                .notes("Very friendly")
                .build();
        dog = dogRepository.save(dog);

        mockMvc.perform(get("/dogs/{id}", dog.getId())
                        .header("Authorization", "Bearer " + tokenTenantA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(dog.getId().toString()))
                .andExpect(jsonPath("$.name").value("Rex"))
                .andExpect(jsonPath("$.breed").value("German Shepherd"));
    }

    @Test
    void shouldUpdateDog() throws Exception {
        Dog dog = Dog.builder()
                .tenantId(tenantA.getId())
                .customer(customerA)
                .name("Rex")
                .breed("German Shepherd")
                .build();
        dog = dogRepository.save(dog);

        UpdateDogRequest updateRequest = UpdateDogRequest.builder()
                .name("Rex Jr")
                .breed("German Shepherd Mix")
                .birthDate(LocalDate.of(2021, 3, 10))
                .notes("Updated notes")
                .build();

        mockMvc.perform(put("/dogs/{id}", dog.getId())
                        .header("Authorization", "Bearer " + tokenTenantA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rex Jr"))
                .andExpect(jsonPath("$.breed").value("German Shepherd Mix"));
    }

    @Test
    void shouldDeleteDog() throws Exception {
        Dog dog = Dog.builder()
                .tenantId(tenantA.getId())
                .customer(customerA)
                .name("Rex")
                .breed("German Shepherd")
                .build();
        dog = dogRepository.save(dog);

        mockMvc.perform(delete("/dogs/{id}", dog.getId())
                        .header("Authorization", "Bearer " + tokenTenantA))
                .andExpect(status().isNoContent());

        assertThat(dogRepository.findById(dog.getId())).isEmpty();
    }

    @Test
    void shouldReturn404WhenTenantBTriesToAccessTenantADog() throws Exception {
        Dog dog = Dog.builder()
                .tenantId(tenantA.getId())
                .customer(customerA)
                .name("Rex")
                .breed("German Shepherd")
                .build();
        dog = dogRepository.save(dog);

        // Tenant B tries to access Tenant A's dog
        mockMvc.perform(get("/dogs/{id}", dog.getId())
                        .header("Authorization", "Bearer " + tokenTenantB))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenCreatingDogForCustomerOfAnotherTenant() throws Exception {
        CreateDogRequest request = CreateDogRequest.builder()
                .customerId(customerB.getId()) // Customer from Tenant B
                .name("Rex")
                .breed("German Shepherd")
                .build();

        // Tenant A tries to create dog for Tenant B's customer
        mockMvc.perform(post("/dogs")
                        .header("Authorization", "Bearer " + tokenTenantA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn401WhenNoToken() throws Exception {
        mockMvc.perform(get("/dogs"))
                .andExpect(status().isUnauthorized());
    }
}
