package com.medvault. repository;

import com.medvault.model.Appointment;
import com.medvault.model.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data. jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java. util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientIdOrderByAppointmentDateTimeDesc(Long patientId);
    List<Appointment> findByDoctorIdOrderByAppointmentDateTimeDesc(Long doctorId);
    List<Appointment> findByPatientIdAndStatusOrderByAppointmentDateTimeDesc(Long patientId, AppointmentStatus status);
    List<Appointment> findByDoctorIdAndStatusOrderByAppointmentDateTimeDesc(Long doctorId, AppointmentStatus status);
    
    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND a.appointmentDateTime BETWEEN :start AND :end")
    List<Appointment> findDoctorAppointmentsBetween(Long doctorId, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT a FROM Appointment a WHERE a.patient.id = :patientId AND a. appointmentDateTime > :now ORDER BY a.appointmentDateTime ASC")
    List<Appointment> findUpcomingAppointmentsByPatient(Long patientId, LocalDateTime now);
}