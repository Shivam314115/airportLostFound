package in.edu.sgsits.airportlostfound.staff;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateStaffRequest(
        @NotBlank(message = "Full name is required.")
        @Size(max = 150, message = "Full name must not exceed 150 characters.")
        String fullName,

        @NotBlank(message = "Email is required.")
        @Email(message = "Must be a valid email address.")
        @Size(max = 254, message = "Email must not exceed 254 characters.")
        String email,

        @NotBlank(message = "Password is required.")
        @Size(min = 6, message = "Password must be at least 6 characters.")
        String password,

        @NotBlank(message = "Staff role is required.")
        @Pattern(
                regexp = "^(lost_found_agent|claims_verification_officer|inventory_storage_officer|return_dispatch_officer|supervisor|ground_staff|system_admin|lost_found_officer|security_officer|administrator)$",
                message = "Invalid staff role specified."
        )
        String staffRole
) {
}
