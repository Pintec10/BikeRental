package test.bikerental;

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Bike {
    public enum BikeType {
        NORMAL, MOUNTAIN
    }

    //fields
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;

    private BikeType bikeType;

    @OneToMany(mappedBy = "bike")
    private Set<Rental> rentalsPerBike = new HashSet<>();

    //constructors
    public Bike(){};

    public Bike(BikeType bikeType) {
        this.bikeType = bikeType;
    }


    //methods
    public Long getId() {
        return id;
    }
    public BikeType getBikeType() {
        return bikeType;
    }
    public Set<Rental> getRentalsPerBike() { return rentalsPerBike; }

    public void setBikeType (BikeType bikeType) {
        this.bikeType = bikeType;
    }

    @Override
    public String toString() {
        return "Bike id: " + id + ", Bike type: " + bikeType;
    }
}
