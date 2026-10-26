package in.edu.sgsits.airportlostfound.staff;

import java.util.List;
import java.util.Locale;

import in.edu.sgsits.airportlostfound.match.MatchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StaffService {

    private final StaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final MatchRepository matchRepository;

    public StaffService(StaffRepository staffRepository, PasswordEncoder passwordEncoder, MatchRepository matchRepository) {
        this.staffRepository = staffRepository;
        this.passwordEncoder = passwordEncoder;
        this.matchRepository = matchRepository;
    }

    public StaffProfile getStaffByEmail(String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        return staffRepository.findProfileByEmail(normalizedEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Staff profile not found."
                ));
    }

    public StaffProfile getCurrentStaff(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }
        return getStaffByEmail(authentication.getName());
    }

    @Transactional(readOnly = true)
    public List<StaffFoundItem> getFoundItems(String status, String search, Authentication authentication) {
        StaffProfile currentStaff = getCurrentStaff(authentication);

        // Ground staff can see items reported by them, or all items if supervisor/admin
        Long staffFilter = null;
        if ("ground_staff".equalsIgnoreCase(currentStaff.staffRole())
                || "security_officer".equalsIgnoreCase(currentStaff.staffRole())) {
            staffFilter = currentStaff.staffId();
        }

        return staffRepository.findFoundItems(status, staffFilter, search);
    }

    @Transactional
    public long registerFoundItem(CreateFoundItemRequest request, Authentication authentication) {
        StaffProfile currentStaff = getCurrentStaff(authentication);

        long foundItemId = staffRepository.registerFoundItem(
                request.categoryId(),
                request.itemName().trim(),
                request.brand() != null ? request.brand().trim() : null,
                request.colour() != null ? request.colour().trim() : null,
                request.description() != null ? request.description().trim() : null,
                request.estimatedValue(),
                request.itemCondition(),
                request.flightId(),
                request.foundLocationId(),
                request.foundAt(),
                currentStaff.staffId(),
                request.photos()
        );

        staffRepository.recordAuditLog(
                currentStaff.staffId(),
                "INSERT",
                "found_item",
                foundItemId,
                "{\"itemName\": \"" + request.itemName().replace("\"", "\\\"") + "\", \"reportedBy\": \"" + currentStaff.fullName() + "\"}"
        );

        matchRepository.autoCorrelateMatches();

        return foundItemId;
    }

    @Transactional
    public List<StaffClaimItem> getClaims(String status) {
        matchRepository.autoCorrelateMatches();
        return staffRepository.findAllClaims(status);
    }

    @Transactional
    public void reviewClaim(long claimId, ReviewClaimRequest request, Authentication authentication) {
        StaffProfile currentStaff = getCurrentStaff(authentication);

        staffRepository.reviewClaim(
                claimId,
                request.decision().toLowerCase(Locale.ROOT),
                request.verificationMethod(),
                request.notes(),
                currentStaff.staffId()
        );

        staffRepository.recordAuditLog(
                currentStaff.staffId(),
                "CLAIM_DECISION",
                "claim",
                claimId,
                "{\"decision\": \"" + request.decision() + "\", \"reviewedBy\": \"" + currentStaff.fullName() + "\"}"
        );
    }

    @Transactional(readOnly = true)
    public List<StorageLocationInfo> getStorageLocations() {
        return staffRepository.findAllStorageLocations();
    }

    @Transactional
    public void assignStorageSlot(AssignStorageRequest request, Authentication authentication) {
        StaffProfile currentStaff = getCurrentStaff(authentication);

        staffRepository.assignStorageSlot(
                request.foundItemId(),
                request.storageId(),
                request.slotNumber(),
                currentStaff.staffId()
        );

        staffRepository.recordAuditLog(
                currentStaff.staffId(),
                "UPDATE",
                "storage_assignment",
                request.foundItemId(),
                "{\"storageId\": " + request.storageId() + ", \"slot\": " + request.slotNumber() + ", \"assignedBy\": \"" + currentStaff.fullName() + "\"}"
        );
    }

    @Transactional(readOnly = true)
    public List<StaffReturnItem> getReturns(String status) {
        return staffRepository.findAllReturns(status);
    }

    @Transactional
    public void processReturn(ProcessReturnRequest request, Authentication authentication) {
        StaffProfile currentStaff = getCurrentStaff(authentication);

        staffRepository.processReturn(
                request.claimId(),
                request.returnMethod(),
                request.returnStatus(),
                request.deliveryNotes(),
                currentStaff.staffId()
        );

        staffRepository.recordAuditLog(
                currentStaff.staffId(),
                "ITEM_RETURNED",
                "return_delivery",
                request.claimId(),
                "{\"returnStatus\": \"" + request.returnStatus() + "\", \"handledBy\": \"" + currentStaff.fullName() + "\"}"
        );
    }

    @Transactional(readOnly = true)
    public SupervisorStats getSupervisorStats() {
        return staffRepository.getSupervisorStats();
    }

    @Transactional(readOnly = true)
    public List<StaffAuditLog> getAuditLogs(int limit) {
        return staffRepository.findAuditLogs(Math.min(limit, 200));
    }

    // -------------------------------------------------------------
    // Admin Operations
    // -------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<StaffMember> getAllStaff() {
        return staffRepository.findAllStaff();
    }

    @Transactional
    public StaffMember createStaff(CreateStaffRequest request, Authentication authentication) {
        StaffProfile currentStaff = getCurrentStaff(authentication);

        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (staffRepository.findLoginByEmail(normalizedEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A staff member with this email already exists.");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        StaffMember created = staffRepository.createStaff(
                request.fullName().trim(),
                normalizedEmail,
                passwordHash,
                request.staffRole().trim().toLowerCase(Locale.ROOT)
        );

        staffRepository.recordAuditLog(
                currentStaff.staffId(),
                "INSERT",
                "staff",
                created.staffId(),
                "{\"createdStaffEmail\": \"" + created.email() + "\", \"role\": \"" + created.staffRole() + "\"}"
        );

        return created;
    }

    @Transactional
    public void updateStaffStatus(long staffId, boolean isActive, Authentication authentication) {
        StaffProfile currentStaff = getCurrentStaff(authentication);

        staffRepository.updateStaffStatus(staffId, isActive);

        staffRepository.recordAuditLog(
                currentStaff.staffId(),
                "UPDATE",
                "staff",
                staffId,
                "{\"isActive\": " + isActive + "}"
        );
    }

    @Transactional
    public void updateStaffRole(long staffId, String staffRole, Authentication authentication) {
        StaffProfile currentStaff = getCurrentStaff(authentication);

        staffRepository.updateStaffRole(staffId, staffRole.trim().toLowerCase(Locale.ROOT));

        staffRepository.recordAuditLog(
                currentStaff.staffId(),
                "UPDATE",
                "staff",
                staffId,
                "{\"newRole\": \"" + staffRole + "\"}"
        );
    }
}
