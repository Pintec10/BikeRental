package test.bikerental;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDate;

@Entity
public class Rental {

    //fields
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;

    private LocalDate startDate;
    private Integer agreedDurationDays;
    private Double bikeDailyPrice;
    private Double extraDailyPrice;
    private Double upfrontPayment;
    private LocalDate actualEndDate;
    private Double finalCost;


    @ManyToOne
    @JoinColumn(name="bike_id")
    private Bike bike;

    @ManyToOne
    @JoinColumn(name="customer_id")
    private Customer customer;


    //constructors
    public Rental(){};
    public Rental(LocalDate startDate, int agreedDurationDays, Double bikeDailyPrice, Double extraDailyPrice, Double upfrontPayment, Bike bike, Customer customer, LocalDate actualEndDate, Double finalCost) {
        this.startDate = startDate;
        this.agreedDurationDays = agreedDurationDays;
        this.bikeDailyPrice = bikeDailyPrice;
        this.extraDailyPrice = extraDailyPrice;
        this.upfrontPayment = upfrontPayment;
        this.bike = bike;
        this.customer = customer;
        this.actualEndDate = actualEndDate;
        this.finalCost = finalCost;
    }


    //methods
    public Long getId(){ return id;}
    public LocalDate getStartDate() { return startDate; }
    public Integer getAgreedDurationDays() { return agreedDurationDays; }
    public Double getBikeDailyPrice() { return bikeDailyPrice; }
    public Double getExtraDailyPrice() { return extraDailyPrice; }
    public Double getUpfrontPayment() { return upfrontPayment; }
    public LocalDate getActualEndDate() { return actualEndDate; }
    public Double getFinalCost() { return finalCost; }
    public Bike getBike() { return bike; }
    public Customer getCustomer() { return customer; }

    public LocalDate getExpectedEndDate() {
        return startDate.plusDays(agreedDurationDays -1);
    }

    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setAgreedDurationDays(Integer agreedDurationDays) { this.agreedDurationDays = agreedDurationDays; }
    public void setBikeDailyPrice(Double bikeDailyPrice) { this.bikeDailyPrice = bikeDailyPrice; }
    public void setExtraDailyPrice(Double extraDailyPrice) { this.extraDailyPrice = extraDailyPrice; }
    public void setUpfrontPayment(Double upfrontPayment) { this.upfrontPayment = upfrontPayment; }
    public void setActualEndDate(LocalDate actualEndDate) { this.actualEndDate = actualEndDate; }
    public void setFinalCost(Double finalCost) { this.finalCost = finalCost; }
    public void setBike(Bike bike) { this.bike = bike; }
    public void setCustomer(Customer customer) { this.customer = customer; }


    @Override
    public String toString(){
        return "Rental id: " + id + System.lineSeparator() +
                "Bike id: " + bike + System.lineSeparator() +
                "Customer id: " + customer + System.lineSeparator() +
                "Rental date: " + startDate;
    }
}
