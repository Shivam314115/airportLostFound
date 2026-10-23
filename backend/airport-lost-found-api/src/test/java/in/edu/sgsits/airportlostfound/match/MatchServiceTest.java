package in.edu.sgsits.airportlostfound.match;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private MatchRepository matchRepository;

    @InjectMocks
    private MatchService matchService;

    @Test
    void returnsMatchesWhenNoFiltersAreProvided() {
        List<MatchOverview> expected = List.of();

        when(matchRepository.findAll(null, null))
                .thenReturn(expected);

        List<MatchOverview> actual =
                matchService.getMatches(null, null);

        assertSame(expected, actual);

        verify(matchRepository).findAll(null, null);
    }

    @Test
    void normalizesStatusBeforeQueryingRepository() {
        BigDecimal minScore = BigDecimal.valueOf(80);

        when(matchRepository.findAll("candidate", minScore))
                .thenReturn(List.of());

        matchService.getMatches(" Candidate ", minScore);

        verify(matchRepository).findAll("candidate", minScore);
    }

    @Test
    void rejectsUnknownStatus() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> matchService.getMatches("banana", null)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST.value(),
                exception.getStatusCode().value()
        );

        verifyNoInteractions(matchRepository);
    }

    @Test
    void rejectsScoreBelowZero() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> matchService.getMatches(
                        null, BigDecimal.valueOf(-1)
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST.value(),
                exception.getStatusCode().value()
        );

        verifyNoInteractions(matchRepository);
    }

    @Test
    void rejectsScoreAboveOneHundred() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> matchService.getMatches(
                        null, BigDecimal.valueOf(101)
                )
        );

        assertEquals(
                HttpStatus.BAD_REQUEST.value(),
                exception.getStatusCode().value()
        );

        verifyNoInteractions(matchRepository);
    }
}
