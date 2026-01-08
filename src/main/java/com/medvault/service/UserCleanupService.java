package com.medvault.service;

import com.medvault.model.User;
import com.medvault.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCleanupService {

    private final UserRepository userRepository;

    /**
     * Scheduled task to automatically delete unverified users with expired tokens
     * Runs every day at 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredUnverifiedUsers() {
        log.info("Starting cleanup of expired unverified users");

        List<User> allUsers = userRepository.findAll();
        int deletedCount = 0;

        for (User user : allUsers) {
            // Check if user is not verified and token has expired
            if (!user.getIsPasswordSet() &&
                    user.getPasswordResetTokenExpiry() != null &&
                    user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {

                log.info("Deleting expired unverified user: {} (email: {})", user.getId(), user.getEmail());
                userRepository.delete(user);
                deletedCount++;
            }
        }

        log.info("Cleanup completed. Deleted {} expired unverified users", deletedCount);
    }

    /**
     * Manual cleanup method that can be called from admin controller if needed
     */
    @Transactional
    public int manualCleanupExpiredUsers() {
        log.info("Manual cleanup of expired unverified users triggered");

        List<User> allUsers = userRepository.findAll();
        int deletedCount = 0;

        for (User user : allUsers) {
            if (!user.getIsPasswordSet() &&
                    user.getPasswordResetTokenExpiry() != null &&
                    user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {

                userRepository.delete(user);
                deletedCount++;
            }
        }

        return deletedCount;
    }
}
