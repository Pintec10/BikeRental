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

    //constructors
    public RentalForm(){};
    public RentalForm (String name, String email, String phoneNumber, String bikeType, LocalDate startDate, Integer agreedDurationDays){
        this.name= name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.bikeType = bikeType;
        this.startDate = startDate;
        this.agreedDurationDays = agreedDurationDays;
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
}
