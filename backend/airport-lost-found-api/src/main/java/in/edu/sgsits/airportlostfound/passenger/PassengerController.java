package in.edu.sgsits.airportlostfound.passenger;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/passengers")
public class PassengerController {

    private final PassengerService passengerService;

    public PassengerController(PassengerService passengerService) {
        this.passengerService = passengerService;
    }

    @PostMapping
    public ResponseEntity<PassengerProfile> registerPassenger(
            @Valid @RequestBody RegisterPassengerRequest request) {

        PassengerProfile registeredPassenger =
                passengerService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(registeredPassenger);
    }
}