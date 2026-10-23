package in.edu.sgsits.airportlostfound.itemphoto;

import java.time.OffsetDateTime;

/**
 * Entity representing an attached photograph for an item or recovered property.
 * Dependent entity for FoundItem and Item, keyed by (item_id, photo_number) or (found_item_id, photo_number).
 */
public record ItemPhoto(
        long itemId,
        Long foundItemId,
        int photoNumber,
        String photoUrl,
        String caption,
        String photoType,
        OffsetDateTime uploadedAt
) {
}
