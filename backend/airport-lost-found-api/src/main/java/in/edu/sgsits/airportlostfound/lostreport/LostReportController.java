package in.edu.sgsits.airportlostfound.lostreport;

import java.util.List;

import in.edu.sgsits.airportlostfound.auth.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/lost-reports")
public class LostReportController {

    private final LostReportService lostReportService;
    private final AuthService authService;

    public LostReportController(
            LostReportService lostReportService,
            AuthService authService) {

        this.lostReportService = lostReportService;
        this.authService = authService;
    }

    @GetMapping
    public List<LostReportOverview> getLostReports(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long passengerId,
            Authentication authentication) {

        // A passenger-specific filter can only target the current passenger.
        if (passengerId != null) {
            long authenticatedPassengerId =
                    requireAuthenticatedPassengerId(authentication);

            if (passengerId.longValue() != authenticatedPassengerId) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "You can only filter lost reports by your own passenger ID."
                );
            }
        }

        // With no passengerId filter, this remains the discovery listing.
        return lostReportService.getLostReports(status, passengerId);
    }

    @PostMapping
    public ResponseEntity<LostReportOverview> createLostReport(
            @Valid @RequestBody CreateLostReportRequest request,
            Authentication authentication) {

        long authenticatedPassengerId =
                requireAuthenticatedPassengerId(authentication);

        LostReportOverview created =
                lostReportService.createLostReport(
                        authenticatedPassengerId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(created);
    }

    private long requireAuthenticatedPassengerId(
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required."
            );
        }

        return authService
                .getProfileByEmail(authentication.getName())
                .passengerId();
    }
}