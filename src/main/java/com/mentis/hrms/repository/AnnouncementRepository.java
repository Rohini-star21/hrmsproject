package com.mentis.hrms.repository;

import com.mentis.hrms.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    // Find active announcements for employees
    @Query("SELECT a FROM Announcement a WHERE a.isActive = true " +
            "AND (a.targetAudience = 'ALL' OR a.targetAudience = 'EMPLOYEES_ONLY') " +
            "AND (a.expiresAt IS NULL OR a.expiresAt > CURRENT_TIMESTAMP) " +
            "ORDER BY a.isPinned DESC, a.createdAt DESC")
    List<Announcement> findActiveForEmployee();

    // Find active announcements for HR
    @Query("SELECT a FROM Announcement a WHERE a.isActive = true " +
            "AND (a.targetAudience = 'ALL' OR a.targetAudience = 'HR_ONLY') " +
            "ORDER BY a.isPinned DESC, a.createdAt DESC")
    List<Announcement> findActiveForHR();

    // Find all announcements (for admin)
    @Query("SELECT a FROM Announcement a ORDER BY a.isPinned DESC, a.createdAt DESC")
    List<Announcement> findAllForAdmin();

    // Find expired temporarya announcements
    @Query("SELECT a FROM Announcement a WHERE a.type = :type " +
            "AND a.expiresAt < :now " +
            "AND a.isActive = true")
    List<Announcement> findExpiredTemporaryAnnouncements(
            @Param("now") LocalDateTime now,
            @Param("type") Announcement.AnnouncementType type
    );

    // Count by target audience
    @Query("SELECT COUNT(a) FROM Announcement a WHERE a.targetAudience = :audience AND a.isActive = true")
    long countByTargetAudience(@Param("audience") Announcement.TargetAudience audience);

    // Find announcements by category
    @Query("SELECT a FROM Announcement a WHERE a.category = :category AND a.isActive = true ORDER BY a.createdAt DESC")
    List<Announcement> findByCategory(@Param("category") String category);
}