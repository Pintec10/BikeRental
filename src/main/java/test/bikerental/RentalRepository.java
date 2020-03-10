package test.bikerental;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface RentalRepository extends JpaRepository<Rental, Long> {
}