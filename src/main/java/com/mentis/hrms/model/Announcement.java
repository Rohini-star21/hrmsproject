package com.mentis.hrms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "announcements", indexes = {
        @Index(name = "idx_announcement_type", columnList = "announcement_type"),
        @Index(name = "idx_announcement_active", columnList = "is_active"),
        @Index(name = "idx_announcement_expiry", columnList = "expiry_date")
})
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnnouncementType type;

    @Column(name = "announcement_type", length = 20)
    private String announcementType;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "category", length = 100)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private AnnouncementPriority priority = AnnouncementPriority.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_audience")
    private TargetAudience targetAudience = TargetAudience.ALL;

    @Column(name = "created_by_name", length = 255)
    private String createdByName;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_pinned")
    private Boolean isPinned = false;

    // ===== ENUMS =====
    public enum AnnouncementType {
        PERMANENT, TEMPORARY
    }

    public enum AnnouncementPriority {
        LOW, NORMAL, HIGH, URGENT
    }

    public enum TargetAudience {
        ALL, HR_ONLY, EMPLOYEES_ONLY
    }

    // ===== CONSTRUCTORS =====
    public Announcement() {}

    public Announcement(Long id, String title, String description, AnnouncementType type,
                        String announcementType, String createdBy, LocalDateTime createdAt,
                        LocalDateTime expiryDate, Boolean isActive, String category,
                        AnnouncementPriority priority, TargetAudience targetAudience,
                        String createdByName, LocalDateTime expiresAt, Boolean isPinned) {
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
    public AnnouncementType getType() { return type; }
    public String getAnnouncementType() { return announcementType; }
    public String getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiryDate() { return expiryDate; }
    public Boolean getIsActive() { return isActive; }
    public String getCategory() { return category; }
    public AnnouncementPriority getPriority() { return priority; }
    public TargetAudience getTargetAudience() { return targetAudience; }
    public String getCreatedByName() { return createdByName; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public Boolean getIsPinned() { return isPinned; }

    // ===== SETTERS =====
    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setType(AnnouncementType type) { this.type = type; }
    public void setAnnouncementType(String announcementType) { this.announcementType = announcementType; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public void setCategory(String category) { this.category = category; }
    public void setPriority(AnnouncementPriority priority) { this.priority = priority; }
    public void setTargetAudience(TargetAudience targetAudience) { this.targetAudience = targetAudience; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public void setIsPinned(Boolean isPinned) { this.isPinned = isPinned; }

    // ===== TOSTRING (for logging/debugging) =====
    @Override
    public String toString() {
        return "Announcement{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", type=" + type +
                ", priority=" + priority +
                ", targetAudience=" + targetAudience +
                ", isActive=" + isActive +
                ", isPinned=" + isPinned +
                ", createdBy='" + createdBy + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    // ===== EQUALS & HASHCODE =====
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Announcement that = (Announcement) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    // ===== LIFECYCLE =====
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.isPinned == null) {
            this.isPinned = false;
        }
    }
}