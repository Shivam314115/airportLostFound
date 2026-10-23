package in.edu.sgsits.airportlostfound.match;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MatchService {

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "candidate",
            "under_review",
            "verified",
            "rejected"
    );

    private final MatchRepository matchRepository;

    public MatchService(MatchRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    public java.util.List<MatchOverview> getMatches(
            String status,
            BigDecimal minScore) {

        String normalizedStatus = null;

        if (status != null) {
            normalizedStatus = status.trim().toLowerCase(Locale.ROOT);

            if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid status. Allowed values: "
                                + String.join(", ", ALLOWED_STATUSES)
                );
            }
        }

        if (minScore != null
                && (minScore.compareTo(BigDecimal.ZERO) < 0
                || minScore.compareTo(BigDecimal.valueOf(100)) > 0)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "minScore must be between 0 and 100."
            );
        }

        return matchRepository.findAll(normalizedStatus, minScore);
    }
}