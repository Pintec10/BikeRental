package test.bikerental;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.Date;

@SpringBootApplication
public class BikeRentalApplication {

	public static void main(String[] args) {
		SpringApplication.run(BikeRentalApplication.class, args);
	}

	//TESTBED
	@Bean
	public CommandLineRunner initTestData (BikeRepository bikeRepository, CustomerRepository customerRepository,
										   RentalRepository rentalRepository) {
		return (args) -> {
			Bike bk01 = new Bike(Bike.BikeType.NORMAL);
			Bike bk02 = new Bike(Bike.BikeType.MOUNTAIN);
			Bike bk03 = new Bike(Bike.BikeType.MOUNTAIN);
			bikeRepository.save(bk01);
			bikeRepository.save(bk02);
			bikeRepository.save(bk03);

			Customer cs01 = new Customer("Bartek Gabryelczyk", "bartek@gmail.com", "+358123456");
			Customer cs02 = new Customer("Timo Moisio", "timo@gmail.com", "+358654321");
			customerRepository.save(cs01);
			customerRepository.save(cs02);

			Date d01 = new Date();
			Date d02 = Date.from(d01.toInstant().plusSeconds(60*60*24));
			Date d03 = Date.from(d02.toInstant().plusSeconds(60*60*25));
			Rental rn01 = new Rental(d01, 1, 10.0, bk01, cs01);
			Rental rn02 = new Rental(d02, 1, 12.0, bk02, cs02);
			Rental rn03 = new Rental(d03, 3, 36.0, bk02, cs01);
			rentalRepository.save(rn01);
			rentalRepository.save(rn02);
			rentalRepository.save(rn03);
		};
	}
}
