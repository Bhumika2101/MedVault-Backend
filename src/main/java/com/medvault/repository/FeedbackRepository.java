package com.medvault.repository;

import com.medvault.model.Feedback;
import org. springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa. repository.Query;
import org. springframework.data.repository.query. Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    // Find feedbacks by doctor
    List<Feedback> findByDoctorIdOrderByCreatedAtDesc(Long doctorId);

    List<Feedback> findByDoctorId(Long doctorId);

    // Find feedbacks by patient
    List<Feedback> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    // Find feedbacks by appointment
    List<Feedback> findByAppointmentId(Long appointmentId);

    // Count feedbacks for a doctor
    Long countByDoctorId(Long doctorId);

    // Get average rating for a doctor
    @Query("SELECT AVG(f.rating) FROM Feedback f WHERE f.doctor.id = :doctorId")
    Double getAverageRatingByDoctorId(@Param("doctorId") Long doctorId);

    // Count feedbacks by rating for a doctor
    Long countByDoctorIdAndRating(Long doctorId, Integer rating);
}