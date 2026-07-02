package it.cinofilo.bookings;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.cinofilo.AbstractPostgresIT;
import it.cinofilo.auth.LoginRequest;
import it.cinofilo.auth.LoginResponse;
import it.cinofilo.bookings.dto.BookingResponse;
import it.cinofilo.bookings.dto.CreateBookingRequest;
import it.cinofilo.bookings.dto.UpdateBookingRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.profiles.active=test")
class BookingControllerIT extends AbstractPostgresIT {

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

    private String tenantAToken;
    private String tenantBToken;
    private UUID tenantAId;
    private UUID tenantBId;
    private UUID customerAId;
    private UUID dogAId;

    @BeforeEach
    void setUp() throws Exception {
        // Database is wiped by AbstractPostgresIT#cleanDatabase before each test.

        // Setup Tenant A with capacity of 2 boxes
        Tenant tenantA = new Tenant();
        tenantA.setName("Centro A");
        tenantA.setSlug("centro-a");
        tenantA.setType("PENSIONE");
        tenantA.setCapacityBoxes(2);
        tenantA = tenantRepository.save(tenantA);
        tenantAId = tenantA.getId();

        AppUser userA = new AppUser();
        userA.setUsername("userA");
        userA.setPasswordHash(passwordEncoder.encode("password"));
        userA.setRole(Role.TENANT_OWNER);
        userA.setTenant(tenantA);
        userA.setEmailVerified(true);
        appUserRepository.save(userA);

        // Setup Tenant B with capacity of 1 box
        Tenant tenantB = new Tenant();
        tenantB.setName("Centro B");
        tenantB.setSlug("centro-b");
        tenantB.setType("PENSIONE");
        tenantB.setCapacityBoxes(1);
        tenantB = tenantRepository.save(tenantB);
        tenantBId = tenantB.getId();
        activateBaseModule(tenantAId, tenantBId);

        AppUser userB = new AppUser();
        userB.setUsername("userB");
        userB.setPasswordHash(passwordEncoder.encode("password"));
        userB.setRole(Role.TENANT_OWNER);
        userB.setTenant(tenantB);
        userB.setEmailVerified(true);
        appUserRepository.save(userB);

        // Login as Tenant A
        LoginRequest loginRequestA = new LoginRequest("centro-a", "userA", "password");
        MvcResult resultA = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequestA)))
                .andExpect(status().isOk())
                .andReturn();
        LoginResponse loginResponseA = objectMapper.readValue(
                resultA.getResponse().getContentAsString(),
                LoginResponse.class
        );
        tenantAToken = loginResponseA.getToken();

        // Login as Tenant B
        LoginRequest loginRequestB = new LoginRequest("centro-b", "userB", "password");
        MvcResult resultB = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequestB)))
                .andExpect(status().isOk())
                .andReturn();
        LoginResponse loginResponseB = objectMapper.readValue(
                resultB.getResponse().getContentAsString(),
                LoginResponse.class
        );
        tenantBToken = loginResponseB.getToken();

        // Create a customer for Tenant A
        Customer customerA = new Customer();
        customerA.setTenant(tenantA);
        customerA.setFirstName("Mario");
        customerA.setLastName("Rossi");
        customerA.setEmail("mario@example.com");
        customerA.setPhone("1234567890");
        customerA = customerRepository.save(customerA);
        customerAId = customerA.getId();

        // Create a dog for Tenant A
        Dog dogA = new Dog();
        dogA.setTenantId(tenantAId);
        dogA.setCustomer(customerA);
        dogA.setName("Fido");
        dogA.setBreed("Labrador");
        dogA.setBirthDate(LocalDate.of(2020, 1, 1));
        dogA = dogRepository.save(dogA);
        dogAId = dogA.getId();
    }

    @Test
    void shouldCreateBookingWhenCapacityAvailable() throws Exception {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .customerId(customerAId)
                .dogId(dogAId)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .notes("First booking")
                .build();

        mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + tenantAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.customerId").value(customerAId.toString()))
                .andExpect(jsonPath("$.dogId").value(dogAId.toString()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.notes").value("First booking"));

        // Verify booking was saved
        assertThat(bookingRepository.findAllByTenantId(tenantAId)).hasSize(1);
    }

    @Test
    void shouldRejectBookingWhenOverCapacity() throws Exception {
        // Create 2 bookings (Tenant A has capacity=2)
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(3);

        // Create first booking
        Booking booking1 = Booking.builder()
                .tenantId(tenantAId)
                .customer(customerRepository.findById(customerAId).orElseThrow())
                .dog(dogRepository.findById(dogAId).orElseThrow())
                .startDate(start)
                .endDate(end)
                .status(BookingStatus.CONFIRMED)
                .build();
        bookingRepository.save(booking1);

        // Create another customer and dog
        Customer customer2 = new Customer();
        customer2.setTenant(tenantRepository.findById(tenantAId).orElseThrow());
        customer2.setFirstName("Luigi");
        customer2.setLastName("Verdi");
        customer2.setEmail("luigi@example.com");
        customer2.setPhone("9876543210");
        customer2 = customerRepository.save(customer2);

        Dog dog2 = new Dog();
        dog2.setTenantId(tenantAId);
        dog2.setCustomer(customer2);
        dog2.setName("Rex");
        dog2.setBreed("Pastore Tedesco");
        dog2.setBirthDate(LocalDate.of(2019, 5, 10));
        dog2 = dogRepository.save(dog2);

        // Create second booking
        Booking booking2 = Booking.builder()
                .tenantId(tenantAId)
                .customer(customer2)
                .dog(dog2)
                .startDate(start)
                .endDate(end)
                .status(BookingStatus.CONFIRMED)
                .build();
        bookingRepository.save(booking2);

        // Try to create third booking (should fail)
        Customer customer3 = new Customer();
        customer3.setTenant(tenantRepository.findById(tenantAId).orElseThrow());
        customer3.setFirstName("Anna");
        customer3.setLastName("Bianchi");
        customer3.setEmail("anna@example.com");
        customer3.setPhone("5551234567");
        customer3 = customerRepository.save(customer3);

        Dog dog3 = new Dog();
        dog3.setTenantId(tenantAId);
        dog3.setCustomer(customer3);
        dog3.setName("Luna");
        dog3.setBreed("Golden Retriever");
        dog3.setBirthDate(LocalDate.of(2021, 3, 15));
        dog3 = dogRepository.save(dog3);

        CreateBookingRequest request = CreateBookingRequest.builder()
                .customerId(customer3.getId())
                .dogId(dog3.getId())
                .startDate(start)
                .endDate(end)
                .notes("Should fail")
                .build();

        mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + tenantAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.date").exists())
                .andExpect(jsonPath("$.capacity").value(2))
                .andExpect(jsonPath("$.booked").value(2));

        // Verify only 2 bookings exist
        assertThat(bookingRepository.findAllByTenantId(tenantAId)).hasSize(2);
    }

    @Test
    void shouldIgnoreCancelledBookingsInAvailability() throws Exception {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = LocalDate.now().plusDays(3);

        // Create a CANCELLED booking
        Booking cancelledBooking = Booking.builder()
                .tenantId(tenantAId)
                .customer(customerRepository.findById(customerAId).orElseThrow())
                .dog(dogRepository.findById(dogAId).orElseThrow())
                .startDate(start)
                .endDate(end)
                .status(BookingStatus.CANCELLED)
                .build();
        bookingRepository.save(cancelledBooking);

        // Should be able to create a new booking (CANCELLED doesn't count)
        CreateBookingRequest request = CreateBookingRequest.builder()
                .customerId(customerAId)
                .dogId(dogAId)
                .startDate(start)
                .endDate(end)
                .notes("Should succeed")
                .build();

        mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + tenantAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Verify we have 2 bookings (1 CANCELLED, 1 CONFIRMED)
        assertThat(bookingRepository.findAllByTenantId(tenantAId)).hasSize(2);
    }

    @Test
    void shouldReturn404WhenTenantBAccessesTenantABooking() throws Exception {
        // Create booking for Tenant A
        Booking bookingA = Booking.builder()
                .tenantId(tenantAId)
                .customer(customerRepository.findById(customerAId).orElseThrow())
                .dog(dogRepository.findById(dogAId).orElseThrow())
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .status(BookingStatus.CONFIRMED)
                .build();
        bookingA = bookingRepository.save(bookingA);

        // Tenant B tries to access Tenant A's booking
        mockMvc.perform(get("/bookings/" + bookingA.getId())
                        .header("Authorization", "Bearer " + tenantBToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Booking not found with ID: " + bookingA.getId()));
    }

    @Test
    void shouldReturn401WhenNoToken() throws Exception {
        mockMvc.perform(get("/bookings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldUpdateBookingDatesWithAvailabilityCheck() throws Exception {
        // Create initial booking
        Booking booking = Booking.builder()
                .tenantId(tenantAId)
                .customer(customerRepository.findById(customerAId).orElseThrow())
                .dog(dogRepository.findById(dogAId).orElseThrow())
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .status(BookingStatus.CONFIRMED)
                .notes("Original")
                .build();
        booking = bookingRepository.save(booking);

        // Update dates
        UpdateBookingRequest updateRequest = UpdateBookingRequest.builder()
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(5))
                .notes("Updated")
                .build();

        mockMvc.perform(put("/bookings/" + booking.getId())
                        .header("Authorization", "Bearer " + tenantAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value(LocalDate.now().plusDays(2).toString()))
                .andExpect(jsonPath("$.endDate").value(LocalDate.now().plusDays(5).toString()))
                .andExpect(jsonPath("$.notes").value("Updated"));
    }

    @Test
    void shouldExcludeSelfWhenUpdatingDates() throws Exception {
        // Create booking for Tenant B (capacity=1)
        Customer customerB = new Customer();
        customerB.setTenant(tenantRepository.findById(tenantBId).orElseThrow());
        customerB.setFirstName("Paolo");
        customerB.setLastName("Neri");
        customerB.setEmail("paolo@example.com");
        customerB.setPhone("1111111111");
        customerB = customerRepository.save(customerB);

        Dog dogB = new Dog();
        dogB.setTenantId(tenantBId);
        dogB.setCustomer(customerB);
        dogB.setName("Max");
        dogB.setBreed("Beagle");
        dogB.setBirthDate(LocalDate.of(2018, 7, 20));
        dogB = dogRepository.save(dogB);

        Booking booking = Booking.builder()
                .tenantId(tenantBId)
                .customer(customerB)
                .dog(dogB)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .status(BookingStatus.CONFIRMED)
                .build();
        booking = bookingRepository.save(booking);

        // Update the same booking's dates (should succeed even with capacity=1)
        UpdateBookingRequest updateRequest = UpdateBookingRequest.builder()
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(4))
                .build();

        mockMvc.perform(put("/bookings/" + booking.getId())
                        .header("Authorization", "Bearer " + tenantBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value(LocalDate.now().plusDays(2).toString()))
                .andExpect(jsonPath("$.endDate").value(LocalDate.now().plusDays(4).toString()));
    }

    @Test
    void shouldReturnDailyAvailability() throws Exception {
        Tenant tenantB = tenantRepository.findById(tenantBId).orElseThrow();
        Customer customerB = createCustomerForTenant(tenantB, "Paolo", "Neri", "paolo.av@example.com");
        Dog dogB = createDogForTenant(customerB, tenantBId, "Max", "Beagle");

        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2);

        Booking booking = Booking.builder()
                .tenantId(tenantBId)
                .customer(customerB)
                .dog(dogB)
                .startDate(start)
                .endDate(end)
                .status(BookingStatus.CONFIRMED)
                .build();
        bookingRepository.save(booking);

        mockMvc.perform(get("/bookings/availability")
                        .header("Authorization", "Bearer " + tenantBToken)
                        .param("start", start.toString())
                        .param("end", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].date").value(start.toString()))
                .andExpect(jsonPath("$[0].capacity").value(1))
                .andExpect(jsonPath("$[0].booked").value(1))
                .andExpect(jsonPath("$[0].available").value(0));
    }

    @Test
    void shouldRejectCreateWhenOverCapacity() throws Exception {
        Tenant tenantB = tenantRepository.findById(tenantBId).orElseThrow();
        Customer customerB = createCustomerForTenant(tenantB, "Paolo", "Neri", "paolo.cap@example.com");
        Dog dogB = createDogForTenant(customerB, tenantBId, "Max", "Beagle");

        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2);

        Booking booking = Booking.builder()
                .tenantId(tenantBId)
                .customer(customerB)
                .dog(dogB)
                .startDate(start)
                .endDate(end)
                .status(BookingStatus.CONFIRMED)
                .build();
        bookingRepository.save(booking);

        CreateBookingRequest request = CreateBookingRequest.builder()
                .customerId(customerB.getId())
                .dogId(dogB.getId())
                .startDate(start)
                .endDate(end)
                .notes("Should fail - capacity 1")
                .build();

        mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + tenantBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.date").value(start.toString()))
                .andExpect(jsonPath("$.capacity").value(1))
                .andExpect(jsonPath("$.booked").value(1));
    }

    @Test
    void shouldAllowCreateWhenCancelledBookingExists() throws Exception {
        Tenant tenantB = tenantRepository.findById(tenantBId).orElseThrow();
        Customer customerB = createCustomerForTenant(tenantB, "Paolo", "Neri", "paolo.cancel@example.com");
        Dog dogB = createDogForTenant(customerB, tenantBId, "Max", "Beagle");

        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2);

        Booking cancelled = Booking.builder()
                .tenantId(tenantBId)
                .customer(customerB)
                .dog(dogB)
                .startDate(start)
                .endDate(end)
                .status(BookingStatus.CANCELLED)
                .build();
        bookingRepository.save(cancelled);

        CreateBookingRequest request = CreateBookingRequest.builder()
                .customerId(customerB.getId())
                .dogId(dogB.getId())
                .startDate(start)
                .endDate(end)
                .notes("Should succeed")
                .build();

        mockMvc.perform(post("/bookings")
                        .header("Authorization", "Bearer " + tenantBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void shouldRequireAuthForAvailabilityEndpoint() throws Exception {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2);

        mockMvc.perform(get("/bookings/availability")
                        .param("start", start.toString())
                        .param("end", end.toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldCancelBooking() throws Exception {
        // Create booking
        Booking booking = Booking.builder()
                .tenantId(tenantAId)
                .customer(customerRepository.findById(customerAId).orElseThrow())
                .dog(dogRepository.findById(dogAId).orElseThrow())
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .status(BookingStatus.CONFIRMED)
                .build();
        booking = bookingRepository.save(booking);

        // Cancel it
        mockMvc.perform(post("/bookings/" + booking.getId() + "/cancel")
                        .header("Authorization", "Bearer " + tenantAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        // Verify status changed
        Booking updated = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void shouldListAllBookingsForTenant() throws Exception {
        // Create 2 bookings for Tenant A
        Booking booking1 = Booking.builder()
                .tenantId(tenantAId)
                .customer(customerRepository.findById(customerAId).orElseThrow())
                .dog(dogRepository.findById(dogAId).orElseThrow())
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .status(BookingStatus.CONFIRMED)
                .build();
        bookingRepository.save(booking1);

        Booking booking2 = Booking.builder()
                .tenantId(tenantAId)
                .customer(customerRepository.findById(customerAId).orElseThrow())
                .dog(dogRepository.findById(dogAId).orElseThrow())
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(7))
                .status(BookingStatus.CONFIRMED)
                .build();
        bookingRepository.save(booking2);

        // List bookings
        mockMvc.perform(get("/bookings")
                        .header("Authorization", "Bearer " + tenantAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

        private Customer createCustomerForTenant(Tenant tenant, String firstName, String lastName, String email) {
                Customer customer = new Customer();
                customer.setTenant(tenant);
                customer.setFirstName(firstName);
                customer.setLastName(lastName);
                customer.setEmail(email);
                customer.setPhone("1111111111");
                return customerRepository.save(customer);
        }

        private Dog createDogForTenant(Customer customer, UUID tenantId, String name, String breed) {
                Dog dog = new Dog();
                dog.setTenantId(tenantId);
                dog.setCustomer(customer);
                dog.setName(name);
                dog.setBreed(breed);
                dog.setBirthDate(LocalDate.of(2018, 7, 20));
                return dogRepository.save(dog);
        }
}
