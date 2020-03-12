package test.bikerental;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.time.LocalDate;
import java.util.List;

@RepositoryRestResource
public interface RentalRepository extends JpaRepository<Rental, Long> {
    public Rental findByStartDateAndBike_id(LocalDate startDate, Long id);
}
