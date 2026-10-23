package in.edu.sgsits.airportlostfound.staff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ReviewClaimRequest(
        @NotBlank(message = "Decision is required.")
        @Pattern(regexp = "^(approved|rejected|under_review)$", message = "Decision must be approved, rejected, or under_review.")
        String decision,

        String verificationMethod,

        String notes
) {
}
