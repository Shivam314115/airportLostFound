package in.edu.sgsits.airportlostfound.common;

import java.net.URI;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class DatabaseConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfiguration.class);

    @Bean
    @Primary
    public DataSource dataSource(
            @Value("${DATABASE_URL:#{null}}") String cloudDatabaseUrl,
            @Value("${spring.datasource.url}") String defaultJdbcUrl,
            @Value("${spring.datasource.username}") String defaultUsername,
            @Value("${spring.datasource.password}") String defaultPassword
    ) {
        if (cloudDatabaseUrl != null && !cloudDatabaseUrl.isBlank()) {
            try {
                String cleanUrl = cloudDatabaseUrl.trim();
                if (cleanUrl.startsWith("postgres://") || cleanUrl.startsWith("postgresql://")) {
                    URI uri = new URI(cleanUrl);
                    String userInfo = uri.getUserInfo();
                    String username = defaultUsername;
                    String password = defaultPassword;
                    if (userInfo != null && userInfo.contains(":")) {
                        String[] parts = userInfo.split(":", 2);
                        username = parts[0];
                        password = parts[1];
                    } else if (userInfo != null) {
                        username = userInfo;
                    }
                    int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                    String path = uri.getPath();
                    String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + port + path;

                    log.info("Configured cloud PostgreSQL DataSource from DATABASE_URL -> {}", jdbcUrl);

                    HikariDataSource ds = new HikariDataSource();
                    ds.setJdbcUrl(jdbcUrl);
                    ds.setUsername(username);
                    ds.setPassword(password);
                    ds.setDriverClassName("org.postgresql.Driver");
                    return ds;
                }
            } catch (Exception ex) {
                log.warn("Failed to parse DATABASE_URL ({}). Using spring.datasource.url.", ex.getMessage());
            }
        }

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(defaultJdbcUrl);
        ds.setUsername(defaultUsername);
        ds.setPassword(defaultPassword);
        ds.setDriverClassName("org.postgresql.Driver");
        return ds;
    }

    @Component
    public static class CloudDatabaseInitializer implements ApplicationRunner {

        private static final Logger log = LoggerFactory.getLogger(CloudDatabaseInitializer.class);
        private final DataSource dataSource;

        public CloudDatabaseInitializer(DataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public void run(ApplicationArguments args) {
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                // Check if app schema and lost_report table already exist
                boolean schemaExists = false;
                try (ResultSet rs = stmt.executeQuery("SELECT to_regclass('app.lost_report')")) {
                    if (rs.next() && rs.getString(1) != null) {
                        schemaExists = true;
                    }
                }

                if (schemaExists) {
                    log.info("Database schema (app.lost_report) already initialized. Checking storage and workflow seeds...");
                } else {
                    log.info("Database is empty or missing app schema. Running cloud_bootstrap.sql...");
                    Resource script = new ClassPathResource("database/cloud_bootstrap.sql");
                    if (script.exists()) {
                        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(script);
                        populator.setContinueOnError(true);
                        populator.populate(conn);
                        log.info("Cloud database bootstrap completed successfully!");
                    } else {
                        log.warn("database/cloud_bootstrap.sql not found on classpath.");
                    }
                }

                // Check if app.storage_location has any records. If not, auto-seed storage and demonstration workflow records
                boolean hasStorage = false;
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM app.storage_location")) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        hasStorage = true;
                    }
                } catch (Exception e) {
                    log.debug("Could not query app.storage_location: {}", e.getMessage());
                }

                if (!hasStorage) {
                    log.info("No storage locations found in database. Initializing default storage facilities and workflow seeds...");
                    Resource storageScript = new ClassPathResource("database/12_storage_and_workflow_seed.sql");
                    if (storageScript.exists()) {
                        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(storageScript);
                        populator.setContinueOnError(true);
                        populator.populate(conn);
                        log.info("Default storage locations and workflow seeds successfully applied!");
                    } else {
                        log.warn("database/12_storage_and_workflow_seed.sql not found on classpath.");
                    }
                }

                // Check and auto-correlate candidate matches on server startup
                try {
                    String startupMatchSql = """
                        INSERT INTO app.item_match (lost_report_id, found_item_id, match_score, verification_status, remarks)
                        SELECT
                            lr.lost_report_id, fi.found_item_id,
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
                        ON CONFLICT (lost_report_id, found_item_id) DO NOTHING;

                        INSERT INTO app.claim (match_id, claim_status)
                        SELECT m.match_id, 'submitted'
                        FROM app.item_match m
                        WHERE NOT EXISTS (SELECT 1 FROM app.claim c WHERE c.match_id = m.match_id)
                        ON CONFLICT (match_id) DO NOTHING;

                        INSERT INTO app.identity_verification (claim_id, verification_method, verification_result, verification_notes)
                        SELECT c.claim_id, 'in_person_document_check', 'pending', 'Automated match candidate awaiting claims officer document verification.'
                        FROM app.claim c
                        WHERE NOT EXISTS (SELECT 1 FROM app.identity_verification iv WHERE iv.claim_id = c.claim_id)
                        ON CONFLICT DO NOTHING;

                        UPDATE app.lost_report lr
                        SET report_status = 'candidate_found'
                        WHERE lr.report_status = 'open'
                          AND EXISTS (SELECT 1 FROM app.item_match m WHERE m.lost_report_id = lr.lost_report_id);
                    """;
                    stmt.execute(startupMatchSql);
                    log.info("Auto-correlation startup check completed.");
                } catch (Exception matchEx) {
                    log.debug("Auto-correlation startup check skipped: {}", matchEx.getMessage());
                }

            } catch (Exception ex) {
                log.warn("Could not check or run cloud database bootstrap: {}", ex.getMessage());
            }
        }
    }
}
