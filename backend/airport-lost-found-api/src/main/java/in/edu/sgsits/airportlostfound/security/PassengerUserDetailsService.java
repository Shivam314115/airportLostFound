package in.edu.sgsits.airportlostfound.security;

import java.util.Locale;

import in.edu.sgsits.airportlostfound.passenger.PassengerLoginData;
import in.edu.sgsits.airportlostfound.passenger.PassengerRepository;
import in.edu.sgsits.airportlostfound.staff.StaffLoginData;
import in.edu.sgsits.airportlostfound.staff.StaffRepository;
import in.edu.sgsits.airportlostfound.staff.StaffRole;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PassengerUserDetailsService implements UserDetailsService {

    private final PassengerRepository passengerRepository;
    private final StaffRepository staffRepository;

    public PassengerUserDetailsService(
            PassengerRepository passengerRepository,
            StaffRepository staffRepository) {
        this.passengerRepository = passengerRepository;
        this.staffRepository = staffRepository;
    }

    public static String resolveEmailAlias(String email) {
        if (email == null) return "";
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "agent@airport.test" -> "aditi.agent@airport.test";
            case "claims@airport.test" -> "vikram.claims@airport.test";
            case "inventory@airport.test" -> "pooja.inventory@airport.test";
            case "dispatch@airport.test" -> "rahul.dispatch@airport.test";
            case "supervisor@airport.test" -> "sunita.supervisor@airport.test";
            case "security@airport.test" -> "karan.security@airport.test";
            case "passenger@airport.test" -> "riya.active@example.test";
            default -> normalized;
        };
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        String normalizedEmail = resolveEmailAlias(email);

        var passengerOpt = passengerRepository.findLoginDataByEmail(normalizedEmail);
        if (passengerOpt.isPresent()) {
            PassengerLoginData passenger = passengerOpt.get();
            return User.withUsername(passenger.email())
                    .password(passenger.passwordHash())
                    .authorities("ROLE_PASSENGER")
                    .build();
        }

        var staffOpt = staffRepository.findLoginByEmail(normalizedEmail);
        if (staffOpt.isPresent()) {
            StaffLoginData staff = staffOpt.get();
            if (!staff.isActive()) {
                throw new UsernameNotFoundException("Staff account is deactivated.");
            }
            StaffRole role = StaffRole.fromCode(staff.staffRole());
            return User.withUsername(staff.email())
                    .password(staff.passwordHash())
                    .authorities(role.getAuthorities())
                    .build();
        }

        throw new UsernameNotFoundException("Invalid email or password.");
    }
}
