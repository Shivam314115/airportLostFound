package in.edu.sgsits.airportlostfound.staff;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record StaffFoundItem(
        long foundItemId,
        long itemId,
        String itemName,
        long categoryId,
        String categoryName,
        String brand,
        String colour,
        String description,
        BigDecimal estimatedValue,
        String itemCondition,
        String itemStatus,
        Long foundLocationId,
        String foundLocationName,
        Long flightId,
        String flightNumber,
        OffsetDateTime foundAt,
        OffsetDateTime receivedAt,
        long foundByStaffId,
        String foundByStaffName,
        String storageName,
        Integer slotNumber,
        List<String> photoUrls,
        Integer daysInStorage
) {
    public StaffFoundItem(
            long foundItemId,
            long itemId,
            String itemName,
            long categoryId,
            String categoryName,
            String brand,
            String colour,
            String description,
            BigDecimal estimatedValue,
            String itemCondition,
            String itemStatus,
            Long foundLocationId,
            String foundLocationName,
            Long flightId,
            String flightNumber,
            OffsetDateTime foundAt,
            OffsetDateTime receivedAt,
            long foundByStaffId,
            String foundByStaffName,
            String storageName,
            Integer slotNumber
    ) {
        this(foundItemId, itemId, itemName, categoryId, categoryName, brand, colour, description,
                estimatedValue, itemCondition, itemStatus, foundLocationId, foundLocationName,
                flightId, flightNumber, foundAt, receivedAt, foundByStaffId, foundByStaffName,
                storageName, slotNumber, List.of(), 0);
    }
}
