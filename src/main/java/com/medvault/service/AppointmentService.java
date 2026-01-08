package com.medvault.service;

import com.medvault.dto.request.AppointmentRequest;
import com.medvault.dto.response.AppointmentResponse;
import com.medvault.exception.ResourceNotFoundException;
import com.medvault.model.Appointment;
import com.medvault.model.Doctor;
import com.medvault.model.Patient;
import com.medvault.model.enums.AppointmentStatus;
import com.medvault.repository.AppointmentRepository;
import com.medvault.repository.DoctorRepository;
import com.medvault.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;

    // Get appointments for a patient
    public List<AppointmentResponse> getPatientAppointments(Long patientId) {
        log.info("Fetching appointments for patient: {}", patientId);
        return appointmentRepository.findByPatientIdOrderByAppointmentDateTimeDesc(patientId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // Get appointments for a doctor
    public List<AppointmentResponse> getDoctorAppointments(Long doctorId) {
        log.info("Fetching appointments for doctor: {}", doctorId);
        return appointmentRepository.findByDoctorIdOrderByAppointmentDateTimeDesc(doctorId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // Book a new appointment
    @Transactional
    public AppointmentResponse bookAppointment(Long patientId, AppointmentRequest request) {
        log.info("Booking appointment for patient: {} with doctor: {}", patientId, request.getDoctorId());

        // Validate patient exists
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + patientId));

        // Validate doctor exists
        Doctor doctor = doctorRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found with id: " + request.getDoctorId()));

        // Check if doctor is active
        if (!doctor.getIsActive()) {
            throw new IllegalStateException("Doctor is not currently accepting appointments");
        }

        // Create new appointment using Builder
        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .appointmentDateTime(request.getAppointmentDateTime())
                .reasonForVisit(request.getReasonForVisit())
                .symptoms(request.getSymptoms())
                .status(AppointmentStatus.PENDING)
                .build();

        // Save appointment
        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Create notification for patient
        try {
            String doctorName = "Dr. " + doctor.getFirstName() + " " + doctor.getLastName();
            notificationService.createNotification(
                    patient,
                    "Appointment Booked",
                    String.format("Your appointment with %s has been booked successfully and is pending approval.",
                            doctorName),
                    "APPOINTMENT");
        } catch (Exception e) {
            log.error("Failed to create notification", e);
        }

        // Send confirmation email to patient
        try {
            String patientName = patient.getFirstName() + " " + patient.getLastName();
            String doctorName = "Dr. " + doctor.getFirstName() + " " + doctor.getLastName();
            emailService.sendAppointmentConfirmationEmail(
                    patient.getEmail(),
                    patientName,
                    doctorName,
                    request.getAppointmentDateTime().toString());
        } catch (Exception e) {
            log.error("Failed to send appointment confirmation email", e);
            // Don't fail the appointment booking if email fails
        }

        // Send notification email to doctor about new booking
        try {
            String patientName = patient.getFirstName() + " " + patient.getLastName();
            String doctorName = doctor.getFirstName() + " " + doctor.getLastName();
            emailService.sendDoctorNewBookingEmail(
                    doctor.getEmail(),
                    doctorName,
                    patientName,
                    request.getAppointmentDateTime().toString(),
                    request.getReasonForVisit());
            log.info("✅ Notification email sent to doctor: {}", doctor.getEmail());
        } catch (Exception e) {
            log.error("Failed to send doctor notification email", e);
            // Don't fail the appointment booking if email fails
        }

        log.info("✅ Appointment booked successfully: {}", savedAppointment.getId());
        return convertToResponse(savedAppointment);
    }

    // Update appointment status (for doctors)
    @Transactional
    public AppointmentResponse updateAppointmentStatus(Long appointmentId, AppointmentStatus status,
            String notes, String doctorId) {
        log.info("Updating appointment {} status to {}", appointmentId, status);

        // Find appointment
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));

        // Verify doctor owns this appointment
        if (!appointment.getDoctor().getId().toString().equals(doctorId)) {
            throw new IllegalStateException("You can only update your own appointments");
        }

        // Update status and notes
        appointment.setStatus(status);

        if (notes != null && !notes.isEmpty()) {
            if (status == AppointmentStatus.REJECTED) {
                appointment.setRejectionReason(notes);
            } else {
                appointment.setDoctorNotes(notes);
            }
        }

        Appointment updatedAppointment = appointmentRepository.save(appointment);

        // Create notification for patient
        try {
            String doctorName = "Dr. " + appointment.getDoctor().getFirstName() + " " +
                    appointment.getDoctor().getLastName();
            String notificationTitle = "Appointment Status Updated";
            String notificationMessage;

            switch (status) {
                case APPROVED:
                    notificationMessage = String.format("Your appointment with %s has been approved.", doctorName);
                    break;
                case REJECTED:
                    notificationMessage = String.format("Your appointment with %s has been rejected.", doctorName);
                    break;
                case COMPLETED:
                    notificationMessage = String
                            .format("Your appointment with %s has been completed. Please leave feedback!", doctorName);

                    // Send feedback request email
                    try {
                        String patientName = appointment.getPatient().getFirstName() + " " +
                                appointment.getPatient().getLastName();
                        emailService.sendFeedbackRequestEmail(
                                appointment.getPatient().getEmail(),
                                patientName,
                                doctorName,
                                appointment.getId());
                    } catch (Exception ex) {
                        log.error("Failed to send feedback request email", ex);
                    }
                    break;
                case CANCELLED:
                    notificationMessage = String.format("Your appointment with %s has been cancelled.", doctorName);
                    break;
                default:
                    notificationMessage = String.format("Your appointment status with %s has been updated to %s.",
                            doctorName, status.name());
            }

            notificationService.createNotification(
                    appointment.getPatient(),
                    notificationTitle,
                    notificationMessage,
                    "APPOINTMENT");
        } catch (Exception e) {
            log.error("Failed to create notification", e);
        }

        // Send status update email to patient
        try {
            String patientName = appointment.getPatient().getFirstName() + " " +
                    appointment.getPatient().getLastName();
            String doctorName = "Dr.  " + appointment.getDoctor().getFirstName() + " " +
                    appointment.getDoctor().getLastName();

            emailService.sendAppointmentStatusEmail(
                    appointment.getPatient().getEmail(),
                    patientName,
                    status.name(),
                    doctorName,
                    appointment.getAppointmentDateTime().toString());
        } catch (Exception e) {
            log.error("Failed to send appointment status email", e);
        }

        log.info("✅ Appointment status updated successfully");
        return convertToResponse(updatedAppointment);
    }

    // Get appointment by ID
    public AppointmentResponse getAppointmentById(Long id) {
        log.info("Fetching appointment: {}", id);
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + id));
        return convertToResponse(appointment);
    }

    // Cancel appointment (for patients)
    @Transactional
    public void cancelAppointment(Long appointmentId, Long userId) {
        log.info("Cancelling appointment: {} by user: {}", appointmentId, userId);

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found with id: " + appointmentId));

        // Verify the user has permission to cancel (must be the patient who booked it)
        if (!appointment.getPatient().getId().equals(userId)) {
            throw new IllegalStateException("You can only cancel your own appointments");
        }

        // Check if appointment can be cancelled
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel completed appointments");
        }

        // Update status to REJECTED (cancelled by patient)
        appointment.setStatus(AppointmentStatus.REJECTED);
        appointment.setRejectionReason("Cancelled by patient");
        appointmentRepository.save(appointment);

        log.info("✅ Appointment cancelled successfully");
    }

    // Helper method to convert Appointment entity to AppointmentResponse DTO
    private AppointmentResponse convertToResponse(Appointment appointment) {
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .patientId(appointment.getPatient().getId())
                .patientName(appointment.getPatient().getFirstName() + " " +
                        appointment.getPatient().getLastName())
                .doctorId(appointment.getDoctor().getId())
                .doctorName("Dr. " + appointment.getDoctor().getFirstName() + " " +
                        appointment.getDoctor().getLastName())
                .doctorSpecialization(appointment.getDoctor().getSpecialization())
                .appointmentDateTime(appointment.getAppointmentDateTime())
                .reasonForVisit(appointment.getReasonForVisit()) // Changed from . reason()
                .symptoms(appointment.getSymptoms())
                .status(appointment.getStatus())
                .doctorNotes(appointment.getDoctorNotes())
                .rejectionReason(appointment.getRejectionReason())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .build();
    }
}