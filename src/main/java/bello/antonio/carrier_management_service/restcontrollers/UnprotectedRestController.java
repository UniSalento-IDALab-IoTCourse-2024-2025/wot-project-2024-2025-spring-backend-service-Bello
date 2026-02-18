package bello.antonio.carrier_management_service.restcontrollers;

import bello.antonio.carrier_management_service.domain.*;
import bello.antonio.carrier_management_service.dto.*;
import bello.antonio.carrier_management_service.repositories.UserRepository;
import bello.antonio.carrier_management_service.repositories.ShipmentRepository;
import bello.antonio.carrier_management_service.repositories.TripRepository;
import bello.antonio.carrier_management_service.repositories.VehicleRepository;
import bello.antonio.carrier_management_service.security.JwtUtilities;
import bello.antonio.carrier_management_service.service.GeoUtils;
import bello.antonio.carrier_management_service.service.PolylineUtils;
import bello.antonio.carrier_management_service.service.TripRoutingService;
import com.google.maps.model.LatLng;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static bello.antonio.carrier_management_service.configuration.SecurityConfig.passwordEncoder;

@RestController
@RequestMapping("/api/carrier")
public class UnprotectedRestController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtilities jwtUtilities;

    @Autowired
    private TripRoutingService tripRoutingService;

    @Autowired
    private UserRepository userRepository;

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

    @RequestMapping(value="/register",
            method=RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponseDTO<UserDTO>> save(@RequestBody UserDTO userDTO) {
        userDTO.setRole(Role.CLIENT);
        // Controllo se l'email è già registrata
        if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            ApiResponseDTO<UserDTO> response = new ApiResponseDTO<>(
                    "User with this email already exists",
                    409, // 409 = Conflict
                    null
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        User user = new User();
        user.setEmail(userDTO.getEmail());
        user.setPassword(passwordEncoder().encode(userDTO.getPassword()));
        user.setRole(userDTO.getRole());

        user = userRepository.save(user);
        userDTO.setId(user.getId());
        ApiResponseDTO<UserDTO> response = new ApiResponseDTO<>(
                "New client added successfully",
                200,
                userDTO
        );

        return ResponseEntity.ok(response);
    }

    @RequestMapping(value = "/addUser", method = RequestMethod.POST)
    public ResponseEntity<ApiResponseDTO<UserDTO>> addUser(@RequestBody UserDTO userDTO) {

        // Controllo se l'email è già registrata
        if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            ApiResponseDTO<UserDTO> response = new ApiResponseDTO<>(
                    "User with this email already exists",
                    409, // 409 = Conflict
                    null
            );
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        User user = new User();
        user.setEmail(userDTO.getEmail());
        user.setPassword(passwordEncoder().encode(userDTO.getPassword()));
        user.setRole(userDTO.getRole());

        user = userRepository.save(user);
        userDTO.setId(user.getId());

        ApiResponseDTO<UserDTO> response = new ApiResponseDTO<>(
                "User added successfully",
                200,
                userDTO
        );

        return ResponseEntity.ok(response);
    }


    @RequestMapping(value = "/deleteUser", method = RequestMethod.POST)
    public ResponseEntity<ApiResponseDTO<Void>> deleteUser(@RequestBody UserDTO userDTO) {

        String email = userDTO.getEmail();
        if (email == null || email.isBlank()) {
            ApiResponseDTO<Void> response = new ApiResponseDTO<>("Missing email in request body", 400, null);
            return ResponseEntity.badRequest().body(response);
        }

        Optional<User> carrierManagerOpt = userRepository.findByEmail(email);
        if (carrierManagerOpt.isEmpty()) {
            ApiResponseDTO<Void> response = new ApiResponseDTO<>("User not found", 404, null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        userRepository.deleteById(carrierManagerOpt.get().getId());

        ApiResponseDTO<Void> response = new ApiResponseDTO<>("User deleted successfully", 200, null);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/retrieveTrips")
    public ResponseEntity<ApiResponseDTO<RetrievedTripsDTO>> retrieveTrips(@RequestBody ShipmentDTO shipmentDTO) {

        System.out.println("========== RETRIEVE TRIPS DEBUG ==========");
        System.out.println("📦 Shipment richiesto:");
        System.out.println("   - Peso: " + shipmentDTO.getWeight() + " kg");
        System.out.println("   - Dimensioni: " + shipmentDTO.getWidth() + "x" + shipmentDTO.getHeight() + "x" + shipmentDTO.getLength());
        System.out.println("   - Refrigerato: " + shipmentDTO.isRefrigerated());
        System.out.println("   - Da: " + shipmentDTO.getDepartureAddress());
        System.out.println("   - A: " + shipmentDTO.getArrivalAddress());
        System.out.println("   - Data arrivo: " + shipmentDTO.getArrivalDate());

        List<Vehicle> availableVehicles = vehicleRepository.findAvailableVehicles(
                shipmentDTO.getWeight(),
                shipmentDTO.getWidth(),
                shipmentDTO.getHeight(),
                shipmentDTO.getLength(),
                shipmentDTO.isRefrigerated()
        );

        System.out.println("\n🚛 FASE 1 - Veicoli disponibili (senza trip attivi): " + availableVehicles.size());
        for (Vehicle v : availableVehicles) {
            System.out.println("   - " + v.getVehicleName() + " (max " + v.getMaxWeight() + "kg, " +
                    v.getWidth() + "x" + v.getHeight() + "x" + v.getLength() + ", refrigerato: " + v.isRefrigerated() +
                    ", €" + v.getPricePerKm() + "/km)");
        }

        LatLng departure;
        LatLng arrival;

        if (shipmentDTO.getDepartureLatLng() != null && shipmentDTO.getArrivalLatLng() != null) {
            departure = shipmentDTO.getDepartureLatLng();
            arrival = shipmentDTO.getArrivalLatLng();
            System.out.println("\n📍 Coordinate dal frontend:");
        } else {
            departure = tripRoutingService.geocode(shipmentDTO.getDepartureAddress());
            arrival = tripRoutingService.geocode(shipmentDTO.getArrivalAddress());
            System.out.println("\n📍 Coordinate da geocoding:");
        }
        System.out.println("   - Partenza: " + departure.lat + ", " + departure.lng);
        System.out.println("   - Arrivo: " + arrival.lat + ", " + arrival.lng);

        RouteInfoDTO routeInfo = tripRoutingService.computeRoute(departure, arrival);
        String polyline = routeInfo.getPolyline();
        double distanceKm = routeInfo.getDistanceKm();
        double durationSec = routeInfo.getDuration();

        System.out.println("\n🛣️ Route calcolata:");
        System.out.println("   - Distanza: " + String.format("%.2f", distanceKm) + " km");
        System.out.println("   - Durata: " + String.format("%.0f", durationSec) + " sec (" + String.format("%.1f", durationSec/60) + " min)");

        shipmentDTO.setDistanceKm(distanceKm);
        shipmentDTO.setDuration(durationSec);

        List<TripDTO> availableTripsDTO = availableVehicles.stream().map(vehicle -> {
            TripDTO trip = new TripDTO();
            trip.setVehicleName(vehicle.getVehicleName());
            trip.setRemainingWidth(vehicle.getWidth());
            trip.setRemainingHeight(vehicle.getHeight());
            trip.setRemainingLength(vehicle.getLength());
            trip.setRemainingWeight(vehicle.getMaxWeight());
            trip.setRefrigerated(vehicle.isRefrigerated());
            trip.setArrivalDate(shipmentDTO.getArrivalDate());
            trip.setDepartureLatLng(departure);
            trip.setArrivalLatLng(arrival);
            trip.setPathPolyline(polyline);
            trip.setDistanceKm(distanceKm);
            trip.setDuration(durationSec);
            trip.setPrice((float)(vehicle.getPricePerKm() * distanceKm));
            trip.setScheduled(false);
            trip.setStarted(false);
            return trip;
        }).toList();

        System.out.println("\n✅ FASE 2 - Trip creati da veicoli disponibili: " + availableTripsDTO.size());
        for (TripDTO t : availableTripsDTO) {
            System.out.println("   - " + t.getVehicleName() + " → €" + String.format("%.2f", t.getPrice()) + " (scheduled: " + t.isScheduled() + ")");
        }

        List<Trip> busyTrips = tripRepository.findBusyTrips(
                shipmentDTO.getWeight(),
                shipmentDTO.getWidth(),
                shipmentDTO.getHeight(),
                shipmentDTO.getLength(),
                shipmentDTO.isRefrigerated(),
                shipmentDTO.getArrivalDate()
        );

        System.out.println("\n🔄 FASE 3 - Busy trips dal DB (aggregation): " + busyTrips.size());
        for (Trip t : busyTrips) {
            System.out.println("   - ID: " + t.getId() + ", Veicolo: " + t.getVehicleName() +
                    ", Data: " + t.getArrivalDate() + ", €" + t.getPrice());
        }

        List<TripDTO> busyTripsDTO = busyTrips.stream().map(trip -> {
            TripDTO dto = new TripDTO();
            dto.setId(trip.getId());
            dto.setVehicleName(trip.getVehicleName());
            dto.setDepartureLatLng(trip.getDepartureLatLng());
            dto.setArrivalLatLng(trip.getArrivalLatLng());
            dto.setArrivalDate(trip.getArrivalDate());
            dto.setPathPolyline(trip.getPathPolyline());
            dto.setDuration(trip.getDuration());
            dto.setDistanceKm(trip.getDistanceKm());
            dto.setRefrigerated(trip.isRefrigerated());
            dto.setPrice(trip.getPrice());
            dto.setScheduled(true);
            dto.setStarted(false);
            dto.setRemainingWidth(trip.getRemainingWidth());
            dto.setRemainingHeight(trip.getRemainingHeight());
            dto.setRemainingLength(trip.getRemainingLength());
            dto.setRemainingWeight(trip.getRemainingWeight());
            return dto;
        }).toList();

        LatLng shipmentDeparture = departure;
        LatLng shipmentArrival = arrival;

        double maxDistanceKm = 1.5;
        double toleranceSec = 1800;

        System.out.println("\n🔍 FASE 4 - Filtraggio busy trips (soglia: " + maxDistanceKm + " km, tolleranza: " + toleranceSec + " sec)");

        List<TripDTO> filteredBusyTripsDTO = new ArrayList<>();

        for (TripDTO trip : busyTripsDTO) {
            System.out.println("\n   Analizzando trip: " + trip.getVehicleName() + " (ID: " + trip.getId() + ")");

            List<LatLng> polylinePoints = PolylineUtils.decode(trip.getPathPolyline());
            System.out.println("      Polyline decodificata: " + polylinePoints.size() + " punti");

            double distancePickup = GeoUtils.distanceToPolyline(shipmentDeparture, polylinePoints);
            System.out.println("      Distanza pickup dalla polyline: " + String.format("%.3f", distancePickup) + " km");
            if (distancePickup > maxDistanceKm) {
                System.out.println("      ❌ SCARTATO: pickup troppo lontano (>" + maxDistanceKm + " km)");
                continue;
            }

            double distanceDelivery = GeoUtils.distanceToPolyline(shipmentArrival, polylinePoints);
            System.out.println("      Distanza delivery dalla polyline: " + String.format("%.3f", distanceDelivery) + " km");
            if (distanceDelivery > maxDistanceKm) {
                System.out.println("      ❌ SCARTATO: delivery troppo lontano (>" + maxDistanceKm + " km)");
                continue;
            }

            double distPickupFromStart = GeoUtils.haversineKm(trip.getDepartureLatLng(), shipmentDeparture);
            double distDeliveryFromStart = GeoUtils.haversineKm(trip.getDepartureLatLng(), shipmentArrival);
            System.out.println("      Distanza pickup da start: " + String.format("%.3f", distPickupFromStart) + " km");
            System.out.println("      Distanza delivery da start: " + String.format("%.3f", distDeliveryFromStart) + " km");
            if (distPickupFromStart > distDeliveryFromStart) {
                System.out.println("      ❌ SCARTATO: pickup viene DOPO delivery nel percorso");
                continue;
            }


            System.out.println("      🔄 Calcolo nuova route con waypoints...");

            // Recupera tutti gli shipment esistenti per questo trip/vehicle
            List<Shipment> existingShipments = shipmentRepository.findByVehicleName(trip.getVehicleName());
            System.out.println("      📦 Shipment esistenti per " + trip.getVehicleName() + ": " + existingShipments.size());

            // Costruisci liste parallele di departures e arrivals
            List<LatLng> departures = new ArrayList<>();
            List<LatLng> arrivals = new ArrayList<>();
            List<String> labels = new ArrayList<>(); // per debug

            // Aggiungi gli shipment esistenti
            for (Shipment existingShipment : existingShipments) {
                departures.add(existingShipment.getDepartureLatLng());
                arrivals.add(existingShipment.getArrivalLatLng());
                labels.add(existingShipment.getDepartureAddress() + " → " + existingShipment.getArrivalAddress());
                System.out.println("         - Esistente: " + existingShipment.getDepartureAddress() +
                        " → " + existingShipment.getArrivalAddress());
            }

            // Aggiungi il nuovo shipment
            departures.add(shipmentDeparture);
            arrivals.add(shipmentArrival);
            labels.add(shipmentDTO.getDepartureAddress() + " → " + shipmentDTO.getArrivalAddress());
            System.out.println("         - Nuovo: " + shipmentDTO.getDepartureAddress() +
                    " → " + shipmentDTO.getArrivalAddress());

            // Ordina i waypoints rispettando i vincoli (pickup prima di delivery)
            System.out.println("      🔀 Ordinamento waypoints con vincoli...");
            List<LatLng> orderedWaypoints = PolylineUtils.orderWaypointsWithConstraints(
                    trip.getDepartureLatLng(),
                    departures,
                    arrivals,
                    labels
            );

            System.out.println("      📍 Totale waypoints ordinati: " + orderedWaypoints.size());

            RouteInfoDTO newRoute = tripRoutingService.computeRouteWithWaypoints(
                    trip.getDepartureLatLng(),
                    trip.getArrivalLatLng(),
                    orderedWaypoints  // ✅ Ora include TUTTI i waypoints ordinati correttamente
            );

            double durationDiff = Math.abs(newRoute.getDuration() - trip.getDuration());
            System.out.println("      Nuova durata: " + String.format("%.0f", newRoute.getDuration()) + " sec");
            System.out.println("      Durata originale: " + String.format("%.0f", trip.getDuration()) + " sec");
            System.out.println("      Differenza: " + String.format("%.0f", durationDiff) + " sec");


            Optional<Vehicle> vehicle = vehicleRepository.findByVehicleName(trip.getVehicleName());
            if (vehicle.isEmpty()) {
                System.out.println("      ❌ SCARTATO: veicolo non trovato");
                continue;
            }

            if (durationDiff <= toleranceSec) {
                System.out.println("      ✅ ACCETTATO: differenza durata <= " + toleranceSec + " sec");
                trip.setDistanceKm(newRoute.getDistanceKm());
                trip.setDuration(newRoute.getDuration());
                trip.setPathPolyline(newRoute.getPolyline());

                // n = numero shipment già presenti
                int n = existingShipments.size();
                float fullPartialPrice = (float) (distanceKm * vehicle.get().getPricePerKm());
                float discountedPrice = (float) (fullPartialPrice / Math.pow(2, n));
                trip.setPrice(discountedPrice);

                System.out.println("      📦 Shipment totali (incluso nuovo): " + n);
                System.out.println("      💰 Prezzo: €" + String.format("%.2f", fullPartialPrice) + " / 2^" + n + " = €" + String.format("%.2f", discountedPrice));
                filteredBusyTripsDTO.add(trip);
            } else {
                System.out.println("      ❌ SCARTATO: differenza durata troppo alta (>" + toleranceSec + " sec)");
            }

        }

        System.out.println("\n✅ FASE 5 - Busy trips filtrati compatibili: " + filteredBusyTripsDTO.size());
        for (TripDTO t : filteredBusyTripsDTO) {
            System.out.println("   - " + t.getVehicleName() + " → €" + String.format("%.2f", t.getPrice()) +
                    " (distanza: " + String.format("%.2f", t.getDistanceKm()) + " km)");
        }

        List<TripDTO> allTrips = new ArrayList<>();
        allTrips.addAll(availableTripsDTO);
        allTrips.addAll(filteredBusyTripsDTO);

        System.out.println("\n========== RIEPILOGO FINALE ==========");
        System.out.println("📊 Trip totali restituiti: " + allTrips.size());
        System.out.println("   - Da veicoli disponibili: " + availableTripsDTO.size());
        System.out.println("   - Da busy trips compatibili: " + filteredBusyTripsDTO.size());
        System.out.println("=======================================\n");

        RetrievedTripsDTO retrievedTripsDTO = new RetrievedTripsDTO();
        retrievedTripsDTO.setShipmentDTO(shipmentDTO);
        retrievedTripsDTO.setTripsDTO(allTrips);
        ApiResponseDTO<RetrievedTripsDTO> response = new ApiResponseDTO<>(
                "Available trips retrieved successfully",
                200,
                retrievedTripsDTO
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/selectTrip")
    @Transactional
    public ResponseEntity<ApiResponseDTO<Void>> selectTrip(
            @RequestBody SelectedTripDTO request) {

        TripDTO t = request.getTrip();
        ShipmentDTO s = request.getShipment();

        // DEBUG: Stampa tutti i dati ricevuti dal frontend
        System.out.println("========== SELECT TRIP DEBUG ==========");
        System.out.println("=== TRIP DATA RECEIVED ===");
        System.out.println("Trip ID: " + t.getId());
        System.out.println("Trip vehicleName: " + t.getVehicleName());
        System.out.println("Trip arrivalDate: " + t.getArrivalDate());
        System.out.println("Trip pathPolyline: " + (t.getPathPolyline() != null ? t.getPathPolyline().substring(0, Math.min(50, t.getPathPolyline().length())) + "..." : "null"));
        System.out.println("Trip departureLatLng: " + t.getDepartureLatLng());
        System.out.println("Trip arrivalLatLng: " + t.getArrivalLatLng());
        System.out.println("Trip distanceKm: " + t.getDistanceKm());
        System.out.println("Trip duration: " + t.getDuration());
        System.out.println("Trip price: " + t.getPrice());
        System.out.println("Trip refrigerated: " + t.isRefrigerated());
        System.out.println("Trip scheduled: " + t.isScheduled());
        System.out.println("Trip started: " + t.isStarted());

        System.out.println("=== SHIPMENT DATA RECEIVED ===");
        System.out.println("Shipment idTrip: " + s.getIdTrip());
        System.out.println("Shipment vehicleName: " + s.getVehicleName());
        System.out.println("Shipment departureAddress: " + s.getDepartureAddress());
        System.out.println("Shipment arrivalAddress: " + s.getArrivalAddress());
        System.out.println("Shipment departureLatLng: " + s.getDepartureLatLng());
        System.out.println("Shipment arrivalLatLng: " + s.getArrivalLatLng());
        System.out.println("Shipment arrivalDate: " + s.getArrivalDate());
        System.out.println("Shipment distanceKm: " + s.getDistanceKm());
        System.out.println("Shipment duration: " + s.getDuration());
        System.out.println("Shipment price: " + s.getPrice());
        System.out.println("Shipment weight: " + s.getWeight());
        System.out.println("Shipment width: " + s.getWidth());
        System.out.println("Shipment height: " + s.getHeight());
        System.out.println("Shipment length: " + s.getLength());
        System.out.println("Shipment refrigerated: " + s.isRefrigerated());
        System.out.println("========================================");

        // Se NON è scheduled, crea un nuovo trip
        if (!t.isScheduled()) {
            System.out.println(">>> Creating NEW trip (not scheduled)");
            Trip trip = new Trip();
            trip.setVehicleName(t.getVehicleName());
            trip.setArrivalDate(t.getArrivalDate());
            trip.setPathPolyline(t.getPathPolyline());
            trip.setDepartureLatLng(t.getDepartureLatLng());
            trip.setArrivalLatLng(t.getArrivalLatLng());
            trip.setDistanceKm(t.getDistanceKm());
            trip.setDuration(t.getDuration());
            trip.setScheduled(true); // ora diventa scheduled
            trip.setStarted(false);
            trip.setRefrigerated(t.isRefrigerated());
            trip.setRemainingWidth(t.getRemainingWidth()-s.getWidth());
            trip.setRemainingHeight(t.getRemainingHeight()-s.getHeight());
            trip.setRemainingLength(t.getRemainingLength()-s.getLength());
            trip.setRemainingWeight(t.getRemainingWeight()-s.getWeight());
            Trip savedTrip = tripRepository.save(trip);
            System.out.println(">>> Trip saved with ID: " + savedTrip.getId());
        } else {
            // Trip già scheduled: aggiorna con il nuovo percorso (include le tappe intermedie)
            System.out.println(">>> Updating EXISTING trip (already scheduled)");
            Optional<Trip> existingTripOpt = tripRepository.findByVehicleName(t.getVehicleName());
            if (existingTripOpt.isPresent()) {
                Trip existingTrip = existingTripOpt.get();
                System.out.println(">>> Found existing trip, old values:");
                System.out.println("    Old distanceKm: " + existingTrip.getDistanceKm());
                System.out.println("    Old duration: " + existingTrip.getDuration());

                // Aggiorna con i nuovi valori che includono le tappe intermedie
                existingTrip.setDistanceKm(t.getDistanceKm());
                existingTrip.setDuration(t.getDuration());
                existingTrip.setPathPolyline(t.getPathPolyline());
                existingTrip.setRemainingWidth(t.getRemainingWidth()-s.getWidth());
                existingTrip.setRemainingHeight(t.getRemainingHeight()-s.getHeight());
                existingTrip.setRemainingLength(t.getRemainingLength()-s.getLength());
                existingTrip.setRemainingWeight(t.getRemainingWeight()-s.getWeight());
                Trip updatedTrip = tripRepository.save(existingTrip);
                System.out.println(">>> Trip updated with new values:");
                System.out.println("    New distanceKm: " + updatedTrip.getDistanceKm());
                System.out.println("    New duration: " + updatedTrip.getDuration());
            } else {
                System.out.println(">>> WARNING: Trip with ID " + t.getId() + " not found in database!");
            }
        }
        Optional<Trip> savedTripOpt = tripRepository.findByVehicleName(t.getVehicleName());
        if(savedTripOpt.isPresent()) {
            Trip savedTrip = savedTripOpt.get();
            // Salva lo shipment
            System.out.println(">>> Creating shipment...");
            Shipment shipment = new Shipment();
            shipment.setIdTrip(savedTrip.getId());
            shipment.setVehicleName(s.getVehicleName());
            shipment.setDepartureAddress(s.getDepartureAddress());
            shipment.setArrivalAddress(s.getArrivalAddress());
            shipment.setDepartureLatLng(s.getDepartureLatLng());
            shipment.setArrivalLatLng(s.getArrivalLatLng());
            shipment.setArrivalDate(s.getArrivalDate());
            shipment.setDuration(s.getDuration());
            shipment.setDistanceKm(s.getDistanceKm());
            shipment.setPrice(t.getPrice());
            shipment.setWeight(s.getWeight());
            shipment.setWidth(s.getWidth());
            shipment.setHeight(s.getHeight());
            shipment.setLength(s.getLength());
            shipment.setRefrigerated(s.isRefrigerated());
            Shipment savedShipment = shipmentRepository.save(shipment);
            System.out.println(">>> Shipment saved with ID: " + savedShipment.getId());
        }
        System.out.println("========== END SELECT TRIP ==========");

        return ResponseEntity.ok(
                new ApiResponseDTO<>("Trip selected and shipment created", 200, null)
        );
    }

}