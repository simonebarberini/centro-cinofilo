package it.cinofilo.bookings;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.cinofilo.AbstractPostgresIT;
import it.cinofilo.auth.LoginRequest;
import it.cinofilo.auth.LoginResponse;
import it.cinofilo.customer.Customer;
import it.cinofilo.customer.CustomerRepository;
import it.cinofilo.dogs.Dog;
import it.cinofilo.dogs.DogRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.profiles.active=test")
class CalendarControllerIT extends AbstractPostgresIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DogRepository dogRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String token;
    private UUID tenantId;
    private UUID dogId;

    @BeforeEach
    void setUp() throws Exception {
        // Database is wiped by AbstractPostgresIT#cleanDatabase before each test.

        // Setup Tenant
        Tenant tenant = new Tenant();
        tenant.setName("Centro Test");
        tenant.setSlug("centro-test");
        tenant.setType("PENSIONE");
        tenant.setCapacityBoxes(5);
        tenant = tenantRepository.save(tenant);
        tenantId = tenant.getId();

        // Create user
        AppUser user = new AppUser();
        user.setUsername("testuser");
        user.setPasswordHash(passwordEncoder.encode("password"));
        user.setRole(Role.TENANT_OWNER);
        user.setTenant(tenant);
        user.setEmailVerified(true);
        appUserRepository.save(user);

        // Login
        LoginRequest loginRequest = new LoginRequest("centro-test", "testuser", "password");
        MvcResult result = mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        LoginResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                LoginResponse.class);
        token = response.getToken();

        // Create customer
        Customer customer = new Customer();
        customer.setTenant(tenant);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john@example.com");
        customer.setPhone("1234567890");
        customer = customerRepository.save(customer);

        // Create dog
        Dog dog = new Dog();
        dog.setTenantId(tenantId);
        dog.setCustomer(customer);
        dog.setName("Rex");
        dog = dogRepository.save(dog);
        dogId = dog.getId();
    }

    @Test
    void shouldReturnCalendarWithDaysAndBookings() throws Exception {
        // Create bookings
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 8);

        Booking confirmedBooking = Booking.builder()
                .tenantId(tenantId)
                .customer(customerRepository.findAll().stream().findFirst().orElseThrow())
                .dog(dogRepository.findById(dogId).orElseThrow())
                .startDate(LocalDate.of(2026, 2, 2))
                .endDate(LocalDate.of(2026, 2, 4))
                .status(BookingStatus.CONFIRMED)
                .notes("Regular boarding")
                .build();
        bookingRepository.save(confirmedBooking);

        Booking cancelledBooking = Booking.builder()
                .tenantId(tenantId)
                .customer(customerRepository.findAll().stream().findFirst().orElseThrow())
                .dog(dogRepository.findById(dogId).orElseThrow())
                .startDate(LocalDate.of(2026, 2, 5))
                .endDate(LocalDate.of(2026, 2, 7))
                .status(BookingStatus.CANCELLED)
                .notes("Cancelled")
                .build();
        bookingRepository.save(cancelledBooking);

        // Test
        mockMvc.perform(get("/bookings/calendar")
                .param("start", startDate.toString())
                .param("end", endDate.toString())
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.start", is(startDate.toString())))
                .andExpect(jsonPath("$.end", is(endDate.toString())))
                .andExpect(jsonPath("$.capacity", is(5)))
                .andExpect(jsonPath("$.days", hasSize(7)))
                .andExpect(jsonPath("$.days[0].date", is("2026-02-01")))
                .andExpect(jsonPath("$.bookings", hasSize(2)))
                .andExpect(jsonPath("$.bookings[0].status", is("CONFIRMED")))
                .andExpect(jsonPath("$.bookings[0].notes", is("Regular boarding")))
                .andExpect(jsonPath("$.bookings[1].status", is("CANCELLED")))
                .andExpect(jsonPath("$.bookings[1].notes", is("Cancelled")));
    }

    @Test
    void shouldReturn401WhenNoToken() throws Exception {
        LocalDate startDate = LocalDate.of(2026, 2, 1);
        LocalDate endDate = LocalDate.of(2026, 2, 8);

        mockMvc.perform(get("/bookings/calendar")
                .param("start", startDate.toString())
                .param("end", endDate.toString())
                .contentType(APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn400WhenInvalidRange() throws Exception {
        LocalDate startDate = LocalDate.of(2026, 2, 8);
        LocalDate endDate = LocalDate.of(2026, 2, 1);

        mockMvc.perform(get("/bookings/calendar")
                .param("start", startDate.toString())
                .param("end", endDate.toString())
                .header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
