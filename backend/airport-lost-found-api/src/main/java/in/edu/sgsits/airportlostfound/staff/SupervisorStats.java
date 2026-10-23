package in.edu.sgsits.airportlostfound.staff;

public record SupervisorStats(
        long totalFoundItems,
        long awaitingReviewCount,
        long storedCount,
        long claimedCount,
        long returnedCount,
        long totalLostReports,
        long pendingClaimsCount,
        long approvedClaimsCount,
        long totalStorageSlots,
        long occupiedStorageSlots,
        long activeStaffCount,
        long totalAuditsCount
) {
}
