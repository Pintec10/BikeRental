package test.bikerental;

import org.apache.tomcat.jni.Local;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

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
    public ResponseEntity<Map<String, Object>> createNewRental(@RequestBody RentalForm rentalForm) {

        //setup
        String requiredBikeType = rentalForm.getBikeType();
        PriceList priceListInstance = priceListRepository.findAll().stream().findFirst().orElse(null);
        Rental newRental = new Rental();

        //form validation
        Map<Boolean, ResponseEntity<Map<String, Object>>> formIsValid = rentalFormValidation(rentalForm);
        if(formIsValid.containsKey(false)) {
            return formIsValid.get(false);
        }

        //looks for one random available bike of the required type
        Bike availableBike = bikeRepository.findByBikeType(requiredBikeType).stream()
                .filter(oneBike -> isAvailable(oneBike, rentalForm.getStartDate(),
                        rentalForm.getAgreedDurationDays())).findAny().orElse(null);
        System.out.println(("availableBike"));
        System.out.println(availableBike);

        //if bike is not available, return error message
        if (availableBike == null) {
            return new ResponseEntity(makeMap("error", "No bikes available! Change/check bike type or time slot"), HttpStatus.FORBIDDEN);
        }

        //calculate and set upfront payment; if bike model is not in the price list, return error
        if (priceListInstance.getBikePriceList().containsKey(requiredBikeType) ) {
            Double upfrontPayment = priceListInstance.getBikePriceList().get(requiredBikeType)
                    * rentalForm.getAgreedDurationDays() ;
            newRental.setUpfrontPayment(upfrontPayment);
        } else {
            return new ResponseEntity(makeMap("error", "This bike model is not in the price list"), HttpStatus.FORBIDDEN);
        }

        //if customer is not already in database, then save it
        Customer customerInRepository = customerRepository.findByEmail(rentalForm.getEmail()).orElse(null);
        if(customerInRepository == null) {
            customerRepository.save(new Customer(rentalForm.getName().trim(), rentalForm.getEmail(), rentalForm.getPhoneNumber().trim()));
            customerInRepository = customerRepository.findByEmail(rentalForm.getEmail()).orElse(null);
        }

        //save the new Rental in database and return map with data for frontend consumption
        newRental.setCustomer(customerInRepository);
        newRental.setBike(availableBike);
        newRental.setStartDate(rentalForm.getStartDate());
        newRental.setAgreedDurationDays(rentalForm.getAgreedDurationDays());
        newRental.setBikeDailyPrice(priceListInstance.getBikePriceList().get(availableBike.getBikeType()));
        newRental.setExtraDailyPrice(priceListInstance.getBikePriceList().get("extraFee"));
        newRental.setActualEndDate(null);
        newRental.setFinalCost(null);
        rentalRepository.save(newRental);

        Map<String, Object> output = new LinkedHashMap<>();
        output.putAll(rentalMapper(newRental));
        return new ResponseEntity(output, HttpStatus.CREATED);
    }



    private Map<Boolean, ResponseEntity<Map<String, Object>>>  rentalFormValidation (RentalForm rentalForm) {
        Map<Boolean, ResponseEntity<Map<String, Object>>> output = new HashMap<>();

        //if rentalForm info is not complete, return error message
        if (rentalForm.getStartDate() == null || rentalForm.getAgreedDurationDays() == null
                || rentalForm.getBikeType() == null || rentalForm.getName() == null
                || rentalForm.getEmail() == null || rentalForm.getPhoneNumber() == null) {
            ResponseEntity response = new ResponseEntity(makeMap("error", "Please send all required information"), HttpStatus.BAD_REQUEST);
            output.put(false, response);
            return output;
        }

        // minimum check for email format
        if (!rentalForm.getEmail().contains("@") || rentalForm.getEmail().contains(" ")) {
            ResponseEntity response = new ResponseEntity(makeMap("error", "Invalid email format: must contain a @ sign and no spaces"), HttpStatus.FORBIDDEN);
            output.put(false, response);
            return output;
        }

        // minimum check for rental duration
        if (rentalForm.getAgreedDurationDays() < 1) {
            ResponseEntity response = new ResponseEntity(makeMap("error", "Rental duration must be at least one day"), HttpStatus.FORBIDDEN);
            output.put(false, response);
            return output;
        }

        //if startDate is in the past, send error message
        if(rentalForm.getStartDate().isBefore(LocalDate.now())) {
            ResponseEntity response = new ResponseEntity(makeMap("error", "Rental start date is in the past"), HttpStatus.FORBIDDEN);
            output.put(false, response);
            return output;
        }

        ResponseEntity response = new ResponseEntity(makeMap("valid", "Form ok"), HttpStatus.OK);
        output.put(true, response);
        return output;

    }


    private Boolean isAvailable(Bike oneBike, LocalDate reqStartRentalDate, int reqRentalDurationDays) {
        LocalDate reqEndRentalDate = reqStartRentalDate.plusDays(reqRentalDurationDays - 1);
        Set<Rental> singleBikeRentals = oneBike.getRentalsPerBike();

        // bike is available if all its existing bookings end before the required start date,
        // or start after the required end date
        return singleBikeRentals.stream().allMatch(oneRental -> oneRental.getExpectedEndDate().isBefore(reqStartRentalDate)
        || oneRental.getStartDate().isAfter(reqEndRentalDate));
    }


    private Map<String, Object> makeMap(String key, Object value) {
        Map<String, Object> output = new HashMap<>();
        output.put(key, value);
        return output;
    }


    // ----- RETURN ENDPOINT -----
    @RequestMapping(value="/return/{rentalId}", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> returnBike(@PathVariable Long rentalId) {

        //setup
        Rental rentalInRepository = rentalRepository.findById(rentalId).orElse(null);

        //if no Rental is found, send an error message
        if (rentalInRepository == null) {
            return new ResponseEntity(makeMap("error", "Reservation not found"),
                    HttpStatus.NOT_FOUND);
        }

        //if bike return has been already handled, send an error message
        if(rentalInRepository.getActualEndDate() != null) {
            return new ResponseEntity(makeMap("error", "Bike was already returned"), HttpStatus.FORBIDDEN);
        }

        //calculate actual rental duration; avoid negative extra days if returned earlier
        LocalDate returnDay = LocalDate.now();
        LocalDate rentalStartDate = rentalInRepository.getStartDate();
        Integer actualRentalDurationDays = Period.between(rentalStartDate, returnDay.plusDays(1)).getDays();
        if(returnDay.isBefore(rentalStartDate)) {
            return new ResponseEntity(makeMap("error", "rental end date cannot be before start date"), HttpStatus.FORBIDDEN);
        }
        Integer extraDays = actualRentalDurationDays - rentalInRepository.getAgreedDurationDays();
        if(extraDays < 0) {
            extraDays = 0;
        }

        //calculate final price; if bike is returned earlier than agreed, there is no discount (bargain with the owner!)
        Double bikePricePerDay = rentalInRepository.getBikeDailyPrice();
        Double extraPricePerDay = rentalInRepository.getExtraDailyPrice();
        Double finalCost = (actualRentalDurationDays * bikePricePerDay) + (extraDays * extraPricePerDay);
        if(finalCost < rentalInRepository.getUpfrontPayment()) {
            finalCost = rentalInRepository.getUpfrontPayment();
        }

        // update RentalRepository and send information to front-end
        rentalInRepository.setActualEndDate(returnDay);
        rentalInRepository.setFinalCost(finalCost);
        rentalRepository.save(rentalInRepository);

        Map<String, Object> output = new LinkedHashMap<>();
        output.putAll(rentalMapper(rentalInRepository));
        return new ResponseEntity(output, HttpStatus.OK);

    }

    private Map<String,Object> rentalMapper (Rental rental) {
        Map<String, Object> output = new LinkedHashMap<>();

        output.put("rental_id", rental.getId());
        output.put("customer", customerMapper(rental.getCustomer()));
        output.put("bike", bikeMapper(rental.getBike()));
        output.put("bike_daily_price", rental.getBikeDailyPrice());
        output.put("extra_daily_price", rental.getExtraDailyPrice());
        output.put("agreed_duration_days", rental.getAgreedDurationDays());
        output.put("upfront_payment", rental.getUpfrontPayment());
        output.put("start_date", rental.getStartDate().toString());
        output.put("expected_end_date", rental.getExpectedEndDate());
        if(rental.getActualEndDate() !=  null) {
            output.put("actual_end_date", rental.getActualEndDate().toString());
        } else { output.put("actual_end_date", null); }
        if (rental.getFinalCost() != null) {
            output.put("final_cost", rental.getFinalCost());
        } else { output.put("final_cost", null); }

        return output;
    }

    private Map<String, Object> customerMapper(Customer customer) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("id", customer.getId());
        output.put("name", customer.getName());
        output.put("email", customer.getEmail());
        output.put("phone_number", customer.getPhoneNumber());
        return output;
    }

    private Map<String, Object> bikeMapper(Bike bike) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("id", bike.getId().toString());
        output.put("type", bike.getBikeType());
        return output;
    }

    /* ----- ALTERNATIVE ENDPOINT FOR BIKE RETURN -----
       // more suitable if there is no endpoint for retrieving a list of all rentals (can't easily get rentalId)
    @RequestMapping(value="/return", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> returnBike(@RequestBody RentalForm returnForm) {

    Rental rentalInRepository = rentalRepository.findByStartDateAndBike_id(returnForm.getStartDate(), returnForm.getBikeId());

     // (... proceeds similarly to the existing method)
    }
     */


    // ----- ALL RENTALS VIEW ENDPOINT -----
    @RequestMapping(value="/rentals", method = RequestMethod.GET)
    public ResponseEntity<List<Map<String, Object>>> viewAllRentals() {
        List<Map<String, Object>> allRentals = rentalRepository.findAll().stream()
                .sorted(Comparator.comparingLong(Rental::getId))
                .map(oneRental -> rentalMapper(oneRental))
                .collect(Collectors.toList());
        return new ResponseEntity(allRentals, HttpStatus.OK);
    }

    // ----- REMOVE SINGLE RENTAL ENDPOINT
    @RequestMapping(value="/rentals/{rentalId}/delete", method = RequestMethod.DELETE)
    public ResponseEntity<Map<String, Object>> removeOneRental(@PathVariable Long rentalId) {
        Rental requestedRental = rentalRepository.findById(rentalId).orElse(null);
        if(requestedRental == null) {
            return  new ResponseEntity(makeMap("error", "Requested Rental not found"), HttpStatus.NOT_FOUND);
        }
        rentalRepository.delete(requestedRental);
        return new ResponseEntity(makeMap("success", "Requested Rental deleted from database"), HttpStatus.OK);
    }
}
