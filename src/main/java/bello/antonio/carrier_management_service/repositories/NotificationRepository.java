package bello.antonio.carrier_management_service.repositories;

import bello.antonio.carrier_management_service.domain.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface NotificationRepository extends MongoRepository<Notification, String> {
    boolean existsByVehicleNameAndReadFalse(String vehicleName);
    Optional<Notification> findByVehicleNameAndReadFalse(String vehicleName);
}
