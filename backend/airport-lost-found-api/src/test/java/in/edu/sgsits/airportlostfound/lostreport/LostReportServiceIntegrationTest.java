package in.edu.sgsits.airportlostfound.lostreport;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class LostReportServiceIntegrationTest {

    @Autowired
    private LostReportService lostReportService;

    @Test
    void createsItemAndLostReportInDatabaseTransaction() {
        OffsetDateTime lostAt = OffsetDateTime.now().minusHours(2);

        CreateLostReportRequest request = new CreateLostReportRequest(
                2L,
                "Integration Test Blue Travel Pouch",
                "TestBrand",
                "Blue",
                "Temporary report created by the integration test.",
                new BigDecimal("1250.00"),
                "good",
                lostAt,
                null,
                null,
                "This report should be rolled back after the test."
        );

        LostReportOverview created =
                lostReportService.createLostReport(2L, request);

        assertNotNull(created);
        assertTrue(created.lostReportId() > 0);
        assertTrue(created.itemId() > 0);

        assertEquals(2L, created.passengerId());
        assertEquals("Airport Passenger", created.passengerName());

        assertEquals(
                "Integration Test Blue Travel Pouch",
                created.itemName()
        );

        assertEquals("Documents", created.categoryName());
        assertEquals("open", created.reportStatus());

        assertEquals(
                "This report should be rolled back after the test.",
                created.additionalDetails()
        );

        assertNotNull(created.reportedAt());
        assertNotNull(created.updatedAt());
    }
}