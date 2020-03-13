package test.bikerental;

import org.apache.tomcat.jni.Local;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.time.LocalDate;


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
				priceList.addOrUpdatePriceListItem("extraFee", 3.0);
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
			/*Customer cs01 = new Customer("Bartek Gabryelczyk", "bartek@gmail.com", "+358123456");
			Customer cs02 = new Customer("Timo Moisio", "timo@gmail.com", "+358654321");
			customerRepository.save(cs01);
			customerRepository.save(cs02);


			LocalDate d01 = LocalDate.now().minusDays(3);
			LocalDate d02 = d01.plusDays(1);
			LocalDate d03 = d02.plusDays(1);

			Rental rn01 = new Rental(d01, 4, 10.0, 3.0, 40.0, bk01, cs01, null, null);
			Rental rn02 = new Rental(d02, 1, 12.0, 3.0, 12.0, bk04, cs02, null, null);
			Rental rn03 = new Rental(d03, 5, 12.0, 3.0, 60.0, bk04, cs01, null, null);
			Rental rn04 = new Rental(d01, 2, 10.0, 3.0, 20.0, bk02, cs01, null, null);
			Rental rn05 = new Rental(d02, 4, 12.0, 3.0, 40.0, bk05, cs02, null, null);
			rentalRepository.save(rn01);
			rentalRepository.save(rn02);
			rentalRepository.save(rn03);
			rentalRepository.save(rn04);
			rentalRepository.save(rn05);*/
			// ----- TESTBED ENDS HERE

			}
		};
	}
}
