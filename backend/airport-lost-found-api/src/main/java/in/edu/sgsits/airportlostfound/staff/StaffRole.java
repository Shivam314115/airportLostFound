package in.edu.sgsits.airportlostfound.staff;

import java.util.Arrays;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public enum StaffRole {
    LOST_FOUND_AGENT(
            "lost_found_agent",
            "Lost & Found Agent",
            "Item intake, registration, and inventory search",
            List.of("ROLE_STAFF", "ROLE_STAFF_INTAKE", "ROLE_STAFF_SEARCH")
    ),
    CLAIMS_VERIFICATION_OFFICER(
            "claims_verification_officer",
            "Claims Verification Officer",
            "Claim review, identity verification, and match validation",
            List.of("ROLE_STAFF", "ROLE_CLAIMS_OFFICER", "ROLE_STAFF_SEARCH")
    ),
    INVENTORY_STORAGE_OFFICER(
            "inventory_storage_officer",
            "Inventory & Storage Officer",
            "Physical custody tracking, locker and slot assignments",
            List.of("ROLE_STAFF", "ROLE_INVENTORY_OFFICER", "ROLE_STAFF_SEARCH")
    ),
    RETURN_DISPATCH_OFFICER(
            "return_dispatch_officer",
            "Return & Dispatch Officer",
            "Item release, terminal counter handover, and courier dispatch",
            List.of("ROLE_STAFF", "ROLE_DISPATCH_OFFICER", "ROLE_STAFF_SEARCH")
    ),
    SUPERVISOR(
            "supervisor",
            "Lost Property Supervisor",
            "Operational oversight, escalation handling, and audit telemetry",
            List.of("ROLE_STAFF", "ROLE_SUPERVISOR", "ROLE_STAFF_INTAKE", "ROLE_STAFF_SEARCH", "ROLE_CLAIMS_OFFICER", "ROLE_INVENTORY_OFFICER", "ROLE_DISPATCH_OFFICER")
    ),
    GROUND_STAFF(
            "ground_staff",
            "Security / Baggage / Cleaning Staff",
            "Restricted found-item discovery reporting across terminals and gates",
            List.of("ROLE_STAFF", "ROLE_GROUND_STAFF", "ROLE_STAFF_INTAKE")
    ),
    SYSTEM_ADMIN(
            "system_admin",
            "System Administrator",
            "Staff user provisioning, system security, roles, and master audit logs",
            List.of("ROLE_STAFF", "ROLE_SYSTEM_ADMIN", "ROLE_SUPERVISOR", "ROLE_STAFF_INTAKE", "ROLE_STAFF_SEARCH", "ROLE_CLAIMS_OFFICER", "ROLE_INVENTORY_OFFICER", "ROLE_DISPATCH_OFFICER")
    ),
    // Backward compatibility for legacy seed entries
    LOST_FOUND_OFFICER(
            "lost_found_officer",
            "Lost & Found Officer",
            "Intake and claim resolution officer",
            List.of("ROLE_STAFF", "ROLE_STAFF_INTAKE", "ROLE_STAFF_SEARCH", "ROLE_CLAIMS_OFFICER")
    ),
    SECURITY_OFFICER(
            "security_officer",
            "Security Officer",
            "Terminal patrol and restricted item intake",
            List.of("ROLE_STAFF", "ROLE_GROUND_STAFF", "ROLE_STAFF_INTAKE")
    ),
    ADMINISTRATOR(
            "administrator",
            "Administrator",
            "Airport systems administrator",
            List.of("ROLE_STAFF", "ROLE_SYSTEM_ADMIN", "ROLE_SUPERVISOR", "ROLE_STAFF_INTAKE", "ROLE_STAFF_SEARCH", "ROLE_CLAIMS_OFFICER", "ROLE_INVENTORY_OFFICER", "ROLE_DISPATCH_OFFICER")
    );

    private final String code;
    private final String title;
    private final String description;
    private final List<String> authorityNames;

    StaffRole(String code, String title, String description, List<String> authorityNames) {
        this.code = code;
        this.title = title;
        this.description = description;
        this.authorityNames = authorityNames;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getAuthorityNames() {
        return authorityNames;
    }

    public List<GrantedAuthority> getAuthorities() {
        return authorityNames.stream()
                .map(SimpleGrantedAuthority::new)
                .map(GrantedAuthority.class::cast)
                .toList();
    }

    public static StaffRole fromCode(String code) {
        if (code == null) {
            return LOST_FOUND_AGENT;
        }
        return Arrays.stream(values())
                .filter(role -> role.code.equalsIgnoreCase(code.trim()))
                .findFirst()
                .orElse(LOST_FOUND_AGENT);
    }
}
