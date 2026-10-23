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
                    log.info("Database schema (app.lost_report) already initialized. Skipping cloud bootstrap.");
                    return;
                }

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

            } catch (Exception ex) {
                log.warn("Could not check or run cloud database bootstrap: {}", ex.getMessage());
            }
        }
    }
}
