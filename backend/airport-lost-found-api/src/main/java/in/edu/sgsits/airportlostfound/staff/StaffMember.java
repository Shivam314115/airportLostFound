package in.edu.sgsits.airportlostfound.staff;

import java.time.OffsetDateTime;

public record StaffMember(
        long staffId,
        String fullName,
        String email,
        String staffRole,
        String staffRoleTitle,
        boolean isActive,
        OffsetDateTime createdAt
) {
}
