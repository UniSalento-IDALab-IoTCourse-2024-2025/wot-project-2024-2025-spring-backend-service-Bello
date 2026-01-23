package bello.antonio.carrier_management_service.service;

import bello.antonio.carrier_management_service.domain.CarrierManager;
import bello.antonio.carrier_management_service.repositories.CarrierManagerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    CarrierManagerRepository carrierManagerRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Optional<CarrierManager> user = carrierManagerRepository.findByEmail(email);

        if(user.isEmpty()) {
            throw new UsernameNotFoundException(email);
        }

        return org.springframework.security.core.userdetails.User.withUsername(user.get().getEmail()).password(user.get().getPassword()).roles(user.get().getRole()).build();
    }
}

