package com.medvault.controller;

import com.medvault.dto.request.DoctorCreationRequest;
import com.medvault.dto.response.ApiResponse;
import com.medvault.dto.response.UserResponse;
import com.medvault.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final DoctorService doctorService;

    @PostMapping("/doctors")
    public ResponseEntity<ApiResponse<UserResponse>> createDoctor(
            @Valid @RequestBody DoctorCreationRequest request) {
        UserResponse response = doctorService.createDoctor(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Doctor created successfully.  Email sent to set password.", response));
    }

    @GetMapping("/doctors")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllDoctors() {
        List<UserResponse> doctors = doctorService.getAllActiveDoctors();
        return ResponseEntity.ok(ApiResponse.success("Doctors retrieved successfully", doctors));
    }

    @PostMapping("/doctors/{doctorId}/resend-invitation")
    public ResponseEntity<ApiResponse<String>> resendInvitation(@PathVariable Long doctorId) {
        doctorService.resendInvitation(doctorId);
        return ResponseEntity.ok(ApiResponse.success("Invitation email resent successfully", null));
    }

    @DeleteMapping("/doctors/cleanup-expired")
    public ResponseEntity<ApiResponse<String>> cleanupExpiredDoctors() {
        int count = doctorService.cleanupExpiredUnverifiedDoctors();
        return ResponseEntity.ok(ApiResponse.success(
                "Cleanup completed. Deleted " + count + " expired unverified doctors",
                String.valueOf(count)));
    }

    @PutMapping("/doctors/{doctorId}/availability")
    public ResponseEntity<ApiResponse<UserResponse>> toggleDoctorAvailability(@PathVariable Long doctorId) {
        UserResponse response = doctorService.toggleDoctorAvailability(doctorId);
        return ResponseEntity.ok(ApiResponse.success("Doctor availability updated successfully", response));
    }
}