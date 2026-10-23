package in.edu.sgsits.airportlostfound.passenger;

/**
 * Internal database data required to authenticate a passenger.
 *
 * Never return this record directly from an API endpoint.
 */
public record PassengerLoginData(
        long passengerId,
        String fullName,
        String email,
        String passwordHash
) {
}
