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
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.client.RestTemplate;

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
    private UserRepository userRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${FRIDGE_API_URL:http://fridge-streamer:8002}")
    private String FRIDGE_API_URL;

    @RequestMapping(value="/authenticate", method = RequestMethod.POST)
    public ResponseEntity<ApiResponseDTO<AuthenticationResponseDTO>> createAuthenticationToken(@RequestBody LoginDTO loginDTO) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Recupera l'utente per ottenere il ruolo
            Optional<User> userOpt = userRepository.findByEmail(loginDTO.getEmail());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new ApiResponseDTO<>("User not found", 500, null));
            }

            User user = userOpt.get();
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", user.getId());
            claims.put("email", authentication.getName());
            claims.put("role", user.getRole().toString());
            final String jwt = jwtUtilities.generateToken(authentication.getName(), claims);

            // Crea la risposta con id, JWT e ruolo
            AuthenticationResponseDTO authResponse = new AuthenticationResponseDTO(jwt, user.getRole(), user.getId());
            System.out.println("L'id è: " + authResponse.getUserId());
            return ResponseEntity.ok(
                    new ApiResponseDTO<>("Authentication successful", 200, authResponse)
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
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setPhone(userDTO.getPhone());

        user = userRepository.save(user);
        userDTO.setId(user.getId());
        userDTO.setPassword(null); // non restituire la password nella risposta
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

    @PostMapping("/trip/startSimulation/{vehicleName}")
    public ResponseEntity<ApiResponseDTO<Void>> startSimulation(@PathVariable String vehicleName) {
        if (vehicleName.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponseDTO<>("Missing vehicleName", 400, null));
        }

        try {
            restTemplate.postForEntity(FRIDGE_API_URL + "/stream/start/" + vehicleName, null, Map.class);
            return ResponseEntity.ok(new ApiResponseDTO<>("Simulation started for " + vehicleName, 200, null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>("Error starting simulation: " + e.getMessage(), 500, null));
        }
    }

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