package test.bikerental;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Price {

    //fields
    @Id
    private String bikeType;

    private Double pricePerDay;


    //constructors
    public Price(){};
    public Price(String bikeType, Double pricePerDay) {
        this.bikeType = bikeType;
        this.pricePerDay = pricePerDay;
    }

    //methods
    public String getBikeType() { return bikeType; }
    public Double getPricePerDay() { return pricePerDay; }
    public void setBikeType(String bikeType) { this.bikeType = bikeType; }
    public void setPricePerDay(Double pricePerDay) { this.pricePerDay = pricePerDay; }
}
