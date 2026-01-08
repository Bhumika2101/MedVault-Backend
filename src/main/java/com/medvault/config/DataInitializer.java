package com.medvault.config;

import com.medvault.model.User;
import com.medvault.model.enums.Role;
import com.medvault.repository. UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j. Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initializeData() {
        return args -> {
            try {
                // Check if admin exists
                if (userRepository.findByEmail("admin@medvault.com"). isEmpty()) {
                    log. info("==========================================");
                    log.info("Creating default admin user.. .");
                    log.info("==========================================");

                    User admin = new User();
                    admin.setEmail("admin@medvault.com");

                    // IMPORTANT: Encode password properly
                    String rawPassword = "admin123";
                    String encodedPassword = passwordEncoder.encode(rawPassword);
                    admin.setPassword(encodedPassword);

                    admin.setFirstName("Admin");
                    admin.setLastName("User");
                    admin.setPhoneNumber("1234567890");
                    admin.setRole(Role.ADMIN);
                    admin.setIsActive(true);
                    admin.setIsPasswordSet(true);
                    admin.setCreatedAt(LocalDateTime.now());
                    admin. setUpdatedAt(LocalDateTime. now());

                    userRepository.save(admin);

                    log.info("==========================================");
                    log.info("✓ Default admin user created successfully!");
                    log.info("Email: admin@medvault.com");
                    log.info("Password: admin123");
                    log.info("Encoded Password: {}", encodedPassword);
                    log.info("==========================================");
                } else {
                    log.info("Admin user already exists - skipping creation");

                    // Optional: Update existing admin password if needed
                    User existingAdmin = userRepository.findByEmail("admin@medvault.com").get();
                    log.info("Existing admin password hash: {}", existingAdmin.getPassword());
                }
            } catch (Exception e) {
                log.error("Error creating admin user: {}", e. getMessage(), e);
            }
        };
    }
}