package com.mentis.hrms.controller;

import com.mentis.hrms.dto.AnnouncementDTO;
import com.mentis.hrms.service.AnnouncementService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/announcements")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class AnnouncementController {

    private static final Logger logger = LoggerFactory.getLogger(AnnouncementController.class);

    @Autowired
    private AnnouncementService announcementService;

    /**
     * Create a new announcement
     * POST /api/announcements/create
     */
    @PostMapping("/create")
    public ResponseEntity<?> createAnnouncement(
            @RequestBody AnnouncementDTO dto,
            HttpSession session) {
        logger.info("=== POST /api/announcements/create ===");
        logger.info("Creating announcement: {}", dto.getTitle());

        try {
            // Validate required fields
            if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "Title is required"));
            }

            if (dto.getDescription() == null || dto.getDescription().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "Description is required"));
            }

            AnnouncementDTO created = announcementService.createAnnouncement(dto, session);
            logger.info("✅ Announcement created successfully: ID {}", created.getId());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("success", true, "data", created, "message", "Announcement created successfully"));
        } catch (Exception e) {
            logger.error("❌ Error creating announcement: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get announcements for current employee
     * GET /api/announcements/employee
     */
    @GetMapping("/employee")
    public ResponseEntity<?> getAnnouncementsForEmployee(HttpServletRequest request) {
        logger.info("=== GET /api/announcements/employee ===");
        // Log session info for debugging
        HttpSession session = request.getSession(false);
        logger.info("Session exists: {}", session != null);

        try {
            List<AnnouncementDTO> announcements = announcementService.getAnnouncementsForEmployee();
            logger.info("✅ Retrieved {} announcements for employee", announcements.size());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", announcements,
                    "count", announcements.size()
            ));
        } catch (Exception e) {
            logger.error("❌ Error fetching announcements: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage(), "data", List.of()));
        }
    }

    /**
     * Get announcements for HR
     * GET /api/announcements/hr
     */
    @GetMapping("/hr")
    public ResponseEntity<?> getAnnouncementsForHR() {
        logger.info("=== GET /api/announcements/hr ===");

        try {
            List<AnnouncementDTO> announcements = announcementService.getAnnouncementsForHR();
            logger.info("✅ Retrieved {} announcements for HR", announcements.size());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", announcements,
                    "count", announcements.size()
            ));
        } catch (Exception e) {
            logger.error("❌ Error fetching announcements: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage(), "data", List.of()));
        }
    }

    /**
     * Get all announcements for admin
     * GET /api/announcements/admin
     */
    @GetMapping("/admin")
    public ResponseEntity<?> getAllAnnouncementsForAdmin() {
        logger.info("=== GET /api/announcements/admin ===");

        try {
            List<AnnouncementDTO> announcements = announcementService.getAllAnnouncementsForAdmin();
            logger.info("✅ Retrieved {} announcements for admin", announcements.size());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", announcements,
                    "count", announcements.size()
            ));
        } catch (Exception e) {
            logger.error("❌ Error fetching announcements: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage(), "data", List.of()));
        }
    }

    /**
     * Get all announcements (no filtering)
     * GET /api/announcements
     */
    @GetMapping
    public ResponseEntity<?> getAllAnnouncements() {
        logger.info("=== GET /api/announcements ===");

        try {
            List<AnnouncementDTO> announcements = announcementService.getAllAnnouncementsForAdmin();
            logger.info("✅ Retrieved {} announcements", announcements.size());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", announcements,
                    "count", announcements.size()
            ));
        } catch (Exception e) {
            logger.error("❌ Error fetching announcements: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage(), "data", List.of()));
        }
    }

    /**
     * Toggle pin status
     * PATCH /api/announcements/{id}/pin
     */
    @PatchMapping("/{id}/pin")
    public ResponseEntity<?> togglePin(@PathVariable Long id) {
        logger.info("=== PATCH /api/announcements/{}/pin ===", id);

        try {
            Optional<AnnouncementDTO> updated = announcementService.togglePin(id);
            if (updated.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "error", "Announcement not found"));
            }

            logger.info("✅ Announcement pin toggled: {}", id);
            return ResponseEntity.ok(Map.of("success", true, "data", updated.get()));
        } catch (Exception e) {
            logger.error("❌ Error toggling pin: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Deactivate announcement
     * PATCH /api/announcements/{id}/deactivate
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateAnnouncement(@PathVariable Long id) {
        logger.info("=== PATCH /api/announcements/{}/deactivate ===", id);

        try {
            Optional<AnnouncementDTO> updated = announcementService.deactivateAnnouncement(id);
            if (updated.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "error", "Announcement not found"));
            }

            logger.info("✅ Announcement deactivated: {}", id);
            return ResponseEntity.ok(Map.of("success", true, "data", updated.get()));
        } catch (Exception e) {
            logger.error("❌ Error deactivating announcement: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
    /**
     * Toggle pin status (correct mapping)
     * PUT /api/announcements/{id}/toggle-pin
     */
    @PutMapping("/{id}/toggle-pin")
    public ResponseEntity<?> togglePinPut(@PathVariable Long id) {
        logger.info("=== PUT /api/announcements/{}/toggle-pin ===", id);
        try {
            Optional<AnnouncementDTO> updated = announcementService.togglePin(id);
            if (updated.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "error", "Announcement not found"));
            }
            return ResponseEntity.ok(Map.of("success", true, "data", updated.get()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }


















    /**
     * Deactivate (PUT variant for frontend compatibility)
     */
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivatePut(@PathVariable Long id) {
        return deactivateAnnouncement(id);
    }
    /**
     * Delete announcement
     * DELETE /api/announcements/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAnnouncement(@PathVariable Long id) {
        logger.info("=== DELETE /api/announcements/{} ===", id);

        try {
            boolean deleted = announcementService.deleteAnnouncement(id);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("success", false, "error", "Announcement not found"));
            }

            logger.info("✅ Announcement deleted: {}", id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Announcement deleted successfully"));
        } catch (Exception e) {
            logger.error("❌ Error deleting announcement: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}