package bello.antonio.carrier_management_service.repositories;

import bello.antonio.carrier_management_service.domain.Trip;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TripRepository extends MongoRepository<Trip, String> {
    Optional<Trip> findByVehicleName(String vehicleName);
    void deleteAllByVehicleName(String vehicleName);
}

