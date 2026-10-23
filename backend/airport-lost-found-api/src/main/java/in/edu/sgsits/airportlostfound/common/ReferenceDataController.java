package in.edu.sgsits.airportlostfound.common;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reference-data")
public class ReferenceDataController {

    private final JdbcTemplate jdbcTemplate;

    public ReferenceDataController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record CategoryOption(long categoryId, String categoryName, String description, Long parentCategoryId) {
        public CategoryOption(long categoryId, String categoryName, String description) {
            this(categoryId, categoryName, description, null);
        }
    }
    public record LocationOption(long locationId, String locationName, String locationType, String description) {}
    public record FlightOption(long flightId, String flightNumber, String airline) {}
    public record ReferenceData(
            List<CategoryOption> categories,
            List<LocationOption> locations,
            List<FlightOption> flights
    ) {}

    @GetMapping
    public ReferenceData getReferenceData() {
        List<CategoryOption> categories = jdbcTemplate.query(
                "SELECT category_id, category_name, description, parent_category_id FROM app.item_category ORDER BY category_id",
                (rs, i) -> new CategoryOption(
                        rs.getLong("category_id"),
                        rs.getString("category_name"),
                        rs.getString("description"),
                        rs.getObject("parent_category_id", Long.class)
                )
        );

        List<LocationOption> locations = jdbcTemplate.query(
                "SELECT location_id, location_name, location_type, description FROM app.airport_location ORDER BY location_id",
                (rs, i) -> new LocationOption(
                        rs.getLong("location_id"),
                        rs.getString("location_name"),
                        rs.getString("location_type"),
                        rs.getString("description")
                )
        );

        List<FlightOption> flights = jdbcTemplate.query(
                "SELECT flight_id, flight_number, airline FROM app.flight ORDER BY flight_id",
                (rs, i) -> new FlightOption(
                        rs.getLong("flight_id"),
                        rs.getString("flight_number"),
                        rs.getString("airline")
                )
        );

        return new ReferenceData(categories, locations, flights);
    }
}
