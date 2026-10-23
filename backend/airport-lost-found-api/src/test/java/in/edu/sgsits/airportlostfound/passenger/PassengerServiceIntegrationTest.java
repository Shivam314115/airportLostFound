package in.edu.sgsits.airportlostfound.passenger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
class PassengerServiceIntegrationTest {

    @Autowired
    private PassengerService passengerService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String normalizedEmail;

    private static final String RAW_PASSWORD =
            "Correct-Horse-Battery-42";

    @BeforeEach
    void setUp() {
        normalizedEmail = "passenger-integration-"
                + UUID.randomUUID()
                + "@example.test";
    }

    @AfterEach
    void cleanUp() {
        if (normalizedEmail != null) {
            jdbcTemplate.update(
                    "DELETE FROM app.passenger WHERE email = ?",
                    normalizedEmail
            );
        }
    }

    @Test
    void registrationStoresEncodedPasswordAndReturnsSafeProfile() {
        RegisterPassengerRequest request =
                new RegisterPassengerRequest(
                        "  Passenger Integration Test  ",
                        "  " + normalizedEmail.toUpperCase(Locale.ROOT) + "  ",
                        "9876543210",
                        RAW_PASSWORD
                );

        PassengerProfile profile = passengerService.register(request);

        assertTrue(profile.passengerId() > 0);
        assertEquals("Passenger Integration Test", profile.fullName());
        assertEquals(normalizedEmail, profile.email());
        assertNotNull(profile.createdAt());

        String storedPasswordHash = jdbcTemplate.queryForObject(
                """
                SELECT password_hash
                FROM app.passenger
                WHERE email = ?
                """,
                String.class,
                normalizedEmail
        );

        assertNotNull(storedPasswordHash);
        assertNotEquals(RAW_PASSWORD, storedPasswordHash);
        assertTrue(storedPasswordHash.startsWith("{bcrypt}"));
        assertTrue(passwordEncoder.matches(
                RAW_PASSWORD,
                storedPasswordHash
        ));
    }

    @Test
    void duplicateEmailDoesNotCreateAnotherPassenger() {
        RegisterPassengerRequest request =
                new RegisterPassengerRequest(
                        "Duplicate Test Passenger",
                        normalizedEmail,
                        "9876543210",
                        RAW_PASSWORD
                );

        passengerService.register(request);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> passengerService.register(request)
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());

        Integer passengerCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM app.passenger
                WHERE email = ?
                """,
                Integer.class,
                normalizedEmail
        );

        assertEquals(1, passengerCount);
    }
}
