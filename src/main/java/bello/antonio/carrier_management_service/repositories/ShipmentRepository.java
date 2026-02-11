package bello.antonio.carrier_management_service.repositories;

import bello.antonio.carrier_management_service.domain.Shipment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ShipmentRepository extends MongoRepository<Shipment, String> {

    List<Shipment> findByVehicleName(String vehicleName);
    List<Shipment> findByIdTrip(String idTrip);
    void deleteAllByVehicleName(String vehicleName);
    void deleteByVehicleName(String vehicleName);

}
