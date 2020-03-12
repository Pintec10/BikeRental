package test.bikerental;

import org.apache.tomcat.jni.Local;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Period;
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


    // ----- RENTAL ENDPOINT -----
    @RequestMapping(value="/rent", method = RequestMethod.POST)
    public ResponseEntity<Map<String, String>> createNewRental(@RequestBody RentalForm rentalForm) {

        //setup
        String reqBikeType = rentalForm.getBikeType();
        PriceList priceListInstance = priceListRepository.findAll().stream().findFirst().orElse(null);
        Rental newRental = new Rental();

        //if rentalForm info is not complete, return error message
        if (rentalForm.getStartDate() == null || rentalForm.getAgreedDurationDays() == null
                || rentalForm.getBikeType() == null || rentalForm.getName() == null
                || rentalForm.getEmail() == null || rentalForm.getPhoneNumber() == null) {
            return new ResponseEntity(makeMap("error", "Please send all required information"), HttpStatus.BAD_REQUEST);
        }

        // minimum check for email format
        if (!rentalForm.getEmail().contains("@") || rentalForm.getEmail().contains(" ")) {
            return new ResponseEntity(makeMap("error", "Invalid email format"), HttpStatus.FORBIDDEN);
        }

        // minimum check for rental duration
        if (rentalForm.getAgreedDurationDays() < 1) {
            return new ResponseEntity(makeMap("error", "Rental duration must be at least one day"), HttpStatus.FORBIDDEN);
        }

        //looks for one random available bike of the required type
        Bike availableBike = bikeRepository.findByBikeType(reqBikeType).stream()
                .filter(oneBike -> isAvailable(oneBike, rentalForm.getStartDate(),
                        rentalForm.getAgreedDurationDays())).findAny().orElse(null);

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
            customerRepository.save(new Customer(rentalForm.getName().trim(), rentalForm.getEmail(), rentalForm.getPhoneNumber()));
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
        output.put("customer_email", newRental.getCustomer().getEmail());
        output.put("customer_phoneNumber", newRental.getCustomer().getPhoneNumber());
        output.put("bike_type", newRental.getBike().getBikeType());
        output.put("bike_id", newRental.getBike().getId().toString());
        output.put("rental_start", newRental.getStartDate().toString());
        output.put("agreed_duration_days", newRental.getAgreedDurationDays().toString());
        output.put("upfront_payment", newRental.getUpfrontPayment().toString());

        return new ResponseEntity(output, HttpStatus.CREATED);
    }

    private Boolean isAvailable(Bike oneBike, LocalDate reqStartRentalDate, int reqRentalDurationDays) {
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


    // ----- RETURN ENDPOINT -----
    @RequestMapping(value="/return/{rentalId}", method = RequestMethod.POST)
    public ResponseEntity<Map<String, String>> returnBike(@PathVariable Long rentalId) {

        //setup
        PriceList priceListInstance = priceListRepository.findAll().stream().findFirst().orElse(null);
        Rental rentalInRepository = rentalRepository.findById(rentalId).orElse(null);

        System.out.println("rental in repository id:");
        System.out.println(rentalInRepository.getId());

        //if no Rental is found, send an error message
        if (rentalInRepository == null) {
            return new ResponseEntity(makeMap("error", "Reservation not found, check bike id and rental starting date (YYYY-MM-DD)"),
                    HttpStatus.NOT_FOUND);
        }

        //calculate actual rental duration
        LocalDate returnDay = LocalDate.now();
            // if bike return has been handled already, the original return day will be used (results won't change)
        if(rentalInRepository.getActualEndDate() != null) {
            returnDay = rentalInRepository.getActualEndDate();
        }
        LocalDate rentalStartDate = rentalInRepository.getStartDate();
        Integer actualRentalDurationDays = Period.between(rentalStartDate, returnDay.plusDays(1)).getDays();
        if(actualRentalDurationDays < 0) {
            return new ResponseEntity(makeMap("error", "rental end date cannot be before start date"), HttpStatus.FORBIDDEN);
        }
        Integer extraDays = actualRentalDurationDays - rentalInRepository.getAgreedDurationDays();
        if(extraDays < 0) {
            extraDays = 0;
        }

        //calculate final price
        Double bikePricePerDay = priceListInstance.getBikePriceList().get(rentalInRepository.getBike().getBikeType());
        Double extraPricePerDay = priceListInstance.getBikePriceList().get("extraFee");
        if(bikePricePerDay == null || extraPricePerDay == null) {
            return new ResponseEntity(makeMap("error", "the daily price for this bike or the late return fee " +
                    "are not defined in the price list"), HttpStatus.NOT_FOUND);
        }
        //if bike is returned earlier than agreed, there is no discount (they can bargain with the owner!)
        Double finalCost = (actualRentalDurationDays * bikePricePerDay) + (extraDays * extraPricePerDay);
        if(finalCost < rentalInRepository.getUpfrontPayment()) {
            finalCost = rentalInRepository.getUpfrontPayment();
        }

        // update RentalRepository and send information to front-end, if the return was not already handled before
        if(rentalInRepository.getFinalCost() == null) {
            rentalInRepository.setActualEndDate(returnDay);
            rentalInRepository.setFinalCost(finalCost);
            rentalRepository.save(rentalInRepository);
        } else { System.out.println("case already handled");}

        Map<String, String> output = new LinkedHashMap<>();
        output.putAll(rentalMapper(rentalInRepository));
        output.put("actual_rental_duration_days", actualRentalDurationDays.toString());
        output.put("final_cost", rentalInRepository.getFinalCost().toString());
        output.put("bike_cost_per_day", bikePricePerDay.toString());
        output.put("extra_cost_per_day", extraPricePerDay.toString());
        //output.put("upfront_payment", rentalInRepository.getUpfrontPayment().toString());

        return new ResponseEntity(output, HttpStatus.OK);

    }

    private Map<String,String> rentalMapper (Rental rental) {
        Map<String, String> output = new LinkedHashMap<>();
        output.put("rental_id", rental.getId().toString());
        output.put("customer_name", rental.getCustomer().getName());
        output.put("bike_type", rental.getBike().getBikeType());
        output.put("bike_id", rental.getBike().getId().toString());
        output.put("start_date", rental.getStartDate().toString());
        output.put("agreed_duration_days", rental.getAgreedDurationDays().toString());
        //output.put("actual_rental_duration", actualRentalDurationDays.toString());
        //output.put("bike_cost_per_day", bikePricePerDay.toString());
        //output.put("extra_cost_per_day", extraPricePerDay.toString());
        output.put("upfront_payment", rental.getUpfrontPayment().toString());

        return output;
    }

    /* ----- INITIAL ENDPOINT -----
       @RequestMapping(value="/return", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> returnBike(@RequestBody RentalForm returnForm) {
    Rental rentalInRepository = rentalRepository.findByStartDateAndBike_id(returnForm.getStartDate(), returnForm.getBikeId());
    }
     */
}
