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
import bello.antonio.carrier_management_service.websocket.TelemetryWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/carrier")
public class ClientProtectedRestController {


    @Autowired
    private TelemetryWebSocketHandler telemetryHandler;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${FRIDGE_API_URL:http://fridge-streamer:8002}")
    private String FRIDGE_API_URL;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @PostMapping("/addVehicle")
    public ResponseEntity<ApiResponseDTO<Vehicle>> postAddVehicle(@RequestBody VehicleDTO vehicleDTO) {
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
    }

    @GetMapping("/vehicles")
    public ResponseEntity<ApiResponseDTO<List<VehicleDTO>>> getAllVehicles() {
        List<VehicleDTO> vehicles = vehicleRepository.findAll().stream().map(v -> {
            VehicleDTO dto = new VehicleDTO();
            dto.setId(v.getId());
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
        String idVehicle = vehicleDTO.getId();
        if (idVehicle == null || idVehicle.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseDTO("Missing vehicleName in request body", 400, null));
        }

        // Cancella dagli shipment
        shipmentRepository.deleteAllByVehicleName(vehicleDTO.getVehicleName());

        // Cancella dai trip
        tripRepository.deleteAllByVehicleName(vehicleDTO.getVehicleName());

        // Cancella dal vehicle
        Optional<Vehicle> vehicleOpt = vehicleRepository.findById(vehicleDTO.getId());
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
        String idTrip = tripDTO.getId();
        if (idTrip == null || idTrip.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseDTO<>("Missing id in request body", 400, null));
        }

        // Elimina tutti gli shipment con questo idTrip
        List<Shipment> shipments = shipmentRepository.findByIdTrip(idTrip);
        if (!shipments.isEmpty()) {
            shipmentRepository.deleteAll(shipments);
        }

        // Elimina il trip con questo idTrip
        Optional<Trip> tripOpt = tripRepository.findById(idTrip);
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

        String idTrip = tripDTO.getId();

        List<ShipmentDTO> shipments = shipmentRepository
                .findByIdTrip(idTrip)
                .stream()
                .map(s -> {
                    ShipmentDTO dto = new ShipmentDTO();
                    dto.setId(s.getId());
                    dto.setIdTrip(s.getIdTrip());
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
    public ResponseEntity<ApiResponseDTO<Void>> deleteShipment(@RequestBody ShipmentDTO shipmentDTO) {

        String idShipment = shipmentDTO.getId();
        if (idShipment == null || idShipment.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseDTO<>("Missing id of Shipment", 400, null));
        }

        // Recupera lo shipment dal DB
        Shipment shipment = shipmentRepository.findById(idShipment)
                .orElseThrow(() -> new RuntimeException("Shipment not found"));

        // Recupera il Trip associato
        Trip trip = tripRepository.findById(shipment.getIdTrip())
                .orElseThrow(() -> new RuntimeException("Trip not found"));

        // Aggiorna la capacità residua
        trip.setRemainingWidth(trip.getRemainingWidth() + shipment.getWidth());
        trip.setRemainingHeight(trip.getRemainingHeight() + shipment.getHeight());
        trip.setRemainingLength(trip.getRemainingLength() + shipment.getLength());
        trip.setRemainingWeight(trip.getRemainingWeight() + shipment.getWeight());

        tripRepository.save(trip);

        // Elimina lo shipment
        shipmentRepository.deleteById(idShipment);

        return ResponseEntity.ok(
                new ApiResponseDTO<>("Shipment deleted and trip capacity updated successfully", 200, null)
        );
    }


    /**
     * Avvia la simulazione e prepara il WebSocket per ricevere dati.
     * Il frontend deve già essere connesso a ws://localhost:8080/ws/telemetry
     */
    @PostMapping("/trip/startSimulation")
    public ResponseEntity<ApiResponseDTO<Void>> startSimulation(@RequestBody TripDTO tripDTO) {
        String vehicleName = tripDTO.getVehicleName();

        if (vehicleName == null || vehicleName.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseDTO<>("Missing vehicleName", 400, null));
        }

        try {
            // 1. Chiama Fridge API per avviare lo stream
            String url = FRIDGE_API_URL + "/stream/start/" + vehicleName;
            restTemplate.postForEntity(url, null, Map.class);

            return ResponseEntity.ok(
                    new ApiResponseDTO<>("Simulation started for " + vehicleName, 200, null)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>("Error starting simulation: " + e.getMessage(), 500, null));
        }
    }

    /**
     * Ferma la simulazione e chiude il WebSocket.
     */
    @PostMapping("/trip/stopSimulation")
    public ResponseEntity<ApiResponseDTO<Void>> stopSimulation() {
        try {
            // 1. Chiama Fridge API per fermare lo stream
            String url = FRIDGE_API_URL + "/stream/stop";
            try {
                restTemplate.postForEntity(url, null, Map.class);
            } catch (org.springframework.web.client.HttpClientErrorException e) {
                // Se ritorna 400, significa che lo stream è già terminato (normale)
                if (e.getStatusCode() != HttpStatus.BAD_REQUEST) {
                    throw e;
                }
                System.out.println("ℹ️ Stream già terminato (stream ha finito il CSV)");
            }

            // 2. Chiudi la sessione WebSocket
            telemetryHandler.stopSession();

            return ResponseEntity.ok(
                    new ApiResponseDTO<>("Simulation stopped", 200, null)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>("Error stopping simulation: " + e.getMessage(), 500, null));
        }
    }

    /**
     * Verifica lo stato della simulazione.
     */
    @GetMapping("/simulation/status")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> getSimulationStatus() {
        try {
            String url = FRIDGE_API_URL + "/stream/status";
            Map response = restTemplate.getForObject(url, Map.class);

            return ResponseEntity.ok(
                    new ApiResponseDTO<>("Status retrieved", 200, response)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>("Error getting status: " + e.getMessage(), 500, null));
        }
    }


}


