package com.medvault.controller;

import com.medvault.dto.request.UpdatePatientProfileRequest;
import com.medvault.dto.response.ApiResponse;
import com.medvault.dto.response.DashboardResponse;
import com.medvault.model.Patient;
import com.medvault.service.AuthenticationService;
import com.medvault.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patient")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class PatientController {

    private final PatientService patientService;
    private final AuthenticationService authenticationService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        try {
            Long patientId = authenticationService.getCurrentUserId();
            log.info("📊 Fetching dashboard for patient ID: {}", patientId);

            DashboardResponse dashboard = patientService.getPatientDashboard(patientId);

            log.info("✅ Dashboard retrieved successfully");
            return ResponseEntity.ok(ApiResponse.success("Dashboard data retrieved", dashboard));

        } catch (Exception e) {
            log.error("❌ Error fetching patient dashboard", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching dashboard: " + e.getMessage()));
        }
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Patient>> getProfile() {
        try {
            Long patientId = authenticationService.getCurrentUserId();
            log.info("📋 Fetching profile for patient ID: {}", patientId);

            Patient patient = patientService.getPatientById(patientId);

            log.info("✅ Profile retrieved successfully");
            return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", patient));

        } catch (Exception e) {
            log.error("❌ Error fetching patient profile", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching profile: " + e.getMessage()));
        }
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<Patient>> updateProfile(@Valid @RequestBody UpdatePatientProfileRequest request) {
        try {
            Long patientId = authenticationService.getCurrentUserId();
            log.info("✏️ Updating profile for patient ID: {}", patientId);

            Patient updatedPatient = patientService.updateProfile(patientId, request);

            log.info("✅ Profile updated successfully");
            return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", updatedPatient));

        } catch (Exception e) {
            log.error("❌ Error updating patient profile", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Error updating profile: " + e.getMessage()));
        }
    }
}