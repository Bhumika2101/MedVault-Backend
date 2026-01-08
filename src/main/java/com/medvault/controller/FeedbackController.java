package com.medvault.controller;

import com.medvault.dto.request.FeedbackRequest;
import com. medvault.dto.response. ApiResponse;
import com.medvault.dto.response.FeedbackResponse;
import com. medvault.service.AuthenticationService;
import com.medvault.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework. security.access.prepost.PreAuthorize;
import org. springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedbacks")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final AuthenticationService authenticationService;

    // Submit feedback (Patient only)
    @PostMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<FeedbackResponse>> submitFeedback(
            @Valid @RequestBody FeedbackRequest request) {
        try {
            Long patientId = authenticationService.getCurrentUserId();
            log.info("Patient {} submitting feedback for doctor {}", patientId, request.getDoctorId());

            FeedbackResponse feedback = feedbackService.submitFeedback(patientId, request);
            return ResponseEntity.ok(ApiResponse. success("Feedback submitted successfully", feedback));
        } catch (Exception e) {
            log.error("Error submitting feedback", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Error submitting feedback: " + e.getMessage()));
        }
    }

    // Get all feedbacks for a doctor (Public - to display on doctor profile)
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getDoctorFeedbacks(
            @PathVariable Long doctorId) {
        try {
            log.info("Fetching feedbacks for doctor: {}", doctorId);
            List<FeedbackResponse> feedbacks = feedbackService.getDoctorFeedbacks(doctorId);
            return ResponseEntity.ok(ApiResponse.success("Feedbacks retrieved", feedbacks));
        } catch (Exception e) {
            log. error("Error fetching feedbacks", e);
            return ResponseEntity. internalServerError()
                    .body(ApiResponse.error("Error fetching feedbacks: " + e.getMessage()));
        }
    }

    // Get doctor's average rating and stats (Public)
    @GetMapping("/doctor/{doctorId}/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDoctorStats(
            @PathVariable Long doctorId) {
        try {
            log.info("Fetching stats for doctor: {}", doctorId);
            Map<String, Object> stats = feedbackService.getDoctorStats(doctorId);
            return ResponseEntity.ok(ApiResponse.success("Stats retrieved", stats));
        } catch (Exception e) {
            log.error("Error fetching stats", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching stats: " + e.getMessage()));
        }
    }

    // Get my feedbacks (Patient)
    @GetMapping("/my-feedbacks")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getMyFeedbacks() {
        try {
            Long patientId = authenticationService. getCurrentUserId();
            log. info("Fetching feedbacks for patient: {}", patientId);
            List<FeedbackResponse> feedbacks = feedbackService. getPatientFeedbacks(patientId);
            return ResponseEntity.ok(ApiResponse.success("Feedbacks retrieved", feedbacks));
        } catch (Exception e) {
            log.error("Error fetching feedbacks", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching feedbacks: " + e.getMessage()));
        }
    }

    // Update feedback (Patient - only their own)
    @PutMapping("/{feedbackId}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<FeedbackResponse>> updateFeedback(
            @PathVariable Long feedbackId,
            @Valid @RequestBody FeedbackRequest request) {
        try {
            Long patientId = authenticationService.getCurrentUserId();
            log.info("Patient {} updating feedback {}", patientId, feedbackId);

            FeedbackResponse feedback = feedbackService.updateFeedback(feedbackId, patientId, request);
            return ResponseEntity.ok(ApiResponse. success("Feedback updated successfully", feedback));
        } catch (Exception e) {
            log.error("Error updating feedback", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse. error("Error updating feedback: " + e.getMessage()));
        }
    }

    // Delete feedback (Patient - only their own)
    @DeleteMapping("/{feedbackId}")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<String>> deleteFeedback(@PathVariable Long feedbackId) {
        try {
            Long patientId = authenticationService.getCurrentUserId();
            log.info("Patient {} deleting feedback {}", patientId, feedbackId);

            feedbackService.deleteFeedback(feedbackId, patientId);
            return ResponseEntity.ok(ApiResponse.success("Feedback deleted successfully", null));
        } catch (Exception e) {
            log.error("Error deleting feedback", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Error deleting feedback: " + e.getMessage()));
        }
    }

}