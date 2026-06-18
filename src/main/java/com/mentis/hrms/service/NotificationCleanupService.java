package com.mentis.hrms.service;

import com.mentis.hrms.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class NotificationCleanupService {

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Runs daily at 2 AM to clean up old notifications
     * Deletes temporary notifications older than 3 days
     */
    @Scheduled(cron = "0 0 2 * * ?") // 2 AM daily
    @Transactional
    public void cleanupOldNotifications() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(3);

            // Delete temporary notifications older than 3 days
            int deletedCount = notificationRepository.deleteTemporaryNotificationsOlderThan(cutoffDate);

            System.out.println("✅ Cleaned up " + deletedCount + " old temporary notifications");

            // Also mark temporary notifications as read after 1 day
            notificationRepository.markOldTemporaryAsRead(cutoffDate.minusDays(1));

        } catch (Exception e) {
            System.err.println("❌ Error cleaning up notifications: " + e.getMessage());
        }
    }
}