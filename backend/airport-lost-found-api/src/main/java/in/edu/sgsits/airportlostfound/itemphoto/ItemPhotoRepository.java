package in.edu.sgsits.airportlostfound.itemphoto;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ItemPhotoRepository {

    private final JdbcTemplate jdbcTemplate;

    public ItemPhotoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int getNextPhotoNumber(long itemId) {
        String sql = "SELECT COALESCE(MAX(photo_number), 0) + 1 FROM app.item_photo WHERE item_id = ?";
        Integer nextNum = jdbcTemplate.queryForObject(sql, Integer.class, itemId);
        return nextNum != null ? nextNum : 1;
    }

    public void savePhoto(
            long itemId,
            Long foundItemId,
            int photoNumber,
            String photoUrl,
            String caption,
            String photoType
    ) {
        String sql = """
                INSERT INTO app.item_photo (
                    item_id,
                    found_item_id,
                    photo_number,
                    photo_url,
                    caption,
                    photo_type,
                    uploaded_at
                )
                VALUES (?, ?, ?, ?, ?, COALESCE(?, 'overview'), CURRENT_TIMESTAMP)
                ON CONFLICT (item_id, photo_number) DO UPDATE
                SET photo_url = EXCLUDED.photo_url,
                    caption = EXCLUDED.caption,
                    photo_type = EXCLUDED.photo_type,
                    found_item_id = COALESCE(EXCLUDED.found_item_id, app.item_photo.found_item_id)
                """;

        jdbcTemplate.update(
                sql,
                itemId,
                foundItemId,
                photoNumber,
                photoUrl,
                caption,
                photoType != null ? photoType : "overview"
        );
    }

    public List<ItemPhoto> findByItemId(long itemId) {
        String sql = """
                SELECT item_id, found_item_id, photo_number, photo_url, caption, photo_type, uploaded_at
                FROM app.item_photo
                WHERE item_id = ?
                ORDER BY photo_number ASC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new ItemPhoto(
                rs.getLong("item_id"),
                rs.getObject("found_item_id", Long.class),
                rs.getInt("photo_number"),
                rs.getString("photo_url"),
                rs.getString("caption"),
                rs.getString("photo_type"),
                rs.getObject("uploaded_at", OffsetDateTime.class)
        ), itemId);
    }

    public List<ItemPhoto> findByFoundItemId(long foundItemId) {
        String sql = """
                SELECT p.item_id, p.found_item_id, p.photo_number, p.photo_url, p.caption, p.photo_type, p.uploaded_at
                FROM app.item_photo p
                JOIN app.found_item fi ON (p.found_item_id = fi.found_item_id OR p.item_id = fi.item_id)
                WHERE fi.found_item_id = ?
                ORDER BY p.photo_number ASC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new ItemPhoto(
                rs.getLong("item_id"),
                rs.getObject("found_item_id", Long.class),
                rs.getInt("photo_number"),
                rs.getString("photo_url"),
                rs.getString("caption"),
                rs.getString("photo_type"),
                rs.getObject("uploaded_at", OffsetDateTime.class)
        ), foundItemId);
    }

    public List<String> findPhotoUrlsByItemId(long itemId) {
        String sql = "SELECT photo_url FROM app.item_photo WHERE item_id = ? ORDER BY photo_number ASC";
        return jdbcTemplate.queryForList(sql, String.class, itemId);
    }

    public List<String> findPhotoUrlsByFoundItemId(long foundItemId) {
        String sql = """
                SELECT p.photo_url
                FROM app.item_photo p
                JOIN app.found_item fi ON (p.found_item_id = fi.found_item_id OR p.item_id = fi.item_id)
                WHERE fi.found_item_id = ?
                ORDER BY p.photo_number ASC
                """;
        return jdbcTemplate.queryForList(sql, String.class, foundItemId);
    }
}
