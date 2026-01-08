package com.medvault.service;

import com.medvault.dto.request.LoginRequest;
import com.medvault.dto.request.PatientRegistrationRequest;
import com.medvault.dto.response.LoginResponse;
import com.medvault.dto.response.UserResponse;
import com.medvault.exception.ResourceNotFoundException;
import com.medvault.model.Patient;
import com.medvault.model.User;
import com.medvault.model.enums.Role;
import com.medvault.repository.PatientRepository;
import com.medvault.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for user: {}", request.getEmail());

        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        // Get user from database
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        // Check if password is set (for doctors created by admin)
        if (!user.getIsPasswordSet()) {
            // Check if token has expired
            if (user.getPasswordResetTokenExpiry() != null &&
                    user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
                // Token expired, delete the user
                userRepository.delete(user);
                throw new IllegalStateException(
                        "Your verification link has expired. Please contact admin to resend invitation.");
            }
            throw new IllegalStateException(
                    "Please verify your account by setting password. Check your email for verification link.");
        }
        // Check if user is active
        if (!user.getIsActive()) {
            throw new IllegalStateException("Account is not active.  Please complete password setup.");
        }

        // Generate JWT token with email and role
        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());

        log.info("Login successful for user: {} with role: {}", user.getEmail(), user.getRole());

        return LoginResponse.builder()
                .token(token)
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional
    public UserResponse registerPatient(PatientRegistrationRequest request) {
        log.info("Registering new patient: {}", request.getEmail());

        // Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalStateException("User with this email already exists");
        }

        // Create patient
        Patient patient = new Patient();
        patient.setEmail(request.getEmail());
        patient.setPassword(passwordEncoder.encode(request.getPassword()));
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setPhoneNumber(request.getPhoneNumber());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setGender(request.getGender());
        patient.setBloodGroup(request.getBloodGroup());
        patient.setAddress(request.getAddress());
        patient.setRole(Role.PATIENT);
        patient.setIsActive(true);
        patient.setIsPasswordSet(true);
        patient.setCreatedAt(LocalDateTime.now());
        patient.setUpdatedAt(LocalDateTime.now());

        Patient savedPatient = patientRepository.save(patient);

        log.info("Patient registered successfully: {}", savedPatient.getEmail());

        return UserResponse.builder()
                .id(savedPatient.getId())
                .email(savedPatient.getEmail())
                .firstName(savedPatient.getFirstName())
                .lastName(savedPatient.getLastName())
                .phoneNumber(savedPatient.getPhoneNumber())
                .role(savedPatient.getRole())
                .isActive(savedPatient.getIsActive())
                .createdAt(savedPatient.getCreatedAt())
                .build();
    }

    @Transactional
    public void setPassword(String token, String password) { // Changed from newPassword to password
        log.info("Password setup attempt with token: {}", token);

        // Find user by reset token
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired token"));

        // Check if token is expired
        if (user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Token has expired");
        }

        // Set new password
        user.setPassword(passwordEncoder.encode(password)); // Changed from newPassword to password
        user.setIsPasswordSet(true);
        user.setIsActive(true);
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        user.setUpdatedAt(LocalDateTime.now());

        userRepository.save(user);

        log.info("Password set successfully for user: {}", user.getEmail());
    }

    public boolean verifyPasswordResetToken(String token) {
        log.info("Verifying password reset token");

        Optional<User> userOptional = userRepository.findByPasswordResetToken(token);

        if (userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();

        // Check if token is expired
        if (user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            // Auto-delete expired unverified user
            if (!user.getIsPasswordSet()) {
                userRepository.delete(user);
            }
            return false;
        }

        return true;
    }
}