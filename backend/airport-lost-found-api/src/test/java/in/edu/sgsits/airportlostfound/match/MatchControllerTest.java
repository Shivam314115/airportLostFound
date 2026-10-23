	package in.edu.sgsits.airportlostfound.match;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MatchController.class)
class MatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatchService matchService;

    @Test
    void returnsMatchesAsJson() throws Exception {
        MatchOverview match = sampleMatch();

        when(matchService.getMatches(null, null))
                .thenReturn(List.of(match));

        mockMvc.perform(get("/api/matches"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].passengerName")
                        .value("Airport Passenger"))
                .andExpect(jsonPath("$[0].matchScore")
                        .value(96.50))
                .andExpect(jsonPath("$[0].matchStatus")
                        .value("verified"));

        verify(matchService).getMatches(null, null);
    }

    @Test
    void passesFiltersToService() throws Exception {
        BigDecimal minScore = new BigDecimal("70");

        when(matchService.getMatches(
                eq("candidate"), eq(minScore)))
                .thenReturn(List.of(sampleMatch()));

        mockMvc.perform(get("/api/matches")
                        .param("status", "candidate")
                        .param("minScore", "70"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(matchService).getMatches("candidate", minScore);
    }

    @Test
    void rejectsNonNumericMinScore() throws Exception {
        mockMvc.perform(get("/api/matches")
                        .param("minScore", "not-a-number"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(matchService);
    }

    private static MatchOverview sampleMatch() {
        OffsetDateTime timestamp =
                OffsetDateTime.parse("2026-10-20T10:00:00+05:30");

        return new MatchOverview(
                1L,
                "Airport Passenger",
                "Black wireless earbuds",
                "Electronics",
                "resolved",
                timestamp,

                1L,
                "Black wireless earbuds",
                "Electronics",
                "Gate 4",
                timestamp,

                new BigDecimal("96.50"),
                "verified",
                "Strong candidate based on item description.",
                "Airport Staff Officer",
                timestamp,
                timestamp
        );
    }
}
