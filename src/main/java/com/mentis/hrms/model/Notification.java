package com.mentis.hrms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_id", nullable = false)
    private String recipientId;

    @Column(name = "recipient_type", nullable = false)
    private String recipientType;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "sender")
    private String sender;

    // NEW: Persistent flag to distinguish permanent vs temporary notifications
    @Column(name = "persistent", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean persistent = true;

    // NEW: Additional metadata for temporary notifications
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public String getRecipientType() { return recipientType; }
    public void setRecipientType(String recipientType) { this.recipientType = recipientType; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }

    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    // NEW: Getter and Setter for persistent field
    public boolean isPersistent() { return persistent; }
    public void setPersistent(boolean persistent) { this.persistent = persistent; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    // Helper methods
    public boolean isRead() {
        return readAt != null;
    }

    public void markAsRead() {
        this.readAt = LocalDateTime.now();
    }

    public void markAsUnread() {
        this.readAt = null;
    }

    // Get notification color based on type
    public String getNotificationColor() {
        switch (this.type) {
            case "DOCUMENT_UPLOADED":
                return "info";
            case "DOCUMENT_VERIFIED":
                return "success";
            case "DOCUMENT_REJECTED":
                return "danger";
            case "ONBOARDING_COMPLETED":
                return "success";
            case "DEADLINE_WARNING":
                return "warning";
            case "DEADLINE_REACHED":
                return "danger";
            case "ALL_DOCUMENTS_UPLOADED":
                return "success";
            case "VALIDATION_REQUIRED":
                return "warning";
            default:
                return "primary";
        }
    }

    // Get notification icon based on type
    public String getNotificationIcon() {
        switch (this.type) {
            case "DOCUMENT_UPLOADED":
                return "fa-file-upload";
            case "DOCUMENT_VERIFIED":
                return "fa-check-circle";
            case "DOCUMENT_REJECTED":
                return "fa-times-circle";
            case "ONBOARDING_COMPLETED":
                return "fa-trophy";
            case "DEADLINE_WARNING":
                return "fa-clock";
            case "DEADLINE_REACHED":
                return "fa-exclamation-triangle";
            case "ALL_DOCUMENTS_UPLOADED":
                return "fa-check-double";
            case "VALIDATION_REQUIRED":
                return "fa-exclamation-circle";
            default:
                return "fa-bell";
        }
    }
}