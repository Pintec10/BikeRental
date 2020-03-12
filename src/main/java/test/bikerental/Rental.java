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
    private LocalDate actualEndDate;
    private Integer agreedDurationDays;
    private Double finalCost;
    private Double upfrontPayment;

    @ManyToOne
    @JoinColumn(name="bike_id")
    private Bike bike;

    @ManyToOne
    @JoinColumn(name="customer_id")
    private Customer customer;


    //constructors
    public Rental(){};
    public Rental(LocalDate startDate, int agreedDurationDays, Double upfrontPayment) {
        this.startDate = startDate;
        this.agreedDurationDays = agreedDurationDays;
        this.upfrontPayment = upfrontPayment;
    }
    public Rental(LocalDate startDate, int agreedDurationDays, Double upfrontPayment, Bike bike, Customer customer) {
        this.startDate = startDate;
        this.agreedDurationDays = agreedDurationDays;
        this.upfrontPayment = upfrontPayment;
        this.bike = bike;
        this.customer = customer;
    }


    //methods
    public Long getId(){ return id;}
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getActualEndDate() { return actualEndDate; }
    public Integer getAgreedDurationDays() { return agreedDurationDays; }
    public LocalDate getExpectedEndDate() {
        return startDate.plusDays(agreedDurationDays);
    }
    public Double getFinalCost() { return finalCost; }
    public Double getUpfrontPayment() { return upfrontPayment; }
    public Bike getBike() { return bike; }
    public Customer getCustomer() { return customer; }

    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setActualEndDate(LocalDate actualEndDate) { this.actualEndDate = actualEndDate; }
    public void setAgreedDurationDays(Integer agreedDurationDays) { this.agreedDurationDays = agreedDurationDays; }
    public void setFinalCost(Double finalCost) { this.finalCost = finalCost; }
    public void setUpfrontPayment(Double upfrontPayment) { this.upfrontPayment = upfrontPayment; }
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
