package in.edu.sgsits.airportlostfound.passenger;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Public passenger / staff details returned by the API.
 *
 * Intentionally excludes password_hash.
 */
public record PassengerProfile(
        long passengerId,
        String fullName,
        String email,
        String phone,
        OffsetDateTime createdAt,
        String userType,
        String staffRole,
        String staffRoleTitle,
        Long staffId,
        List<String> authorities
) {
    public PassengerProfile(
            long passengerId,
            String fullName,
            String email,
            String phone,
            OffsetDateTime createdAt) {
        this(passengerId, fullName, email, phone, createdAt, "passenger", null, null, null, List.of("ROLE_PASSENGER"));
    }

    public static PassengerProfile forPassenger(
            long passengerId,
            String fullName,
            String email,
            String phone,
            OffsetDateTime createdAt) {
        return new PassengerProfile(passengerId, fullName, email, phone, createdAt, "passenger", null, null, null, List.of("ROLE_PASSENGER"));
    }

    public static PassengerProfile forStaff(
            long staffId,
            String fullName,
            String email,
            String staffRole,
            String staffRoleTitle,
            List<String> authorities,
            OffsetDateTime createdAt) {
        return new PassengerProfile(0L, fullName, email, null, createdAt, "staff", staffRole, staffRoleTitle, staffId, authorities);
    }
}
