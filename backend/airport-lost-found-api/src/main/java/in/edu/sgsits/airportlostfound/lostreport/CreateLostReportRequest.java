package in.edu.sgsits.airportlostfound.lostreport;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import in.edu.sgsits.airportlostfound.itemphoto.ItemPhotoRequest;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreateLostReportRequest(

        @NotNull(message = "categoryId is required")
        @Positive(message = "categoryId must be positive")
        Long categoryId,

        @NotBlank(message = "itemName is required")
        @Size(max = 150, message = "itemName cannot exceed 150 characters")
        String itemName,

        @Size(max = 100, message = "brand cannot exceed 100 characters")
        String brand,

        @Size(max = 60, message = "colour cannot exceed 60 characters")
        String colour,

        @Size(max = 4000, message = "description cannot exceed 4000 characters")
        String description,

        @DecimalMin(value = "0.00", message = "estimatedValue cannot be negative")
        @Digits(
                integer = 10,
                fraction = 2,
                message = "estimatedValue supports up to 10 integer digits and 2 decimal places"
        )
        BigDecimal estimatedValue,

        @Pattern(
                regexp = "(?i)(new|good|fair|poor|damaged|unknown)",
                message = "itemCondition must be new, good, fair, poor, damaged, or unknown"
        )
        String itemCondition,

        @NotNull(message = "lostAt is required")
        OffsetDateTime lostAt,

        @Positive(message = "flightId must be positive")
        Long flightId,

        @Positive(message = "lastSeenLocationId must be positive")
        Long lastSeenLocationId,

        @Size(max = 4000, message = "additionalDetails cannot exceed 4000 characters")
        String additionalDetails,

        List<ItemPhotoRequest> photos

) {
    public CreateLostReportRequest(
            Long categoryId,
            String itemName,
            String brand,
            String colour,
            String description,
            BigDecimal estimatedValue,
            String itemCondition,
            OffsetDateTime lostAt,
            Long flightId,
            Long lastSeenLocationId,
            String additionalDetails
    ) {
        this(categoryId, itemName, brand, colour, description, estimatedValue, itemCondition,
                lostAt, flightId, lastSeenLocationId, additionalDetails, List.of());
    }
}
