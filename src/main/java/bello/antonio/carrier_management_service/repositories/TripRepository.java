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
            // join con trips
            "{ $lookup: { from: 'trips', localField: 'vehicleName', foreignField: 'vehicleName', as: 'trips' } }",

            // tieni solo veicoli con almeno un trip
            "{ $match: { 'trips.0': { $exists: true } } }",

            // esplodi array trips
            "{ $unwind: '$trips' }",

            // filtro arrivalDate >= shipment.arrivalDate
            "{ $match: { 'trips.arrivalDate': { $gte: ?0 } } }",

            // join con shipment
            "{ $lookup: { from: 'shipment', localField: 'vehicleName', foreignField: 'vehicleName', as: 'shipments' } }",

            // esplodi shipments
            "{ $unwind: '$shipments' }",

            // group per veicolo + trip
            "{ $group: { " +
                    "_id: { tripId: '$trips._id', vehicleName: '$vehicleName' }, " +
                    "trip: { $first: '$trips' }, " +
                    "minPrice: { $min: '$shipments.price' } " +
                    "} }",

            // dimezza il prezzo
            "{ $addFields: { " +
                    "'trip.price': { $divide: ['$minPrice', 2] }, " +
                    "'trip.scheduled': true " +
                    "} }",


            // sostituisci root con trip
            "{ $replaceRoot: { newRoot: '$trip' } }"
    })
    List<Trip> findBusyTrips(Date arrivalDate);


}

