package in.edu.sgsits.airportlostfound.passenger;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Input DTO for passenger registration.
 *
 * Passwords must never be logged or returned in API responses.
 */
public record RegisterPassengerRequest(

        @NotBlank(message = "fullName is required")
        @Size(max = 150, message = "fullName cannot exceed 150 characters")
        String fullName,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid email address")
        @Size(max = 254, message = "email cannot exceed 254 characters")
        String email,

        @Size(max = 20, message = "phone cannot exceed 20 characters")
        String phone,

        @NotBlank(message = "password is required")
        @Size(
                min = 8,
                max = 72,
                message = "password must contain between 8 and 72 characters"
        )
        String password

) {
    public RegisterPassengerRequest {
        fullName = fullName != null ? fullName.trim() : null;
        email = email != null ? email.trim().toLowerCase(java.util.Locale.ROOT) : null;
        phone = (phone != null && !phone.trim().isEmpty()) ? phone.trim() : null;
    }

    /**
     * Prevent accidental password exposure if this request is logged.
     */
    @Override
    public String toString() {
        return "RegisterPassengerRequest[" +
                "fullName=" + fullName +
                ", email=" + email +
                ", phone=" + phone +
                ", password=[REDACTED]]";
    }
}
