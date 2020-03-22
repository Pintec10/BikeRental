package test.bikerental;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

@RepositoryRestResource
public interface PriceRepository extends JpaRepository <Price, String> {
    Optional<Price> findByBikeType(String bikeType);
}
