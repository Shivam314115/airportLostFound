package in.edu.sgsits.airportlostfound.common;

import java.util.Map;

public record ApiError(
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {
    public ApiError {
        fieldErrors = fieldErrors == null ? Map.of() : Map.copyOf(fieldErrors);
    }
}
