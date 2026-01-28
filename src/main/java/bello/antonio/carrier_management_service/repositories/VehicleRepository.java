package bello.antonio.carrier_management_service.repositories;

import bello.antonio.carrier_management_service.domain.Vehicle;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends MongoRepository<Vehicle, String> {

    Optional<Vehicle> findByVehicleName(String vehicleName);
    boolean existsByVehicleName(String vehicleName);
    List<Vehicle> findByMaxWeightGreaterThanAndWidthGreaterThanAndHeightGreaterThanAndLengthGreaterThanAndRefrigerated(
            float maxWeight, int width, int height, int length, boolean refrigerated
    );
    void deleteAllByVehicleName(String vehicleName);

    @Aggregation(pipeline = {
            "{ $lookup: { from: 'trip', localField: 'vehicleName', foreignField: 'vehicleName', as: 'trips' } }",
            "{ $match: { 'trips.0': { $exists: true } } }"
    })
    List<Vehicle> findVehiclesWithTrips();

    @Aggregation(pipeline = {
            // 1️⃣ Filtro veicoli per dimensioni / peso / refrigerazione
            "{ $match: { " +
                    "maxWeight: { $gte: ?0 }, " +
                    "width: { $gte: ?1 }, " +
                    "height: { $gte: ?2 }, " +
                    "length: { $gte: ?3 }, " +
                    "refrigerated: { $eq: ?4 } " +
                    "} }",

            // 2️⃣ Join con trips
            "{ $lookup: { " +
                    "from: 'trip', " +
                    "localField: 'vehicleName', " +
                    "foreignField: 'vehicleName', " +
                    "as: 'trips' " +
                    "} }",

            // 3️⃣ Solo veicoli senza trip
            "{ $match: { 'trips.0': { $exists: false } } }"
    })
    List<Vehicle> findAvailableVehicles(
            double weight, int width, int height, int length, boolean refrigerated
    );

}
