package com.medvault.repository;

import com.medvault.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java. util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<Notification> findByPatientIdAndIsReadFalseOrderByCreatedAtDesc(Long patientId);
    Long countByPatientIdAndIsReadFalse(Long patientId);
    List<Notification> findByScheduledForBeforeAndIsReadFalse(LocalDateTime now);
}