
package in.edu.sgsits.airportlostfound.lostreport;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class LostReportRepository {

    private static final String SELECT_SQL = """
            SELECT
                lr.lost_report_id,
                p.passenger_id,
                p.full_name AS passenger_name,
                i.item_id,
                i.item_name,
                c.category_name,
                lr.report_status,
                lr.lost_at,
                loc.location_name AS last_seen_location,
                f.flight_number,
                lr.additional_details,
                lr.reported_at,
                lr.updated_at,
                COALESCE((SELECT ARRAY_AGG(p.photo_url ORDER BY p.photo_number) FROM app.item_photo p WHERE p.item_id = lr.item_id), ARRAY[]::TEXT[]) AS photo_urls
            FROM app.lost_report AS lr
            JOIN app.passenger AS p
                ON p.passenger_id = lr.passenger_id
            JOIN app.item AS i
                ON i.item_id = lr.item_id
            JOIN app.item_category AS c
                ON c.category_id = i.category_id
            LEFT JOIN app.airport_location AS loc
                ON loc.location_id = lr.last_seen_location_id
            LEFT JOIN app.flight AS f
                ON f.flight_id = lr.flight_id
            """;

    private static final String ORDER_BY_SQL = """
            ORDER BY lr.reported_at DESC,
                     lr.lost_report_id DESC
            """;

    private static final String INSERT_ITEM_SQL = """
            INSERT INTO app.item (
                category_id,
                item_name,
                brand,
                colour,
                description,
                estimated_value,
                item_condition
            )
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING item_id
            """;

    private static final String INSERT_REPORT_SQL = """
            INSERT INTO app.lost_report (
                passenger_id,
                item_id,
                flight_id,
                last_seen_location_id,
                lost_at,
                additional_details
            )
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING lost_report_id
            """;

    private final JdbcTemplate jdbcTemplate;

    public LostReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<LostReportOverview> findAll(
            String status,
            Long passengerId) {

        StringBuilder sql = new StringBuilder(SELECT_SQL);
        List<Object> parameters = new ArrayList<>();
        boolean hasWhere = false;

        if (status != null) {
            sql.append("\nWHERE lr.report_status = ?");
            parameters.add(status);
            hasWhere = true;
        }

        if (passengerId != null) {
            sql.append(hasWhere
                    ? "\nAND lr.passenger_id = ?"
                    : "\nWHERE lr.passenger_id = ?");

            parameters.add(passengerId);
        }

        sql.append("\n").append(ORDER_BY_SQL);

        return jdbcTemplate.query(
                sql.toString(),
                REPORT_ROW_MAPPER,
                parameters.toArray()
        );
    }

    public Optional<LostReportOverview> findById(long lostReportId) {
        List<LostReportOverview> results = jdbcTemplate.query(
                SELECT_SQL + "\nWHERE lr.lost_report_id = ?",
                REPORT_ROW_MAPPER,
                lostReportId
        );

        return results.stream().findFirst();
    }

    public long insertItem(
            Long categoryId,
            String itemName,
            String brand,
            String colour,
            String description,
            BigDecimal estimatedValue,
            String itemCondition) {

        Long itemId = jdbcTemplate.queryForObject(
                INSERT_ITEM_SQL,
                Long.class,
                categoryId,
                itemName,
                brand,
                colour,
                description,
                estimatedValue,
                itemCondition
        );

        if (itemId == null) {
            throw new IllegalStateException(
                    "Database did not return the generated item ID."
            );
        }

        return itemId;
    }

    public long insertLostReport(
            Long passengerId,
            long itemId,
            Long flightId,
            Long lastSeenLocationId,
            OffsetDateTime lostAt,
            String additionalDetails) {

        Long lostReportId = jdbcTemplate.queryForObject(
                INSERT_REPORT_SQL,
                Long.class,
                passengerId,
                itemId,
                flightId,
                lastSeenLocationId,
                lostAt,
                additionalDetails
        );

        if (lostReportId == null) {
            throw new IllegalStateException(
                    "Database did not return the generated lost report ID."
            );
        }

        return lostReportId;
    }

    public void insertPhoto(long itemId, int photoNumber, String photoUrl, String caption, String photoType) {
        String sql = """
                INSERT INTO app.item_photo (item_id, photo_number, photo_url, caption, photo_type, uploaded_at)
                VALUES (?, ?, ?, ?, COALESCE(?, 'overview'), CURRENT_TIMESTAMP)
                ON CONFLICT (item_id, photo_number) DO UPDATE
                SET photo_url = EXCLUDED.photo_url, caption = EXCLUDED.caption, photo_type = EXCLUDED.photo_type
                """;
        jdbcTemplate.update(sql, itemId, photoNumber, photoUrl, caption, photoType != null ? photoType : "overview");
    }

    private static final RowMapper<LostReportOverview> REPORT_ROW_MAPPER =
            (ResultSet rs, int rowNum) -> mapReport(rs);

    private static LostReportOverview mapReport(ResultSet rs)
            throws SQLException {

        List<String> photoUrls = List.of();
        try {
            java.sql.Array arr = rs.getArray("photo_urls");
            if (arr != null) {
                String[] items = (String[]) arr.getArray();
                if (items != null) {
                    photoUrls = List.of(items);
                }
            }
        } catch (Exception ignored) {
        }

        return new LostReportOverview(
                rs.getLong("lost_report_id"),
                rs.getLong("passenger_id"),
                rs.getString("passenger_name"),

                rs.getLong("item_id"),
                rs.getString("item_name"),
                rs.getString("category_name"),

                rs.getString("report_status"),
                rs.getObject("lost_at", OffsetDateTime.class),
                rs.getString("last_seen_location"),
                rs.getString("flight_number"),
                rs.getString("additional_details"),
                rs.getObject("reported_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class),
                photoUrls
        );
    }
}
