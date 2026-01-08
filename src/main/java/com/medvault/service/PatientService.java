package com.medvault.service;

import com.medvault.dto.request.UpdatePatientProfileRequest;
import com.medvault.dto.response.DashboardResponse;
import com.medvault.exception.ResourceNotFoundException;
import com.medvault.model.Patient;
import com.medvault.repository.AppointmentRepository;
import com.medvault.repository.MedicalRecordRepository;
import com.medvault.repository.NotificationRepository;
import com.medvault.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final NotificationRepository notificationRepository;

    public Patient getPatientById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));
    }

    public DashboardResponse getPatientDashboard(Long patientId) {
        Patient patient = getPatientById(patientId);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalAppointments",
                appointmentRepository.findByPatientIdOrderByAppointmentDateTimeDesc(patientId).size());
        statistics.put("upcomingAppointments",
                appointmentRepository.findUpcomingAppointmentsByPatient(patientId, LocalDateTime.now()).size());
        statistics.put("totalRecords", medicalRecordRepository.countByPatientIdAndIsDeletedFalse(patientId));
        statistics.put("unreadNotifications", notificationRepository.countByPatientIdAndIsReadFalse(patientId));

        return DashboardResponse.builder()
                .userId(patient.getId())
                .userName(patient.getFirstName() + " " + patient.getLastName())
                .role("PATIENT")
                .statistics(statistics)
                .recentActivity(appointmentRepository.findUpcomingAppointmentsByPatient(patientId, LocalDateTime.now()))
                .build();
    }

    @Transactional
    public Patient updateProfile(Long patientId, UpdatePatientProfileRequest request) {
        Patient patient = getPatientById(patientId);

        // Update basic info
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setPhoneNumber(request.getPhoneNumber());

        // Update patient-specific fields
        if (request.getDateOfBirth() != null) {
            patient.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            patient.setGender(request.getGender());
        }
        if (request.getBloodGroup() != null) {
            patient.setBloodGroup(request.getBloodGroup());
        }
        if (request.getAddress() != null) {
            patient.setAddress(request.getAddress());
        }
        if (request.getMedicalHistory() != null) {
            patient.setMedicalHistory(request.getMedicalHistory());
        }
        if (request.getAllergies() != null) {
            patient.setAllergies(request.getAllergies());
        }
        if (request.getEmergencyContactName() != null) {
            patient.setEmergencyContactName(request.getEmergencyContactName());
        }
        if (request.getEmergencyContactPhone() != null) {
            patient.setEmergencyContactPhone(request.getEmergencyContactPhone());
        }

        return patientRepository.save(patient);
    }
}