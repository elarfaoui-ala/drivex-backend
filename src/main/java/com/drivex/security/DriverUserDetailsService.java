package com.drivex.security;

import com.drivex.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * DriverUserDetailsService — loads a Driver by email for Spring Security.
 * The password stored in DB is BCrypt-hashed.
 */
@Service
@RequiredArgsConstructor
public class DriverUserDetailsService implements UserDetailsService {

    private final DriverRepository driverRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return driverRepository.findByEmail(email)
            .map(driver -> User.builder()
                .username(driver.getEmail())
                .password(driver.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_DRIVER")))
                .build())
            .orElseThrow(() -> new UsernameNotFoundException("Driver not found: " + email));
    }
}
