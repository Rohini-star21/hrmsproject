package com.mentis.hrms.service;

import com.mentis.hrms.model.Notification;
import com.mentis.hrms.model.OnboardingDocument;
import com.mentis.hrms.model.Employee;
import com.mentis.hrms.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Lazy;
@Service
@Transactional
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    @Lazy  // ← ADD THIS ANNOTATION HERE
    private DocumentService documentService;

    // ========== BACKWARD COMPATIBLE METHODS ==========

    // Original method signature for backward compatibility
    public Notification createNotification(String recipientId, String recipientType,
                                           String type, String title, String message,
                                           String referenceId, String referenceType,
                                           String sender) {
        return createNotification(recipientId, recipientType, type, title, message,
                referenceId, referenceType, sender, true);
    }

    // New method with persistent parameter
    public Notification createNotification(String recipientId, String recipientType,
                                           String type, String title, String message,
                                           String referenceId, String referenceType,
                                           String sender, boolean persistent) {

        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setRecipientType(recipientType);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setReferenceId(referenceId);
        notification.setReferenceType(referenceType);
        notification.setSender(sender);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setReadAt(null);
        notification.setPersistent(persistent);

        Notification saved = notificationRepository.save(notification);
        sendRealTimeNotification(saved);
        return saved;
    }

    // Original getNotifications method
    public List<Notification> getNotifications(String recipientId, String recipientType) {
        return notificationRepository.findByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(recipientId, recipientType);
    }

    // New method for persistent only
    public List<Notification> getPersistentNotifications(String recipientId, String recipientType) {
        return notificationRepository.findPersistentByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(recipientId, recipientType);
    }

    // FIXED: Added getAllNotifications method
    public List<Notification> getAllNotifications(String recipientId, String recipientType) {
        return getNotifications(recipientId, recipientType);
    }

    // Original getUnreadCount method
    public long getUnreadCount(String recipientId, String recipientType) {
        return notificationRepository.countByRecipientIdAndRecipientTypeAndReadAtIsNull(recipientId, recipientType);
    }

    // New method for persistent unread count
    public long getUnreadPersistentCount(String recipientId, String recipientType) {
        return notificationRepository.countPersistentUnreadByRecipientIdAndRecipientType(recipientId, recipientType);
    }

    // FIXED: Correct markAsRead method
    public void markAsRead(Long notificationId, String recipientId, String recipientType) {
        // FIX: Use findByIdAndRecipientIdAndRecipientType which returns Optional<Notification>
        Optional<Notification> notificationOpt = notificationRepository.findByIdAndRecipientIdAndRecipientType(
                notificationId, recipientId, recipientType);

        if (notificationOpt.isPresent()) {
            Notification notification = notificationOpt.get();
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
        }
    }

    public void markAllAsRead(String recipientId, String recipientType) {
        notificationRepository.markAllAsRead(recipientId, recipientType);
    }

    // ========== NOTIFICATION TYPES ==========

    // Permanent notification for all documents uploaded
    // Permanent notification for all documents uploaded
    public void notifyAllDocumentsUploaded(Employee employee) {
        createNotification(
                employee.getEmployeeId(),
                "EMPLOYEE",
                "ALL_DOCUMENTS_UPLOADED",
                "✅ All Documents Uploaded Successfully!",
                "Congratulations! All your onboarding documents have been uploaded and submitted successfully. HR will now review them.",
                employee.getEmployeeId(),
                "EMPLOYEE",
                "HR_SYSTEM",
                true  // ✅ PERMANENT - always visible in notifications page
        );
    }

    // Temporary deadline warning
    public void notifyDeadlineApproaching(Employee employee, long hoursLeft) {
        String timeUnit = hoursLeft >= 24 ? (hoursLeft/24) + " days" : hoursLeft + " hours";
        createNotification(
                employee.getEmployeeId(),
                "EMPLOYEE",
                "DEADLINE_WARNING",
                "⚠️ Document Upload Deadline Approaching",
                "You have " + timeUnit + " left to upload all required documents.",
                employee.getEmployeeId(),
                "EMPLOYEE",
                "HR_SYSTEM",
                false  // ✅ TEMPORARY - won't appear in notifications page
        );
    }

    // Temporary deadline reached
    public void notifyDeadlineReached(Employee employee) {
        createNotification(
                employee.getEmployeeId(),
                "EMPLOYEE",
                "DEADLINE_REACHED",
                "⏰ Document Upload Deadline Reached",
                "Your document upload deadline has passed. Please contact HR immediately.",
                employee.getEmployeeId(),
                "EMPLOYEE",
                "HR_SYSTEM",
                false  // Temporary
        );
    }

    // Document verified (persistent for employee)
    public void notifyDocumentVerified(OnboardingDocument document, String verifiedBy) {
        Employee employee = document.getEmployee();
        String status = document.getStatus();
        boolean isVerified = "VERIFIED".equals(status);

        String title = isVerified ? "✅ Document Verified" : "❌ Document Rejected";
        String message = isVerified ?
                String.format("✅ Your %s has been verified by %s.", document.getDocumentName(), verifiedBy) :
                String.format("❌ Your %s has been rejected by %s.", document.getDocumentName(), verifiedBy);

        // Employee notification - PERMANENT (stored)
        createNotification(
                employee.getEmployeeId(),
                "EMPLOYEE",
                "DOCUMENT_" + status,
                title,
                message,
                String.valueOf(document.getId()),
                "DOCUMENT",
                verifiedBy,
                true  // ✅ PERMANENT - appears in notifications page
        );

        // HR notification - TEMPORARY (toast only)
        // ✅ This will NOT appear in HR notifications page
        createNotification(
                "HR_SYSTEM",
                "HR",
                "DOCUMENT_" + status,
                title,
                String.format("%s for %s has been %s", document.getDocumentName(), employee.getFirstName(), status.toLowerCase()),
                String.valueOf(document.getId()),
                "DOCUMENT",
                verifiedBy,
                false  // ✅ TEMPORARY - toast only
        );
    }

    // Document uploaded (temporary for HR)
    public void notifyDocumentUploaded(OnboardingDocument document) {
        Employee employee = document.getEmployee();

        createNotification(
                "HR_SYSTEM",
                "HR",
                "DOCUMENT_UPLOADED",
                "📄 New Document Submitted",
                employee.getFirstName() + " uploaded: " + document.getDocumentName(),
                String.valueOf(document.getId()),
                "DOCUMENT",
                employee.getEmployeeId(),
                false  // Temporary
        );
    }

    // Onboarding completed notification
    public void notifyOnboardingCompleted(Employee employee) {
        createNotification(
                employee.getEmployeeId(),
                "EMPLOYEE",
                "ONBOARDING_COMPLETED",
                "🎉 Onboarding Complete!",
                "Welcome to Menti's IT Solutions! Your onboarding process is complete.",
                employee.getEmployeeId(),
                "EMPLOYEE",
                "HR_SYSTEM",
                true  // Permanent
        );
    }

    private void sendRealTimeNotification(Notification notification) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", notification.getId());
        payload.put("type", notification.getType());
        payload.put("title", notification.getTitle());
        payload.put("message", notification.getMessage());
        payload.put("referenceId", notification.getReferenceId());
        payload.put("sender", notification.getSender());
        payload.put("createdAt", notification.getCreatedAt().toString());
        payload.put("read", notification.getReadAt() != null);
        payload.put("persistent", notification.isPersistent());
        payload.put("color", notification.getNotificationColor());
        payload.put("icon", notification.getNotificationIcon());

        String topic = String.format("/topic/%s/%s/notifications",
                notification.getRecipientType().toLowerCase(),
                notification.getRecipientId());

        messagingTemplate.convertAndSend(topic, payload);
    }
}