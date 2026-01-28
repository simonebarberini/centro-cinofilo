package it.cinofilo.customer;

import it.cinofilo.tenancy.Tenant;
import it.cinofilo.tenancy.TenantContext;
import it.cinofilo.tenancy.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final TenantRepository tenantRepository;

    /**
     * Create a new customer for the current tenant.
     *
     * @param customer the customer to create
     * @return the created customer
     * @throws IllegalStateException if tenant context is not set
     */
    public Customer create(Customer customer) {
        UUID tenantId = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant not found for ID: " + tenantId));
        
        customer.setTenant(tenant);
        return customerRepository.save(customer);
    }

    /**
     * Get all customers for the current tenant.
     *
     * @return list of customers
     * @throws IllegalStateException if tenant context is not set
     */
    @Transactional(readOnly = true)
    public List<Customer> getAllForCurrentTenant() {
        UUID tenantId = TenantContext.getTenantId();
        return customerRepository.findAllByTenantId(tenantId);
    }

    /**
     * Get a customer by ID, ensuring it belongs to the current tenant.
     *
     * @param id the customer ID
     * @return the customer
     * @throws IllegalStateException if tenant context is not set
     * @throws CustomerNotFoundException if customer not found or doesn't belong to current tenant
     */
    @Transactional(readOnly = true)
    public Customer getById(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        return customerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + id));
    }

    /**
     * Update a customer, ensuring it belongs to the current tenant.
     *
     * @param id the customer ID
     * @param customerUpdate the updated customer data
     * @return the updated customer
     * @throws IllegalStateException if tenant context is not set
     * @throws CustomerNotFoundException if customer not found or doesn't belong to current tenant
     */
    public Customer update(UUID id, Customer customerUpdate) {
        UUID tenantId = TenantContext.getTenantId();
        Customer customer = customerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + id));

        if (customerUpdate.getFirstName() != null) {
            customer.setFirstName(customerUpdate.getFirstName());
        }
        if (customerUpdate.getLastName() != null) {
            customer.setLastName(customerUpdate.getLastName());
        }
        if (customerUpdate.getEmail() != null) {
            customer.setEmail(customerUpdate.getEmail());
        }
        if (customerUpdate.getPhone() != null) {
            customer.setPhone(customerUpdate.getPhone());
        }
        if (customerUpdate.getNotes() != null) {
            customer.setNotes(customerUpdate.getNotes());
        }

        return customerRepository.save(customer);
    }

    /**
     * Delete a customer, ensuring it belongs to the current tenant.
     *
     * @param id the customer ID
     * @throws IllegalStateException if tenant context is not set
     * @throws CustomerNotFoundException if customer not found or doesn't belong to current tenant
     */
    public void delete(UUID id) {
        UUID tenantId = TenantContext.getTenantId();
        Customer customer = customerRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + id));

        customerRepository.delete(customer);
    }
}
