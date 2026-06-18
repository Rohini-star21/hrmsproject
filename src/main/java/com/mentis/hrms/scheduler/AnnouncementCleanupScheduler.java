package com.mentis.hrms.scheduler;

import com.mentis.hrms.service.AnnouncementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AnnouncementCleanupScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AnnouncementCleanupScheduler.class);

    @Autowired
    private AnnouncementService announcementService;

    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredAnnouncements() {
        try {
            announcementService.deactivateExpiredAnnouncements();
        } catch (Exception e) {
            logger.error("Announcement cleanup failed: {}", e.getMessage());
        }
    }
}
