package bello.antonio.carrier_management_service.repositories;

import bello.antonio.carrier_management_service.domain.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);
    Optional<User> findFirstByOrderByIdAsc();
}
