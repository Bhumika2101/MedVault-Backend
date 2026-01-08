package com.medvault.controller;

import com.medvault.dto.request.LoginRequest;
import com.medvault.dto.request.PatientRegistrationRequest;
import com.medvault.dto.request.SetPasswordRequest;
import com.medvault.dto.response.ApiResponse;
import com.medvault.dto.response.LoginResponse;
import com.medvault.dto.response.UserResponse;
import com.medvault.model.User;
import com.medvault.service.AuthService;
import com.medvault.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/register/patient")
    public ResponseEntity<ApiResponse<UserResponse>> registerPatient(
            @Valid @RequestBody PatientRegistrationRequest request) {
        UserResponse response = authService.registerPatient(request);
        return ResponseEntity.ok(ApiResponse.success("Patient registered successfully", response));
    }

    @PostMapping("/set-password")
    public ResponseEntity<ApiResponse<String>> setPassword(@Valid @RequestBody SetPasswordRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Passwords do not match"));
        }

        authService.setPassword(request.getToken(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.success("Password set successfully", "Password updated"));
    }

    @GetMapping("/verify-token/{token}")
    public ResponseEntity<ApiResponse<String>> verifyToken(@PathVariable String token) {
        boolean isValid = authService.verifyPasswordResetToken(token);
        if (isValid) {
            return ResponseEntity.ok(ApiResponse.success("Token is valid", "valid"));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Token is invalid or expired"));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        try {
            User user = authenticationService.getCurrentUser();

            UserResponse response = UserResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .phoneNumber(user.getPhoneNumber())
                    .role(user.getRole())
                    .isActive(user.getIsActive())
                    .createdAt(user.getCreatedAt())
                    .build();

            return ResponseEntity.ok(ApiResponse.success("User details retrieved", response));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching user details: " + e.getMessage()));
        }
    }
}