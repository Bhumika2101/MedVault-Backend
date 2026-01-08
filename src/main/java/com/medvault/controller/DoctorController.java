package com.medvault.controller;

import com.medvault.dto.response.ApiResponse;
import com.medvault.dto.response.DashboardResponse;
import com.medvault.dto.response.UserResponse;
import com.medvault.model.Doctor;
import com.medvault.model.User;
import com.medvault.service.AuthenticationService;
import com.medvault.service.DoctorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctor")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class DoctorController {

    private final DoctorService doctorService;
    private final AuthenticationService authenticationService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        try {
            User currentUser = authenticationService.getCurrentUser();
            log.info("Fetching dashboard for doctor: {}", currentUser.getEmail());

            if (!(currentUser instanceof Doctor)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User is not a doctor"));
            }

            Doctor doctor = (Doctor) currentUser;
            DashboardResponse dashboard = doctorService.getDoctorDashboard(doctor.getId());
            return ResponseEntity.ok(ApiResponse.success("Dashboard data retrieved", dashboard));
        } catch (Exception e) {
            log.error("Error fetching doctor dashboard", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching dashboard: " + e.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllActiveDoctors() {
        try {
            List<UserResponse> doctors = doctorService.getAvailableDoctors();
            return ResponseEntity.ok(ApiResponse.success("Available doctors retrieved", doctors));
        } catch (Exception e) {
            log.error("Error fetching doctors", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching doctors: " + e.getMessage()));
        }
    }
}