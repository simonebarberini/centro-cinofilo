package it.cinofilo.catalog;

import it.cinofilo.AbstractPostgresIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogAdminControllerIT extends AbstractPostgresIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listActive_requiresAdminRole() throws Exception {
        mockMvc.perform(get("/admin/catalog"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN_APP")
    void listAll_returnsAllSeededModules() throws Exception {
        mockMvc.perform(get("/admin/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.moduleKey == 'base')].activationStatus").value("ACTIVE"))
                .andExpect(jsonPath("$[?(@.moduleKey == 'staff')].activationStatus").value("INACTIVE"))
                .andExpect(jsonPath("$[?(@.moduleKey == 'sms')].activationStatus").value("INACTIVE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN_APP")
    void listAll_includesInactiveModules() throws Exception {
        mockMvc.perform(get("/admin/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.moduleKey == 'staff')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.moduleKey == 'sms')]").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = "TENANT_OWNER")
    void listActive_forbiddenForTenantRole() throws Exception {
        mockMvc.perform(get("/admin/catalog"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN_APP")
    void getByKey_returnsModuleWithEntitlements() throws Exception {
        mockMvc.perform(get("/admin/catalog/base"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moduleKey").value("base"))
                .andExpect(jsonPath("$.entitlements").isArray())
                .andExpect(jsonPath("$.entitlements[?(@.entitlementKey == 'BOOKING_MANAGEMENT')].boolValue")
                        .value(true))
                .andExpect(jsonPath("$.entitlements[?(@.entitlementKey == 'MAX_DOGS_PER_TENANT')].quotaValue")
                        .value(500));
    }

    @Test
    @WithMockUser(roles = "ADMIN_APP")
    void getByKey_returns404ForUnknownModule() throws Exception {
        mockMvc.perform(get("/admin/catalog/nonexistent"))
                .andExpect(status().isNotFound());
    }
}
