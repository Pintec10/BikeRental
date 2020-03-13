package test.bikerental;

import org.hibernate.annotations.GenericGenerator;
import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity
public class PriceList {

    //fields
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Long id;

    @ElementCollection
    private Map<String, Double> bikePriceList = new HashMap<>();

    //constructors
    public PriceList(){};
    public PriceList(Map<String, Double> bikePriceList) {
        this.bikePriceList = bikePriceList;
    }


    //methods
    public Map<String, Double> getBikePriceList() {
        return bikePriceList;
    }

        // --> may allow to later change the priceList
    public void addOrUpdatePriceListItem(String bikeType, Double pricePerDay) {
        this.bikePriceList.put(bikeType, pricePerDay);
    }

        // --> may allow to later change the priceList
    public void removePriceListItem (String bikeType) {
        if (this.bikePriceList.containsKey(bikeType)) {
            this.bikePriceList.remove(bikeType);
        }
    }
}
