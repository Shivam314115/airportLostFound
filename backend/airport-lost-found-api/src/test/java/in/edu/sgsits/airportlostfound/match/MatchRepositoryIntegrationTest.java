package in.edu.sgsits.airportlostfound.match;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class MatchRepositoryIntegrationTest {

    @Autowired
    private MatchRepository matchRepository;

    @Test
    void readsMatchRecordsFromPostgres() {
        List<MatchOverview> matches =
                matchRepository.findAll(null, null);

        assertFalse(matches.isEmpty(),
                "Expected match records from PostgreSQL");

        assertTrue(
                matches.stream().anyMatch(match ->
                        "Airport Passenger".equals(match.passengerName())
                                && "verified".equals(match.matchStatus())
                ),
                "Expected passenger verified match"
        );
    }

    @Test
    void filtersCandidateMatchesInDatabase() {
        List<MatchOverview> candidates =
                matchRepository.findAll("candidate", null);

        assertFalse(candidates.isEmpty(),
                "Expected at least one candidate match");

        assertTrue(
                candidates.stream().allMatch(match ->
                        "candidate".equals(match.matchStatus())
                ),
                "Every returned match should have candidate status"
        );
    }

    @Test
    void filtersMatchesByMinimumScoreInDatabase() {
        BigDecimal minimumScore = new BigDecimal("80");

        List<MatchOverview> matches =
                matchRepository.findAll(null, minimumScore);

        assertFalse(matches.isEmpty(),
                "Expected at least one match with score >= 80");

        for (MatchOverview match : matches) {
            assertNotNull(match.matchScore());

            assertTrue(
                    match.matchScore().compareTo(minimumScore) >= 0,
                    "Returned match score must be at least 80"
            );
        }
    }
}
