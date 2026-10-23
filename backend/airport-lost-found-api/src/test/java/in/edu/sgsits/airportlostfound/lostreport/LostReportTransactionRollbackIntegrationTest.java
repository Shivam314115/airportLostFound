package in.edu.sgsits.airportlostfound.lostreport;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class LostReportTransactionRollbackIntegrationTest {

    @Autowired
    private LostReportService lostReportService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void rollsBackItemWhenLostReportInsertFails() {
        // Use a unique name so this test does not collide with other records.
        String itemName = "Rollback Test " + UUID.randomUUID();

        // Confirm the prerequisite category exists.
        Integer categoryCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM app.item_category
                WHERE category_id = ?
                """,
                Integer.class,
                2L
        );

        assertEquals(
                1,
                categoryCount,
                "The Documents category must exist for this test."
        );

        // This ID must not belong to an existing passenger.
        Long nonexistentPassengerId = Long.MAX_VALUE;

        Integer passengerCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM app.passenger
                WHERE passenger_id = ?
                """,
                Integer.class,
                nonexistentPassengerId
        );

        assertEquals(
                0,
                passengerCount,
                "The test passenger ID must not already exist."
        );

        Integer itemsBefore = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM app.item
                WHERE item_name = ?
                """,
                Integer.class,
                itemName
        );

        CreateLostReportRequest request = new CreateLostReportRequest(
                2L,
                itemName,
                "TestBrand",
                "Blue",
                "Temporary record for the rollback integration test.",
                new BigDecimal("250.00"),
                "good",
                OffsetDateTime.now(),
                null,
                null,
                "The report insert should fail because the passenger does not exist."
        );

        // The item INSERT should succeed first. The lost_report INSERT
        // should then fail its passenger foreign-key constraint.
        assertThrows(
                DataIntegrityViolationException.class,
                () -> lostReportService.createLostReport(
                        nonexistentPassengerId,
                        request
                )
        );

        // The transaction must roll back the earlier item INSERT.
        Integer itemsAfter = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM app.item
                WHERE item_name = ?
                """,
                Integer.class,
                itemName
        );

        assertEquals(
                itemsBefore,
                itemsAfter,
                "The item INSERT must be rolled back when report creation fails."
        );
    }
}