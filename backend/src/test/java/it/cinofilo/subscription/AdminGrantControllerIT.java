package it.cinofilo.subscription;

import it.cinofilo.AbstractPostgresIT;
import it.cinofilo.catalog.Module;
import it.cinofilo.catalog.ModuleRepository;
import it.cinofilo.catalog.ModuleType;
import it.cinofilo.tenancy.Tenant;
import it.cinofilo.tenancy.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AdminGrantControllerIT extends AbstractPostgresIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private ModuleRepository moduleRepository;

    private static final String MODULE_KEY = "agc-test";
    private UUID tenantId;

    @BeforeEach
    void setup() {
        moduleRepository.deleteAllById(java.util.List.of(MODULE_KEY));
        moduleRepository.save(Module.builder()
                .moduleKey(MODULE_KEY)
                .name("GrantController Test")
                .type(ModuleType.OPTIONAL)
                .activationStatus(it.cinofilo.catalog.ModuleActivationStatus.ACTIVE)
                .build());
        tenantId = tenantRepository.save(Tenant.builder()
                .name("Test Kennel GC")
                .type("KENNEL")
                .slug("test-kennel-gc")
                .build()).getId();
    }

    @Test
    void grant_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/admin/grants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(grantBody(tenantId, MODULE_KEY, null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN_APP", username = "admin@cinofilo.it")
    void grant_createsSubscriptionAndReturns201() throws Exception {
        mockMvc.perform(post("/admin/grants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(grantBody(tenantId, MODULE_KEY, "beta")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(tenantId.toString()))
                .andExpect(jsonPath("$.moduleKey").value(MODULE_KEY))
                .andExpect(jsonPath("$.grantedBy").value("admin@cinofilo.it"))
                .andExpect(jsonPath("$.note").value("beta"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_APP")
    void grant_returns409_whenAlreadyActive() throws Exception {
        mockMvc.perform(post("/admin/grants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(grantBody(tenantId, MODULE_KEY, null)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/admin/grants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(grantBody(tenantId, MODULE_KEY, null)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN_APP")
    void grant_returns404_whenModuleNotFound() throws Exception {
        mockMvc.perform(post("/admin/grants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(grantBody(tenantId, "nonexistent", null)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "TENANT_OWNER")
    void grant_returns403_forNonAdminRole() throws Exception {
        mockMvc.perform(post("/admin/grants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(grantBody(tenantId, MODULE_KEY, null)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN_APP", username = "admin@cinofilo.it")
    void listByTenant_returnsGrantsForTenant() throws Exception {
        mockMvc.perform(post("/admin/grants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(grantBody(tenantId, MODULE_KEY, "first grant")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/admin/grants/{tenantId}", tenantId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].moduleKey").value(MODULE_KEY))
                .andExpect(jsonPath("$[0].grantedBy").value("admin@cinofilo.it"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_APP")
    void listByTenant_returnsEmptyList_whenNoGrants() throws Exception {
        mockMvc.perform(get("/admin/grants/{tenantId}", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String grantBody(UUID tenantId, String moduleKey, String note) {
        String noteJson = note != null ? "\"" + note + "\"" : "null";
        return """
                {"tenantId":"%s","moduleKey":"%s","note":%s}
                """.formatted(tenantId, moduleKey, noteJson);
    }
}
