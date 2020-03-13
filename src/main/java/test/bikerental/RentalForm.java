package test.bikerental;

import java.time.LocalDate;

public class RentalForm {


    //fields
    private String name;
    private String email;
    private String phoneNumber;
    private String bikeType;
    private LocalDate startDate;
    private Integer agreedDurationDays;
    private Long bikeId;


    //constructors
    public RentalForm(){};
    public RentalForm (String name, String email, String phoneNumber, String bikeType, LocalDate startDate, Integer agreedDurationDays, Long bikeId){
        this.name= name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.bikeType = bikeType;
        this.startDate = startDate;
        this.agreedDurationDays = agreedDurationDays;
        // 'bikeId' field is needed only if the alternative Bike Return endpoint is used
        /*this.bikeId = bikeId;*/
    }


    //methods
    public String getName() {
        return name;
    }
    public String getEmail() {
        return email;
    }
    public String getPhoneNumber() {
        return phoneNumber;
    }
    public String getBikeType() {
        return bikeType;
    }
    public LocalDate getStartDate() {
        return startDate;
    }
    public Integer getAgreedDurationDays() {
        return agreedDurationDays;
    }
    /*public Long getBikeId() { return bikeId; }*/
}
