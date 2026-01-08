package com.medvault.service;

import com.medvault.model.User;
import com.medvault.repository.UserRepository;
import lombok. RequiredArgsConstructor;
import lombok.extern.slf4j. Slf4j;
import org. springframework.security.core.authority. SimpleGrantedAuthority;
import org.springframework.security.core. userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org. springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("User not found with email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });

        log.debug("User found: {} with role: {}", user.getEmail(), user.getRole());

        // Check if user is active
        if (!user.getIsActive()) {
            log. warn("User account is inactive: {}", email);
            throw new UsernameNotFoundException("User account is inactive");
        }

        // IMPORTANT: Add "ROLE_" prefix for Spring Security
        String roleWithPrefix = "ROLE_" + user.getRole().name();
        log.debug("Granted authority: {}", roleWithPrefix);

        return new org. springframework.security.core.userdetails.User(
                user. getEmail(),
                user.getPassword(),
                user.getIsActive(),
                true, // accountNonExpired
                true, // credentialsNonExpired
                true, // accountNonLocked
                Collections. singletonList(new SimpleGrantedAuthority(roleWithPrefix))
        );
    }
}