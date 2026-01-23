package bello.antonio.carrier_management_service.restcontrollers;

import bello.antonio.carrier_management_service.repositories.CarrierManagerRepository;
import bello.antonio.carrier_management_service.security.JwtUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carrier")
public class CarrierProtectedRestController {
    private static final Logger logger = LoggerFactory.getLogger(CarrierProtectedRestController.class);
    @Autowired
    private JwtUtilities jwtUtilities;

    @Autowired
    private CarrierManagerRepository carrierManagerRepository;



}


