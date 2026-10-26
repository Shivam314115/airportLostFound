package in.edu.sgsits.airportlostfound.itemphoto;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ItemPhotoController {

    private final ItemPhotoRepository itemPhotoRepository;

    public ItemPhotoController(ItemPhotoRepository itemPhotoRepository) {
        this.itemPhotoRepository = itemPhotoRepository;
    }

    @GetMapping("/items/{itemId}/photos")
    public List<ItemPhoto> getPhotosByItemId(@PathVariable long itemId) {
        return itemPhotoRepository.findByItemId(itemId);
    }

    @PostMapping("/items/{itemId}/photos")
    public ResponseEntity<ItemPhoto> addPhotoToItem(
            @PathVariable long itemId,
            @Valid @RequestBody ItemPhotoRequest request) {

        int nextNumber = itemPhotoRepository.getNextPhotoNumber(itemId);
        itemPhotoRepository.savePhoto(
                itemId,
                null,
                nextNumber,
                request.photoUrl(),
                request.caption(),
                request.photoType()
        );

        List<ItemPhoto> photos = itemPhotoRepository.findByItemId(itemId);
        ItemPhoto saved = photos.stream()
                .filter(p -> p.photoNumber() == nextNumber)
                .findFirst()
                .orElse(new ItemPhoto(itemId, null, nextNumber, request.photoUrl(), request.caption(), request.photoType(), null));

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/staff/found-items/{foundItemId}/photos")
    public List<ItemPhoto> getPhotosByFoundItemId(@PathVariable long foundItemId) {
        return itemPhotoRepository.findByFoundItemId(foundItemId);
    }

    @PostMapping("/staff/found-items/{foundItemId}/photos")
    public ResponseEntity<ItemPhoto> addPhotoToFoundItem(
            @PathVariable long foundItemId,
            @Valid @RequestBody ItemPhotoRequest request) {

        // Get itemId for this foundItem
        long itemId = itemPhotoRepository.findItemIdByFoundItemId(foundItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Found item not found."));
        int nextNumber = itemPhotoRepository.getNextPhotoNumber(itemId);

        itemPhotoRepository.savePhoto(
                itemId,
                foundItemId,
                nextNumber,
                request.photoUrl(),
                request.caption(),
                request.photoType()
        );

        List<ItemPhoto> photos = itemPhotoRepository.findByFoundItemId(foundItemId);
        ItemPhoto saved = photos.stream()
                .filter(p -> p.photoNumber() == nextNumber)
                .findFirst()
                .orElse(new ItemPhoto(itemId, foundItemId, nextNumber, request.photoUrl(), request.caption(), request.photoType(), null));

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
