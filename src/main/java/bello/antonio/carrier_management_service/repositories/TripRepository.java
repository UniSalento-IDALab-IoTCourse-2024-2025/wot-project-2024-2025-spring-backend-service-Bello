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
            // 1️⃣ Join con vehicles per ottenere refrigerated
            "{ $lookup: { " +
                    "from: 'vehicle', " +
                    "localField: 'vehicleName', " +
                    "foreignField: 'vehicleName', " +
                    "as: 'vehicle' " +
                    "} }",

            // 2️⃣ Esplodi l'array vehicle
            "{ $unwind: '$vehicle' }",

            // 3️⃣ Filtra per requisiti (dimensioni/peso dal trip, refrigerated dal vehicle)
            "{ $match: { " +
                    "'remainingWeight': { $gte: ?0 }, " +
                    "'remainingWidth': { $gte: ?1 }, " +
                    "'remainingHeight': { $gte: ?2 }, " +
                    "'remainingLength': { $gte: ?3 }, " +
                    "'vehicle.refrigerated': { $eq: ?4 }, " +
                    "'arrivalDate': { $lte: ?5 } " +
                    "} }",

            // 4️⃣ Join con shipment per calcolare il prezzo
            "{ $lookup: { " +
                    "from: 'shipment', " +
                    "localField: 'vehicleName', " +
                    "foreignField: 'vehicleName', " +
                    "as: 'shipments' " +
                    "} }",

            // 5️⃣ Calcola il prezzo minimo e dimezzalo
            "{ $addFields: { " +
                    "'price': { $divide: [{ $min: '$shipments.price' }, 2] }, " +
                    "'scheduled': true " +
                    "} }",

            // 6️⃣ Rimuovi i campi temporanei
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

