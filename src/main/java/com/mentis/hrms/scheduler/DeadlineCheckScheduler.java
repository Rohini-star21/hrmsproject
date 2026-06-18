package com.mentis.hrms.scheduler;

import com.mentis.hrms.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DeadlineCheckScheduler {

    @Autowired
    private DocumentService documentService;

    /**
     * Check deadlines every day at 9:00 AM
     */
    @Scheduled(cron = "0 0 9 * * ?") // 9:00 AM daily
    public void checkDeadlinesDaily() {
        try {
            System.out.println("🔔 Checking document deadlines...");
            documentService.checkAndSendDeadlineWarnings();
        } catch (Exception e) {
            System.err.println("❌ Error checking deadlines: " + e.getMessage());
        }
    }

    /**
     * Check deadlines every hour during business hours (9 AM - 6 PM)
     */
    @Scheduled(cron = "0 0 9-18 * * ?") // Every hour from 9 AM to 6 PM
    public void checkDeadlinesHourly() {
        try {
            System.out.println("🔔 Hourly deadline check...");
            documentService.checkAndSendDeadlineWarnings();
        } catch (Exception e) {
            System.err.println("❌ Error in hourly deadline check: " + e.getMessage());
        }
    }
}