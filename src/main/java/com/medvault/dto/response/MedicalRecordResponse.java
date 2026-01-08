package com.medvault.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecordResponse {
    private Long id;
    private Long patientId;
    private String patientName;
    private String recordType;
    private String fileName;
    private String filePath;
    private String description;
    private LocalDate recordDate;
    private LocalDateTime uploadedAt;
    private Long uploadedBy;
    private String uploadedByName;
}