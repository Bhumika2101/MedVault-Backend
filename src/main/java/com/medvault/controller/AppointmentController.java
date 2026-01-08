package com.medvault.controller;

import com. medvault.dto.request. AppointmentRequest;
import com.medvault.dto.response.ApiResponse;
import com. medvault.dto.response.AppointmentResponse;
import com.medvault. model.User;
import com.medvault.model.enums.AppointmentStatus;
import com.medvault.service.AppointmentService;
import com.medvault.service. AuthenticationService;
import lombok. RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AuthenticationService authenticationService;

    @GetMapping("/my-appointments")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR')")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getMyAppointments() {
        try {
            User currentUser = authenticationService. getCurrentUser();
            log.info("Fetching appointments for user: {} with role: {}",
                    currentUser.getEmail(), currentUser.getRole());

            List<AppointmentResponse> appointments;

            switch (currentUser.getRole()) {
                case PATIENT:
                    appointments = appointmentService.getPatientAppointments(currentUser.getId());
                    break;
                case DOCTOR:
                    appointments = appointmentService.getDoctorAppointments(currentUser.getId());
                    break;
                default:
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("Invalid user role"));
            }

            return ResponseEntity. ok(ApiResponse. success("Appointments retrieved", appointments));
        } catch (Exception e) {
            log.error("Error fetching appointments", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching appointments: " + e.getMessage()));
        }
    }

    @PostMapping("/book")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> bookAppointment(@RequestBody AppointmentRequest request) {
        try {
            Long patientId = authenticationService.getCurrentUserId();
            log.info("Patient {} booking appointment with doctor {}", patientId, request. getDoctorId());

            AppointmentResponse appointment = appointmentService. bookAppointment(patientId, request);
            return ResponseEntity.ok(ApiResponse.success("Appointment booked successfully", appointment));
        } catch (Exception e) {
            log.error("Error booking appointment", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Error booking appointment: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> updateAppointmentStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String notes) {
        try {
            Long doctorId = authenticationService.getCurrentUserId();

            // Convert string status to enum
            AppointmentStatus appointmentStatus;
            try {
                appointmentStatus = AppointmentStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity. badRequest()
                        .body(ApiResponse.error("Invalid status.  Must be PENDING, APPROVED, REJECTED, or COMPLETED"));
            }

            AppointmentResponse appointment = appointmentService.updateAppointmentStatus(
                    id,
                    appointmentStatus,
                    notes != null ? notes : "",
                    doctorId. toString()
            );

            return ResponseEntity.ok(ApiResponse.success("Appointment status updated", appointment));
        } catch (Exception e) {
            log.error("Error updating appointment status", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Error updating status: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getAppointmentById(@PathVariable Long id) {
        try {
            AppointmentResponse appointment = appointmentService.getAppointmentById(id);
            return ResponseEntity.ok(ApiResponse.success("Appointment retrieved", appointment));
        } catch (Exception e) {
            log.error("Error fetching appointment", e);
            return ResponseEntity. badRequest()
                    .body(ApiResponse.error("Error fetching appointment: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<String>> cancelAppointment(@PathVariable Long id) {
        try {
            Long userId = authenticationService.getCurrentUserId();
            appointmentService.cancelAppointment(id, userId);
            return ResponseEntity.ok(ApiResponse. success("Appointment cancelled successfully", null));
        } catch (Exception e) {
            log.error("Error cancelling appointment", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse. error("Error cancelling appointment: " + e.getMessage()));
        }
    }
}