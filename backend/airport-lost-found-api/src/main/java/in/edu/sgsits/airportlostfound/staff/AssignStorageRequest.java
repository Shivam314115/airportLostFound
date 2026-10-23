package in.edu.sgsits.airportlostfound.staff;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AssignStorageRequest(
        @NotNull(message = "Found item ID is required.")
        Long foundItemId,

        @NotNull(message = "Storage ID is required.")
        Long storageId,

        @NotNull(message = "Slot number is required.")
        @Min(value = 1, message = "Slot number must be at least 1.")
        Integer slotNumber
) {
}
