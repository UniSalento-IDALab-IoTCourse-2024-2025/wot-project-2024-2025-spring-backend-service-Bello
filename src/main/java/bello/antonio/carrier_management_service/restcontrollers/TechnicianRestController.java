package bello.antonio.carrier_management_service.restcontrollers;

import bello.antonio.carrier_management_service.domain.Notification;
import bello.antonio.carrier_management_service.domain.Telemetry;
import bello.antonio.carrier_management_service.dto.ApiResponseDTO;
import bello.antonio.carrier_management_service.repositories.NotificationRepository;
import bello.antonio.carrier_management_service.repositories.TelemetryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/carrier")
public class TechnicianRestController {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TelemetryRepository telemetryRepository;

    @GetMapping("/notifications")
    public ResponseEntity<ApiResponseDTO<List<Notification>>> getNotifications() {
        List<Notification> notifications = notificationRepository.findAll();
        return ResponseEntity.ok(
                new ApiResponseDTO<>("Notifications retrieved successfully", 200, notifications)
        );
    }

    @PostMapping("/notifications/read/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> markAsRead(@PathVariable String id) {
        Optional<Notification> notificationOpt = notificationRepository.findById(id);

        if (notificationOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDTO<>("Notification not found", 404, null));
        }

        Notification notification = notificationOpt.get();
        notification.setRead(true);
        notificationRepository.save(notification);

        return ResponseEntity.ok(
                new ApiResponseDTO<>("Notification marked as read", 200, null)
        );
    }

    @GetMapping("/telemetry/{vehicleName}")
    public ResponseEntity<ApiResponseDTO<List<Telemetry>>> getTelemetryHistory(
            @PathVariable String vehicleName) {

        List<Telemetry> telemetryList = telemetryRepository
                .findByVehicleNameOrderByTimestampDesc(vehicleName);

        if (telemetryList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDTO<>("No telemetry found for " + vehicleName, 404, null));
        }

        return ResponseEntity.ok(
                new ApiResponseDTO<>("Telemetry retrieved successfully", 200, telemetryList)
        );
    }
}
