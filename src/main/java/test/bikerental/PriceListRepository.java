package test.bikerental;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface PriceListRepository extends JpaRepository<PriceList, Long> {

}