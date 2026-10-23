package in.edu.sgsits.airportlostfound.staff;

import java.time.OffsetDateTime;

public record StaffLoginData(
        long staffId,
        String fullName,
        String email,
        String passwordHash,
        String staffRole,
        boolean isActive
) {
}
