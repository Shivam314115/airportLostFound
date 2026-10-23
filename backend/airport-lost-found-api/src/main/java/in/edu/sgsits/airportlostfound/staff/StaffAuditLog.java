package in.edu.sgsits.airportlostfound.staff;

import java.time.OffsetDateTime;

public record StaffAuditLog(
        long auditId,
        Long actorStaffId,
        String actorStaffName,
        String actionType,
        String entityName,
        Long entityId,
        String details,
        OffsetDateTime occurredAt
) {
}
