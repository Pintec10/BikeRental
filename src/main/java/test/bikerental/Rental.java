package test.bikerental;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

@Entity
public class Rental {

    //fields
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;

    private Date startDate;
    private Date endDate;
    private int agreedDurationDays;
    private Double finalCost;
    private Double sumPaid;

    @ManyToOne
    @JoinColumn(name="bike_id")
    private Bike bike;

    @ManyToOne
    @JoinColumn(name="customer_id")
    private Customer customer;


    //constructors
    public Rental(){};
    public Rental(Date startDate, int agreedDurationDays, Double sumPaid) {
        this.startDate = startDate;
        this.agreedDurationDays = agreedDurationDays;
        this.sumPaid = sumPaid;
    }
    public Rental(Date startDate, int agreedDurationDays, Double sumPaid, Bike bike, Customer customer) {
        this.startDate = startDate;
        this.agreedDurationDays = agreedDurationDays;
        this.sumPaid = sumPaid;
        this.bike = bike;
        this.customer = customer;
    }


    //methods
    public Long getId(){ return id;}
    public Date getStartDate() { return startDate; }
    public Date getEndDate() { return endDate; }
    public int getAgreedDurationDays() { return agreedDurationDays; }
    public Double getFinalCost() { return finalCost; }
    public Double getSumPaid() { return sumPaid; }
    public Bike getBike() { return bike; }
    public Customer getCustomer() { return customer; }

    public void setStartDate(Date startDate) { this.startDate = startDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }
    public void setAgreedDurationDays(int agreedDurationDays) { this.agreedDurationDays = agreedDurationDays; }
    public void setFinalCost(Double finalCost) { this.finalCost = finalCost; }
    public void setSumPaid(Double sumPaid) { this.sumPaid = sumPaid; }
    public void setBike(Bike bike) { this.bike = bike; }
    public void setCustomer(Customer customer) { this.customer = customer; }

    @Override
    public String toString(){
        return "Rental id: " + id + System.lineSeparator() +
                "Bike id: " + bike + System.lineSeparator() +
                //"Customer id: " + customer + System.lineSeparator() +
                "Rental date: " + startDate;
    }
}
