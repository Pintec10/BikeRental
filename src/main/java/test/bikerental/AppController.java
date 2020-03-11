package test.bikerental;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api")
public class AppController {

    @Autowired
    BikeRepository bikeRepository;

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    RentalRepository rentalRepository;

    /*@RequestMapping(value="/rent", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> createNewRental(@RequestBody Rental newRental) {
        //CHECK IF Object is necessary in the Map, or you can put String!
        Bike bike = newRental.getBike();
        Customer customer = newRental.getCustomer();
        Set<Bike> bikeList = bikeRepository.findByBikeType(bike.getBikeType());
        Optional<Bike> availableBike = bikeList.stream().filter(oneBike -> isAvailable(oneBike, newRental.getStartDate(),
                newRental.getAgreedDurationDays())).findAny();

        //if bike is not available, send error message
        if (availableBike == null) {
            return new ResponseEntity(makeMap("error", "No bikes of this type available in this time slot!"), HttpStatus.FORBIDDEN);
        }

        //if user is not already in database, save it
        if(customerRepository.findByEmail(customer.getEmail()) == null) {
            customerRepository.save(customer);
        }



        return //
    }*/

    private Boolean isAvailable(Bike oneBike, LocalDate reqStartRentalDate, int reqRentalDurationDays) {
        LocalDate reqEndRentalDate = reqStartRentalDate.plusDays(reqRentalDurationDays);
        Set<Rental> singleBikeRentals = oneBike.getRentalsPerBike();

        // bike is available if all its existing bookings end before the required start date,
        // or start after the required end date
        return singleBikeRentals.stream().allMatch(oneRental -> oneRental.getExpectedEndDate().isBefore(reqStartRentalDate)
        || oneRental.getStartDate().isAfter(reqEndRentalDate));
    }

    private Map<String, Object> makeMap(String key, String value) {
        Map<String, Object> output = new HashMap<>();
        output.put(key, value);
        return output;
    }
}
