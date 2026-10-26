package in.edu.sgsits.airportlostfound.staff;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class StaffRepository {

    private final JdbcTemplate jdbcTemplate;

    public StaffRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<StaffLoginData> findLoginByEmail(String email) {
        String sql = """
                SELECT staff_id, full_name, email, password_hash, staff_role, is_active
                FROM app.staff
                WHERE email = ?
                """;
        List<StaffLoginData> list = jdbcTemplate.query(sql, (rs, rowNum) -> new StaffLoginData(
                rs.getLong("staff_id"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("staff_role"),
                rs.getBoolean("is_active")
        ), email);
        return list.stream().findFirst();
    }

    public Optional<StaffProfile> findProfileById(long staffId) {
        String sql = """
                SELECT staff_id, full_name, email, staff_role, is_active, created_at
                FROM app.staff
                WHERE staff_id = ?
                """;
        List<StaffProfile> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            String roleCode = rs.getString("staff_role");
            StaffRole role = StaffRole.fromCode(roleCode);
            return new StaffProfile(
                    rs.getLong("staff_id"),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    role.getCode(),
                    role.getTitle(),
                    role.getDescription(),
                    rs.getBoolean("is_active"),
                    role.getAuthorityNames(),
                    rs.getObject("created_at", OffsetDateTime.class)
            );
        }, staffId);
        return list.stream().findFirst();
    }

    public Optional<StaffProfile> findProfileByEmail(String email) {
        String sql = """
                SELECT staff_id, full_name, email, staff_role, is_active, created_at
                FROM app.staff
                WHERE email = ?
                """;
        List<StaffProfile> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            String roleCode = rs.getString("staff_role");
            StaffRole role = StaffRole.fromCode(roleCode);
            return new StaffProfile(
                    rs.getLong("staff_id"),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    role.getCode(),
                    role.getTitle(),
                    role.getDescription(),
                    rs.getBoolean("is_active"),
                    role.getAuthorityNames(),
                    rs.getObject("created_at", OffsetDateTime.class)
            );
        }, email);
        return list.stream().findFirst();
    }

    public List<StaffMember> findAllStaff() {
        String sql = """
                SELECT staff_id, full_name, email, staff_role, is_active, created_at
                FROM app.staff
                ORDER BY staff_id ASC
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            String roleCode = rs.getString("staff_role");
            StaffRole role = StaffRole.fromCode(roleCode);
            return new StaffMember(
                    rs.getLong("staff_id"),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    role.getCode(),
                    role.getTitle(),
                    rs.getBoolean("is_active"),
                    rs.getObject("created_at", OffsetDateTime.class)
            );
        });
    }

    public StaffMember createStaff(String fullName, String email, String passwordHash, String staffRole) {
        String sql = """
                INSERT INTO app.staff (full_name, email, password_hash, staff_role, is_active)
                VALUES (?, ?, ?, ?, true)
                RETURNING staff_id, full_name, email, staff_role, is_active, created_at
                """;
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            String roleCode = rs.getString("staff_role");
            StaffRole role = StaffRole.fromCode(roleCode);
            return new StaffMember(
                    rs.getLong("staff_id"),
                    rs.getString("full_name"),
                    rs.getString("email"),
                    role.getCode(),
                    role.getTitle(),
                    rs.getBoolean("is_active"),
                    rs.getObject("created_at", OffsetDateTime.class)
            );
        }, fullName, email, passwordHash, staffRole);
    }

    public void updateStaffStatus(long staffId, boolean isActive) {
        String sql = "UPDATE app.staff SET is_active = ? WHERE staff_id = ?";
        jdbcTemplate.update(sql, isActive, staffId);
    }

    public void updateStaffRole(long staffId, String staffRole) {
        String sql = "UPDATE app.staff SET staff_role = ? WHERE staff_id = ?";
        jdbcTemplate.update(sql, staffRole, staffId);
    }

    // -------------------------------------------------------------
    // Found Items Management
    // -------------------------------------------------------------
    public List<StaffFoundItem> findFoundItems(String status, Long staffId, String search) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    f.found_item_id,
                    f.item_id,
                    i.item_name,
                    i.category_id,
                    cat.category_name,
                    i.brand,
                    i.colour,
                    i.description,
                    i.estimated_value,
                    i.item_condition,
                    f.item_status,
                    f.found_location_id,
                    loc.location_name AS found_location_name,
                    f.flight_id,
                    fl.flight_number,
                    f.found_at,
                    f.received_at,
                    f.found_by_staff_id,
                    s.full_name AS found_by_staff_name,
                    sloc.storage_name,
                    sa.slot_number,
                    COALESCE((SELECT ARRAY_AGG(p.photo_url ORDER BY p.photo_number) FROM app.item_photo p WHERE p.found_item_id = f.found_item_id OR p.item_id = f.item_id), ARRAY[]::TEXT[]) AS photo_urls,
                    COALESCE(EXTRACT(DAY FROM (CURRENT_TIMESTAMP - f.stored_at))::INT, 0) AS days_in_storage
                FROM app.found_item f
                JOIN app.item i ON i.item_id = f.item_id
                JOIN app.item_category cat ON cat.category_id = i.category_id
                JOIN app.staff s ON s.staff_id = f.found_by_staff_id
                LEFT JOIN app.airport_location loc ON loc.location_id = f.found_location_id
                LEFT JOIN app.flight fl ON fl.flight_id = f.flight_id
                LEFT JOIN app.storage_assignment sa ON sa.found_item_id = f.found_item_id AND sa.released_at IS NULL
                LEFT JOIN app.storage_location sloc ON sloc.storage_id = sa.storage_id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();

        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("all")) {
            sql.append(" AND f.item_status = ? ");
            params.add(status.trim().toLowerCase());
        }

        if (staffId != null) {
            sql.append(" AND f.found_by_staff_id = ? ");
            params.add(staffId);
        }

        if (search != null && !search.isBlank()) {
            sql.append(" AND (i.item_name ILIKE ? OR i.brand ILIKE ? OR i.description ILIKE ? OR loc.location_name ILIKE ?) ");
            String term = "%" + search.trim() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }

        sql.append(" ORDER BY f.found_at DESC, f.found_item_id DESC ");

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
            List<String> photoUrls = List.of();
            try {
                java.sql.Array arr = rs.getArray("photo_urls");
                if (arr != null) {
                    String[] items = (String[]) arr.getArray();
                    if (items != null) photoUrls = List.of(items);
                }
            } catch (Exception ignored) {}

            return new StaffFoundItem(
                    rs.getLong("found_item_id"),
                    rs.getLong("item_id"),
                    rs.getString("item_name"),
                    rs.getLong("category_id"),
                    rs.getString("category_name"),
                    rs.getString("brand"),
                    rs.getString("colour"),
                    rs.getString("description"),
                    rs.getBigDecimal("estimated_value"),
                    rs.getString("item_condition"),
                    rs.getString("item_status"),
                    rs.getObject("found_location_id", Long.class),
                    rs.getString("found_location_name"),
                    rs.getObject("flight_id", Long.class),
                    rs.getString("flight_number"),
                    rs.getObject("found_at", OffsetDateTime.class),
                    rs.getObject("received_at", OffsetDateTime.class),
                    rs.getLong("found_by_staff_id"),
                    rs.getString("found_by_staff_name"),
                    rs.getString("storage_name"),
                    rs.getObject("slot_number", Integer.class),
                    photoUrls,
                    rs.getInt("days_in_storage")
            );
        });
    }

    public long registerFoundItem(
            long categoryId,
            String itemName,
            String brand,
            String colour,
            String description,
            BigDecimal estimatedValue,
            String itemCondition,
            Long flightId,
            Long foundLocationId,
            OffsetDateTime foundAt,
            long foundByStaffId
    ) {
        return registerFoundItem(categoryId, itemName, brand, colour, description, estimatedValue,
                itemCondition, flightId, foundLocationId, foundAt, foundByStaffId, List.of());
    }

    public long registerFoundItem(
            long categoryId,
            String itemName,
            String brand,
            String colour,
            String description,
            BigDecimal estimatedValue,
            String itemCondition,
            Long flightId,
            Long foundLocationId,
            OffsetDateTime foundAt,
            long foundByStaffId,
            List<in.edu.sgsits.airportlostfound.itemphoto.ItemPhotoRequest> photos
    ) {
        String itemSql = """
                INSERT INTO app.item (
                    category_id,
                    item_name,
                    brand,
                    colour,
                    description,
                    estimated_value,
                    item_condition
                )
                VALUES (?, ?, ?, ?, ?, ?, COALESCE(?, 'good'))
                RETURNING item_id
                """;

        Long itemId = jdbcTemplate.queryForObject(
                itemSql,
                Long.class,
                categoryId,
                itemName,
                brand,
                colour,
                description,
                estimatedValue,
                itemCondition
        );

        String foundSql = """
                INSERT INTO app.found_item (
                    item_id,
                    found_by_staff_id,
                    flight_id,
                    found_location_id,
                    found_at,
                    item_status
                )
                VALUES (?, ?, ?, ?, ?, 'awaiting_review')
                RETURNING found_item_id
                """;

        Long foundItemId = jdbcTemplate.queryForObject(
                foundSql,
                Long.class,
                itemId,
                foundByStaffId,
                flightId,
                foundLocationId,
                foundAt
        );

        if (photos != null && !photos.isEmpty()) {
            int photoNum = 1;
            for (var p : photos) {
                if (p.photoUrl() != null && !p.photoUrl().isBlank()) {
                    insertFoundItemPhoto(itemId, foundItemId, photoNum++, p.photoUrl().trim(), p.caption(), p.photoType());
                }
            }
        }

        return foundItemId;
    }

    public void updateFoundItemStatus(long foundItemId, String status) {
        String sql = "UPDATE app.found_item SET item_status = ?, updated_at = CURRENT_TIMESTAMP WHERE found_item_id = ?";
        jdbcTemplate.update(sql, status, foundItemId);
    }

    // -------------------------------------------------------------
    // Claims Verification
    // -------------------------------------------------------------
    public List<StaffClaimItem> findAllClaims(String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    c.claim_id,
                    c.match_id,
                    m.lost_report_id,
                    m.found_item_id,
                    p.full_name AS passenger_name,
                    p.email AS passenger_email,
                    p.phone AS passenger_phone,
                    lost_i.item_name AS lost_item_name,
                    found_i.item_name AS found_item_name,
                    m.match_score,
                    m.verification_status AS match_status,
                    c.claim_status,
                    c.submitted_at,
                    st.full_name AS claim_reviewed_by,
                    c.reviewed_at AS claim_reviewed_at,
                    c.decision_notes,
                    iv.verification_method,
                    iv.verification_result,
                    iv.verified_at,
                    rd.return_method,
                    rd.return_status
                FROM app.claim c
                JOIN app.item_match m ON m.match_id = c.match_id
                JOIN app.lost_report lr ON lr.lost_report_id = m.lost_report_id
                JOIN app.passenger p ON p.passenger_id = lr.passenger_id
                JOIN app.item lost_i ON lost_i.item_id = lr.item_id
                JOIN app.found_item f ON f.found_item_id = m.found_item_id
                JOIN app.item found_i ON found_i.item_id = f.item_id
                LEFT JOIN app.staff st ON st.staff_id = c.reviewed_by_staff_id
                LEFT JOIN LATERAL (
                    SELECT verification_method, verification_result, verified_at
                    FROM app.identity_verification
                    WHERE claim_id = c.claim_id
                    ORDER BY verification_id DESC
                    LIMIT 1
                ) iv ON true
                LEFT JOIN app.return_delivery rd ON rd.claim_id = c.claim_id
                WHERE 1=1
                """);

        List<Object> params = new ArrayList<>();
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("all")) {
            sql.append(" AND c.claim_status = ? ");
            params.add(status.trim().toLowerCase());
        }

        sql.append(" ORDER BY c.submitted_at DESC ");

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> new StaffClaimItem(
                rs.getLong("claim_id"),
                rs.getLong("match_id"),
                rs.getLong("lost_report_id"),
                rs.getLong("found_item_id"),
                rs.getString("passenger_name"),
                rs.getString("passenger_email"),
                rs.getString("passenger_phone"),
                rs.getString("lost_item_name"),
                rs.getString("found_item_name"),
                rs.getBigDecimal("match_score"),
                rs.getString("match_status"),
                rs.getString("claim_status"),
                rs.getObject("submitted_at", OffsetDateTime.class),
                rs.getString("claim_reviewed_by"),
                rs.getObject("claim_reviewed_at", OffsetDateTime.class),
                rs.getString("decision_notes"),
                rs.getString("verification_method"),
                rs.getString("verification_result"),
                rs.getObject("verified_at", OffsetDateTime.class),
                rs.getString("return_method"),
                rs.getString("return_status")
        ));
    }

    public void reviewClaim(long claimId, String decision, String verificationMethod, String notes, long staffId) {
        OffsetDateTime now = OffsetDateTime.now();

        // If approved, create or update identity_verification first because trigger fn_validate_claim_approval checks it!
        if ("approved".equalsIgnoreCase(decision)) {
            String method = (verificationMethod != null && !verificationMethod.isBlank())
                    ? verificationMethod : "in_person_document_check";

            String insertIdVerif = """
                    INSERT INTO app.identity_verification (
                        claim_id,
                        verification_method,
                        verification_result,
                        verified_by_staff_id,
                        verification_notes,
                        verified_at
                    )
                    VALUES (?, ?, 'verified', ?, ?, ?)
                    """;
            jdbcTemplate.update(insertIdVerif, claimId, method, staffId, notes, now);
        }

        String claimSql = """
                UPDATE app.claim
                SET claim_status = ?,
                    reviewed_by_staff_id = ?,
                    reviewed_at = ?,
                    decision_notes = ?
                WHERE claim_id = ?
                """;
        jdbcTemplate.update(claimSql, decision, staffId, now, notes, claimId);

        // Update match status accordingly
        String matchStatus = "approved".equalsIgnoreCase(decision) ? "verified" : ("rejected".equalsIgnoreCase(decision) ? "rejected" : "under_review");
        String matchSql = """
                UPDATE app.item_match
                SET verification_status = ?,
                    reviewed_by_staff_id = ?,
                    reviewed_at = ?,
                    remarks = COALESCE(?, remarks)
                WHERE match_id = (SELECT match_id FROM app.claim WHERE claim_id = ?)
                """;
        jdbcTemplate.update(matchSql, matchStatus, staffId, now, notes, claimId);

        // If approved, update found item status to 'claimed' and ensure return_delivery record exists
        if ("approved".equalsIgnoreCase(decision)) {
            String updateFoundSql = """
                    UPDATE app.found_item
                    SET item_status = 'claimed',
                        updated_at = CURRENT_TIMESTAMP
                    WHERE found_item_id = (
                        SELECT m.found_item_id
                        FROM app.claim c
                        JOIN app.item_match m ON m.match_id = c.match_id
                        WHERE c.claim_id = ?
                    )
                    """;
            jdbcTemplate.update(updateFoundSql, claimId);

            String checkReturnSql = "SELECT COUNT(*) FROM app.return_delivery WHERE claim_id = ?";
            Integer count = jdbcTemplate.queryForObject(checkReturnSql, Integer.class, claimId);
            if (count == null || count == 0) {
                String insertReturnSql = """
                        INSERT INTO app.return_delivery (
                            claim_id,
                            return_method,
                            return_status,
                            handled_by_staff_id,
                            delivery_notes,
                            created_at
                        )
                        VALUES (?, 'counter_handover', 'initiated', ?, 'Claim approved; ready for terminal counter release or courier dispatch.', CURRENT_TIMESTAMP)
                        """;
                jdbcTemplate.update(insertReturnSql, claimId, staffId);
            }
        }
    }

    // -------------------------------------------------------------
    // Storage & Inventory Management
    // -------------------------------------------------------------
    public List<StorageLocationInfo> findAllStorageLocations() {
        String locSql = """
                SELECT
                    sl.storage_id,
                    sl.storage_name,
                    loc.location_name,
                    sl.description,
                    sl.capacity
                FROM app.storage_location sl
                JOIN app.airport_location loc ON loc.location_id = sl.location_id
                WHERE sl.is_active = true
                ORDER BY sl.storage_id ASC
                """;

        String slotsSql = """
                SELECT
                    ss.storage_id,
                    ss.slot_number,
                    ss.slot_status,
                    sa.found_item_id,
                    i.item_name
                FROM app.storage_slot ss
                LEFT JOIN app.storage_assignment sa
                    ON sa.storage_id = ss.storage_id
                    AND sa.slot_number = ss.slot_number
                    AND sa.released_at IS NULL
                LEFT JOIN app.found_item f ON f.found_item_id = sa.found_item_id
                LEFT JOIN app.item i ON i.item_id = f.item_id
                ORDER BY ss.storage_id, ss.slot_number
                """;

        Map<Long, List<StorageLocationInfo.StorageSlotInfo>> slotsMap = new LinkedHashMap<>();
        jdbcTemplate.query(slotsSql, (rs) -> {
            long stId = rs.getLong("storage_id");
            slotsMap.computeIfAbsent(stId, k -> new ArrayList<>()).add(
                    new StorageLocationInfo.StorageSlotInfo(
                            stId,
                            rs.getInt("slot_number"),
                            rs.getString("slot_status"),
                            rs.getObject("found_item_id", Long.class),
                            rs.getString("item_name")
                    )
            );
        });

        return jdbcTemplate.query(locSql, (rs, rowNum) -> {
            long stId = rs.getLong("storage_id");
            int capacity = rs.getInt("capacity");
            List<StorageLocationInfo.StorageSlotInfo> slots = slotsMap.getOrDefault(stId, List.of());
            int occupied = (int) slots.stream().filter(s -> s.assignedFoundItemId() != null).count();
            int available = Math.max(0, capacity - occupied);

            return new StorageLocationInfo(
                    stId,
                    rs.getString("storage_name"),
                    rs.getString("location_name"),
                    rs.getString("description"),
                    capacity,
                    available,
                    occupied,
                    slots
            );
        });
    }

    public void assignStorageSlot(long foundItemId, long storageId, int slotNumber, long staffId) {
        // Release any existing assignment for this item
        String releaseSql = """
                UPDATE app.storage_assignment
                SET released_at = CURRENT_TIMESTAMP
                WHERE found_item_id = ? AND released_at IS NULL
                """;
        jdbcTemplate.update(releaseSql, foundItemId);

        // Insert new assignment
        String assignSql = """
                INSERT INTO app.storage_assignment (
                    found_item_id,
                    storage_id,
                    slot_number,
                    assigned_by_staff_id,
                    assigned_at
                )
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                """;
        jdbcTemplate.update(assignSql, foundItemId, storageId, slotNumber, staffId);

        // Update found item status to 'stored', assign slot and stored_at timestamp
        String updateItemSql = """
                UPDATE app.found_item
                SET item_status = 'stored',
                    storage_id = ?,
                    slot_number = ?,
                    stored_at = COALESCE(stored_at, CURRENT_TIMESTAMP),
                    updated_at = CURRENT_TIMESTAMP
                WHERE found_item_id = ?
                """;
        jdbcTemplate.update(updateItemSql, storageId, slotNumber, foundItemId);
    }

    public void insertFoundItemPhoto(long itemId, long foundItemId, int photoNumber, String photoUrl, String caption, String photoType) {
        String sql = """
                INSERT INTO app.item_photo (item_id, found_item_id, photo_number, photo_url, caption, photo_type, uploaded_at)
                VALUES (?, ?, ?, ?, ?, COALESCE(?, 'overview'), CURRENT_TIMESTAMP)
                ON CONFLICT (item_id, photo_number) DO UPDATE
                SET photo_url = EXCLUDED.photo_url, caption = EXCLUDED.caption, photo_type = EXCLUDED.photo_type, found_item_id = EXCLUDED.found_item_id
                """;
        jdbcTemplate.update(sql, itemId, foundItemId, photoNumber, photoUrl, caption, photoType != null ? photoType : "overview");
    }

    // -------------------------------------------------------------
    // Return & Dispatch Management
    // -------------------------------------------------------------
    public List<StaffReturnItem> findAllReturns(String status) {
        StringBuilder sql = new StringBuilder("""
                SELECT
                    COALESCE(rd.return_id, 0) AS return_id,
                    c.claim_id,
                    f.found_item_id,
                    p.full_name AS passenger_name,
                    p.email AS passenger_email,
                    p.phone AS passenger_phone,
                    i.item_name,
                    COALESCE(rd.return_method, 'counter_handover') AS return_method,
                    COALESCE(rd.return_status, 'initiated') AS return_status,
                    rd.handled_by_staff_id,
                    s.full_name AS handled_by_staff_name,
                    rd.delivery_notes,
                    COALESCE(rd.created_at, c.reviewed_at, c.created_at) AS created_at,
                    rd.completed_at
                FROM app.claim c
                JOIN app.item_match m ON m.match_id = c.match_id
                JOIN app.lost_report lr ON lr.lost_report_id = m.lost_report_id
                JOIN app.passenger p ON p.passenger_id = lr.passenger_id
                JOIN app.found_item f ON f.found_item_id = m.found_item_id
                JOIN app.item i ON i.item_id = f.item_id
                LEFT JOIN app.return_delivery rd ON rd.claim_id = c.claim_id
                LEFT JOIN app.staff s ON s.staff_id = rd.handled_by_staff_id
                WHERE c.claim_status = 'approved'
                """);

        List<Object> params = new ArrayList<>();
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("all")) {
            sql.append(" AND COALESCE(rd.return_status, 'initiated') = ? ");
            params.add(status.trim().toLowerCase());
        }

        sql.append(" ORDER BY COALESCE(rd.created_at, c.reviewed_at) DESC ");

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> new StaffReturnItem(
                rs.getLong("return_id"),
                rs.getLong("claim_id"),
                rs.getLong("found_item_id"),
                rs.getString("passenger_name"),
                rs.getString("passenger_email"),
                rs.getString("passenger_phone"),
                rs.getString("item_name"),
                rs.getString("return_method"),
                rs.getString("return_status"),
                rs.getLong("handled_by_staff_id"),
                rs.getString("handled_by_staff_name"),
                rs.getString("delivery_notes"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("completed_at", OffsetDateTime.class)
        ));
    }

    public void processReturn(long claimId, String returnMethod, String returnStatus, String notes, long staffId) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime completedAt = "completed".equalsIgnoreCase(returnStatus) ? now : null;

        // Check if a return_delivery record already exists for this claim
        String checkSql = "SELECT return_id FROM app.return_delivery WHERE claim_id = ?";
        List<Long> existingIds = jdbcTemplate.query(checkSql, (rs, rowNum) -> rs.getLong("return_id"), claimId);

        if (existingIds.isEmpty()) {
            String insertSql = """
                    INSERT INTO app.return_delivery (
                        claim_id,
                        return_method,
                        return_status,
                        handled_by_staff_id,
                        delivery_notes,
                        created_at,
                        completed_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """;
            jdbcTemplate.update(insertSql, claimId, returnMethod, returnStatus, staffId, notes, now, completedAt);
        } else {
            String updateSql = """
                    UPDATE app.return_delivery
                    SET return_method = ?,
                        return_status = ?,
                        handled_by_staff_id = ?,
                        delivery_notes = ?,
                        completed_at = ?
                    WHERE claim_id = ?
                    """;
            jdbcTemplate.update(updateSql, returnMethod, returnStatus, staffId, notes, completedAt, claimId);
        }

        // If completed, release storage slot and update found_item status to 'returned'
        if ("completed".equalsIgnoreCase(returnStatus)) {
            String releaseStorageSql = """
                    UPDATE app.storage_assignment
                    SET released_at = CURRENT_TIMESTAMP
                    WHERE found_item_id = (
                        SELECT m.found_item_id
                        FROM app.claim c
                        JOIN app.item_match m ON m.match_id = c.match_id
                        WHERE c.claim_id = ?
                    )
                    AND released_at IS NULL
                    """;
            jdbcTemplate.update(releaseStorageSql, claimId);

            String updateFoundSql = """
                    UPDATE app.found_item
                    SET item_status = 'returned',
                        updated_at = CURRENT_TIMESTAMP
                    WHERE found_item_id = (
                        SELECT m.found_item_id
                        FROM app.claim c
                        JOIN app.item_match m ON m.match_id = c.match_id
                        WHERE c.claim_id = ?
                    )
                    """;
            jdbcTemplate.update(updateFoundSql, claimId);
        }
    }

    // -------------------------------------------------------------
    // Supervisor Telemetry & Audit Logs
    // -------------------------------------------------------------
    public SupervisorStats getSupervisorStats() {
        String statsSql = """
                SELECT
                    (SELECT COUNT(*) FROM app.found_item) AS total_found,
                    (SELECT COUNT(*) FROM app.found_item WHERE item_status = 'awaiting_review') AS awaiting_review,
                    (SELECT COUNT(*) FROM app.found_item WHERE item_status = 'stored') AS stored_count,
                    (SELECT COUNT(*) FROM app.found_item WHERE item_status = 'claimed') AS claimed_count,
                    (SELECT COUNT(*) FROM app.found_item WHERE item_status = 'returned') AS returned_count,
                    (SELECT COUNT(*) FROM app.lost_report) AS total_lost_reports,
                    (SELECT COUNT(*) FROM app.claim WHERE claim_status IN ('submitted', 'under_review')) AS pending_claims,
                    (SELECT COUNT(*) FROM app.claim WHERE claim_status = 'approved') AS approved_claims,
                    (SELECT COUNT(*) FROM app.storage_slot) AS total_slots,
                    (SELECT COUNT(*) FROM app.storage_assignment WHERE released_at IS NULL) AS occupied_slots,
                    (SELECT COUNT(*) FROM app.staff WHERE is_active = true) AS active_staff,
                    (SELECT COUNT(*) FROM app.audit_log) AS total_audits
                """;

        return jdbcTemplate.queryForObject(statsSql, (rs, rowNum) -> new SupervisorStats(
                rs.getLong("total_found"),
                rs.getLong("awaiting_review"),
                rs.getLong("stored_count"),
                rs.getLong("claimed_count"),
                rs.getLong("returned_count"),
                rs.getLong("total_lost_reports"),
                rs.getLong("pending_claims"),
                rs.getLong("approved_claims"),
                rs.getLong("total_slots"),
                rs.getLong("occupied_slots"),
                rs.getLong("active_staff"),
                rs.getLong("total_audits")
        ));
    }

    public List<StaffAuditLog> findAuditLogs(int limit) {
        String sql = """
                SELECT
                    a.audit_id,
                    a.actor_staff_id,
                    s.full_name AS actor_staff_name,
                    a.action_type,
                    a.entity_name,
                    a.entity_id,
                    a.details::text AS details_text,
                    a.occurred_at
                FROM app.audit_log a
                LEFT JOIN app.staff s ON s.staff_id = a.actor_staff_id
                ORDER BY a.occurred_at DESC
                LIMIT ?
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new StaffAuditLog(
                rs.getLong("audit_id"),
                rs.getObject("actor_staff_id", Long.class),
                rs.getString("actor_staff_name"),
                rs.getString("action_type"),
                rs.getString("entity_name"),
                rs.getObject("entity_id", Long.class),
                rs.getString("details_text"),
                rs.getObject("occurred_at", OffsetDateTime.class)
        ), limit);
    }

    public void recordAuditLog(Long actorStaffId, String actionType, String entityName, Long entityId, String detailsJson) {
        String sql = """
                INSERT INTO app.audit_log (
                    actor_staff_id,
                    action_type,
                    entity_name,
                    entity_id,
                    details
                )
                VALUES (?, ?, ?, ?, ?::jsonb)
                """;
        try {
            jdbcTemplate.update(sql, actorStaffId, actionType, entityName, entityId, detailsJson != null ? detailsJson : "{}");
        } catch (Exception ex) {
            // Non-blocking for audit failure
        }
    }
}
