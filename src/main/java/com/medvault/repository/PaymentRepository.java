package com.medvault.repository;

import com.medvault.model.Payment;
import com.medvault.model.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findByAppointmentId(Long appointmentId);

    List<Payment> findByStatus(PaymentStatus status);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED'")
    Double getTotalRevenue();

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.appointment.doctor.id = :doctorId AND p.status = 'COMPLETED'")
    Double getDoctorRevenue(@Param("doctorId") Long doctorId);

    @Query("SELECT p FROM Payment p WHERE p.appointment.doctor.id = :doctorId AND p.status = 'COMPLETED' ORDER BY p.paidAt DESC")
    List<Payment> getDoctorPayments(@Param("doctorId") Long doctorId);
}
