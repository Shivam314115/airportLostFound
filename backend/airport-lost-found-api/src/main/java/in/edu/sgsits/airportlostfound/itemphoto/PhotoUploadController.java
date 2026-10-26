package in.edu.sgsits.airportlostfound.itemphoto;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/photos")
public class PhotoUploadController {

    private final Path uploadDir;

    public PhotoUploadController() {
        this.uploadDir = Paths.get("uploads", "photos").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize photo upload directory", e);
        }
    }

    /**
     * Upload an image file via multipart/form-data.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadMultipart(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption,
            @RequestParam(value = "photoType", required = false, defaultValue = "overview") String photoType) {

        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is empty.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        if (!isValidImageExtension(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only JPEG, PNG, WebP and GIF images are supported.");
        }

        String filename = UUID.randomUUID().toString() + extension;
        Path targetLocation = this.uploadDir.resolve(filename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store file.", ex);
        }

        String photoUrl = "/api/photos/" + filename;
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "photoUrl", photoUrl,
                "caption", caption != null ? caption : "",
                "photoType", photoType != null ? photoType : "overview",
                "filename", filename
        ));
    }

    /**
     * Upload an image via JSON with Base64 dataUrl or direct image URL.
     */
    @PostMapping(value = {"/upload", "/upload-base64"}, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> uploadJson(@RequestBody Map<String, String> payload) {
        String dataUrl = payload.get("dataUrl");
        if (dataUrl == null || dataUrl.isBlank()) {
            dataUrl = payload.get("base64Data");
        }
        String photoUrl = payload.get("photoUrl");
        String caption = payload.getOrDefault("caption", "");
        String photoType = payload.getOrDefault("photoType", "overview");
        String clientFilename = payload.get("filename");

        if (photoUrl != null && !photoUrl.isBlank()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "photoUrl", photoUrl,
                    "caption", caption,
                    "photoType", photoType
            ));
        }

        if (dataUrl == null || dataUrl.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Either dataUrl, base64Data or photoUrl is required.");
        }

        try {
            // Parse data URL format: data:image/png;base64,iVBORw... or raw base64 string
            String base64Data = dataUrl;
            String extension = ".png";

            if (dataUrl.contains(",")) {
                String[] parts = dataUrl.split(",", 2);
                String metadata = parts[0];
                base64Data = parts[1];

                if (metadata.contains("image/jpeg") || metadata.contains("image/jpg")) {
                    extension = ".jpg";
                } else if (metadata.contains("image/webp")) {
                    extension = ".webp";
                } else if (metadata.contains("image/gif")) {
                    extension = ".gif";
                }
            } else if (clientFilename != null && clientFilename.contains(".")) {
                String ext = getExtension(clientFilename);
                if (isValidImageExtension(ext)) {
                    extension = ext;
                }
            }

            byte[] decoded = Base64.getDecoder().decode(base64Data);
            String filename = UUID.randomUUID().toString() + extension;
            Path targetLocation = this.uploadDir.resolve(filename);
            Files.write(targetLocation, decoded);

            String savedUrl = "/api/photos/" + filename;
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "photoUrl", savedUrl,
                    "caption", caption,
                    "photoType", photoType,
                    "filename", filename
            ));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to parse and store base64 image data.", ex);
        }
    }

    /**
     * Serve uploaded images publicly.
     */
    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getPhoto(@PathVariable String filename) {
        try {
            Path filePath = this.uploadDir.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found: " + filename);
            }

            String contentType = determineContentType(filename);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                    .body(resource);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Photo not found: " + filename);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    private boolean isValidImageExtension(String ext) {
        return ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png")
                || ext.equals(".webp") || ext.equals(".gif");
    }

    private String determineContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        return "image/jpeg";
    }
}
