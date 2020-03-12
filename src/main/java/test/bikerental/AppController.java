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

    @Autowired
    PriceListRepository priceListRepository;

    @RequestMapping(value="/rent", method = RequestMethod.POST)
    public ResponseEntity<Map<String, String>> createNewRental(@RequestBody RentalForm rentalForm) {

        //setup
        System.out.println("rent request received");
        String reqBikeType = rentalForm.getBikeType();
            //Set<Bike> bikeList = bikeRepository.findByBikeType(reqBikeType);
        PriceList priceListInstance = priceListRepository.findAll().stream().findFirst().orElse(null);
        Rental newRental = new Rental();

        //looks for one random available bike of the required type
        Bike availableBike = bikeRepository.findByBikeType(reqBikeType).stream()
                .filter(oneBike -> isAvailable(oneBike, rentalForm.getStartDate(),
                        rentalForm.getAgreedDurationDays())).findAny().orElse(null);

        //if rentalForm info is not complete, return error message
        if (rentalForm.getStartDate() == null || rentalForm.getAgreedDurationDays() == null
                || rentalForm.getBikeType() == null || rentalForm.getName() == null
                || rentalForm.getEmail() == null || rentalForm.getPhoneNumber() == null) {
            return new ResponseEntity(makeMap("error", "Please send all required information"), HttpStatus.BAD_REQUEST);
        }

        //if bike is not available, return error message
        if (availableBike == null) {
            return new ResponseEntity(makeMap("error", "No bikes available! Change bike type or time slot"), HttpStatus.FORBIDDEN);
        }

        //calculate upfront payment; if bike model is not in the price list, return error
        if (priceListInstance.getBikePriceList().containsKey(reqBikeType) ) {
            Double upfrontPayment = priceListInstance.getBikePriceList().get(reqBikeType)
                    * rentalForm.getAgreedDurationDays() ;
            newRental.setUpfrontPayment(upfrontPayment);
        } else {
            return new ResponseEntity(makeMap("error", "This bike model is not in the price list"), HttpStatus.FORBIDDEN);
        }

        //if customer is not already in database, then save it
        Customer customerInRepository = customerRepository.findByEmail(rentalForm.getEmail()).orElse(null);
        if(customerInRepository == null) {
            System.out.println("new customer!");
            System.out.println(rentalForm.getEmail());
            customerRepository.save(new Customer(rentalForm.getName(), rentalForm.getEmail(), rentalForm.getPhoneNumber()));
            customerInRepository = customerRepository.findByEmail(rentalForm.getEmail()).orElse(null);
        }

        //save the new Rental in database and return map with data for frontend consumption
        newRental.setCustomer(customerInRepository);
        newRental.setBike(availableBike);
        newRental.setStartDate(rentalForm.getStartDate());
        newRental.setAgreedDurationDays(rentalForm.getAgreedDurationDays());
        rentalRepository.save(newRental);

        Map<String, String> output = new HashMap<>();
        output.put("customer_name", newRental.getCustomer().getName());
        output.put("bike_type", newRental.getBike().getBikeType());
        output.put("rental_start", newRental.getStartDate().toString());
        output.put("agreed_duration_days", newRental.getAgreedDurationDays().toString());
        output.put("upfront_payment", newRental.getUpfrontPayment().toString());

        return new ResponseEntity(output, HttpStatus.CREATED);
    }

    private Boolean isAvailable(Bike oneBike, LocalDate reqStartRentalDate, int reqRentalDurationDays) {
        System.out.println("checking availability");
        LocalDate reqEndRentalDate = reqStartRentalDate.plusDays(reqRentalDurationDays - 1);
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
