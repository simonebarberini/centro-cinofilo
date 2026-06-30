package it.cinofilo.bookings;

import it.cinofilo.AbstractPostgresIT;
import it.cinofilo.bookings.dto.CreateBookingRequest;
import it.cinofilo.customer.Customer;
import it.cinofilo.customer.CustomerRepository;
import it.cinofilo.dogs.Dog;
import it.cinofilo.dogs.DogRepository;
import it.cinofilo.tenancy.Tenant;
import it.cinofilo.tenancy.TenantContext;
import it.cinofilo.tenancy.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that concurrent booking creation cannot overbook a tenant.
 *
 * <p>With capacity = 1 and two bookings for the same date range fired
 * simultaneously, exactly one must succeed and the other must be rejected with
 * {@link OverbookingException}. This is the time-of-check/time-of-use race that
 * {@link TenantCapacityGuard}'s per-tenant pessimistic lock closes: without the
 * lock both requests could read "0/1 booked" and both persist.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.profiles.active=test")
class BookingConcurrencyIT extends AbstractPostgresIT {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DogRepository dogRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void concurrentBookingsAtCapacityOneAllowExactlyOne() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setName("Centro Concorrenza");
        tenant.setSlug("centro-concorrenza");
        tenant.setType("PENSIONE");
        tenant.setCapacityBoxes(1);
        tenant = tenantRepository.save(tenant);
        UUID tenantId = tenant.getId();

        Customer customer1 = createCustomer(tenant, "Mario", "Rossi", "mario.conc@example.com");
        Customer customer2 = createCustomer(tenant, "Luigi", "Verdi", "luigi.conc@example.com");
        Dog dog1 = createDog(tenantId, customer1, "Fido");
        Dog dog2 = createDog(tenantId, customer2, "Rex");

        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2);

        CreateBookingRequest request1 = CreateBookingRequest.builder()
                .customerId(customer1.getId())
                .dogId(dog1.getId())
                .startDate(start)
                .endDate(end)
                .build();
        CreateBookingRequest request2 = CreateBookingRequest.builder()
                .customerId(customer2.getId())
                .dogId(dog2.getId())
                .startDate(start)
                .endDate(end)
                .build();

        // Both threads block on the barrier and are released together to make
        // the two create() calls overlap as tightly as possible.
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = pool.submit(() -> attemptBooking(tenantId, request1, barrier));
            Future<Boolean> second = pool.submit(() -> attemptBooking(tenantId, request2, barrier));

            boolean firstSucceeded = first.get(30, TimeUnit.SECONDS);
            boolean secondSucceeded = second.get(30, TimeUnit.SECONDS);

            assertThat(firstSucceeded ^ secondSucceeded)
                    .as("exactly one of the two concurrent bookings must succeed")
                    .isTrue();
        } finally {
            pool.shutdownNow();
        }

        long confirmed = bookingRepository.findAllByTenantId(tenantId).stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .count();
        assertThat(confirmed)
                .as("capacity=1 must never be exceeded")
                .isEqualTo(1);
    }

    /**
     * Runs one booking attempt on a dedicated thread. Returns {@code true} on
     * success, {@code false} if rejected for overbooking; any other failure is
     * rethrown so the test fails loudly with the cause.
     */
    private boolean attemptBooking(UUID tenantId, CreateBookingRequest request, CyclicBarrier barrier) {
        try {
            TenantContext.setTenantId(tenantId);
            barrier.await(10, TimeUnit.SECONDS);
            bookingService.create(request);
            return true;
        } catch (OverbookingException e) {
            return false;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            TenantContext.clear();
        }
    }

    private Customer createCustomer(Tenant tenant, String firstName, String lastName, String email) {
        Customer customer = new Customer();
        customer.setTenant(tenant);
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setEmail(email);
        customer.setPhone("0000000000");
        return customerRepository.save(customer);
    }

    private Dog createDog(UUID tenantId, Customer customer, String name) {
        Dog dog = new Dog();
        dog.setTenantId(tenantId);
        dog.setCustomer(customer);
        dog.setName(name);
        dog.setBreed("Meticcio");
        dog.setBirthDate(LocalDate.of(2020, 1, 1));
        return dogRepository.save(dog);
    }
}
