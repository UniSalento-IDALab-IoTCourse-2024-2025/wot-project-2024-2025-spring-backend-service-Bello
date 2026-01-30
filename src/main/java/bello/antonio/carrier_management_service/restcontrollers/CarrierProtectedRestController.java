package bello.antonio.carrier_management_service.restcontrollers;

import bello.antonio.carrier_management_service.domain.Shipment;
import bello.antonio.carrier_management_service.domain.Trip;
import bello.antonio.carrier_management_service.domain.Vehicle;
import bello.antonio.carrier_management_service.dto.ApiResponseDTO;
import bello.antonio.carrier_management_service.dto.ShipmentDTO;
import bello.antonio.carrier_management_service.dto.TripDTO;
import bello.antonio.carrier_management_service.dto.VehicleDTO;
import bello.antonio.carrier_management_service.repositories.ShipmentRepository;
import bello.antonio.carrier_management_service.repositories.TripRepository;
import bello.antonio.carrier_management_service.repositories.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/carrier")
public class CarrierProtectedRestController {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @PostMapping("/addVehicle")
    public ResponseEntity<ApiResponseDTO<Vehicle>> postAddVehicle(@RequestBody VehicleDTO vehicleDTO) {
        try {

            if (vehicleRepository.existsByVehicleName(vehicleDTO.getVehicleName())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ApiResponseDTO<>("Vehicle name already exists", 409, null));
            }

            Vehicle vehicle = new Vehicle();
            vehicle.setVehicleName(vehicleDTO.getVehicleName());
            vehicle.setHeight(vehicleDTO.getHeight());
            vehicle.setLength(vehicleDTO.getLength());
            vehicle.setWidth(vehicleDTO.getWidth());
            vehicle.setRefrigerated(vehicleDTO.isRefrigerated());
            vehicle.setMaxWeight(vehicleDTO.getMaxWeight());
            vehicle.setPricePerKm(vehicleDTO.getPricePerKm());

            vehicleRepository.save(vehicle);

            return ResponseEntity.ok(
                    new ApiResponseDTO<>("New vehicle added successfully", 200, vehicle)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>("Internal server error", 500, null));
        }
    }

    @GetMapping("/vehicles")
    public ResponseEntity<ApiResponseDTO<List<VehicleDTO>>> getAllVehicles() {
        List<VehicleDTO> vehicles = vehicleRepository.findAll().stream().map(v -> {
            VehicleDTO dto = new VehicleDTO();
            dto.setVehicleName(v.getVehicleName());
            dto.setHeight(v.getHeight());
            dto.setLength(v.getLength());
            dto.setWidth(v.getWidth());
            dto.setRefrigerated(v.isRefrigerated());
            dto.setMaxWeight(v.getMaxWeight());
            dto.setPricePerKm(v.getPricePerKm());
            return dto;
        }).toList();

        return ResponseEntity.ok(new ApiResponseDTO<>("List of vehicles", 200, vehicles));
    }


    @PostMapping("/deleteVehicle")
    public ResponseEntity<ApiResponseDTO> deleteVehicleCascade(@RequestBody VehicleDTO vehicleDTO) {
        String vehicleName = vehicleDTO.getVehicleName();
        if (vehicleName == null || vehicleName.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseDTO("Missing vehicleName in request body", 400, null));
        }

        // 1️⃣ Cancella dagli shipment
        shipmentRepository.deleteAllByVehicleName(vehicleName);

        // 2️⃣ Cancella dai trip
        tripRepository.deleteAllByVehicleName(vehicleName);

        // 3️⃣ Cancella dal vehicle
        Optional<Vehicle> vehicleOpt = vehicleRepository.findByVehicleName(vehicleName);
        if (vehicleOpt.isPresent()) {
            vehicleRepository.deleteById(vehicleOpt.get().getId());
            return ResponseEntity.ok(new ApiResponseDTO("Vehicle and related shipments/trips deleted successfully", 200, null));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDTO("Vehicle not found", 404, null));
        }
    }

    @GetMapping("/trips")
    public ResponseEntity<ApiResponseDTO<List<TripDTO>>> getAllTrips() {
        List<Trip> trips = tripRepository.findAll();

        List<TripDTO> tripDTOs = trips.stream().map(trip -> {
            TripDTO dto = new TripDTO();
            dto.setId(trip.getId());
            dto.setVehicleName(trip.getVehicleName());
            dto.setDepartureLatLng(trip.getDepartureLatLng());
            dto.setArrivalLatLng(trip.getArrivalLatLng());
            dto.setPathPolyline(trip.getPathPolyline());
            dto.setDuration(trip.getDuration());
            dto.setDistanceKm(trip.getDistanceKm());
            dto.setStarted(trip.isStarted());
            dto.setScheduled(trip.isScheduled());
            dto.setArrivalDate(trip.getArrivalDate());
            dto.setPrice(trip.getPrice());
            dto.setRefrigerated(trip.isRefrigerated());
            dto.setRemainingWeight(trip.getRemainingWeight());
            dto.setRemainingWidth(trip.getRemainingWidth());
            dto.setRemainingHeight(trip.getRemainingHeight());
            dto.setRemainingLength(trip.getRemainingLength());
            return dto;
        }).toList();

        ApiResponseDTO<List<TripDTO>> response =
                new ApiResponseDTO<>("Trips retrieved successfully", 200, tripDTOs);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/deleteTrip")
    public ResponseEntity<ApiResponseDTO<Void>> deleteTrip(@RequestBody TripDTO tripDTO) {
        String vehicleName = tripDTO.getVehicleName();
        if (vehicleName == null || vehicleName.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseDTO<>("Missing vehicleName in request body", 400, null));
        }

        // 1️⃣ Elimina tutti gli shipment con questo vehicleName
        List<Shipment> shipments = shipmentRepository.findByVehicleName(vehicleName);
        if (!shipments.isEmpty()) {
            shipmentRepository.deleteAll(shipments);
        }

        // 2️⃣ Elimina il trip con questo vehicleName
        Optional<Trip> tripOpt = tripRepository.findByVehicleName(vehicleName);
        if (tripOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDTO<>("Trip not found", 404, null));
        }
        tripRepository.deleteById(tripOpt.get().getId());

        return ResponseEntity.ok(new ApiResponseDTO<>("Trip and related shipments deleted successfully", 200, null));
    }

    @PostMapping("/shipmentsByTrip")
    public ResponseEntity<ApiResponseDTO<List<ShipmentDTO>>> getShipmentsByTrip(
            @RequestBody TripDTO tripDTO) {

        String vehicleName = tripDTO.getVehicleName();

        List<ShipmentDTO> shipments = shipmentRepository
                .findByVehicleName(vehicleName)
                .stream()
                .map(s -> {
                    ShipmentDTO dto = new ShipmentDTO();
                    dto.setId(s.getId());
                    dto.setDepartureAddress(s.getDepartureAddress());
                    dto.setArrivalAddress(s.getArrivalAddress());
                    dto.setDepartureLatLng(s.getDepartureLatLng());
                    dto.setArrivalLatLng(s.getArrivalLatLng());
                    dto.setDuration(s.getDuration());
                    dto.setDistanceKm(s.getDistanceKm());
                    dto.setPrice(s.getPrice());
                    dto.setWeight(s.getWeight());
                    dto.setWidth(s.getWidth());
                    dto.setHeight(s.getHeight());
                    dto.setLength(s.getLength());
                    dto.setRefrigerated(s.isRefrigerated());
                    dto.setArrivalDate(s.getArrivalDate());
                    dto.setVehicleName(s.getVehicleName());
                    return dto;
                })
                .toList();

        return ResponseEntity.ok(
                new ApiResponseDTO<>("Shipments retrieved successfully", 200, shipments)
        );
    }

    @PostMapping("/deleteShipment")
    public ResponseEntity<ApiResponseDTO<Void>> deleteShipment(
            @RequestBody ShipmentDTO shipmentDTO) {

        String vehicleName = shipmentDTO.getVehicleName();

        if (vehicleName == null || vehicleName.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseDTO<>("Missing vehicleName", 400, null));
        }

        shipmentRepository.deleteByVehicleName(vehicleName);

        return ResponseEntity.ok(
                new ApiResponseDTO<>("Shipments deleted successfully", 200, null)
        );
    }


}


