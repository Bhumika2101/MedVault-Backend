package com.medvault.controller;

import com.medvault. dto.response.ApiResponse;
import com. medvault.dto.response. MedicalRecordResponse;
import com.medvault.model.User;
import com.medvault.service.AuthenticationService;
import com.medvault.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j. Slf4j;
import org. springframework.http.ResponseEntity;
import org.springframework. security.access.prepost.PreAuthorize;
import org. springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/medical-records")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;
    private final AuthenticationService authenticationService;

    @GetMapping
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<List<MedicalRecordResponse>>> getMyRecords() {
        try {
            Long patientId = authenticationService.getCurrentUserId();
            log.info("Fetching medical records for patient: {}", patientId);

            List<MedicalRecordResponse> records = medicalRecordService.getPatientRecords(patientId);
            return ResponseEntity.ok(ApiResponse. success("Medical records retrieved", records));
        } catch (Exception e) {
            log.error("Error fetching medical records", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error fetching records: " + e.getMessage()));
        }
    }

    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<MedicalRecordResponse>>> getPatientRecords(@PathVariable Long patientId) {
        try {
            log.info("Fetching medical records for patient: {}", patientId);
            List<MedicalRecordResponse> records = medicalRecordService. getPatientRecords(patientId);
            return ResponseEntity. ok(ApiResponse.success("Medical records retrieved", records));
        } catch (Exception e) {
            log.error("Error fetching medical records", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse. error("Error fetching records: " + e.getMessage()));
        }
    }

    @PostMapping("/upload")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<ApiResponse<MedicalRecordResponse>> uploadRecord(
            @RequestParam("file") MultipartFile file,
            @RequestParam("recordType") String recordType,
            @RequestParam(value = "description", required = false) String description) {
        try {
            Long patientId = authenticationService. getCurrentUserId();
            log. info("Uploading medical record for patient: {}", patientId);

            MedicalRecordResponse record = medicalRecordService.uploadRecord(patientId, file, recordType, description);
            return ResponseEntity. ok(ApiResponse.success("Record uploaded successfully", record));
        } catch (Exception e) {
            log.error("Error uploading medical record", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Error uploading record: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PATIENT', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteRecord(@PathVariable Long id) {
        try {
            Long userId = authenticationService.getCurrentUserId();
            medicalRecordService.deleteRecord(id, userId);
            return ResponseEntity.ok(ApiResponse.success("Record deleted successfully", null));
        } catch (Exception e) {
            log.error("Error deleting medical record", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Error deleting record: " + e.getMessage()));
        }
    }
}