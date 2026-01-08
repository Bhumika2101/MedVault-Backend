package com.medvault.controller;

import com.medvault.dto.request.UpdateDoctorProfileRequest;
import com.medvault.dto.response.ApiResponse;
import com.medvault.model.Doctor;
import com.medvault.service.AuthenticationService;
import com.medvault.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctor")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class DoctorProfileController {

    private final DoctorService doctorService;
    private final AuthenticationService authenticationService;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<Doctor>> getProfile() {
        try {
            Long doctorId = authenticationService.getCurrentUserId();
            log.info("📋 Fetching profile for doctor ID: {}", doctorId);

            Doctor doctor = doctorService.getDoctorById(doctorId);

            log.info("✅ Profile retrieved successfully");
            return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", doctor));

        } catch (Exception e) {
            log.error("❌ Error fetching doctor profile", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching profile: " + e.getMessage()));
        }
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<Doctor>> updateProfile(@Valid @RequestBody UpdateDoctorProfileRequest request) {
        try {
            Long doctorId = authenticationService.getCurrentUserId();
            log.info("✏️ Updating profile for doctor ID: {}", doctorId);

            Doctor updatedDoctor = doctorService.updateProfile(doctorId, request);

            log.info("✅ Profile updated successfully");
            return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updatedDoctor));

        } catch (Exception e) {
            log.error("❌ Error updating doctor profile", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Error updating profile: " + e.getMessage()));
        }
    }
}
