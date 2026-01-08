package com.medvault.service;

import com.medvault.dto.request.DoctorCreationRequest;
import com.medvault.dto.request.UpdateDoctorProfileRequest;
import com.medvault.dto.response.DashboardResponse;
import com.medvault.dto.response.UserResponse;
import com.medvault.exception.ResourceNotFoundException;
import com.medvault.model.Doctor;
import com.medvault.model.enums.AppointmentStatus;
import com.medvault.model.enums.Role;
import com.medvault.repository.AppointmentRepository;
import com.medvault.repository.DoctorRepository;
import com.medvault.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorService {

        private final DoctorRepository doctorRepository;
        private final AppointmentRepository appointmentRepository;
        private final FeedbackRepository feedbackRepository;
        private final EmailService emailService;
        private final PasswordEncoder passwordEncoder;

        // ===================== CREATE DOCTOR =====================
        @Transactional
        public UserResponse createDoctor(DoctorCreationRequest request) {
                // Check if doctor already exists by email
                Optional<Doctor> existingDoctorByEmail = doctorRepository.findByEmail(request.getEmail());
                if (existingDoctorByEmail.isPresent()) {
                        Doctor existingDoctor = existingDoctorByEmail.get();

                        // If doctor exists but token expired and not verified, delete and recreate
                        if (!existingDoctor.getIsPasswordSet() &&
                                        existingDoctor.getPasswordResetTokenExpiry() != null &&
                                        existingDoctor.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {

                                doctorRepository.delete(existingDoctor);
                                // Continue with creating new doctor
                        } else if (existingDoctor.getIsPasswordSet() || existingDoctor.getIsActive()) {
                                // Doctor exists and is verified/active
                                throw new IllegalStateException("Doctor with this email already exists");
                        } else {
                                // Doctor exists but not verified yet, token still valid
                                throw new IllegalStateException(
                                                "Doctor with this email already exists. Please check email for verification link.");
                        }
                }

                if (doctorRepository.existsByLicenseNumber(request.getLicenseNumber())) {
                        throw new IllegalStateException("Doctor with this license number already exists");
                }

                // Validate consultation fee (minimum ₹1 as per Razorpay requirement)
                if (request.getConsultationFee() == null || request.getConsultationFee() < 1.0) {
                        throw new IllegalStateException("Consultation fee is required and must be at least ₹1");
                }

                Doctor doctor = new Doctor();
                doctor.setEmail(request.getEmail());
                doctor.setFirstName(request.getFirstName());
                doctor.setLastName(request.getLastName());
                doctor.setPhoneNumber(request.getPhoneNumber());
                doctor.setSpecialization(request.getSpecialization());
                doctor.setLicenseNumber(request.getLicenseNumber());
                doctor.setQualification(request.getQualification());
                doctor.setExperienceYears(request.getExperienceYears());
                doctor.setBio(request.getBio());
                doctor.setHospitalAffiliation(request.getHospitalAffiliation());
                doctor.setConsultationFee(request.getConsultationFee());
                doctor.setAvailableTimings(request.getAvailableTimings());
                doctor.setRole(Role.DOCTOR);

                // Generate temporary password
                String tempPassword = UUID.randomUUID().toString();
                doctor.setPassword(passwordEncoder.encode(tempPassword));
                doctor.setIsActive(false);
                doctor.setIsPasswordSet(false);

                // Generate password reset token
                String resetToken = UUID.randomUUID().toString();
                doctor.setPasswordResetToken(resetToken);
                doctor.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(24));

                Doctor savedDoctor = doctorRepository.save(doctor);

                // Send email to doctor for account activation & password setup
                emailService.sendSetPasswordEmail(
                                savedDoctor.getEmail(),
                                savedDoctor.getFirstName() + " " + savedDoctor.getLastName(),
                                resetToken);

                return UserResponse.builder()
                                .id(savedDoctor.getId())
                                .email(savedDoctor.getEmail())
                                .firstName(savedDoctor.getFirstName())
                                .lastName(savedDoctor.getLastName())
                                .phoneNumber(savedDoctor.getPhoneNumber())
                                .role(savedDoctor.getRole())
                                .isActive(savedDoctor.getIsActive())
                                .createdAt(savedDoctor.getCreatedAt())
                                .build();
        }

        // ===================== GET DOCTOR BY ID =====================
        public Doctor getDoctorById(Long id) {
                return doctorRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + id));
        }

        // ===================== GET ALL ACTIVE DOCTORS (For Admin)
        // =====================
        public List<UserResponse> getAllActiveDoctors() {
                return doctorRepository.findByIsActiveTrue().stream()
                                .map(doctor -> UserResponse.builder()
                                                .id(doctor.getId())
                                                .email(doctor.getEmail())
                                                .firstName(doctor.getFirstName())
                                                .lastName(doctor.getLastName())
                                                .phoneNumber(doctor.getPhoneNumber())
                                                .role(doctor.getRole())
                                                .isActive(doctor.getIsActive())
                                                .createdAt(doctor.getCreatedAt())
                                                .specialization(doctor.getSpecialization())
                                                .consultationFee(doctor.getConsultationFee())
                                                .isAvailable(doctor.getIsAvailable())
                                                .build())
                                .collect(Collectors.toList());
        }

        // ===================== GET AVAILABLE DOCTORS (For Patients)
        // =====================
        public List<UserResponse> getAvailableDoctors() {
                return doctorRepository.findByIsActiveTrueAndIsAvailableTrue().stream()
                                .map(doctor -> UserResponse.builder()
                                                .id(doctor.getId())
                                                .email(doctor.getEmail())
                                                .firstName(doctor.getFirstName())
                                                .lastName(doctor.getLastName())
                                                .phoneNumber(doctor.getPhoneNumber())
                                                .role(doctor.getRole())
                                                .isActive(doctor.getIsActive())
                                                .createdAt(doctor.getCreatedAt())
                                                .specialization(doctor.getSpecialization())
                                                .consultationFee(doctor.getConsultationFee())
                                                .isAvailable(doctor.getIsAvailable())
                                                .build())
                                .collect(Collectors.toList());
        }

        // ===================== DOCTOR DASHBOARD =====================
        public DashboardResponse getDoctorDashboard(Long doctorId) {
                Doctor doctor = getDoctorById(doctorId);

                Map<String, Object> statistics = new HashMap<>();
                statistics.put("totalAppointments",
                                appointmentRepository.findByDoctorIdOrderByAppointmentDateTimeDesc(doctorId).size());
                statistics.put("pendingAppointments",
                                appointmentRepository.findByDoctorIdAndStatusOrderByAppointmentDateTimeDesc(
                                                doctorId, AppointmentStatus.PENDING).size());
                statistics.put("averageRating",
                                feedbackRepository.getAverageRatingByDoctorId(doctorId));
                statistics.put("totalFeedbacks",
                                feedbackRepository.countByDoctorId(doctorId));

                return DashboardResponse.builder()
                                .userId(doctor.getId())
                                .userName("Dr. " + doctor.getFirstName() + " " + doctor.getLastName())
                                .role("DOCTOR")
                                .statistics(statistics)
                                .recentActivity(appointmentRepository
                                                .findByDoctorIdAndStatusOrderByAppointmentDateTimeDesc(
                                                                doctorId, AppointmentStatus.PENDING))
                                .build();
        }

        // ===================== RESEND INVITATION =====================
        @Transactional
        public void resendInvitation(Long doctorId) {
                Doctor doctor = getDoctorById(doctorId);

                if (doctor.getIsPasswordSet()) {
                        throw new IllegalStateException("Doctor has already set their password");
                }

                // Generate new reset token with extended expiry
                String resetToken = UUID.randomUUID().toString();
                doctor.setPasswordResetToken(resetToken);
                doctor.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(24));

                doctorRepository.save(doctor);

                // Resend email
                emailService.sendSetPasswordEmail(
                                doctor.getEmail(),
                                doctor.getFirstName() + " " + doctor.getLastName(),
                                resetToken);
        }

        // ===================== CLEANUP EXPIRED UNVERIFIED DOCTORS
        // =====================
        @Transactional
        public int cleanupExpiredUnverifiedDoctors() {
                List<Doctor> allDoctors = doctorRepository.findAll();
                int deletedCount = 0;

                for (Doctor doctor : allDoctors) {
                        if (!doctor.getIsPasswordSet() &&
                                        doctor.getPasswordResetTokenExpiry() != null &&
                                        doctor.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {

                                doctorRepository.delete(doctor);
                                deletedCount++;
                        }
                }

                return deletedCount;
        }

        // ===================== TOGGLE DOCTOR AVAILABILITY =====================
        @Transactional
        public UserResponse toggleDoctorAvailability(Long doctorId) {
                Doctor doctor = getDoctorById(doctorId);
                doctor.setIsAvailable(!doctor.getIsAvailable());
                Doctor updatedDoctor = doctorRepository.save(doctor);

                return UserResponse.builder()
                                .id(updatedDoctor.getId())
                                .email(updatedDoctor.getEmail())
                                .firstName(updatedDoctor.getFirstName())
                                .lastName(updatedDoctor.getLastName())
                                .phoneNumber(updatedDoctor.getPhoneNumber())
                                .role(updatedDoctor.getRole())
                                .isActive(updatedDoctor.getIsActive())
                                .createdAt(updatedDoctor.getCreatedAt())
                                .build();
        }

        // ===================== UPDATE DOCTOR PROFILE =====================
        @Transactional
        public Doctor updateProfile(Long doctorId, UpdateDoctorProfileRequest request) {
                Doctor doctor = getDoctorById(doctorId);

                // Update basic info
                doctor.setFirstName(request.getFirstName());
                doctor.setLastName(request.getLastName());
                doctor.setPhoneNumber(request.getPhoneNumber());

                // Update doctor-specific fields
                if (request.getSpecialization() != null) {
                        doctor.setSpecialization(request.getSpecialization());
                }
                if (request.getQualification() != null) {
                        doctor.setQualification(request.getQualification());
                }
                if (request.getExperienceYears() != null) {
                        doctor.setExperienceYears(request.getExperienceYears());
                }
                if (request.getBio() != null) {
                        doctor.setBio(request.getBio());
                }
                if (request.getHospitalAffiliation() != null) {
                        doctor.setHospitalAffiliation(request.getHospitalAffiliation());
                }
                if (request.getAvailableTimings() != null) {
                        doctor.setAvailableTimings(request.getAvailableTimings());
                }

                return doctorRepository.save(doctor);
        }
}
