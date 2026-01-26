package bello.antonio.carrier_management_service.restcontrollers;

import bello.antonio.carrier_management_service.domain.CarrierManager;
import bello.antonio.carrier_management_service.domain.Shipment;
import bello.antonio.carrier_management_service.domain.Trip;
import bello.antonio.carrier_management_service.domain.Vehicle;
import bello.antonio.carrier_management_service.dto.*;
import bello.antonio.carrier_management_service.repositories.CarrierManagerRepository;
import bello.antonio.carrier_management_service.repositories.ShipmentRepository;
import bello.antonio.carrier_management_service.repositories.TripRepository;
import bello.antonio.carrier_management_service.repositories.VehicleRepository;
import bello.antonio.carrier_management_service.security.JwtUtilities;
import bello.antonio.carrier_management_service.service.TripRoutingService;
import com.google.maps.model.LatLng;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static bello.antonio.carrier_management_service.configuration.SecurityConfig.passwordEncoder;

@RestController
@RequestMapping("/api/carrier")
public class CarrierRestController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtilities jwtUtilities;

    @Autowired
    private TripRoutingService tripRoutingService;

    @Autowired
    private CarrierManagerRepository carrierManagerRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private ShipmentRepository shipmentRepository;

    @RequestMapping(value="/authenticate", method = RequestMethod.POST)
    public ResponseEntity<ApiResponseDTO<String>> createAuthenticationToken(@RequestBody LoginDTO loginDTO) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            Map<String, Object> claims = new HashMap<>();
            claims.put("email", authentication.getName());
            final String jwt = jwtUtilities.generateToken(authentication.getName(), claims);

            return ResponseEntity.ok(
                    new ApiResponseDTO<>("Authentication successful", 200, jwt)
            );

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDTO<>("Invalid email or password", 401, null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>("Error during authentication", 500, null));
        }
    }


    @RequestMapping(value = "/addCarrierManager", method = RequestMethod.POST)
    public ResponseEntity<ApiResponseDTO<CarrierManager>> addCarrierManager(@RequestBody CarrierManagerDTO carrierManagerDTO) {

        // Controllo se l'email è già registrata
        if (carrierManagerRepository.findByEmail(carrierManagerDTO.getEmail()).isPresent()) {
            ApiResponseDTO<CarrierManager> response = new ApiResponseDTO<>(
                    "Carrier manager with this email already exists",
                    409, // 409 = Conflict
                    null
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        CarrierManager carrierManager = new CarrierManager();
        carrierManager.setEmail(carrierManagerDTO.getEmail());
        carrierManager.setPassword(passwordEncoder().encode(carrierManagerDTO.getPassword()));
        carrierManager.setRole("ADMIN");

        carrierManagerRepository.save(carrierManager);

        ApiResponseDTO<CarrierManager> response = new ApiResponseDTO<>(
                "Carrier manager added successfully",
                200,
                carrierManager
        );

        return ResponseEntity.ok(response);
    }


    @RequestMapping(value = "/deleteCarrierManager", method = RequestMethod.POST)
    public ResponseEntity<ApiResponseDTO<Void>> deleteCarrierManager(@RequestBody CarrierManagerDTO carrierManagerDTO) {

        String email = carrierManagerDTO.getEmail();
        if (email == null || email.isBlank()) {
            ApiResponseDTO<Void> response = new ApiResponseDTO<>("Missing email in request body", 400, null);
            return ResponseEntity.badRequest().body(response);
        }

        Optional<CarrierManager> carrierManagerOpt = carrierManagerRepository.findByEmail(email);
        if (carrierManagerOpt.isEmpty()) {
            ApiResponseDTO<Void> response = new ApiResponseDTO<>("Carrier Manager not found", 404, null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        carrierManagerRepository.deleteById(carrierManagerOpt.get().getId());

        ApiResponseDTO<Void> response = new ApiResponseDTO<>("Carrier Manager deleted successfully", 200, null);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/retrieveTrips")
    public ResponseEntity<ApiResponseDTO<List<TripDTO>>> retrieveTrips(@RequestBody ShipmentDTO shipmentDTO) {

        // 1️⃣ Filtra i veicoli disponibili
        List<Vehicle> vehicles =
                vehicleRepository.findByMaxWeightGreaterThanAndWidthGreaterThanAndHeightGreaterThanAndLengthGreaterThanAndRefrigerated(
                        shipmentDTO.getWeight(),
                        shipmentDTO.getWidth(),
                        shipmentDTO.getHeight(),
                        shipmentDTO.getLength(),
                        shipmentDTO.isRefrigerated()
                );

        List<Vehicle> availableVehicles = vehicles.stream()
                .filter(v -> tripRepository.findByVehicleName(v.getVehicleName()).isEmpty())
                .toList();

        // 2️⃣ Coordinate e rotta
        LatLng departure = tripRoutingService.geocode(shipmentDTO.getDepartureAddress());
        LatLng arrival   = tripRoutingService.geocode(shipmentDTO.getArrivalAddress());

        RouteInfoDTO routeInfo = tripRoutingService.computeRoute(departure, arrival);
        String polyline = routeInfo.getPolyline();
        double distanceKm = routeInfo.getDistanceKm();

        // 3️⃣ Costruisci la lista di TripDTO
        List<TripDTO> trips = availableVehicles.stream().map(vehicle -> {
            TripDTO trip = new TripDTO();
            trip.setVehicleName(vehicle.getVehicleName());
            trip.setArrivalDate(shipmentDTO.getArrivalDate());
            trip.setPathPolyline(polyline);
            trip.setDistanceKm(distanceKm);
            trip.setPrice((float)(vehicle.getPricePerKm() * distanceKm));
            trip.setScheduled(false);
            trip.setStarted(false);
            return trip;
        }).toList();

        ApiResponseDTO<List<TripDTO>> response = new ApiResponseDTO<>(
                "Available trips retrieved successfully",
                200,
                trips
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/selectTrip")
    @Transactional
    public ResponseEntity<ApiResponseDTO<Void>> selectTrip(
            @RequestBody SelectedTripDTO request) {

        Trip trip = new Trip();
        TripDTO t = request.getTrip();

        trip.setVehicleName(t.getVehicleName());
        trip.setArrivalDate(t.getArrivalDate());
        trip.setPathPolyline(t.getPathPolyline());
        trip.setDistanceKm(t.getDistanceKm());
        trip.setPrice(t.getPrice());
        trip.setScheduled(t.isScheduled());
        trip.setStarted(t.isStarted());

        Shipment shipment = new Shipment();
        ShipmentDTO s = request.getShipment();

        shipment.setVehicleName(s.getVehicleName());
        shipment.setDepartureAddress(s.getDepartureAddress());
        shipment.setArrivalAddress(s.getArrivalAddress());
        shipment.setArrivalDate(s.getArrivalDate());
        shipment.setWeight(s.getWeight());
        shipment.setWidth(s.getWidth());
        shipment.setHeight(s.getHeight());
        shipment.setLength(s.getLength());
        shipment.setRefrigerated(s.isRefrigerated());

        tripRepository.save(trip);
        shipmentRepository.save(shipment);

        return ResponseEntity.ok(
                new ApiResponseDTO<>("Trip selected and shipment created", 200, null)
        );
    }









}
