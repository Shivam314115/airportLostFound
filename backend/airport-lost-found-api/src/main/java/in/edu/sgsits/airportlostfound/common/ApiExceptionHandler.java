package in.edu.sgsits.airportlostfound.common;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception) {

        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError fieldError :
                exception.getBindingResult().getFieldErrors()) {

            String message = fieldError.getDefaultMessage() == null
                    ? "Invalid value"
                    : fieldError.getDefaultMessage();

            fieldErrors.putIfAbsent(fieldError.getField(), message);
        }

        return response(
                HttpStatus.BAD_REQUEST,
                "Request validation failed.",
                fieldErrors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableRequest(
            HttpMessageNotReadableException exception) {

        return response(
                HttpStatus.BAD_REQUEST,
                "The request body is malformed or contains invalid field values.",
                Map.of()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleParameterTypeMismatch(
            MethodArgumentTypeMismatchException exception) {

        return response(
                HttpStatus.BAD_REQUEST,
                "Query parameter '" + exception.getName()
                        + "' has an invalid value.",
                Map.of()
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleExpectedHttpError(
            ResponseStatusException exception) {

        HttpStatusCode statusCode = exception.getStatusCode();
        HttpStatus knownStatus = HttpStatus.resolve(statusCode.value());

        String error = knownStatus == null
                ? "Request Error"
                : knownStatus.getReasonPhrase();

        String message = exception.getReason() == null
                ? "The request could not be completed."
                : exception.getReason();

        return ResponseEntity.status(statusCode).body(
                new ApiError(
                        statusCode.value(),
                        error,
                        message,
                        Map.of()
                )
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(
            DataIntegrityViolationException exception) {

        Throwable cause =
                NestedExceptionUtils.getMostSpecificCause(exception);

        String sqlState = cause instanceof SQLException sqlException
                ? sqlException.getSQLState()
                : null;

        if ("23503".equals(sqlState)) {
            return response(
                    HttpStatus.BAD_REQUEST,
                    "A referenced passenger, category, flight, or location does not exist.",
                    Map.of()
            );
        }

        if ("23505".equals(sqlState)) {
            return response(
                    HttpStatus.CONFLICT,
                    "A record with a unique value already exists.",
                    Map.of()
            );
        }

        if ("23502".equals(sqlState) || "23514".equals(sqlState)) {
            return response(
                    HttpStatus.BAD_REQUEST,
                    "The request violates a required database constraint.",
                    Map.of()
            );
        }

        return response(
                HttpStatus.CONFLICT,
                "The request conflicts with a database integrity rule.",
                Map.of()
        );
    }

    private static ResponseEntity<ApiError> response(
            HttpStatus status,
            String message,
            Map<String, String> fieldErrors) {

        return ResponseEntity.status(status).body(
                new ApiError(
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        fieldErrors
                )
        );
    }
}
