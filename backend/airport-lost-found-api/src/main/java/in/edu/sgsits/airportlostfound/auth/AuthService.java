package in.edu.sgsits.airportlostfound.auth;

import java.util.Locale;

import in.edu.sgsits.airportlostfound.passenger.PassengerLoginData;
import in.edu.sgsits.airportlostfound.passenger.PassengerProfile;
import in.edu.sgsits.airportlostfound.passenger.PassengerRepository;
import in.edu.sgsits.airportlostfound.security.PassengerUserDetailsService;
import in.edu.sgsits.airportlostfound.staff.StaffLoginData;
import in.edu.sgsits.airportlostfound.staff.StaffProfile;
import in.edu.sgsits.airportlostfound.staff.StaffRepository;
import in.edu.sgsits.airportlostfound.staff.StaffRole;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final PassengerRepository passengerRepository;
    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            PassengerRepository passengerRepository,
            StaffRepository staffRepository,
            PasswordEncoder passwordEncoder) {
        this.passengerRepository = passengerRepository;
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Verifies user credentials and returns the public profile.
     */
    @Transactional(readOnly = true)
    public PassengerProfile authenticate(LoginRequest request) {
        String email = PassengerUserDetailsService.resolveEmailAlias(request.email());

        var passengerOpt = passengerRepository.findLoginDataByEmail(email);
        if (passengerOpt.isPresent()) {
            PassengerLoginData passenger = passengerOpt.get();
            boolean passwordMatches;
            try {
                passwordMatches = passwordEncoder.matches(request.password(), passenger.passwordHash());
            } catch (IllegalArgumentException exception) {
                passwordMatches = false;
            }
            if (!passwordMatches) {
                throw invalidCredentials();
            }
            return passengerRepository
                    .findProfileById(passenger.passengerId())
                    .orElseThrow(AuthService::invalidCredentials);
        }

        var staffOpt = staffRepository.findLoginByEmail(email);
        if (staffOpt.isPresent()) {
            StaffLoginData staff = staffOpt.get();
            if (!staff.isActive()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Staff account is deactivated.");
            }
            boolean passwordMatches;
            try {
                passwordMatches = passwordEncoder.matches(request.password(), staff.passwordHash());
            } catch (IllegalArgumentException exception) {
                passwordMatches = false;
            }
            if (!passwordMatches) {
                throw invalidCredentials();
            }
            StaffProfile sp = staffRepository.findProfileById(staff.staffId())
                    .orElseThrow(AuthService::invalidCredentials);
            StaffRole role = StaffRole.fromCode(sp.staffRole());
            return PassengerProfile.forStaff(
                    sp.staffId(),
                    sp.fullName(),
                    sp.email(),
                    role.getCode(),
                    role.getTitle(),
                    role.getAuthorityNames(),
                    sp.createdAt()
            );
        }

        throw invalidCredentials();
    }

    /**
     * Retrieves a user's public profile after authentication.
     * Does not expose the stored password hash.
     */
    @Transactional(readOnly = true)
    public PassengerProfile getProfileByEmail(String email) {
        String normalizedEmail = PassengerUserDetailsService.resolveEmailAlias(email);

        var passengerOpt = passengerRepository.findProfileByEmail(normalizedEmail);
        if (passengerOpt.isPresent()) {
            return passengerOpt.get();
        }

        var staffOpt = staffRepository.findProfileByEmail(normalizedEmail);
        if (staffOpt.isPresent()) {
            StaffProfile sp = staffOpt.get();
            StaffRole role = StaffRole.fromCode(sp.staffRole());
            return PassengerProfile.forStaff(
                    sp.staffId(),
                    sp.fullName(),
                    sp.email(),
                    role.getCode(),
                    role.getTitle(),
                    role.getAuthorityNames(),
                    sp.createdAt()
            );
        }

        throw invalidCredentials();
    }

    private static ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid email or password."
        );
    }
}
