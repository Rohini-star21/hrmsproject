package com.mentis.hrms.dto;

import com.mentis.hrms.model.Announcement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AnnouncementDTO {

    private Long id;
    private String title;
    private String description;
    private String type;
    private String announcementType;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime expiryDate;
    private Boolean isActive;
    private String category;
    private String priority;
    private String targetAudience;
    private String createdByName;
    private LocalDateTime expiresAt;
    private Boolean isPinned;

    // ===== CONSTRUCTORS =====
    public AnnouncementDTO() {}

    public AnnouncementDTO(Long id, String title, String description, String type,
                           String announcementType, String createdBy, LocalDateTime createdAt,
                           LocalDateTime expiryDate, Boolean isActive, String category,
                           String priority, String targetAudience, String createdByName,
                           LocalDateTime expiresAt, Boolean isPinned) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.type = type;
        this.announcementType = announcementType;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.expiryDate = expiryDate;
        this.isActive = isActive;
        this.category = category;
        this.priority = priority;
        this.targetAudience = targetAudience;
        this.createdByName = createdByName;
        this.expiresAt = expiresAt;
        this.isPinned = isPinned;
    }

    // ===== GETTERS =====
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getType() { return type; }
    public String getAnnouncementType() { return announcementType; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public Boolean getIsActive() { return isActive; }
    public String getCategory() { return category; }
    public String getPriority() { return priority; }
    public String getTargetAudience() { return targetAudience; }
    public String getCreatedByName() { return createdByName; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public Boolean getIsPinned() { return isPinned; }

    // ===== SETTERS =====
    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setType(String type) { this.type = type; }
    public void setAnnouncementType(String announcementType) { this.announcementType = announcementType; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public void setCategory(String category) { this.category = category; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public void setIsPinned(Boolean isPinned) { this.isPinned = isPinned; }

    // ===== MAPPER: Entity → DTO =====
    public static AnnouncementDTO fromEntity(Announcement announcement) {
        if (announcement == null) return null;

        AnnouncementDTO dto = new AnnouncementDTO();
        dto.setId(announcement.getId());
        dto.setTitle(announcement.getTitle());
        dto.setDescription(announcement.getDescription());
        dto.setType(announcement.getType() != null ? announcement.getType().toString() : null);
        dto.setAnnouncementType(announcement.getAnnouncementType());
        dto.setCreatedBy(announcement.getCreatedBy());
        dto.setCreatedAt(announcement.getCreatedAt());
        dto.setExpiryDate(announcement.getExpiryDate());
        dto.setIsActive(announcement.getIsActive() != null ? announcement.getIsActive() : true);
        dto.setCategory(announcement.getCategory());
        dto.setPriority(announcement.getPriority() != null ? announcement.getPriority().toString() : "NORMAL");
        dto.setTargetAudience(announcement.getTargetAudience() != null ? announcement.getTargetAudience().toString() : "ALL");
        dto.setCreatedByName(announcement.getCreatedByName());
        dto.setExpiresAt(announcement.getExpiresAt());
        dto.setIsPinned(announcement.getIsPinned() != null ? announcement.getIsPinned() : false);
        return dto;
    }

    // ===== MAPPER: DTO → Entity =====
    public Announcement toEntity() {
        Announcement announcement = new Announcement();
        announcement.setId(this.id);
        announcement.setTitle(this.title);
        announcement.setDescription(this.description);

        if (this.type != null) {
            announcement.setType(Announcement.AnnouncementType.valueOf(this.type));
        }

        announcement.setAnnouncementType(this.announcementType);
        announcement.setCreatedBy(this.createdBy);
        announcement.setCreatedAt(this.createdAt);
        announcement.setExpiryDate(this.expiryDate);
        announcement.setIsActive(this.isActive != null ? this.isActive : true);
        announcement.setCategory(this.category);

        if (this.priority != null) {
            announcement.setPriority(Announcement.AnnouncementPriority.valueOf(this.priority));
        } else {
            announcement.setPriority(Announcement.AnnouncementPriority.NORMAL);
        }

        if (this.targetAudience != null) {
            announcement.setTargetAudience(Announcement.TargetAudience.valueOf(this.targetAudience));
        } else {
            announcement.setTargetAudience(Announcement.TargetAudience.ALL);
        }

        announcement.setCreatedByName(this.createdByName);
        announcement.setExpiresAt(this.expiresAt);
        announcement.setIsPinned(this.isPinned != null ? this.isPinned : false);
        return announcement;
    }

    // ===== HELPER METHODS =====
    public String getContent() { return this.description; }
    public void setContent(String content) { this.description = content; }

    public String getStatus() {
        return (this.isActive != null && this.isActive) ? "ACTIVE" : "INACTIVE";
    }

    public String getFormattedDate() {
        if (this.createdAt == null) return "Not set";
        return this.createdAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public String getFormattedExpiryDate() {
        if (this.expiresAt == null) return "No expiry";
        return this.expiresAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    @Override
    public String toString() {
        return "AnnouncementDTO{id=" + id + ", title='" + title + "', type='" + type + "', isActive=" + isActive + "}";
    }
}