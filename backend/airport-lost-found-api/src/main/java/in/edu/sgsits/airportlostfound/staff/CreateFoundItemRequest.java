package in.edu.sgsits.airportlostfound.staff;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import in.edu.sgsits.airportlostfound.itemphoto.ItemPhotoRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFoundItemRequest(
        @NotNull(message = "Category ID is required.")
        Long categoryId,

        @NotBlank(message = "Item name is required.")
        @Size(max = 120, message = "Item name must not exceed 120 characters.")
        String itemName,

        @Size(max = 80, message = "Brand must not exceed 80 characters.")
        String brand,

        @Size(max = 40, message = "Colour must not exceed 40 characters.")
        String colour,

        String description,

        BigDecimal estimatedValue,

        String itemCondition,

        Long flightId,

        Long foundLocationId,

        @NotNull(message = "Found timestamp is required.")
        OffsetDateTime foundAt,

        List<ItemPhotoRequest> photos
) {
    public CreateFoundItemRequest(
            Long categoryId,
            String itemName,
            String brand,
            String colour,
            String description,
            BigDecimal estimatedValue,
            String itemCondition,
            Long flightId,
            Long foundLocationId,
            OffsetDateTime foundAt
    ) {
        this(categoryId, itemName, brand, colour, description, estimatedValue, itemCondition,
                flightId, foundLocationId, foundAt, List.of());
    }
}
