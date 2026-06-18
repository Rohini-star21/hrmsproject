package com.mentis.hrms.service;

import com.mentis.hrms.model.Employee;
import com.mentis.hrms.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class DeadlineMonitorService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private NotificationService notificationService;

    // ✅ RUNS EVERY 5 MINUTES FOR TESTING (change to 0 */30 * * * * for production)
    @Scheduled(cron = "0 */5 * * * *")
    public void checkDeadlines() {
        System.out.println("🔄 DEADLINE MONITOR STARTED: " + LocalDateTime.now());

        try {
            List<String> activeStatuses = Arrays.asList(
                    "NOT_STARTED", "DOCUMENTS_PENDING", "DOCUMENTS_SUBMITTED", "IN_PROGRESS"
            );

            List<Employee> employees = employeeRepository.findByDocumentDeadlineIsNotNullAndOnboardingStatusIn(activeStatuses);
            System.out.println("📋 Found " + employees.size() + " employees with active deadlines");

            for (Employee employee : employees) {
                System.out.println("🔍 Checking: " + employee.getEmployeeId() +
                        " | Docs: " + employee.getSubmittedDocuments() + "/" + employee.getTotalDocuments() +
                        " | Deadline: " + employee.getDocumentDeadline() +
                        " | Flags: warning=" + employee.isDeadlineWarningSent() + ", final=" + employee.isDeadlineFinalSent());
                processEmployeeDeadline(employee);
            }
        } catch (Exception e) {
            System.err.println("❌ Deadline monitor system error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    protected void processEmployeeDeadline(Employee employee) {
        if (employee.getDocumentDeadline() == null) return;

        // Skip if all documents are verified
        if (employee.getVerifiedDocuments() >= employee.getTotalDocuments() && employee.getTotalDocuments() > 0) {
            System.out.println("✅ Employee " + employee.getEmployeeId() + " has all documents verified - skipping");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime deadline = employee.getDocumentDeadline();

        long hoursUntilDeadline = ChronoUnit.HOURS.between(now, deadline);
        long daysUntilDeadline = ChronoUnit.DAYS.between(now, deadline);

        System.out.println("⏱️ Hours until deadline: " + hoursUntilDeadline);

        boolean warningSent = employee.isDeadlineWarningSent();
        boolean finalSent = employee.isDeadlineFinalSent();
        boolean needsUpdate = false;

        // Check for overdue (deadline passed)
        if (hoursUntilDeadline <= 0 && !finalSent) {
            System.out.println("🚨 OVERDUE - sending notification to: " + employee.getEmployeeId());
            notificationService.notifyDeadlineReached(employee);
            employee.setDeadlineFinalSent(true);
            needsUpdate = true;
        }
        // Check for 24 hours left
        else if (hoursUntilDeadline <= 24 && hoursUntilDeadline > 0 && !warningSent) {
            System.out.println("⚠️ 24-hour warning - sending notification to: " + employee.getEmployeeId());
            notificationService.notifyDeadlineApproaching(employee, 24);
            employee.setDeadlineWarningSent(true);
            needsUpdate = true;
        }
        // Check for 48 hours left (2 days)
        else if (hoursUntilDeadline <= 48 && hoursUntilDeadline > 24 && !warningSent) {
            System.out.println("⚠️ 48-hour warning - sending notification to: " + employee.getEmployeeId());
            notificationService.notifyDeadlineApproaching(employee, 48);
            employee.setDeadlineWarningSent(true);
            needsUpdate = true;
        }
        // Check for 72 hours left (3 days)
        else if (hoursUntilDeadline <= 72 && hoursUntilDeadline > 48 && !warningSent) {
            System.out.println("⚠️ 72-hour warning - sending notification to: " + employee.getEmployeeId());
            notificationService.notifyDeadlineApproaching(employee, 72);
            employee.setDeadlineWarningSent(true);
            needsUpdate = true;
        }

        if (needsUpdate) {
            employeeRepository.save(employee);
            System.out.println("📝 Flags updated for " + employee.getEmployeeId());
        }
    }
}