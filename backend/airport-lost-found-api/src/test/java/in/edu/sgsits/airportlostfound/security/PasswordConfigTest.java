package in.edu.sgsits.airportlostfound.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordConfigTest {

    private final PasswordEncoder passwordEncoder =
            new PasswordConfig().passwordEncoder();

    @Test
    void encodesPasswordWithoutStoringPlaintext() {
        String rawPassword = "Example-Password-123!";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        assertNotEquals(rawPassword, encodedPassword);
        assertTrue(encodedPassword.startsWith("{bcrypt}"));
        assertTrue(passwordEncoder.matches(
                rawPassword, encodedPassword));
        assertFalse(passwordEncoder.matches(
                "Wrong-Password", encodedPassword));
    }
}
