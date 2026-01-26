package bello.antonio.carrier_management_service.restcontrollers;

import bello.antonio.carrier_management_service.domain.Vehicle;
import bello.antonio.carrier_management_service.dto.ApiResponseDTO;
import bello.antonio.carrier_management_service.dto.VehicleDTO;
import bello.antonio.carrier_management_service.repositories.CarrierManagerRepository;
import bello.antonio.carrier_management_service.repositories.VehicleRepository;
import bello.antonio.carrier_management_service.security.JwtUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/api/carrier")
public class CarrierProtectedRestController {

    @Autowired
    private VehicleRepository vehicleRepository;

    @PostMapping("/addVehicle")
    public ResponseEntity<ApiResponseDTO> postAddVehicle(@RequestBody VehicleDTO vehicleDTO) {
        try {

            if (vehicleRepository.existsByVehicleName(vehicleDTO.getVehicleName())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new ApiResponseDTO("Vehicle name already exists", 409));
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
                    new ApiResponseDTO("New vehicle added successfully", 200)
            );

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO("Internal server error", 500));
        }
    }



}


