package com.medvault.controller;

import com.medvault.dto.response.ApiResponse;
import com.medvault.model.Notification;
import com.medvault.service.AuthenticationService;
import com.medvault.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('PATIENT')")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthenticationService authenticationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Notification>>> getMyNotifications() {
        Long patientId = authenticationService.getCurrentUserId();
        List<Notification> notifications = notificationService.getPatientNotifications(patientId);
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved successfully", notifications));
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<Notification>>> getUnreadNotifications() {
        Long patientId = authenticationService.getCurrentUserId();
        List<Notification> notifications = notificationService.getUnreadNotifications(patientId);
        return ResponseEntity.ok(ApiResponse.success("Unread notifications retrieved successfully", notifications));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    @PutMapping("/mark-all-read")
    public ResponseEntity<ApiResponse<String>> markAllAsRead() {
        Long patientId = authenticationService.getCurrentUserId();
        notificationService.markAllAsRead(patientId);
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }
}