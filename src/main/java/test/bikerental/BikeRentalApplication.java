package test.bikerental;

import org.apache.tomcat.jni.Local;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.EnumMap;

@SpringBootApplication
public class BikeRentalApplication {

	public static void main(String[] args) {
		SpringApplication.run(BikeRentalApplication.class, args);
	}


	@Bean
	public CommandLineRunner initTestData (BikeRepository bikeRepository, CustomerRepository customerRepository,
										   RentalRepository rentalRepository, PriceListRepository priceListRepository) {
		return (args) -> {

			// INITIALIZE STORE DATA

			//populates PriceListRepository with a single instance, only the first time
			if (priceListRepository.count() == 0) {
				System.out.println("initiating PriceList");
				PriceList priceList = new PriceList();
				priceList.addOrUpdatePriceListItem("normal", 10.0);
				priceList.addOrUpdatePriceListItem("mountain", 12.0);
				priceListRepository.save(priceList);
			}

			// populates BikeRepository only the first time
			if (bikeRepository.count() == 0) {
				System.out.println("initiating Bike repository");
				Bike bk01 = new Bike("normal");
				Bike bk02 = new Bike("normal");
				Bike bk03 = new Bike("normal");
				Bike bk04 = new Bike("mountain");
				Bike bk05 = new Bike("mountain");
				bikeRepository.save(bk01);
				bikeRepository.save(bk02);
				bikeRepository.save(bk03);
				bikeRepository.save(bk04);
				bikeRepository.save(bk05);


			// ----- TESTBED FROM HERE - ONLY FOR TESTING PURPOSES. DELETE IN PRODUCTION
			Customer cs01 = new Customer("Bartek Gabryelczyk", "bartek@gmail.com", "+358123456");
			Customer cs02 = new Customer("Timo Moisio", "timo@gmail.com", "+358654321");
			customerRepository.save(cs01);
			customerRepository.save(cs02);


			LocalDate d01 = LocalDate.now();
			LocalDate d02 = d01.plusDays(1);
			LocalDate d03 = d01.plusDays(1);

			Rental rn01 = new Rental(d01, 1, 10.0, bk01, cs01);
			Rental rn02 = new Rental(d02, 1, 12.0, bk04, cs02);
			Rental rn03 = new Rental(d03, 3, 36.0, bk04, cs01);
			rentalRepository.save(rn01);
			rentalRepository.save(rn02);
			rentalRepository.save(rn03);
			// ----- TESTBED ENDS HERE
			}
		};
	}
}
