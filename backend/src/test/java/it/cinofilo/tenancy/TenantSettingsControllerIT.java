package it.cinofilo.tenancy;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.cinofilo.AbstractPostgresIT;
import it.cinofilo.auth.LoginRequest;
import it.cinofilo.auth.LoginResponse;
import it.cinofilo.tenancy.dto.UpdateTenantSettingsRequest;
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

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = "spring.profiles.active=test")
class TenantSettingsControllerIT extends AbstractPostgresIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private String ownerToken;
    private String staffToken;

    @BeforeEach
    void setUp() throws Exception {
        // Database is wiped by AbstractPostgresIT#cleanDatabase before each test.

        // Create tenant
        Tenant tenant = Tenant.builder()
                .name("Centro Test")
                .slug("centro-test")
                .type("PENSIONE")
                .capacityBoxes(10)
                .build();
        tenant = tenantRepository.save(tenant);

        // Create OWNER user
        AppUser owner = AppUser.builder()
                .tenant(tenant)
                .username("owner")
                .passwordHash(passwordEncoder.encode("password"))
                .role(Role.TENANT_OWNER)
                .enabled(true)
                .emailVerified(true)
                .build();
        appUserRepository.save(owner);

        // Create STAFF user
        AppUser staff = AppUser.builder()
                .tenant(tenant)
                .username("staff")
                .passwordHash(passwordEncoder.encode("password"))
                .role(Role.TENANT_STAFF)
                .enabled(true)
                .emailVerified(true)
                .build();
        appUserRepository.save(staff);

        // Login as owner
        ownerToken = login("centro-test", "owner", "password");

        // Login as staff
        staffToken = login("centro-test", "staff", "password");
    }

    @Test
    void shouldReturnTenantSettingsForOwner() throws Exception {
        mockMvc.perform(get("/tenant/me")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Centro Test"))
                .andExpect(jsonPath("$.slug").value("centro-test"))
                .andExpect(jsonPath("$.capacityBoxes").value(10))
                .andExpect(jsonPath("$.preferences").isMap());
    }

    @Test
    void shouldReturnTenantSettingsForStaff() throws Exception {
        mockMvc.perform(get("/tenant/me")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Centro Test"))
                .andExpect(jsonPath("$.capacityBoxes").value(10));
    }

    @Test
    void shouldUpdateCapacityAsOwner() throws Exception {
        UpdateTenantSettingsRequest request = UpdateTenantSettingsRequest.builder()
                .capacityBoxes(20)
                .preferences(Map.of("timezone", "Europe/Rome", "notifications", true))
                .build();

        mockMvc.perform(put("/tenant/me")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacityBoxes").value(20))
                .andExpect(jsonPath("$.preferences.timezone").value("Europe/Rome"))
                .andExpect(jsonPath("$.preferences.notifications").value(true));
    }

    @Test
    void shouldRejectUpdateFromStaff() throws Exception {
        UpdateTenantSettingsRequest request = UpdateTenantSettingsRequest.builder()
                .capacityBoxes(50)
                .build();

        mockMvc.perform(put("/tenant/me")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectNegativeCapacity() throws Exception {
        UpdateTenantSettingsRequest request = UpdateTenantSettingsRequest.builder()
                .capacityBoxes(-1)
                .build();

        mockMvc.perform(put("/tenant/me")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/tenant/me"))
                .andExpect(status().isUnauthorized());
    }

    private String login(String slug, String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(slug, username, password);
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        LoginResponse loginResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), LoginResponse.class);
        return loginResponse.getToken();
    }
}
