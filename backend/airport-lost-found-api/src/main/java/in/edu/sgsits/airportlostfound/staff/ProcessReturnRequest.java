package in.edu.sgsits.airportlostfound.staff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ProcessReturnRequest(
        @NotNull(message = "Claim ID is required.")
        Long claimId,

        @NotBlank(message = "Return method is required.")
        @Pattern(regexp = "^(counter_handover|delivery)$", message = "Return method must be counter_handover or delivery.")
        String returnMethod,

        @NotBlank(message = "Return status is required.")
        @Pattern(regexp = "^(initiated|in_transit|completed|failed|cancelled)$", message = "Invalid return status.")
        String returnStatus,

        String deliveryNotes
) {
}
