package in.edu.sgsits.airportlostfound.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 254, message = "Email is too long")
        String email,

        @NotBlank(message = "Password is required")
        @Size(max = 256, message = "Password input is too long")
        String password

) {
    /**
     * Avoid exposing the supplied password if this record is logged.
     */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email
                + ", password=[REDACTED]]";
    }
}
