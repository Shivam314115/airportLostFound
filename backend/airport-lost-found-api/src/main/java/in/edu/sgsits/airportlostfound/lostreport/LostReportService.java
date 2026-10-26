package in.edu.sgsits.airportlostfound.lostreport;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import in.edu.sgsits.airportlostfound.match.MatchRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LostReportService {

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "open",
            "candidate_found",
            "resolved",
            "closed"
    );

    private final LostReportRepository lostReportRepository;
    private final MatchRepository matchRepository;

    public LostReportService(LostReportRepository lostReportRepository, MatchRepository matchRepository) {
        this.lostReportRepository = lostReportRepository;
        this.matchRepository = matchRepository;
    }

    public List<LostReportOverview> getLostReports(
            String status,
            Long passengerId) {

        String normalizedStatus = null;

        if (status != null) {
            normalizedStatus = status.trim().toLowerCase(Locale.ROOT);

            if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid report status. Allowed values: "
                                + String.join(", ", ALLOWED_STATUSES)
                );
            }
        }

        if (passengerId != null && passengerId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "passengerId must be a positive integer."
            );
        }

        return lostReportRepository.findAll(
                normalizedStatus,
                passengerId
        );
    }

    /**
     * Creates an item and its lost report in one database transaction.
     *
     * The passenger ID must come from the authenticated server-side identity,
     * not from the client request body.
     */
    @Transactional
    public LostReportOverview createLostReport(
            long passengerId,
            CreateLostReportRequest request) {

        if (passengerId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "passengerId must be positive."
            );
        }

        String itemCondition = request.itemCondition() == null
                ? "unknown"
                : request.itemCondition().toLowerCase(Locale.ROOT);

        long itemId = lostReportRepository.insertItem(
                request.categoryId(),
                request.itemName().trim(),
                normalizeOptionalText(request.brand()),
                normalizeOptionalText(request.colour()),
                normalizeOptionalText(request.description()),
                request.estimatedValue(),
                itemCondition
        );

        long lostReportId = lostReportRepository.insertLostReport(
                passengerId,
                itemId,
                request.flightId(),
                request.lastSeenLocationId(),
                request.lostAt(),
                normalizeOptionalText(request.additionalDetails())
        );

        if (request.photos() != null && !request.photos().isEmpty()) {
            int photoNum = 1;
            for (var photo : request.photos()) {
                if (photo.photoUrl() != null && !photo.photoUrl().isBlank()) {
                    lostReportRepository.insertPhoto(
                            itemId,
                            photoNum++,
                            photo.photoUrl().trim(),
                            photo.caption(),
                            photo.photoType()
                    );
                }
            }
        }

        matchRepository.autoCorrelateMatches();

        return lostReportRepository.findById(lostReportId)
                .orElseThrow(() -> new IllegalStateException(
                        "The lost report was inserted but could not be retrieved."
                ));
    }

    private static String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}