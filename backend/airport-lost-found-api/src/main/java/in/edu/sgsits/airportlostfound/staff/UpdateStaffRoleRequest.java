package in.edu.sgsits.airportlostfound.staff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateStaffRoleRequest(
        @NotBlank(message = "Staff role is required.")
        @Pattern(
                regexp = "^(lost_found_agent|claims_verification_officer|inventory_storage_officer|return_dispatch_officer|supervisor|ground_staff|system_admin|lost_found_officer|security_officer|administrator)$",
                message = "Invalid staff role specified."
        )
        String staffRole
) {
}
