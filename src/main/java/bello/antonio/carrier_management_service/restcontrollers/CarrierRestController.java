package bello.antonio.carrier_management_service.restcontrollers;

import bello.antonio.carrier_management_service.domain.CarrierManager;
import bello.antonio.carrier_management_service.domain.Vehicle;
import bello.antonio.carrier_management_service.dto.*;
import bello.antonio.carrier_management_service.repositories.CarrierManagerRepository;
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

    @RequestMapping(value="/authenticate", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthenticationToken(@RequestBody LoginDTO loginDTO) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            Map<String, Object> claims = new HashMap<>();
            claims.put("email", authentication.getName());
            final String jwt = jwtUtilities.generateToken(authentication.getName(), claims);
            return ResponseEntity.ok(new AuthenticationResponseDTO(jwt));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponseDTO("Invalid email or password", 401));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO("Error during authentication", 500));
        }
    }

    @RequestMapping(value = "/addCarrierManager", method = RequestMethod.POST)
    public ResponseEntity<ApiResponseDTO> addCarrierManager(@RequestBody CarrierManagerDTO carrierManagerDTO) {

        CarrierManager carrierManager = new CarrierManager();
        carrierManager.setEmail(carrierManagerDTO.getEmail());
        carrierManager.setPassword(passwordEncoder().encode(carrierManagerDTO.getPassword()));
        carrierManager.setRole("ADMIN");

        carrierManagerRepository.save(carrierManager);

        ApiResponseDTO response = new ApiResponseDTO("Carrier manager added successfully", 200);

        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/deleteCarrierManager", method = RequestMethod.POST)
    public ResponseEntity<ApiResponseDTO> deleteCarrierManager(@RequestBody CarrierManagerDTO carrierManagerDTO) {

        String email = carrierManagerDTO.getEmail();
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseDTO("Missing email in request body", 400));
        }

        Optional<CarrierManager> carrierManagerOpt = carrierManagerRepository.findByEmail(email);
        if (carrierManagerOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponseDTO("Carrier Manager not found", 404));
        }

        carrierManagerRepository.deleteById(carrierManagerOpt.get().getId());

        return ResponseEntity.ok(new ApiResponseDTO("Carrier Manager deleted successfully", 200));
    }

    @PostMapping("/retrieveTrips")
    public ResponseEntity<List<TripDTO>> retrieveTrips(@RequestBody ShipmentDTO shipmentDTO) {

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
            trip.setPrice((float)(vehicle.getPricePerKm() * distanceKm));
            trip.setScheduled(false);
            trip.setStarted(false); // opzionale
            return trip;
        }).toList();

        return ResponseEntity.ok(trips);
    }







}
