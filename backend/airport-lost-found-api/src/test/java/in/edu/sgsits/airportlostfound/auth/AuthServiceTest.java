package in.edu.sgsits.airportlostfound.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import in.edu.sgsits.airportlostfound.passenger.PassengerLoginData;
import in.edu.sgsits.airportlostfound.passenger.PassengerProfile;
import in.edu.sgsits.airportlostfound.passenger.PassengerRepository;

import in.edu.sgsits.airportlostfound.staff.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private PassengerLoginData loginData;
    private PassengerProfile profile;

    @BeforeEach
    void setUp() {
        loginData = new PassengerLoginData(
                42L,
                "Test Passenger",
                "passenger@example.test",
                "{bcrypt}encoded-password"
        );

        profile = new PassengerProfile(
                42L,
                "Test Passenger",
                "passenger@example.test",
                "9876543210",
                OffsetDateTime.parse("2026-10-20T14:00:00+05:30")
        );
    }

    @Test
    void validCredentialsReturnPassengerProfile() {
        when(passengerRepository.findLoginDataByEmail(
                "passenger@example.test"
        )).thenReturn(Optional.of(loginData));

        when(passwordEncoder.matches(
                "Correct-Password-42",
                "{bcrypt}encoded-password"
        )).thenReturn(true);

        when(passengerRepository.findProfileById(42L))
                .thenReturn(Optional.of(profile));

        PassengerProfile result = authService.authenticate(
                new LoginRequest(
                        " PASSENGER@EXAMPLE.TEST ",
                        "Correct-Password-42"
                )
        );

        assertEquals(42L, result.passengerId());
        assertEquals("passenger@example.test", result.email());

        verify(passengerRepository)
                .findLoginDataByEmail("passenger@example.test");
    }

    @Test
    void incorrectPasswordReturnsUnauthorized() {
        when(passengerRepository.findLoginDataByEmail(
                "passenger@example.test"
        )).thenReturn(Optional.of(loginData));

        when(passwordEncoder.matches(
                "Wrong-Password-42",
                "{bcrypt}encoded-password"
        )).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.authenticate(
                        new LoginRequest(
                                "passenger@example.test",
                                "Wrong-Password-42"
                        )
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());

        verify(passengerRepository, never()).findProfileById(42L);
    }

    @Test
    void unknownEmailReturnsUnauthorized() {
        when(passengerRepository.findLoginDataByEmail(
                "unknown@example.test"
        )).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.authenticate(
                        new LoginRequest(
                                "unknown@example.test",
                                "Any-Password-42"
                        )
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());

        verify(passwordEncoder, never()).matches(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString()
        );
    }

    @Test
    void malformedStoredHashReturnsUnauthorized() {
        PassengerLoginData malformedData = new PassengerLoginData(
                42L,
                "Test Passenger",
                "passenger@example.test",
                "malformed-hash"
        );

        when(passengerRepository.findLoginDataByEmail(
                "passenger@example.test"
        )).thenReturn(Optional.of(malformedData));

        when(passwordEncoder.matches(
                "Correct-Password-42",
                "malformed-hash"
        )).thenThrow(new IllegalArgumentException("Invalid password hash"));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> authService.authenticate(
                        new LoginRequest(
                                "passenger@example.test",
                                "Correct-Password-42"
                        )
                )
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }
}
