package in.edu.sgsits.airportlostfound.staff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StaffRbacIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private record AuthSessionContext(MockHttpSession session, String csrfHeader, String csrfToken) {}

    private AuthSessionContext loginAs(String email, String password) throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode csrfJson = objectMapper.readTree(csrfResult.getResponse().getContentAsString());
        String token = csrfJson.get("token").asText();
        String header = csrfJson.get("headerName").asText();
        MockHttpSession initialSession = (MockHttpSession) csrfResult.getRequest().getSession(false);

        MvcResult loginResult = mockMvc.perform(
                post("/api/auth/login")
                        .session(initialSession)
                        .header(header, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        )))
        ).andExpect(status().isOk()).andReturn();

        MockHttpSession authSession = (MockHttpSession) loginResult.getRequest().getSession(false);
        return new AuthSessionContext(authSession != null ? authSession : initialSession, header, token);
    }

    @Test
    void staffRolesCanLoginAndReceiveAppropriateAuthorities() throws Exception {
        // Test login for Lost & Found Agent
        AuthSessionContext agentCtx = loginAs("aditi.agent@airport.test", "shivam123");
        mockMvc.perform(get("/api/auth/me").session(agentCtx.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userType").value("staff"))
                .andExpect(jsonPath("$.staffRole").value("lost_found_agent"))
                .andExpect(jsonPath("$.staffRoleTitle").value("Lost & Found Agent"));

        // Test login for System Admin
        AuthSessionContext adminCtx = loginAs("admin@airport.test", "shivam123");
        mockMvc.perform(get("/api/auth/me").session(adminCtx.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userType").value("staff"))
                .andExpect(jsonPath("$.staffRole").value("system_admin"))
                .andExpect(jsonPath("$.staffRoleTitle").value("System Administrator"));
    }

    @Test
    void lostFoundAgentCanIntakeItemAndSearch() throws Exception {
        AuthSessionContext agentCtx = loginAs("aditi.agent@airport.test", "shivam123");

        // 1. Agent registers a found item
        mockMvc.perform(post("/api/staff/items")
                        .session(agentCtx.session())
                        .header(agentCtx.csrfHeader(), agentCtx.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", 1,
                                "itemName", "Silver MacBook Pro 14",
                                "brand", "Apple",
                                "colour", "Silver",
                                "description", "Left on Gate 3 seat",
                                "itemCondition", "good",
                                "foundAt", OffsetDateTime.now().minusMinutes(15).toString()
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.foundItemId").isNumber());

        // 2. Agent can query found items list
        mockMvc.perform(get("/api/staff/items").session(agentCtx.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void claimsOfficerCanViewClaimsAndRbacPreventsUnauthorizedRole() throws Exception {
        AuthSessionContext claimsCtx = loginAs("vikram.claims@airport.test", "shivam123");

        // Claims officer can view claims
        mockMvc.perform(get("/api/staff/claims").session(claimsCtx.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Ground staff cannot view claims (restricted to intake)
        AuthSessionContext groundStaffCtx = loginAs("karan.security@airport.test", "shivam123");
        mockMvc.perform(get("/api/staff/claims").session(groundStaffCtx.session()))
                .andExpect(status().isForbidden());
    }

    @Test
    void inventoryOfficerCanViewStorageAndSlots() throws Exception {
        AuthSessionContext inventoryCtx = loginAs("pooja.inventory@airport.test", "shivam123");

        mockMvc.perform(get("/api/staff/inventory/locations").session(inventoryCtx.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void dispatchOfficerCanViewDispatches() throws Exception {
        AuthSessionContext dispatchCtx = loginAs("rahul.dispatch@airport.test", "shivam123");

        mockMvc.perform(get("/api/staff/dispatches").session(dispatchCtx.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void supervisorCanAccessTelemetryAndAudits() throws Exception {
        AuthSessionContext supervisorCtx = loginAs("sunita.supervisor@airport.test", "shivam123");

        mockMvc.perform(get("/api/staff/supervisor/stats").session(supervisorCtx.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFoundItems").isNumber())
                .andExpect(jsonPath("$.activeStaffCount").isNumber());

        mockMvc.perform(get("/api/staff/supervisor/audits").session(supervisorCtx.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void systemAdminCanManageStaffAccountsAndRbacRejectsNonAdmin() throws Exception {
        AuthSessionContext adminCtx = loginAs("admin@airport.test", "shivam123");

        // 1. Admin can list all staff
        mockMvc.perform(get("/api/staff/admin/users").session(adminCtx.session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // 2. Admin can create new staff member
        String newStaffEmail = "staff-" + UUID.randomUUID() + "@airport.test";
        mockMvc.perform(post("/api/staff/admin/users")
                        .session(adminCtx.session())
                        .header(adminCtx.csrfHeader(), adminCtx.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "New Security Officer",
                                "email", newStaffEmail,
                                "password", "shivam123",
                                "staffRole", "ground_staff"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(newStaffEmail))
                .andExpect(jsonPath("$.staffRole").value("ground_staff"));

        // 3. Regular agent cannot access admin endpoints
        AuthSessionContext agentCtx = loginAs("aditi.agent@airport.test", "shivam123");
        mockMvc.perform(get("/api/staff/admin/users").session(agentCtx.session()))
                .andExpect(status().isForbidden());
    }
}
