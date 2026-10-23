package in.edu.sgsits.airportlostfound.match;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Represents one lost-and-found match returned by the database.
 *
 * This record is a data-transfer object (DTO).
 * It contains data; it does not execute SQL or implement business rules.
 */
public record MatchOverview(
        long lostReportId,
        String passengerName,
        String lostItemName,
        String lostCategory,
        String reportStatus,
        OffsetDateTime lostAt,

        long foundItemId,
        String foundItemName,
        String foundCategory,
        String foundLocation,
        OffsetDateTime foundAt,

        BigDecimal matchScore,
        String matchStatus,
        String matchRemarks,
        String reviewedBy,
        OffsetDateTime matchedAt,
        OffsetDateTime reviewedAt
) {
}
