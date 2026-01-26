package bello.antonio.carrier_management_service.repositories;

import bello.antonio.carrier_management_service.domain.Vehicle;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends MongoRepository<Vehicle, String> {

    Optional<Vehicle> findByVehicleName(String vehicleName);
    boolean existsByVehicleName(String vehicleName);
    List<Vehicle> findByMaxWeightGreaterThanAndWidthGreaterThanAndHeightGreaterThanAndLengthGreaterThanAndRefrigerated(
            float maxWeight, int width, int height, int length, boolean refrigerated
    );



}
