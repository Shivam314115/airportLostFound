
package in.edu.sgsits.airportlostfound.lostreport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class LostReportApiErrorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void rejectsInvalidFlightAndRollsBackItemInsert() throws Exception {
        // Use a unique name so this test does not interfere with other data.
        String itemName = "FK rollback test " + UUID.randomUUID();

        String lostAt = OffsetDateTime.now()
                .minusHours(1)
                .toString();

        // Authenticate as a real, seeded passenger.
        // Use a nonexistent flight ID to trigger a foreign-key violation
        // after the item has been inserted.
        String requestBody = """
                {
                    "categoryId": 2,
                    "itemName": "%s",
                    "brand": "TestBrand",
                    "colour": "Blue",
                    "description": "Temporary item for rollback testing",
                    "estimatedValue": 250.00,
                    "itemCondition": "good",
                    "lostAt": "%s",
                    "flightId": 9223372036854775807,
                    "additionalDetails": "The flight ID intentionally does not exist."
                }
                """.formatted(itemName, lostAt);

        mockMvc.perform(post("/api/lost-reports")
                        .with(user("kabir.singh@example.test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").exists())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.fieldErrors").exists());

        // The item insert must be rolled back when inserting the report
        // fails because the referenced flight does not exist.
        Integer remainingItemCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM app.item
                WHERE item_name = ?
                """,
                Integer.class,
                itemName
        );

        assertEquals(
                0,
                remainingItemCount,
                "The failed transaction must not leave an orphaned item."
        );
    }
}
