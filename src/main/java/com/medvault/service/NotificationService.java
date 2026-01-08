package com.medvault. service;

import com.medvault.model. Notification;
import com.medvault.model.Patient;
import com.medvault.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    
    @Transactional
    public Notification createNotification(Patient patient, String title, String message, String type) {
        Notification notification = Notification.builder()
                .patient(patient)
                .title(title)
                .message(message)
                .notificationType(type)
                . isRead(false)
                . build();
        
        return notificationRepository.save(notification);
    }
    
    public List<Notification> getPatientNotifications(Long patientId) {
        return notificationRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }
    
    public List<Notification> getUnreadNotifications(Long patientId) {
        return notificationRepository.findByPatientIdAndIsReadFalseOrderByCreatedAtDesc(patientId);
    }
    
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }
    
    @Transactional
    public void markAllAsRead(Long patientId) {
        List<Notification> notifications = notificationRepository
                .findByPatientIdAndIsReadFalseOrderByCreatedAtDesc(patientId);
        notifications.forEach(notification -> notification.setIsRead(true));
        notificationRepository.saveAll(notifications);
    }
}