package com.medvault.service;

import com.medvault.dto.request.MedicalRecordRequest;
import com.medvault.dto.response.MedicalRecordResponse;
import com.medvault.exception.ResourceNotFoundException;
import com.medvault.model.Doctor;
import com.medvault.model.MedicalRecord;
import com.medvault.model.Patient;
import com.medvault.model.enums.RecordType;
import com.medvault.repository.MedicalRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final CloudinaryService cloudinaryService;

    @Value("${file.upload-dir:./uploads/medical-records}")
    private String uploadDir;

    // For the controller - returns DTOs
    public List<MedicalRecordResponse> getPatientRecords(Long patientId) {
        log.info("Fetching medical records for patient: {}", patientId);
        return medicalRecordRepository.findByPatientIdAndIsDeletedFalseOrderByRecordDateDesc(patientId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // Upload with simple parameters (for controller)
    @Transactional
    public MedicalRecordResponse uploadRecord(Long patientId, MultipartFile file,
            String recordType, String description) {
        try {
            log.info("Uploading medical record for patient: {}", patientId);

            // Validate file
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File is required");
            }

            // Validate file size (10MB max)
            long maxSize = 10 * 1024 * 1024; // 10MB in bytes
            if (file.getSize() > maxSize) {
                throw new IllegalArgumentException("File size must not exceed 10MB");
            }

            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !isValidFileType(contentType)) {
                throw new IllegalArgumentException("Invalid file type. Allowed: PDF, JPG, PNG, DOCX");
            }

            Patient patient = patientService.getPatientById(patientId);

            // Upload file to Cloudinary
            log.info("📤 Uploading file to Cloudinary...");
            Map<String, Object> uploadResult = cloudinaryService.uploadFile(file, "medvault/medical-records");

            String cloudinaryUrl = (String) uploadResult.get("secure_url");
            String cloudinaryPublicId = (String) uploadResult.get("public_id");

            // Create record
            MedicalRecord record = MedicalRecord.builder()
                    .patient(patient)
                    .recordType(RecordType.valueOf(recordType.toUpperCase()))
                    .title(file.getOriginalFilename())
                    .description(description)
                    .fileName(file.getOriginalFilename())
                    .filePath(cloudinaryUrl) // Store Cloudinary URL
                    .fileType(file.getContentType())
                    .fileSize(file.getSize())
                    .recordDate(LocalDate.now())
                    .isDeleted(false)
                    .build();

            MedicalRecord savedRecord = medicalRecordRepository.save(record);
            log.info("✅ Medical record uploaded successfully with Cloudinary: {}", savedRecord.getId());

            return convertToResponse(savedRecord);
        } catch (IOException e) {
            log.error("❌ Error uploading file to Cloudinary", e);
            throw new RuntimeException("Failed to upload file: " + e.getMessage());
        }
    }

    // Validate file type
    private boolean isValidFileType(String contentType) {
        return contentType.equals("application/pdf") ||
                contentType.equals("image/jpeg") ||
                contentType.equals("image/jpg") ||
                contentType.equals("image/png") ||
                contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    // Create with full request object (for advanced use)
    @Transactional
    public MedicalRecord createMedicalRecord(Long patientId, MedicalRecordRequest request,
            MultipartFile file) throws IOException {
        Patient patient = patientService.getPatientById(patientId);

        MedicalRecord record = MedicalRecord.builder()
                .patient(patient)
                .recordType(request.getRecordType())
                .title(request.getTitle())
                .description(request.getDescription())
                .recordDate(request.getRecordDate())
                .isDeleted(false)
                .build();

        if (request.getDoctorId() != null) {
            Doctor doctor = doctorService.getDoctorById(request.getDoctorId());
            record.setDoctor(doctor);
        }

        if (file != null && !file.isEmpty()) {
            // Upload to Cloudinary
            Map<String, Object> uploadResult = cloudinaryService.uploadFile(file, "medvault/medical-records");
            String cloudinaryUrl = (String) uploadResult.get("secure_url");

            record.setFileName(file.getOriginalFilename());
            record.setFilePath(cloudinaryUrl);
            record.setFileType(file.getContentType());
            record.setFileSize(file.getSize());
        }

        return medicalRecordRepository.save(record);
    }

    // Get records by type
    public List<MedicalRecord> getPatientRecordsByType(Long patientId, RecordType recordType) {
        return medicalRecordRepository.findByPatientIdAndRecordTypeAndIsDeletedFalseOrderByRecordDateDesc(
                patientId, recordType);
    }

    // Get single record
    public MedicalRecord getRecordById(Long recordId) {
        return medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record not found"));
    }

    // Update record
    @Transactional
    public MedicalRecord updateMedicalRecord(Long recordId, MedicalRecordRequest request,
            MultipartFile file) throws IOException {
        MedicalRecord record = getRecordById(recordId);

        record.setRecordType(request.getRecordType());
        record.setTitle(request.getTitle());
        record.setDescription(request.getDescription());
        record.setRecordDate(request.getRecordDate());

        if (request.getDoctorId() != null) {
            Doctor doctor = doctorService.getDoctorById(request.getDoctorId());
            record.setDoctor(doctor);
        }

        if (file != null && !file.isEmpty()) {
            // Upload new file to Cloudinary
            Map<String, Object> uploadResult = cloudinaryService.uploadFile(file, "medvault/medical-records");
            String cloudinaryUrl = (String) uploadResult.get("secure_url");

            record.setFileName(file.getOriginalFilename());
            record.setFilePath(cloudinaryUrl);
            record.setFileType(file.getContentType());
            record.setFileSize(file.getSize());
        }

        return medicalRecordRepository.save(record);
    }

    // Delete record (soft delete)
    @Transactional
    public void deleteRecord(Long recordId, Long userId) {
        MedicalRecord record = getRecordById(recordId);

        // Verify user owns this record
        if (!record.getPatient().getId().equals(userId)) {
            throw new IllegalStateException("You can only delete your own records");
        }

        record.setIsDeleted(true);
        record.setDeletedAt(LocalDateTime.now());
        medicalRecordRepository.save(record);

        log.info("✅ Medical record deleted: {}", recordId);
    }

    // Save file to disk
    private String saveFile(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir);

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }

    // Convert entity to DTO - FIXED
    private MedicalRecordResponse convertToResponse(MedicalRecord record) {
        String doctorName = null;
        if (record.getDoctor() != null) {
            doctorName = "Dr. " + record.getDoctor().getFirstName() + " " +
                    record.getDoctor().getLastName();
        }

        // Convert LocalDate to LocalDateTime for response
        LocalDateTime uploadedAt = record.getCreatedAt();
        if (uploadedAt == null && record.getRecordDate() != null) {
            // Convert LocalDate to LocalDateTime at start of day
            uploadedAt = record.getRecordDate().atStartOfDay();
        }
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }

        return MedicalRecordResponse.builder()
                .id(record.getId())
                .patientId(record.getPatient().getId())
                .patientName(record.getPatient().getFirstName() + " " +
                        record.getPatient().getLastName())
                .recordType(record.getRecordType() != null ? record.getRecordType().name() : null)
                .fileName(record.getFileName())
                .filePath(record.getFilePath())
                .description(record.getDescription())
                .recordDate(record.getRecordDate())
                .uploadedAt(uploadedAt) // Now properly converted
                .uploadedBy(record.getPatient().getId())
                .uploadedByName(doctorName != null ? doctorName
                        : record.getPatient().getFirstName() + " " +
                                record.getPatient().getLastName())
                .build();
    }
}