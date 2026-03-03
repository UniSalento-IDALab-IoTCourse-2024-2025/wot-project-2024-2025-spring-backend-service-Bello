package bello.antonio.carrier_management_service.repositories;

import bello.antonio.carrier_management_service.domain.Trip;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface TripRepository extends MongoRepository<Trip, String> {
    Optional<Trip> findByVehicleName(String vehicleName);
    void deleteAllByVehicleName(String vehicleName);

    @Aggregation(pipeline = {
            // Join con vehicles per ottenere refrigerated
            "{ $lookup: { " +
                    "from: 'vehicle', " +
                    "localField: 'vehicleName', " +
                    "foreignField: 'vehicleName', " +
                    "as: 'vehicle' " +
                    "} }",

            "{ $unwind: '$vehicle' }",

            // Filtra: peso, volume del NUOVO pacco, refrigerated, data
            "{ $match: { " +
                    "'remainingWeight': { $gte: ?0 }, " +
                    "$expr: { $gte: ['$remainingVolume', { $multiply: [?1, ?2, ?3] }] }, " +
                    "'vehicle.refrigerated': { $eq: ?4 }, " +
                    "'arrivalDate': { $lte: ?5 } " +
                    "} }",

            // Rimuovi il campo temporaneo
            "{ $project: { 'vehicle': 0 } }"
    })
    List<Trip> findBusyTrips(
            int weight,
            int width,
            int height,
            int length,
            boolean refrigerated,
            Date arrivalDate
    );

}

