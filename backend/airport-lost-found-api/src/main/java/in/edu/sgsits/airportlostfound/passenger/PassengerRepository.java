package in.edu.sgsits.airportlostfound.passenger;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PassengerRepository {

    private static final String INSERT_PASSENGER_SQL = """
            INSERT INTO app.passenger (
                full_name,
                email,
                phone,
                password_hash
            )
            VALUES (?, ?, ?, ?)
            RETURNING passenger_id
            """;

    private static final String FIND_PROFILE_BY_ID_SQL = """
            SELECT
                passenger_id,
                full_name,
                email,
                phone,
                created_at
            FROM app.passenger
            WHERE passenger_id = ?
            """;

    private static final String FIND_PROFILE_BY_EMAIL_SQL = """
            SELECT
                passenger_id,
                full_name,
                email,
                phone,
                created_at
            FROM app.passenger
            WHERE email = ?
            """;

    private static final String FIND_LOGIN_DATA_BY_EMAIL_SQL = """
            SELECT
                passenger_id,
                full_name,
                email,
                password_hash
            FROM app.passenger
            WHERE email = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public PassengerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Inserts a passenger and returns the generated passenger ID.
     * The password must already be encoded.
     */
    public long insertPassenger(
            String fullName,
            String email,
            String phone,
            String encodedPassword) {

        Long passengerId = jdbcTemplate.queryForObject(
                INSERT_PASSENGER_SQL,
                Long.class,
                fullName,
                email,
                phone,
                encodedPassword
        );

        if (passengerId == null) {
            throw new IllegalStateException(
                    "Database did not return the generated passenger ID."
            );
        }

        return passengerId;
    }

    /**
     * Retrieves public passenger details using the passenger ID.
     * Does not return the password hash.
     */
    public Optional<PassengerProfile> findProfileById(long passengerId) {
        List<PassengerProfile> profiles = jdbcTemplate.query(
                FIND_PROFILE_BY_ID_SQL,
                (resultSet, rowNumber) -> new PassengerProfile(
                        resultSet.getLong("passenger_id"),
                        resultSet.getString("full_name"),
                        resultSet.getString("email"),
                        resultSet.getString("phone"),
                        resultSet.getObject(
                                "created_at",
                                java.time.OffsetDateTime.class
                        )
                ),
                passengerId
        );

        return profiles.stream().findFirst();
    }

    /**
     * Retrieves public passenger details using the email.
     * The caller should normalize the email before calling this method.
     */
    public Optional<PassengerProfile> findProfileByEmail(String email) {
        List<PassengerProfile> profiles = jdbcTemplate.query(
                FIND_PROFILE_BY_EMAIL_SQL,
                (resultSet, rowNumber) -> new PassengerProfile(
                        resultSet.getLong("passenger_id"),
                        resultSet.getString("full_name"),
                        resultSet.getString("email"),
                        resultSet.getString("phone"),
                        resultSet.getObject(
                                "created_at",
                                java.time.OffsetDateTime.class
                        )
                ),
                email
        );

        return profiles.stream().findFirst();
    }

    /**
     * Retrieves internal login data by normalized email.
     * Never return this data directly from an API endpoint.
     */
    public Optional<PassengerLoginData> findLoginDataByEmail(String email) {
        List<PassengerLoginData> passengers = jdbcTemplate.query(
                FIND_LOGIN_DATA_BY_EMAIL_SQL,
                (resultSet, rowNumber) -> new PassengerLoginData(
                        resultSet.getLong("passenger_id"),
                        resultSet.getString("full_name"),
                        resultSet.getString("email"),
                        resultSet.getString("password_hash")
                ),
                email
        );

        return passengers.stream().findFirst();
    }
}