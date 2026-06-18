package com.mentis.hrms.service;

import com.mentis.hrms.model.OnboardingDocument;
import com.mentis.hrms.model.Employee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class ValidationNotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendValidationNotification(OnboardingDocument document, String hrMessage) {
        Map<String, Object> notification = new HashMap<>();

        // Create comprehensive notification content
        String notificationType = "DOCUMENT_" + document.getStatus();
        String title = getNotificationTitle(document.getStatus());
        String message = formatValidationMessage(document, hrMessage);

        notification.put("type", notificationType);
        notification.put("documentId", document.getId());
        notification.put("documentName", document.getDocumentName());
        notification.put("documentType", document.getDocumentType());
        notification.put("status", document.getStatus());
        notification.put("title", title);
        notification.put("message", message); // Full HR message
        notification.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
        notification.put("verifiedBy", document.getVerifiedBy());
        notification.put("verificationNotes", document.getVerificationNotes());

        // Send to employee-specific topic
        String employeeTopic = "/topic/employee/" + document.getEmployee().getEmployeeId() + "/validation";
        messagingTemplate.convertAndSend(employeeTopic, notification);

        // Also send to general HR notifications
        messagingTemplate.convertAndSend("/topic/hr/validations", notification);
    }

    private String getNotificationTitle(String status) {
        switch (status) {
            case "VERIFIED":
                return "✅ Document Verified";
            case "REJECTED":
                return "❌ Document Rejected";
            default:
                return "📄 Document Update";
        }
    }

    private String formatValidationMessage(OnboardingDocument document, String hrMessage) {
        StringBuilder message = new StringBuilder();

        message.append("Your document '").append(document.getDocumentName()).append("' ");

        switch (document.getStatus()) {
            case "VERIFIED":
                message.append("has been ✅ <span style='color:#10b981;font-weight:bold;'>verified</span> by ").append(document.getVerifiedBy());
                break;
            case "REJECTED":
                message.append("has been ❌ <span style='color:#ef4444;font-weight:bold;'>rejected</span> by ").append(document.getVerifiedBy());
                break;
        }

        if (hrMessage != null && !hrMessage.trim().isEmpty()) {
            message.append("\n\nReason: ").append(hrMessage);
        }

        if (document.getVerifiedDate() != null) {
            message.append("\n\nDate: ").append(
                    document.getVerifiedDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
            );
        }

        return message.toString();
    }}