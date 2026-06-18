package com.mentis.hrms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class TemporaryNotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Send a TEMPORARY notification (toast) that doesn't get saved to database
     */
    public void sendTemporaryNotification(String recipientId, String recipientType,
                                          String type, String title, String message,
                                          String referenceId, String referenceType,
                                          String sender) {

        Map<String, Object> payload = new HashMap<>();
        payload.put("id", UUID.randomUUID().toString()); // Temporary ID
        payload.put("type", type);
        payload.put("title", title);
        payload.put("message", message);
        payload.put("referenceId", referenceId);
        payload.put("referenceType", referenceType);
        payload.put("sender", sender);
        payload.put("createdAt", LocalDateTime.now().toString());
        payload.put("read", false);
        payload.put("persistent", false); // Mark as temporary
        payload.put("color", getColorForType(type));
        payload.put("icon", getIconForType(type));

        String topic = String.format("/topic/%s/%s/notifications",
                recipientType.toLowerCase(),
                recipientId);

        messagingTemplate.convertAndSend(topic, payload);
    }

    /**
     * Send temporary deadline warning (appears as toast, not stored in DB)
     */
    public void sendDeadlineWarning(String employeeId, long hoursLeft) {
        String timeUnit = hoursLeft >= 24 ? (hoursLeft/24) + " days" : hoursLeft + " hours";

        sendTemporaryNotification(
                employeeId,
                "EMPLOYEE",
                "DEADLINE_WARNING",
                "⚠️ Deadline Approaching",
                "You have " + timeUnit + " left to upload all required documents",
                employeeId,
                "EMPLOYEE",
                "HR_SYSTEM"
        );
    }

    /**
     * Send temporary document uploaded toast
     */
    public void sendDocumentUploadedToast(String employeeId, String documentName) {
        sendTemporaryNotification(
                employeeId,
                "EMPLOYEE",
                "DOCUMENT_UPLOADED",
                "✅ Document Uploaded",
                documentName + " has been uploaded successfully",
                employeeId,
                "EMPLOYEE",
                "SYSTEM"
        );
    }

    /**
     * Send temporary document verified toast
     */
    public void sendDocumentVerifiedToast(String employeeId, String documentName, boolean isVerified) {
        String title = isVerified ? "✅ Document Verified" : "❌ Document Rejected";
        String message = isVerified ?
                documentName + " has been verified by HR" :
                documentName + " has been rejected. Please re-upload.";

        sendTemporaryNotification(
                employeeId,
                "EMPLOYEE",
                isVerified ? "DOCUMENT_VERIFIED" : "DOCUMENT_REJECTED",
                title,
                message,
                employeeId,
                "EMPLOYEE",
                "HR_SYSTEM"
        );
    }

    private String getColorForType(String type) {
        if (type.contains("VERIFIED")) return "success";
        if (type.contains("REJECTED")) return "danger";
        if (type.contains("WARNING")) return "warning";
        if (type.contains("UPLOADED")) return "info";
        return "primary";
    }

    private String getIconForType(String type) {
        if (type.contains("VERIFIED")) return "fa-check-circle";
        if (type.contains("REJECTED")) return "fa-times-circle";
        if (type.contains("WARNING")) return "fa-clock";
        if (type.contains("UPLOADED")) return "fa-file-upload";
        return "fa-bell";
    }
}