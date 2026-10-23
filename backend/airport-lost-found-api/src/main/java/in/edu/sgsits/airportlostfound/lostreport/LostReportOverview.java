package in.edu.sgsits.airportlostfound.lostreport;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Read model for a lost report and its related descriptive data.
 *
 * This record contains data returned by a database query.
 * It does not execute SQL or implement business rules.
 */
public record LostReportOverview(
        long lostReportId,
        long passengerId,
        String passengerName,

        long itemId,
        String itemName,
        String categoryName,

        String reportStatus,
        OffsetDateTime lostAt,
        String lastSeenLocation,
        String flightNumber,
        String additionalDetails,
        OffsetDateTime reportedAt,
        OffsetDateTime updatedAt,
        List<String> photoUrls
) {
    public LostReportOverview(
            long lostReportId,
            long passengerId,
            String passengerName,
            long itemId,
            String itemName,
            String categoryName,
            String reportStatus,
            OffsetDateTime lostAt,
            String lastSeenLocation,
            String flightNumber,
            String additionalDetails,
            OffsetDateTime reportedAt,
            OffsetDateTime updatedAt
    ) {
        this(lostReportId, passengerId, passengerName, itemId, itemName, categoryName,
                reportStatus, lostAt, lastSeenLocation, flightNumber, additionalDetails,
                reportedAt, updatedAt, List.of());
    }
}
