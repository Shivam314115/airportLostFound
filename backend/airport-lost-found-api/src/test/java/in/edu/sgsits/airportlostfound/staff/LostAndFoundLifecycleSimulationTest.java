package in.edu.sgsits.airportlostfound.staff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

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

import in.edu.sgsits.airportlostfound.lostreport.CreateLostReportRequest;

/**
 * End-to-end operational lifecycle simulation:
 * 1. Security/Intake Officer logs a recovered item
 * 2. Passenger submits a matching lost report
 * 3. Automated matching engine correlates records & provisions claim
 * 4. Inventory Officer assigns physical vault locker slot
 * 5. Claims Officer verifies identity documents & approves claim
 * 6. Return & Dispatch Officer completes counter handover to passenger
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LostAndFoundLifecycleSimulationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private in.edu.sgsits.airportlostfound.passenger.PassengerService passengerService;

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
    void simulateFullLostPropertyLifecycleFromIntakeToReturn() throws Exception {
        // =========================================================================
        // STEP 1: Security Officer Intakes Found Property
        // =========================================================================
        AuthSessionContext securityCtx = loginAs("meera.security@example.test", "shivam123");

        CreateFoundItemRequest intakeRequest = new CreateFoundItemRequest(
                1L,
                "Titanium Apple Watch Ultra 2",
                "Apple",
                "Titanium / Orange Band",
                "Found on seat 14B during post-flight cabin security sweep",
                new BigDecimal("89900.00"),
                "good",
                1L,
                1L,
                OffsetDateTime.now().minusHours(2),
                List.of()
        );

        MvcResult intakeResult = mockMvc.perform(
                post("/api/staff/items")
                        .session(securityCtx.session())
                        .header(securityCtx.csrfHeader(), securityCtx.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(intakeRequest))
        ).andExpect(status().isCreated()).andReturn();

        JsonNode intakeJson = objectMapper.readTree(intakeResult.getResponse().getContentAsString());
        long foundItemId = intakeJson.get("foundItemId").asLong();
        assertTrue(foundItemId > 0, "Found item must be registered with valid ID");

        // =========================================================================
        // STEP 2: Passenger Registers and Lodges Lost Report
        // =========================================================================
        passengerService.register(new in.edu.sgsits.airportlostfound.passenger.RegisterPassengerRequest(
                "Priya Sharma",
                "priya.sharma@example.test",
                "9876543210",
                "shivam123"
        ));

        AuthSessionContext passengerCtx = loginAs("priya.sharma@example.test", "shivam123");

        CreateLostReportRequest reportRequest = new CreateLostReportRequest(
                1L,
                "Titanium Apple Watch Ultra 2",
                "Apple",
                "Titanium / Orange Band",
                "Accidentally left my watch near seat 14B or overhead bin",
                new BigDecimal("89900.00"),
                "good",
                OffsetDateTime.now().minusHours(3),
                1L,
                1L,
                "Seat 14B, Flight 6E 237",
                List.of()
        );

        MvcResult reportResult = mockMvc.perform(
                post("/api/lost-reports")
                        .session(passengerCtx.session())
                        .header(passengerCtx.csrfHeader(), passengerCtx.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportRequest))
        ).andExpect(status().isCreated()).andReturn();

        JsonNode reportJson = objectMapper.readTree(reportResult.getResponse().getContentAsString());
        long lostReportId = reportJson.get("lostReportId").asLong();
        assertTrue(lostReportId > 0, "Lost report must be created with valid ID");

        // =========================================================================
        // STEP 3: Automated Correlation & Matches Verification
        // =========================================================================
        MvcResult matchesResult = mockMvc.perform(
                get("/api/matches")
                        .session(passengerCtx.session())
        ).andExpect(status().isOk()).andReturn();

        JsonNode matchesJson = objectMapper.readTree(matchesResult.getResponse().getContentAsString());
        assertTrue(matchesJson.isArray());

        boolean matchFound = false;
        for (JsonNode m : matchesJson) {
            if (m.get("lostReportId").asLong() == lostReportId && m.get("foundItemId").asLong() == foundItemId) {
                matchFound = true;
                double score = m.get("matchScore").asDouble();
                assertTrue(score >= 80.0, "Expected high confidence match score >= 80%, got " + score);
                assertEquals("candidate", m.get("matchStatus").asText());
                break;
            }
        }
        assertTrue(matchFound, "Automated matching engine must link lost report and found item");

        // =========================================================================
        // STEP 4: Inventory Officer Assigns Item to Vault Locker Slot
        // =========================================================================
        AuthSessionContext inventoryCtx = loginAs("pooja.inventory@airport.test", "shivam123");

        mockMvc.perform(
                post("/api/staff/inventory/assign")
                        .session(inventoryCtx.session())
                        .header(inventoryCtx.csrfHeader(), inventoryCtx.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "foundItemId", foundItemId,
                                "storageId", 1,
                                "slotNumber", 3
                        )))
        ).andExpect(status().isOk());

        // =========================================================================
        // STEP 5: Claims Officer Verifies Identity & Approves Claim
        // =========================================================================
        AuthSessionContext claimsCtx = loginAs("vikram.claims@airport.test", "shivam123");

        MvcResult claimsResult = mockMvc.perform(
                get("/api/staff/claims")
                        .session(claimsCtx.session())
        ).andExpect(status().isOk()).andReturn();

        JsonNode claimsJson = objectMapper.readTree(claimsResult.getResponse().getContentAsString());
        long claimId = -1;
        for (JsonNode c : claimsJson) {
            if (c.get("lostReportId").asLong() == lostReportId && c.get("foundItemId").asLong() == foundItemId) {
                claimId = c.get("claimId").asLong();
                break;
            }
        }
        assertTrue(claimId > 0, "Candidate claim must be present for review");

        mockMvc.perform(
                post("/api/staff/claims/" + claimId + "/review")
                        .session(claimsCtx.session())
                        .header(claimsCtx.csrfHeader(), claimsCtx.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "decision", "approved",
                                "verificationMethod", "in_person_document_check",
                                "notes", "Boarding pass and passport matched claimant details."
                        )))
        ).andExpect(status().isOk());

        // =========================================================================
        // STEP 6: Return & Dispatch Officer Completes Terminal Counter Handover
        // =========================================================================
        AuthSessionContext dispatchCtx = loginAs("rahul.dispatch@airport.test", "shivam123");

        mockMvc.perform(
                get("/api/staff/dispatches")
                        .session(dispatchCtx.session())
        ).andExpect(status().isOk())
         .andExpect(jsonPath("$[?(@.claimId == " + claimId + ")].returnStatus").value("initiated"));

        mockMvc.perform(
                post("/api/staff/dispatches/process")
                        .session(dispatchCtx.session())
                        .header(dispatchCtx.csrfHeader(), dispatchCtx.csrfToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "claimId", claimId,
                                "returnMethod", "counter_handover",
                                "returnStatus", "completed",
                                "deliveryNotes", "Handed over at Terminal 1 Lost Property Counter."
                        )))
        ).andExpect(status().isOk());

        // Verify lifecycle resolution: Found item returned, report resolved
        mockMvc.perform(
                get("/api/staff/items?status=returned")
                        .session(inventoryCtx.session())
        ).andExpect(status().isOk())
         .andExpect(jsonPath("$[?(@.foundItemId == " + foundItemId + ")].itemStatus").value("returned"));

        mockMvc.perform(
                get("/api/lost-reports")
                        .session(passengerCtx.session())
        ).andExpect(status().isOk())
         .andExpect(jsonPath("$[?(@.lostReportId == " + lostReportId + ")].reportStatus").value("resolved"));
    }
}
