package in.edu.sgsits.airportlostfound.itemphoto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ItemPhotoRequest(
        @NotBlank(message = "photoUrl is required")
        String photoUrl,

        @Size(max = 250, message = "caption cannot exceed 250 characters")
        String caption,

        String photoType
) {
    public ItemPhotoRequest(String photoUrl) {
        this(photoUrl, null, "overview");
    }

    public ItemPhotoRequest(String photoUrl, String caption) {
        this(photoUrl, caption, "overview");
    }
}
