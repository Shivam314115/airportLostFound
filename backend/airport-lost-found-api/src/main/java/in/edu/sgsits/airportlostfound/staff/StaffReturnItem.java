package in.edu.sgsits.airportlostfound.staff;

import java.time.OffsetDateTime;

public record StaffReturnItem(
        long returnId,
        long claimId,
        long foundItemId,
        String passengerName,
        String passengerEmail,
        String passengerPhone,
        String itemName,
        String returnMethod,
        String returnStatus,
        long handledByStaffId,
        String handledByStaffName,
        String deliveryNotes,
        OffsetDateTime createdAt,
        OffsetDateTime completedAt
) {
}
