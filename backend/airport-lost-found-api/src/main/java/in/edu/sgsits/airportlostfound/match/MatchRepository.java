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
