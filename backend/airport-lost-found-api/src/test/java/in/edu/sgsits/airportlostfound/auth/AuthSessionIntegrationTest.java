
package in.edu.sgsits.airportlostfound.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
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
class AuthSessionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registrationOwnershipLoginAndLogoutWork()
            throws Exception {

        String testEmail = "auth-test-" + UUID.randomUUID()
                + "@example.test";

        String password = "Correct-Password-42";

        // 1. Obtain a CSRF token and its associated session.
        MvcResult csrfResult = mockMvc.perform(
                        get("/api/auth/csrf"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode csrfJson = objectMapper.readTree(
                csrfResult.getResponse().getContentAsString()
        );

        String csrfToken = csrfJson.get("token").asText();
        String csrfHeader = csrfJson.get("headerName").asText();

        MockHttpSession session =
                (MockHttpSession) csrfResult.getRequest()
                        .getSession(false);

        assertNotNull(
                session,
                "CSRF request should create a session"
        );

        // 2. Register a passenger.
        MvcResult registrationResult = mockMvc.perform(
                        post("/api/passengers")
                                .session(session)
                                .header(csrfHeader, csrfToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        Map.of(
                                                "fullName", "Auth Test Passenger",
                                                "email", testEmail,
                                                "phone", "9876543210",
                                                "password", password
                                        )
                                )))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode registrationJson = objectMapper.readTree(
                registrationResult.getResponse().getContentAsString()
        );

        assertNotNull(
                registrationJson.get("passengerId"),
                "Registration should return the passenger ID"
        );

        long passengerId =
                registrationJson.get("passengerId").asLong();

        assertTrue(passengerId > 0);

        // 3. Log in with the newly registered credentials.
        MvcResult loginResult = mockMvc.perform(
                        post("/api/auth/login")
                                .session(session)
                                .header(csrfHeader, csrfToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        Map.of(
                                                "email", testEmail,
                                                "password", password
                                        )
                                )))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession authenticatedSession =
                (MockHttpSession) loginResult.getRequest()
                        .getSession(false);

        assertNotNull(
                authenticatedSession,
                "Successful login should retain an HTTP session"
        );

        // 4. Check that /me returns the authenticated passenger.
        MvcResult profileResult = mockMvc.perform(
                        get("/api/auth/me")
                                .session(authenticatedSession))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode profileJson = objectMapper.readTree(
                profileResult.getResponse().getContentAsString()
        );

        assertEquals(testEmail, profileJson.get("email").asText());
        assertTrue(!profileJson.has("password"));
        assertTrue(!profileJson.has("passwordHash"));

        // 5. Attempt to spoof another passenger's ID in the JSON body.
        // The server must assign ownership using the authenticated session.
        String itemName = "Ownership test " + UUID.randomUUID();

        String lostAt = OffsetDateTime.now()
                .minusMinutes(10)
                .toString();

        String reportRequest = objectMapper.writeValueAsString(
                Map.ofEntries(
                        Map.entry("passengerId", 1L),
                        Map.entry("categoryId", 2L),
                        Map.entry("itemName", itemName),
                        Map.entry("brand", "TestBrand"),
                        Map.entry("colour", "Blue"),
                        Map.entry(
                                "description",
                                "Created by the ownership integration test"
                        ),
                        Map.entry(
                                "estimatedValue",
                                new BigDecimal("150.00")
                        ),
                        Map.entry("itemCondition", "good"),
                        Map.entry("lostAt", lostAt),
                        Map.entry("flightId", 1L),
                        Map.entry("lastSeenLocationId", 2L),
                        Map.entry("additionalDetails", "Ownership test")
                )
        );

        MvcResult reportResult = mockMvc.perform(
                        post("/api/lost-reports")
                                .session(authenticatedSession)
                                .header(csrfHeader, csrfToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(reportRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itemName").value(itemName))
                .andReturn();

        JsonNode reportJson = objectMapper.readTree(
                reportResult.getResponse().getContentAsString()
        );

        assertEquals(
                passengerId,
                reportJson.get("passengerId").asLong(),
                "The report must belong to the logged-in passenger, " +
                        "not the passengerId supplied in JSON."
        );

        // 6. The passenger can filter reports by their own ID.
        mockMvc.perform(get("/api/lost-reports")
                        .session(authenticatedSession)
                        .param("passengerId", Long.toString(passengerId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].passengerId").value(passengerId));

        // 7. The passenger cannot filter reports by another passenger's ID.
        mockMvc.perform(get("/api/lost-reports")
                        .session(authenticatedSession)
                        .param("passengerId", "1"))
                .andExpect(status().isForbidden());

        // 8. The authenticated session still accesses protected endpoints.
        mockMvc.perform(get("/api/matches")
                        .session(authenticatedSession))
                .andExpect(status().isOk());

        // 9. Log out using the CSRF token.
        mockMvc.perform(post("/api/auth/logout")
                        .session(authenticatedSession)
                        .header(csrfHeader, csrfToken))
                .andExpect(status().isNoContent());

        // 10. Requests without an authenticated session are rejected.
        mockMvc.perform(get("/api/matches"))
                .andExpect(status().is4xxClientError());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().is4xxClientError());
    }
}
