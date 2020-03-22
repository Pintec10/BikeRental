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
    PriceRepository priceRepository;


    // ----------- RENTAL ENDPOINT -----------
    @RequestMapping(value="/rent", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> createNewRental(@RequestBody RentalForm rentalForm) {

        //setup
        Rental newRental = new Rental();

        //form validation; if not ok, returns error message
        Map<Boolean, ResponseEntity<Map<String, Object>>> formIsValid = rentalFormValidation(rentalForm);
        if(formIsValid.containsKey(false)) {
            return formIsValid.get(false);
        }

        //looks for one random available bike of the required type; if bike is not available, return error message
        Bike availableBike = checkForAvailableBikes(rentalForm);
        if (availableBike == null) {
            return new ResponseEntity<>(makeMap("error", "No bikes available! Change/check bike type or time slot"), HttpStatus.FORBIDDEN);
        }

        //calculate and set bike daily price, possible extra fee for late return, and upfront payment
        Map<String,Double> upfrontRentalPrices = calculateCostsUpfront(rentalForm);
        newRental.setBikeDailyPrice(upfrontRentalPrices.get("requestedBikePrice"));
        newRental.setExtraDailyPrice(upfrontRentalPrices.get("extraPrice"));
        newRental.setUpfrontPayment(upfrontRentalPrices.get("upfrontPayment"));

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
        newRental.setActualEndDate(null);
        newRental.setFinalCost(null);
        rentalRepository.save(newRental);

        Map<String, Object> output = new LinkedHashMap<>();
        output.putAll(rentalMapper(newRental));
        return new ResponseEntity<>(output, HttpStatus.CREATED);
    }


    private Map<Boolean, ResponseEntity<Map<String, Object>>>  rentalFormValidation (RentalForm rentalForm) {
        Map<Boolean, ResponseEntity<Map<String, Object>>> output = new HashMap<>();

        //if rentalForm info is not complete, return error message
        if (rentalForm.getStartDate() == null || rentalForm.getAgreedDurationDays() == null
                || rentalForm.getBikeType() == null || rentalForm.getName() == null
                || rentalForm.getEmail() == null || rentalForm.getPhoneNumber() == null) {
            ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(makeMap("error",
                    "Please send all required information"), HttpStatus.BAD_REQUEST);
            output.put(false, response);
            return output;
        }

        // minimum check for email format
        if (!rentalForm.getEmail().contains("@") || rentalForm.getEmail().contains(" ")) {
            ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(makeMap("error",
                    "Invalid email format: must contain a @ sign and no spaces"), HttpStatus.FORBIDDEN);
            output.put(false, response);
            return output;
        }

        // minimum check for rental duration
        if (rentalForm.getAgreedDurationDays() < 1) {
            ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(makeMap("error",
                    "Rental duration must be at least one day"), HttpStatus.FORBIDDEN);
            output.put(false, response);
            return output;
        }

        //if startDate is in the past, send error message
        if(rentalForm.getStartDate().isBefore(LocalDate.now())) {
            ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(makeMap("error",
                    "Rental start date is in the past"), HttpStatus.FORBIDDEN);
            output.put(false, response);
            return output;
        }

        //if bike model is not present in the price list, return error
        if(!priceRepository.findByBikeType(rentalForm.getBikeType()).isPresent()) {
            ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(makeMap("error",
                    "This bike model is not in the price list"), HttpStatus.FORBIDDEN);
            output.put(false, response);
            return output;
        }

        //if extra price for late return is not set, return error
        if(!priceRepository.findByBikeType("extraFee").isPresent()) {
            ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(makeMap("error",
                    "The extra price for late return has not been set yet."), HttpStatus.FORBIDDEN);
            output.put(false, response);
            return output;
        }

        //if all is good, just return an OK response with "true" as key
        ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(makeMap("valid", "Form ok"), HttpStatus.OK);
        output.put(true, response);
        return output;
    }


    public Bike checkForAvailableBikes (RentalForm rentalForm) {
        //looks for any available bike in BikeRepository
        Bike availableBike = bikeRepository.findByBikeType(rentalForm.getBikeType()).stream()
                .filter(oneBike -> isAvailable(oneBike, rentalForm.getStartDate(),
                        rentalForm.getAgreedDurationDays())).findAny().orElse(null);
        return availableBike;
    }


    private Boolean isAvailable(Bike oneBike, LocalDate reqStartRentalDate, int reqRentalDurationDays) {
        LocalDate reqEndRentalDate = reqStartRentalDate.plusDays(reqRentalDurationDays - 1);
        Set<Rental> singleBikeRentals = oneBike.getRentalsPerBike();

        // bike is available if all its existing bookings end before the required start date,
        // or start after the required end date
        return singleBikeRentals.stream().allMatch(oneRental -> oneRental.getExpectedEndDate().isBefore(reqStartRentalDate)
        || oneRental.getStartDate().isAfter(reqEndRentalDate));
    }


    private Map<String, Double> calculateCostsUpfront(RentalForm rentalForm) {
        Price requestedBikePrice = priceRepository.findByBikeType(rentalForm.getBikeType()).orElse(null);
        Double upfrontPayment = requestedBikePrice.getPricePerDay() * rentalForm.getAgreedDurationDays();
        Price extraPrice = priceRepository.findByBikeType("extraFee").orElse(null);

        Map<String, Double> upfrontCosts = new HashMap<>();
        upfrontCosts.put("requestedBikePrice", requestedBikePrice.getPricePerDay());
        upfrontCosts.put("extraPrice", extraPrice.getPricePerDay());
        upfrontCosts.put("upfrontPayment", upfrontPayment);
        return upfrontCosts;
    }


    private Map<String, Object> makeMap(String key, Object value) {
        Map<String, Object> output = new HashMap<>();
        output.put(key, value);
        return output;
    }



    // ----------- RETURN ENDPOINT -----------
    @RequestMapping(value="/return/{rentalId}", method = RequestMethod.POST)
    public ResponseEntity<Map<String, Object>> returnBike(@PathVariable Long rentalId) {

        //setup
        Rental rentalInRepository = rentalRepository.findById(rentalId).orElse(null);

        //validate if conditions are ok for this return; if not, return error message
        Map<Boolean, ResponseEntity<Map<String, Object>>> returnIsValid = returnValidation(rentalInRepository);
        if(returnIsValid.containsKey(false)) {
            return returnIsValid.get(false);
        }

        //calculate final costs
        Double finalCost = calculateFinalCost(rentalInRepository);

        // update RentalRepository and send information to front-end
        rentalInRepository.setActualEndDate(LocalDate.now());
        rentalInRepository.setFinalCost(finalCost);
        rentalRepository.save(rentalInRepository);

        Map<String, Object> output = new LinkedHashMap<>();
        output.putAll(rentalMapper(rentalInRepository));
        return new ResponseEntity<>(output, HttpStatus.OK);

    }


    private Double calculateFinalCost(Rental rental) {
        Integer extraDays = Period.between(rental.getExpectedEndDate(), LocalDate.now()).getDays();

        //avoid negative extra days if returned earlier; no discounts for early return!
        if(extraDays < 0) {
            extraDays = 0;
        }

        Double finalCost = rental.getUpfrontPayment() +
                (rental.getExtraDailyPrice() + rental.getBikeDailyPrice()) * extraDays;
        return finalCost;
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


    private Map<Boolean, ResponseEntity<Map<String, Object>>>  returnValidation (Rental rental) {
        Map<Boolean, ResponseEntity<Map<String, Object>>> output = new HashMap<>();

        //if no Rental is found, send an error message
        if (rental == null) {
            ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(makeMap("error",
                    "Reservation not found"), HttpStatus.NOT_FOUND);
            output.put(false, response);
            return output;
        }

        //if bike return has been already handled, send an error message
        if(rental.getActualEndDate() != null) {
            ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(makeMap("error",
                    "Bike was already returned"), HttpStatus.FORBIDDEN);
            output.put(false, response);
            return output;
        }

        //if return date is before start date, send error: use delete rental endpoint instead to cancel booking
        if(LocalDate.now().isBefore(rental.getStartDate())) {
            ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(makeMap("error",
                    "rental end date cannot be before start date; cancel rental instead"), HttpStatus.FORBIDDEN);
            output.put(false, response);
            return output;
        }

        //if all is good, just return an OK response with "true" as key
        ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(makeMap("valid", "Form ok"), HttpStatus.OK);
        output.put(true, response);
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
        return new ResponseEntity<>(allRentals, HttpStatus.OK);
    }



    // ----- REMOVE SINGLE RENTAL ENDPOINT
    @RequestMapping(value="/rentals/{rentalId}/delete", method = RequestMethod.DELETE)
    public ResponseEntity<Map<String, Object>> removeOneRental(@PathVariable Long rentalId) {
        Rental requestedRental = rentalRepository.findById(rentalId).orElse(null);
        if(requestedRental == null) {
            return  new ResponseEntity<>(makeMap("error", "Requested Rental not found"), HttpStatus.NOT_FOUND);
        }
        rentalRepository.delete(requestedRental);
        return new ResponseEntity<>(makeMap("success", "Requested Rental deleted from database"), HttpStatus.OK);
    }
}
