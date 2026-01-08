package com. medvault.service;

import com.medvault.dto. request.FeedbackRequest;
import com.medvault.dto.response.FeedbackResponse;
import com.medvault. exception.ResourceNotFoundException;
import com.medvault.model.Appointment;
import com.medvault.model.Doctor;
import com.medvault.model. Feedback;
import com.medvault.model.Patient;
import com.medvault.repository. AppointmentRepository;
import com.medvault.repository.DoctorRepository;
import com. medvault.repository.FeedbackRepository;
import com.medvault.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok. extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation. Transactional;

import java. util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    @Transactional
    public FeedbackResponse submitFeedback(Long patientId, FeedbackRequest request) {
        log.info("Submitting feedback from patient {} for doctor {}", patientId, request.getDoctorId());

        Patient patient = patientRepository.findById(patientId)
                . orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        Doctor doctor = doctorRepository. findById(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        // Optional: Check if appointment exists
        Appointment appointment = null;
        if (request.getAppointmentId() != null) {
            appointment = appointmentRepository.findById(request.getAppointmentId())
                    . orElseThrow(() -> new ResourceNotFoundException("Appointment not found"));

            // Verify the appointment belongs to this patient and doctor
            if (!appointment.getPatient().getId().equals(patientId) ||
                    !appointment.getDoctor().getId().equals(request.getDoctorId())) {
                throw new IllegalStateException("Invalid appointment reference");
            }
        }

        Feedback feedback = Feedback. builder()
                .patient(patient)
                .doctor(doctor)
                .appointment(appointment)
                .rating(request.getRating())
                .comment(request. getComment())
                .build();

        Feedback savedFeedback = feedbackRepository.save(feedback);
        log.info("✅ Feedback submitted successfully: {}", savedFeedback.getId());

        return convertToResponse(savedFeedback);
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getDoctorFeedbacks(Long doctorId) {
        return feedbackRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> getPatientFeedbacks(Long patientId) {
        return feedbackRepository.findByPatientIdOrderByCreatedAtDesc(patientId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDoctorStats(Long doctorId) {
        List<Feedback> feedbacks = feedbackRepository.findByDoctorId(doctorId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFeedbacks", feedbacks. size());

        if (feedbacks.isEmpty()) {
            stats.put("averageRating", 0.0);
            stats.put("ratingDistribution", Map.of(
                    "5", 0L, "4", 0L, "3", 0L, "2", 0L, "1", 0L
            ));
        } else {
            double averageRating = feedbacks.stream()
                    .mapToInt(Feedback::getRating)
                    .average()
                    .orElse(0.0);
            stats.put("averageRating", Math.round(averageRating * 10.0) / 10.0);

            Map<String, Long> distribution = new HashMap<>();
            distribution.put("5", feedbacks.stream(). filter(f -> f.getRating() == 5).count());
            distribution.put("4", feedbacks.stream().filter(f -> f.getRating() == 4).count());
            distribution.put("3", feedbacks.stream(). filter(f -> f.getRating() == 3).count());
            distribution.put("2", feedbacks.stream().filter(f -> f.getRating() == 2).count());
            distribution.put("1", feedbacks.stream(). filter(f -> f.getRating() == 1).count());
            stats.put("ratingDistribution", distribution);
        }

        return stats;
    }

    @Transactional
    public FeedbackResponse updateFeedback(Long feedbackId, Long patientId, FeedbackRequest request) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found"));

        // Verify ownership
        if (!feedback.getPatient().getId().equals(patientId)) {
            throw new IllegalStateException("You can only update your own feedback");
        }

        feedback.setRating(request.getRating());
        feedback.setComment(request.getComment());

        Feedback updatedFeedback = feedbackRepository.save(feedback);
        log.info("✅ Feedback updated successfully: {}", feedbackId);

        return convertToResponse(updatedFeedback);
    }

    @Transactional
    public void deleteFeedback(Long feedbackId, Long patientId) {
        Feedback feedback = feedbackRepository. findById(feedbackId)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback not found"));

        // Verify ownership
        if (!feedback.getPatient().getId().equals(patientId)) {
            throw new IllegalStateException("You can only delete your own feedback");
        }

        feedbackRepository.delete(feedback);
        log.info("✅ Feedback deleted successfully: {}", feedbackId);
    }

    private FeedbackResponse convertToResponse(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback. getId())
                .patientId(feedback.getPatient().getId())
                .patientName(feedback.getPatient().getFirstName() + " " +
                        feedback.getPatient().getLastName())
                . doctorId(feedback.getDoctor().getId())
                .doctorName("Dr. " + feedback.getDoctor().getFirstName() + " " +
                        feedback.getDoctor().getLastName())
                .doctorSpecialization(feedback.getDoctor().getSpecialization())
                .appointmentId(feedback. getAppointment() != null ? feedback.getAppointment().getId() : null)
                .rating(feedback.getRating())
                .comment(feedback. getComment())
                .createdAt(feedback.getCreatedAt())
                .build();
    }

}