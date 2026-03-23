package bello.antonio.carrier_management_service.restcontrollers;

import bello.antonio.carrier_management_service.domain.Notification;
import bello.antonio.carrier_management_service.domain.Telemetry;
import bello.antonio.carrier_management_service.dto.ApiResponseDTO;
import bello.antonio.carrier_management_service.repositories.NotificationRepository;
import bello.antonio.carrier_management_service.repositories.TelemetryRepository;
import bello.antonio.carrier_management_service.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/carrier")
public class TechnicianRestController {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TelemetryRepository telemetryRepository;

    @Autowired
    private ReportService reportService;

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
            @PathVariable String vehicleName,
            @RequestParam(required = false) Long from,
            @RequestParam(required = false) Long to) {

        List<Telemetry> telemetryList;

        if (from != null && to != null) {
            telemetryList = telemetryRepository
                    .findByVehicleNameAndTimestampBetweenOrderByTimestampAsc(
                            vehicleName, new java.util.Date(from), new java.util.Date(to));
        } else {
            telemetryList = telemetryRepository
                    .findByVehicleNameOrderByTimestampDesc(vehicleName);
        }

        if (telemetryList.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDTO<>("No telemetry found for " + vehicleName, 404, null));
        }

        return ResponseEntity.ok(
                new ApiResponseDTO<>("Telemetry retrieved successfully", 200, telemetryList)
        );
    }

    /* ═══════════════════════════════════════════════════════════════════════
       Report Endpoints
       ═══════════════════════════════════════════════════════════════════════ */

    /**
     * Operating Hours — vista mensile, tutti i veicoli
     * GET /api/carrier/reports/operating-hours/monthly?year=2026&month=3
     */
    @GetMapping("/reports/operating-hours/monthly")
    public ResponseEntity<ApiResponseDTO<List<Map<String, Object>>>> getOperatingHoursMonthly(
            @RequestParam int year,
            @RequestParam int month) {
        List<Map<String, Object>> data = reportService.getOperatingHoursMonthlyAll(year, month);
        return ResponseEntity.ok(new ApiResponseDTO<>("Operating hours report (monthly)", 200, data));
    }

    /**
     * Operating Hours — drill-down giornaliero, tutti i veicoli
     * GET /api/carrier/reports/operating-hours/daily?year=2026&month=3&day=22
     */
    @GetMapping("/reports/operating-hours/daily")
    public ResponseEntity<ApiResponseDTO<List<Map<String, Object>>>> getOperatingHoursDaily(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam int day) {
        List<Map<String, Object>> data = reportService.getOperatingHoursDailyAll(year, month, day);
        return ResponseEntity.ok(new ApiResponseDTO<>("Operating hours report (daily)", 200, data));
    }

    /**
     * Cumulative Hours — ore totali per veicolo in un periodo
     * GET /api/carrier/reports/cumulative-hours?from=1711929600000&to=1714521600000
     */
    @GetMapping("/reports/cumulative-hours")
    public ResponseEntity<ApiResponseDTO<List<Map<String, Object>>>> getCumulativeHours(
            @RequestParam Long from,
            @RequestParam Long to) {
        List<Map<String, Object>> data = reportService.getCumulativeHours(new Date(from), new Date(to));
        return ResponseEntity.ok(new ApiResponseDTO<>("Cumulative hours report", 200, data));
    }

    /**
     * Alert Report — anomalie per veicolo, ordinato per conteggio decrescente
     * GET /api/carrier/reports/alerts?year=2026&month=3&day=22&vehicleName=Truck1
     * month, day e vehicleName sono opzionali
     */
    @GetMapping("/reports/alerts")
    public ResponseEntity<ApiResponseDTO<List<Map<String, Object>>>> getAlertReport(
            @RequestParam int year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer day,
            @RequestParam(required = false) String vehicleName) {
        List<Map<String, Object>> data = reportService.getAlertReport(vehicleName, year, month, day);
        return ResponseEntity.ok(new ApiResponseDTO<>("Alert report", 200, data));
    }
}