package in.edu.sgsits.airportlostfound.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards client-side Single Page Application (SPA) routes to index.html.
 */
@Controller
public class SpaWebController {

    @GetMapping(value = {
        "/discover",
        "/my-reports",
        "/matches",
        "/staff"
    })
    public String forwardSpa() {
        return "forward:/index.html";
    }
}
