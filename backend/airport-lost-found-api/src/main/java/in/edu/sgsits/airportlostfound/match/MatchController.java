package in.edu.sgsits.airportlostfound.match;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping
    public List<MatchOverview> getAllMatches(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) BigDecimal minScore) {

        return matchService.getMatches(status, minScore);
    }
}