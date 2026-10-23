package in.edu.sgsits.airportlostfound.staff;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record StaffClaimItem(
        long claimId,
        long matchId,
        long lostReportId,
        long foundItemId,
        String passengerName,
        String passengerEmail,
        String passengerPhone,
        String lostItemName,
        String foundItemName,
        BigDecimal matchScore,
        String matchStatus,
        String claimStatus,
        OffsetDateTime submittedAt,
        String claimReviewedBy,
        OffsetDateTime claimReviewedAt,
        String decisionNotes,
        String verificationMethod,
        String verificationResult,
        OffsetDateTime verifiedAt,
        String returnMethod,
        String returnStatus
) {
}
