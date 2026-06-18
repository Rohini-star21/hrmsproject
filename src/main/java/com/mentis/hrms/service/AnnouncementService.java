package com.mentis.hrms.service;

import com.mentis.hrms.dto.AnnouncementDTO;
import com.mentis.hrms.model.Announcement;
import com.mentis.hrms.repository.AnnouncementRepository;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AnnouncementService {

    private static final Logger logger = LoggerFactory.getLogger(AnnouncementService.class);

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired(required = false)
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Create a new announcement
     */
    public AnnouncementDTO createAnnouncement(AnnouncementDTO dto, HttpSession session) {
        logger.info("=== CREATING ANNOUNCEMENT: {} ===", dto.getTitle());

        try {
            Announcement announcement = new Announcement();
            announcement.setTitle(dto.getTitle());
            announcement.setDescription(dto.getDescription());

            // Set Type (PERMANENT or TEMPORARY)
            if (dto.getType() != null) {
                announcement.setType(Announcement.AnnouncementType.valueOf(dto.getType()));
            } else {
                announcement.setType(Announcement.AnnouncementType.PERMANENT);
            }

            announcement.setCategory(dto.getCategory());

            // Set Priority
            if (dto.getPriority() != null) {
                announcement.setPriority(Announcement.AnnouncementPriority.valueOf(dto.getPriority()));
            } else {
                announcement.setPriority(Announcement.AnnouncementPriority.NORMAL);
            }

            // Set Target Audience
            if (dto.getTargetAudience() != null) {
                announcement.setTargetAudience(Announcement.TargetAudience.valueOf(dto.getTargetAudience()));
            } else {
                announcement.setTargetAudience(Announcement.TargetAudience.ALL);
            }

            announcement.setAnnouncementType(resolveAnnouncementType(dto.getCategory()));
            announcement.setIsPinned(dto.getIsPinned() != null ? dto.getIsPinned() : false);
            announcement.setIsActive(true);
            announcement.setExpiresAt(dto.getExpiresAt());
            String userId = (String) session.getAttribute("userId");
            String userName = (String) session.getAttribute("userName");
// Fallback if session attributes are null
            announcement.setCreatedBy(userId != null ? userId : "ADMIN");
            announcement.setCreatedByName(userName != null ? userName : "Super Admin");

            Announcement saved = announcementRepository.save(announcement);
            logger.info("✅ Announcement created with ID: {}", saved.getId());

            AnnouncementDTO savedDto = AnnouncementDTO.fromEntity(saved);
            broadcastAnnouncement(savedDto);
            return savedDto;
        } catch (Exception e) {
            logger.error("❌ Error creating announcement: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create announcement: " + e.getMessage());
        }
    }

    /**
     * Get announcements for employees
     */
    public List<AnnouncementDTO> getAnnouncementsForEmployee() {
        logger.info("=== FETCHING ANNOUNCEMENTS FOR EMPLOYEE ===");
        try {
            List<Announcement> announcements = announcementRepository.findActiveForEmployee();
            logger.info("✅ Found {} announcements for employee", announcements.size());
            return announcements.stream()
                    .map(AnnouncementDTO::fromEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("❌ Error fetching announcements: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Get announcements for HR
     */
    public List<AnnouncementDTO> getAnnouncementsForHR() {
        logger.info("=== FETCHING ANNOUNCEMENTS FOR HR ===");
        try {
            List<Announcement> announcements = announcementRepository.findActiveForHR();
            logger.info("✅ Found {} announcements for HR", announcements.size());
            return announcements.stream()
                    .map(AnnouncementDTO::fromEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("❌ Error fetching announcements: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Get all announcements for admin
     */
    public List<AnnouncementDTO> getAllAnnouncementsForAdmin() {
        logger.info("=== FETCHING ALL ANNOUNCEMENTS FOR ADMIN ===");
        try {
            List<Announcement> announcements = announcementRepository.findAllForAdmin();
            logger.info("✅ Found {} announcements for admin", announcements.size());
            return announcements.stream()
                    .map(AnnouncementDTO::fromEntity)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("❌ Error fetching announcements: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * Delete an announcement
     */
    public boolean deleteAnnouncement(Long id) {
        logger.info("=== DELETING ANNOUNCEMENT: {} ===", id);
        try {
            if (!announcementRepository.existsById(id)) {
                logger.warn("Announcement not found: {}", id);
                return false;
            }
            announcementRepository.deleteById(id);
            logger.info("✅ Announcement deleted: {}", id);
            return true;
        } catch (Exception e) {
            logger.error("❌ Error deleting announcement: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Toggle pin status
     */
    public Optional<AnnouncementDTO> togglePin(Long id) {
        logger.info("=== TOGGLING PIN FOR ANNOUNCEMENT: {} ===", id);
        try {
            Optional<Announcement> announcementOpt = announcementRepository.findById(id);
            if (announcementOpt.isEmpty()) {
                logger.warn("Announcement not found: {}", id);
                return Optional.empty();
            }

            Announcement announcement = announcementOpt.get();
            announcement.setIsPinned(!announcement.getIsPinned());
            Announcement saved = announcementRepository.save(announcement);
            logger.info("✅ Announcement pin toggled: {} - New status: {}", id, saved.getIsPinned());
            return Optional.of(AnnouncementDTO.fromEntity(saved));
        } catch (Exception e) {
            logger.error("❌ Error toggling pin: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Deactivate an announcement
     */
    public Optional<AnnouncementDTO> deactivateAnnouncement(Long id) {
        logger.info("=== DEACTIVATING ANNOUNCEMENT: {} ===", id);
        try {
            Optional<Announcement> announcementOpt = announcementRepository.findById(id);
            if (announcementOpt.isEmpty()) {
                logger.warn("Announcement not found: {}", id);
                return Optional.empty();
            }

            Announcement announcement = announcementOpt.get();
            announcement.setIsActive(false);
            Announcement saved = announcementRepository.save(announcement);
            logger.info("✅ Announcement deactivated: {}", id);
            return Optional.of(AnnouncementDTO.fromEntity(saved));
        } catch (Exception e) {
            logger.error("❌ Error deactivating announcement: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Deactivate expired announcements (Scheduled job)
     */
    public int deactivateExpiredAnnouncements() {
        logger.info("=== DEACTIVATING EXPIRED ANNOUNCEMENTS ===");
        try {
            List<Announcement> expiredAnnouncements = announcementRepository.findExpiredTemporaryAnnouncements(
                    LocalDateTime.now(),
                    Announcement.AnnouncementType.TEMPORARY
            );

            expiredAnnouncements.forEach(a -> a.setIsActive(false));
            announcementRepository.saveAll(expiredAnnouncements);

            logger.info("✅ Deactivated {} expired announcements", expiredAnnouncements.size());
            return expiredAnnouncements.size();
        } catch (Exception e) {
            logger.error("❌ Error deactivating expired announcements: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Broadcast announcement via WebSocket
     */
    private void broadcastAnnouncement(AnnouncementDTO dto) {
        try {
            if (messagingTemplate == null) {
                logger.warn("⚠️ WebSocket messaging not configured, skipping broadcast");
                return;
            }

            if ("ALL".equals(dto.getTargetAudience())) {
                messagingTemplate.convertAndSend("/topic/announcements/all", dto);
                messagingTemplate.convertAndSend("/topic/announcements/employees", dto);
                messagingTemplate.convertAndSend("/topic/announcements/hr", dto);
                messagingTemplate.convertAndSend("/topic/announcements", dto);
            } else if ("EMPLOYEES_ONLY".equals(dto.getTargetAudience())) {
                messagingTemplate.convertAndSend("/topic/announcements/employees", dto);
                messagingTemplate.convertAndSend("/topic/announcements", dto);
            } else if ("HR_ONLY".equals(dto.getTargetAudience())) {
                messagingTemplate.convertAndSend("/topic/announcements/hr", dto);
                messagingTemplate.convertAndSend("/topic/announcements", dto);
            }

            logger.debug("✅ Announcement broadcasted to: {}", dto.getTargetAudience());
        } catch (Exception e) {
            logger.warn("⚠️ Error broadcasting announcement: {}", e.getMessage());
        }
    }

    /**
     * Resolve announcement type from category
     */
    private String resolveAnnouncementType(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "GENERAL";
        }
        String normalized = category.trim().toUpperCase().replace(' ', '_');
        return normalized.length() > 20 ? normalized.substring(0, 20) : normalized;
    }
}