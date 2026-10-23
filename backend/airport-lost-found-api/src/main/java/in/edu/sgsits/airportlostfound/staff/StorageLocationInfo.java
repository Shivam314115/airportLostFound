package in.edu.sgsits.airportlostfound.staff;

import java.util.List;

public record StorageLocationInfo(
        long storageId,
        String storageName,
        String locationName,
        String description,
        int capacity,
        int availableSlots,
        int occupiedSlots,
        List<StorageSlotInfo> slots
) {
    public record StorageSlotInfo(
            long storageId,
            int slotNumber,
            String slotStatus,
            Long assignedFoundItemId,
            String assignedItemName
    ) {}
}
