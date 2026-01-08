package com.medvault.dto.response;

import com.medvault.model. enums.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {
    private Long id;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private String doctorSpecialization;
    private LocalDateTime appointmentDateTime;
    private String reasonForVisit;  // Added this field
    private String symptoms;         // Added this field
    private AppointmentStatus status;
    private String doctorNotes;      // Added this field
    private String rejectionReason;  // Added this field
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}