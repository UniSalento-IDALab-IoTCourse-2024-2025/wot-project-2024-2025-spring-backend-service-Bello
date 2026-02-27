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
            //  Join con vehicles per ottenere refrigerated
            "{ $lookup: { " +
                    "from: 'vehicle', " +
                    "localField: 'vehicleName', " +
                    "foreignField: 'vehicleName', " +
                    "as: 'vehicle' " +
                    "} }",

            // Esplodi l'array vehicle
            "{ $unwind: '$vehicle' }",

            //  Join con shipment per ottenere dimensioni e prezzo
            "{ $lookup: { " +
                    "from: 'shipment', " +
                    "localField: 'vehicleName', " +
                    "foreignField: 'vehicleName', " +
                    "as: 'shipments' " +
                    "} }",

            //  Esplodi shipments per calcolare il volume
            "{ $unwind: '$shipments' }",

            //  Filtra per requisiti (peso, refrigerated, arrivalDate, volume)
            "{ $match: { " +
                    "'remainingWeight': { $gte: ?0 }, " +
                    "'vehicle.refrigerated': { $eq: ?1 }, " +
                    "'arrivalDate': { $lte: ?2 }, " +
                    "$expr: { $gte: ['$remainingVolume', { $multiply: ['$shipments.width', '$shipments.height', '$shipments.length'] }] } " +
                    "} }",

            //  Calcola il prezzo minimo e dimezzalo
            "{ $addFields: { " +
                    "'price': { $divide: [{ $min: '$shipments.price' }, 2] }, " +
                    "'scheduled': true " +
                    "} }",

            //  Rimuovi i campi temporanei
            "{ $project: { 'vehicle': 0, 'shipments': 0 } }"
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

