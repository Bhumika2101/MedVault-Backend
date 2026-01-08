package com.medvault.dto.request;

import com.medvault.model.enums.RecordType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation. constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class MedicalRecordRequest {
    
    @NotNull(message = "Record type is required")
    private RecordType recordType;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    @NotNull(message = "Record date is required")
    private LocalDate recordDate;
    
    private Long doctorId;
}