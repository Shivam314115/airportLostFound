package in.edu.sgsits.airportlostfound.match;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MatchRepository {

    private static final String SELECT_FROM_SQL = """
            SELECT
                lost_report_id,
                passenger_name,
                lost_item_name,
                lost_category,
                report_status,
                lost_at,
                found_item_id,
                found_item_name,
                found_category,
                found_location,
                found_at,
                match_score,
                match_status,
                match_remarks,
                reviewed_by,
                matched_at,
                reviewed_at
            FROM app.v_match_overview
            """;

    private static final String ORDER_BY_SQL = """
            ORDER BY match_score DESC NULLS LAST,
                     lost_report_id ASC
            """;

    private final JdbcTemplate jdbcTemplate;

    public MatchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MatchOverview> findAll(
            String status,
            BigDecimal minScore) {

        autoCorrelateMatches();

        StringBuilder sql = new StringBuilder(SELECT_FROM_SQL);
        List<Object> parameters = new ArrayList<>();
        boolean hasWhere = false;

        if (status != null) {
            sql.append("\nWHERE match_status = ?");
            parameters.add(status);
            hasWhere = true;
        }

        if (minScore != null) {
            sql.append(hasWhere
                    ? "\nAND match_score >= ?"
                    : "\nWHERE match_score >= ?");

            parameters.add(minScore);
        }

        sql.append("\n").append(ORDER_BY_SQL);

        return jdbcTemplate.query(
                sql.toString(),
                MATCH_ROW_MAPPER,
                parameters.toArray()
        );
    }

    public int autoCorrelateMatches() {
        try {
            String matchQuery = """
                INSERT INTO app.item_match (
                    lost_report_id,
                    found_item_id,
                    match_score,
                    verification_status,
                    remarks
                )
                SELECT
                    lr.lost_report_id,
                    fi.found_item_id,
                    LEAST(99.00,
                        (CASE WHEN li.category_id = found_i.category_id THEN 35.00 ELSE 15.00 END) +
                        (CASE WHEN LOWER(TRIM(li.item_name)) = LOWER(TRIM(found_i.item_name)) THEN 35.00
                              WHEN LOWER(found_i.item_name) LIKE '%' || LOWER(TRIM(li.item_name)) || '%'
                                OR LOWER(li.item_name) LIKE '%' || LOWER(TRIM(found_i.item_name)) || '%' THEN 25.00
                              ELSE 0.00 END) +
                        (CASE WHEN li.brand IS NOT NULL AND found_i.brand IS NOT NULL
                                   AND LOWER(TRIM(li.brand)) = LOWER(TRIM(found_i.brand)) THEN 10.00 ELSE 0.00 END) +
                        (CASE WHEN lr.flight_id IS NOT NULL AND fi.flight_id IS NOT NULL
                                   AND lr.flight_id = fi.flight_id THEN 15.00 ELSE 0.00 END) +
                        (CASE WHEN lr.last_seen_location_id IS NOT NULL AND fi.found_location_id IS NOT NULL
                                   AND lr.last_seen_location_id = fi.found_location_id THEN 10.00 ELSE 0.00 END)
                    ) AS calc_score,
                    'candidate',
                    CONCAT_WS(' ',
                        CASE WHEN li.category_id = found_i.category_id THEN 'Matching category.' END,
                        CASE WHEN LOWER(TRIM(li.item_name)) = LOWER(TRIM(found_i.item_name)) THEN 'Exact name match.' END,
                        CASE WHEN lr.flight_id IS NOT NULL AND lr.flight_id = fi.flight_id THEN 'Matching flight.' END,
                        CASE WHEN lr.last_seen_location_id IS NOT NULL AND lr.last_seen_location_id = fi.found_location_id THEN 'Matching checkpoint.' END
                    ) AS remarks
                FROM app.lost_report lr
                JOIN app.item li ON li.item_id = lr.item_id
                CROSS JOIN app.found_item fi
                JOIN app.item found_i ON found_i.item_id = fi.item_id
                WHERE lr.report_status IN ('open', 'candidate_found')
                  AND fi.item_status IN ('awaiting_review', 'stored')
                  AND li.category_id = found_i.category_id
                  AND (
                      LOWER(TRIM(li.item_name)) = LOWER(TRIM(found_i.item_name))
                      OR LOWER(found_i.item_name) LIKE '%' || LOWER(TRIM(li.item_name)) || '%'
                      OR LOWER(li.item_name) LIKE '%' || LOWER(TRIM(found_i.item_name)) || '%'
                      OR (lr.flight_id IS NOT NULL AND lr.flight_id = fi.flight_id)
                      OR (li.brand IS NOT NULL AND found_i.brand IS NOT NULL AND LOWER(TRIM(li.brand)) = LOWER(TRIM(found_i.brand)))
                  )
                  AND (
                      35.00 +
                      (CASE WHEN LOWER(TRIM(li.item_name)) = LOWER(TRIM(found_i.item_name)) THEN 35.00
                            WHEN LOWER(found_i.item_name) LIKE '%' || LOWER(TRIM(li.item_name)) || '%'
                              OR LOWER(li.item_name) LIKE '%' || LOWER(TRIM(found_i.item_name)) || '%' THEN 25.00
                            ELSE 0.00 END) +
                      (CASE WHEN li.brand IS NOT NULL AND found_i.brand IS NOT NULL
                                 AND LOWER(TRIM(li.brand)) = LOWER(TRIM(found_i.brand)) THEN 10.00 ELSE 0.00 END) +
                      (CASE WHEN lr.flight_id IS NOT NULL AND fi.flight_id IS NOT NULL
                                 AND lr.flight_id = fi.flight_id THEN 15.00 ELSE 0.00 END) +
                      (CASE WHEN lr.last_seen_location_id IS NOT NULL AND fi.found_location_id IS NOT NULL
                                 AND lr.last_seen_location_id = fi.found_location_id THEN 10.00 ELSE 0.00 END)
                  ) >= 50.00
                  AND NOT EXISTS (
                      SELECT 1 FROM app.item_match m
                      WHERE m.lost_report_id = lr.lost_report_id
                        AND m.found_item_id = fi.found_item_id
                  )
                ON CONFLICT (lost_report_id, found_item_id) DO NOTHING
                """;
            int count = jdbcTemplate.update(matchQuery);

            String claimQuery = """
                INSERT INTO app.claim (match_id, claim_status)
                SELECT m.match_id, 'submitted'
                FROM app.item_match m
                WHERE NOT EXISTS (SELECT 1 FROM app.claim c WHERE c.match_id = m.match_id)
                ON CONFLICT (match_id) DO NOTHING
                """;
            jdbcTemplate.update(claimQuery);

            String idVerifQuery = """
                INSERT INTO app.identity_verification (claim_id, verification_method, verification_result, verification_notes)
                SELECT c.claim_id, 'in_person_document_check', 'pending', 'Automated match candidate awaiting claims officer document verification.'
                FROM app.claim c
                WHERE NOT EXISTS (SELECT 1 FROM app.identity_verification iv WHERE iv.claim_id = c.claim_id)
                ON CONFLICT DO NOTHING
                """;
            jdbcTemplate.update(idVerifQuery);

            String updateReportStatus = """
                UPDATE app.lost_report lr
                SET report_status = 'candidate_found'
                WHERE lr.report_status IN ('open', 'candidate_found')
                  AND EXISTS (
                      SELECT 1 FROM app.item_match m
                      WHERE m.lost_report_id = lr.lost_report_id
                  )
                """;
            jdbcTemplate.update(updateReportStatus);

            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    private static final RowMapper<MatchOverview> MATCH_ROW_MAPPER =
            (ResultSet rs, int rowNum) -> mapMatchOverview(rs);

    private static MatchOverview mapMatchOverview(ResultSet rs)
            throws SQLException {

        return new MatchOverview(
                rs.getLong("lost_report_id"),
                rs.getString("passenger_name"),
                rs.getString("lost_item_name"),
                rs.getString("lost_category"),
                rs.getString("report_status"),
                rs.getObject("lost_at", OffsetDateTime.class),

                rs.getLong("found_item_id"),
                rs.getString("found_item_name"),
                rs.getString("found_category"),
                rs.getString("found_location"),
                rs.getObject("found_at", OffsetDateTime.class),

                rs.getBigDecimal("match_score"),
                rs.getString("match_status"),
                rs.getString("match_remarks"),
                rs.getString("reviewed_by"),
                rs.getObject("matched_at", OffsetDateTime.class),
                rs.getObject("reviewed_at", OffsetDateTime.class)
        );
    }
}
