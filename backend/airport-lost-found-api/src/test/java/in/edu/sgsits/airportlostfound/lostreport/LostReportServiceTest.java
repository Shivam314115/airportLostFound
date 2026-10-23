package in.edu.sgsits.airportlostfound.lostreport;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LostReportServiceTest {

    @Mock
    private LostReportRepository lostReportRepository;

    @InjectMocks
    private LostReportService lostReportService;

    @Test
    void returnsReportsWithoutFilters() {
        List<LostReportOverview> expected = List.of();

        when(lostReportRepository.findAll(null, null))
                .thenReturn(expected);

        List<LostReportOverview> actual =
                lostReportService.getLostReports(null, null);

        assertSame(expected, actual);
        verify(lostReportRepository).findAll(null, null);
    }

    @Test
    void normalizesStatusAndPassesPassengerId() {
        when(lostReportRepository.findAll("open", 2L))
                .thenReturn(List.of());

        lostReportService.getLostReports(" OPEN ", 2L);

        verify(lostReportRepository).findAll("open", 2L);
    }

    @Test
    void rejectsInvalidStatus() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> lostReportService.getLostReports("banana", null)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST.value(),
                exception.getStatusCode().value()
        );

        verifyNoInteractions(lostReportRepository);
    }

    @Test
    void rejectsZeroPassengerId() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> lostReportService.getLostReports(null, 0L)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST.value(),
                exception.getStatusCode().value()
        );

        verifyNoInteractions(lostReportRepository);
    }

    @Test
    void rejectsNegativePassengerId() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> lostReportService.getLostReports(null, -1L)
        );

        assertEquals(
                HttpStatus.BAD_REQUEST.value(),
                exception.getStatusCode().value()
        );

        verifyNoInteractions(lostReportRepository);
    }
}
