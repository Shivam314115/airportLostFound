
package in.edu.sgsits.airportlostfound.lostreport;

import java.time.OffsetDateTime;
import java.util.List;

import in.edu.sgsits.airportlostfound.auth.AuthService;
import in.edu.sgsits.airportlostfound.passenger.PassengerProfile;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LostReportController.class)
@AutoConfigureMockMvc(addFilters = false)
class LostReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LostReportService lostReportService;

    @MockitoBean
    private AuthService authService;

    @Test
    void returnsLostReportsAsJson() throws Exception {
        when(lostReportService.getLostReports(null, null))
                .thenReturn(List.of(sampleExistingReport()));

        mockMvc.perform(get("/api/lost-reports"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].passengerName")
                        .value("Kabir Singh"))
                .andExpect(jsonPath("$[0].itemName")
                        .value("Blue document folder"))
                .andExpect(jsonPath("$[0].reportStatus")
                        .value("open"));

        verify(lostReportService).getLostReports(null, null);
    }

    @Test
    void passesFiltersToServiceForAuthenticatedPassenger()
            throws Exception {

        when(authService.getProfileByEmail("kabir.singh@example.test"))
                .thenReturn(samplePassengerProfile());

        when(lostReportService.getLostReports("open", 2L))
                .thenReturn(List.of(sampleExistingReport()));

        mockMvc.perform(get("/api/lost-reports")
                        .param("status", "open")
                        .param("passengerId", "2")
                        .principal(authenticatedKabir()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(lostReportService).getLostReports("open", 2L);
    }

    @Test
    void rejectsFilterForAnotherPassenger() throws Exception {
        when(authService.getProfileByEmail("kabir.singh@example.test"))
                .thenReturn(samplePassengerProfile());

        mockMvc.perform(get("/api/lost-reports")
                        .param("passengerId", "1")
                        .principal(authenticatedKabir()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(lostReportService);
    }

    @Test
    void rejectsNonNumericPassengerId() throws Exception {
        mockMvc.perform(get("/api/lost-reports")
                        .param("passengerId", "not-a-number"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(lostReportService);
    }

    @Test
    void returnsBadRequestForInvalidStatus() throws Exception {
        when(lostReportService.getLostReports("banana", null))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid report status"
                ));

        mockMvc.perform(get("/api/lost-reports")
                        .param("status", "banana"))
                .andExpect(status().isBadRequest());

        verify(lostReportService).getLostReports("banana", null);
    }

    @Test
    void createsReportUsingAuthenticatedPassengerInsteadOfBodyId()
            throws Exception {

        when(authService.getProfileByEmail("kabir.singh@example.test"))
                .thenReturn(samplePassengerProfile());

        when(lostReportService.createLostReport(
                eq(2L),
                any(CreateLostReportRequest.class)))
                .thenReturn(sampleCreatedReport());

        // Intentionally submit passengerId=1 to test spoofing.
        // The authenticated principal belongs to passenger ID 2.
        String requestJson = """
                {
                    "passengerId": 1,
                    "categoryId": 2,
                    "itemName": "Green travel pouch",
                    "brand": "TravelCo",
                    "colour": "Green",
                    "description": "Small green pouch with a zipper",
                    "estimatedValue": 500.00,
                    "itemCondition": "good",
                    "lostAt": "2026-10-20T15:00:00+05:30",
                    "flightId": 1,
                    "lastSeenLocationId": 2,
                    "additionalDetails": "Possibly left near baggage claim"
                }
                """;

        mockMvc.perform(post("/api/lost-reports")
                        .principal(authenticatedKabir())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.lostReportId").value(5))
                .andExpect(jsonPath("$.passengerId").value(2))
                .andExpect(jsonPath("$.passengerName")
                        .value("Kabir Singh"))
                .andExpect(jsonPath("$.itemName")
                        .value("Green travel pouch"))
                .andExpect(jsonPath("$.reportStatus").value("open"));

        verify(lostReportService).createLostReport(
                eq(2L),
                any(CreateLostReportRequest.class)
        );
    }

    @Test
    void rejectsInvalidCreateRequestBeforeCallingService()
            throws Exception {

        String invalidRequestJson = """
                {
                    "categoryId": 2,
                    "itemName": "   ",
                    "lostAt": "2026-10-20T15:00:00+05:30",
                    "itemCondition": "excellent"
                }
                """;

        mockMvc.perform(post("/api/lost-reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequestJson))
                .andExpect(status().isBadRequest());

        verify(lostReportService, never()).createLostReport(
                eq(2L),
                any(CreateLostReportRequest.class)
        );
    }

    private static UsernamePasswordAuthenticationToken authenticatedKabir() {
        return UsernamePasswordAuthenticationToken.authenticated(
                "kabir.singh@example.test",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_PASSENGER"))
        );
    }

    private static PassengerProfile samplePassengerProfile() {
        return new PassengerProfile(
                2L,
                "Kabir Singh",
                "kabir.singh@example.test",
                "9876543210",
                OffsetDateTime.parse("2026-10-20T10:00:00+05:30")
        );
    }

    private static LostReportOverview sampleExistingReport() {
        OffsetDateTime timestamp =
                OffsetDateTime.parse("2026-10-20T10:00:00+05:30");

        return new LostReportOverview(
                2L,
                2L,
                "Kabir Singh",
                4L,
                "Blue document folder",
                "Documents",
                "open",
                timestamp,
                "Security Checkpoint A",
                "6E 237",
                "Passenger reports a missing blue document folder.",
                timestamp,
                timestamp
        );
    }

    private static LostReportOverview sampleCreatedReport() {
        OffsetDateTime timestamp =
                OffsetDateTime.parse("2026-10-20T15:00:00+05:30");

        return new LostReportOverview(
                5L,
                2L,
                "Kabir Singh",
                6L,
                "Green travel pouch",
                "Documents",
                "open",
                timestamp,
                "Baggage Claim 1",
                "6E 237",
                "Possibly left near baggage claim",
                timestamp,
                timestamp
        );
    }
}
