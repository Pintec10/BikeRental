package test.bikerental;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import java.util.Set;

@RepositoryRestResource
public interface BikeRepository extends JpaRepository<Bike, Long> {
    Set<Bike> findByBikeType (String bikeType);
}
