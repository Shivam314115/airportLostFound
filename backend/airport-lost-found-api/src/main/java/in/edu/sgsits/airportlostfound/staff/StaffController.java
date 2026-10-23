package in.edu.sgsits.airportlostfound.staff;

import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('STAFF')")
    public StaffProfile currentStaff(Authentication authentication) {
        return staffService.getCurrentStaff(authentication);
    }

    // -------------------------------------------------------------
    // Found Items (Lost & Found Agent, Ground Staff, Supervisor, Admin)
    // -------------------------------------------------------------
    @GetMapping("/items")
    @PreAuthorize("hasAnyRole('STAFF_INTAKE', 'CLAIMS_OFFICER', 'INVENTORY_OFFICER', 'DISPATCH_OFFICER', 'SUPERVISOR', 'SYSTEM_ADMIN')")
    public List<StaffFoundItem> getFoundItems(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            Authentication authentication
    ) {
        return staffService.getFoundItems(status, search, authentication);
    }

    @PostMapping("/items")
    @PreAuthorize("hasAnyRole('STAFF_INTAKE', 'GROUND_STAFF', 'SUPERVISOR', 'SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, Object>> registerFoundItem(
            @Valid @RequestBody CreateFoundItemRequest request,
            Authentication authentication
    ) {
        long foundItemId = staffService.registerFoundItem(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Found item successfully registered in custody register.",
                "foundItemId", foundItemId
        ));
    }

    // -------------------------------------------------------------
    // Claims Verification (Claims Officer, Supervisor, Admin)
    // -------------------------------------------------------------
    @GetMapping("/claims")
    @PreAuthorize("hasAnyRole('CLAIMS_OFFICER', 'SUPERVISOR', 'SYSTEM_ADMIN')")
    public List<StaffClaimItem> getClaims(
            @RequestParam(required = false) String status
    ) {
        return staffService.getClaims(status);
    }

    @PostMapping("/claims/{claimId}/review")
    @PreAuthorize("hasAnyRole('CLAIMS_OFFICER', 'SUPERVISOR', 'SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, String>> reviewClaim(
            @PathVariable long claimId,
            @Valid @RequestBody ReviewClaimRequest request,
            Authentication authentication
    ) {
        staffService.reviewClaim(claimId, request, authentication);
        return ResponseEntity.ok(Map.of(
                "message", "Claim status updated to " + request.decision() + "."
        ));
    }

    // -------------------------------------------------------------
    // Inventory & Storage (Inventory Officer, Supervisor, Admin)
    // -------------------------------------------------------------
    @GetMapping("/inventory/locations")
    @PreAuthorize("hasAnyRole('INVENTORY_OFFICER', 'SUPERVISOR', 'SYSTEM_ADMIN')")
    public List<StorageLocationInfo> getStorageLocations() {
        return staffService.getStorageLocations();
    }

    @PostMapping("/inventory/assign")
    @PreAuthorize("hasAnyRole('INVENTORY_OFFICER', 'SUPERVISOR', 'SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, String>> assignStorageSlot(
            @Valid @RequestBody AssignStorageRequest request,
            Authentication authentication
    ) {
        staffService.assignStorageSlot(request, authentication);
        return ResponseEntity.ok(Map.of(
                "message", "Item successfully assigned to storage locker slot."
        ));
    }

    // -------------------------------------------------------------
    // Return & Dispatch (Dispatch Officer, Supervisor, Admin)
    // -------------------------------------------------------------
    @GetMapping("/dispatches")
    @PreAuthorize("hasAnyRole('DISPATCH_OFFICER', 'SUPERVISOR', 'SYSTEM_ADMIN')")
    public List<StaffReturnItem> getDispatches(
            @RequestParam(required = false) String status
    ) {
        return staffService.getReturns(status);
    }

    @PostMapping("/dispatches/process")
    @PreAuthorize("hasAnyRole('DISPATCH_OFFICER', 'SUPERVISOR', 'SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, String>> processReturn(
            @Valid @RequestBody ProcessReturnRequest request,
            Authentication authentication
    ) {
        staffService.processReturn(request, authentication);
        return ResponseEntity.ok(Map.of(
                "message", "Return delivery status updated to " + request.returnStatus() + "."
        ));
    }

    // -------------------------------------------------------------
    // Supervisor Telemetry & Audits (Supervisor, Admin)
    // -------------------------------------------------------------
    @GetMapping("/supervisor/stats")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'SYSTEM_ADMIN')")
    public SupervisorStats getSupervisorStats() {
        return staffService.getSupervisorStats();
    }

    @GetMapping("/supervisor/audits")
    @PreAuthorize("hasAnyRole('SUPERVISOR', 'SYSTEM_ADMIN')")
    public List<StaffAuditLog> getAuditLogs(
            @RequestParam(defaultValue = "50") int limit
    ) {
        return staffService.getAuditLogs(limit);
    }

    // -------------------------------------------------------------
    // System Admin: Staff User Management (Admin Only)
    // -------------------------------------------------------------
    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public List<StaffMember> getAllStaff() {
        return staffService.getAllStaff();
    }

    @PostMapping("/admin/users")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<StaffMember> createStaff(
            @Valid @RequestBody CreateStaffRequest request,
            Authentication authentication
    ) {
        StaffMember created = staffService.createStaff(request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/admin/users/{staffId}/status")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, Object>> updateStaffStatus(
            @PathVariable long staffId,
            @RequestBody UpdateStaffStatusRequest request,
            Authentication authentication
    ) {
        staffService.updateStaffStatus(staffId, request.isActive(), authentication);
        return ResponseEntity.ok(Map.of(
                "message", "Staff account active status updated.",
                "staffId", staffId,
                "isActive", request.isActive()
        ));
    }

    @PutMapping("/admin/users/{staffId}/role")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, Object>> updateStaffRole(
            @PathVariable long staffId,
            @Valid @RequestBody UpdateStaffRoleRequest request,
            Authentication authentication
    ) {
        staffService.updateStaffRole(staffId, request.staffRole(), authentication);
        return ResponseEntity.ok(Map.of(
                "message", "Staff role updated.",
                "staffId", staffId,
                "staffRole", request.staffRole()
        ));
    }
}
