package in.edu.sgsits.airportlostfound.staff;

import java.time.OffsetDateTime;
import java.util.List;

public record StaffProfile(
        long staffId,
        String fullName,
        String email,
        String staffRole,
        String staffRoleTitle,
        String staffRoleDescription,
        boolean isActive,
        List<String> authorities,
        OffsetDateTime createdAt
) {
}
