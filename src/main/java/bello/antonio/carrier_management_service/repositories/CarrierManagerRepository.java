package bello.antonio.carrier_management_service.repositories;

import bello.antonio.carrier_management_service.domain.CarrierManager;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CarrierManagerRepository extends MongoRepository<CarrierManager, String> {

    Optional<CarrierManager> findByEmail(String email);
    Optional<CarrierManager> findFirstByOrderByIdAsc();
}
