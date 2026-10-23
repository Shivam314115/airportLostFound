package in.edu.sgsits.airportlostfound.passenger;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PassengerService {

    private final PassengerRepository passengerRepository;
    private final PasswordEncoder passwordEncoder;

    public PassengerService(
            PassengerRepository passengerRepository,
            PasswordEncoder passwordEncoder) {
        this.passengerRepository = passengerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public PassengerProfile register(RegisterPassengerRequest request) {
        String fullName = request.fullName().trim();

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        String phone = normalizeOptionalText(request.phone());
        String rawPassword = request.password();

        // BCrypt supports passwords up to 72 UTF-8 bytes.
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must not exceed 72 UTF-8 bytes."
            );
        }

        // Never save a raw password in the database.
        String encodedPassword = passwordEncoder.encode(rawPassword);

        try {
            long passengerId = passengerRepository.insertPassenger(
                    fullName,
                    email,
                    phone,
                    encodedPassword
            );

            return passengerRepository.findProfileById(passengerId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Passenger was created but could not be retrieved."
                    ));

        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account with this email already exists."
            );
        }
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}