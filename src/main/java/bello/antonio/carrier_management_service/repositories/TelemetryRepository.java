package bello.antonio.carrier_management_service.repositories;

import bello.antonio.carrier_management_service.domain.Telemetry;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface TelemetryRepository extends MongoRepository<Telemetry, String> {
    List<Telemetry> findByVehicleNameOrderByTimestampDesc(String vehicleName);
}
