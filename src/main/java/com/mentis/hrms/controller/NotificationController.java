package com.mentis.hrms.controller;
import com.mentis.hrms.model.Notification;
import com.mentis.hrms.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications/api")
public class NotificationController {
    @Autowired private NotificationService notificationService;

    @GetMapping("/recent")
    public ResponseEntity<List<Notification>> getRecentNotifications(
            @RequestParam(defaultValue = "5") int limit,
            HttpSession session) {

        String userId = (String) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("userRole");

        if (userId == null) return ResponseEntity.status(401).build();

        String recipientType = "SUPER_ADMIN".equals(userRole) || "HR".equals(userRole) ? "HR" : "EMPLOYEE";
        String recipientId = "HR".equals(recipientType) ? "HR_SYSTEM" : userId;

        // FIXED: Use getNotifications instead of getAllNotifications
        List<Notification> notifications = notificationService.getNotifications(recipientId, recipientType);
        return ResponseEntity.ok(notifications.subList(0, Math.min(limit, notifications.size())));
    }

    @GetMapping("/persistent")
    public ResponseEntity<List<Notification>> getPersistentNotifications(
            @RequestParam(defaultValue = "10") int limit,
            HttpSession session) {

        String userId = (String) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("userRole");

        if (userId == null) return ResponseEntity.status(401).build();

        String recipientType = "SUPER_ADMIN".equals(userRole) || "HR".equals(userRole) ? "HR" : "EMPLOYEE";
        String recipientId = "HR".equals(recipientType) ? "HR_SYSTEM" : userId;

        List<Notification> notifications = notificationService.getPersistentNotifications(recipientId, recipientType);
        return ResponseEntity.ok(notifications.subList(0, Math.min(limit, notifications.size())));
    }

    // FIXED: Remove or fix the getAllNotifications endpoint
    @GetMapping("/all")
    public ResponseEntity<List<Notification>> getAllNotifications(
            @RequestParam(defaultValue = "20") int limit,
            HttpSession session) {

        String userId = (String) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("userRole");

        if (userId == null) return ResponseEntity.status(401).build();

        String recipientType = "SUPER_ADMIN".equals(userRole) || "HR".equals(userRole) ? "HR" : "EMPLOYEE";
        String recipientId = "HR".equals(recipientType) ? "HR_SYSTEM" : userId;

        // FIXED: Use getNotifications instead of getAllNotifications
        List<Notification> notifications = notificationService.getNotifications(recipientId, recipientType);
        return ResponseEntity.ok(notifications.subList(0, Math.min(limit, notifications.size())));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("userRole");

        if (userId == null) return ResponseEntity.status(401).build();

        String recipientType = "SUPER_ADMIN".equals(userRole) || "HR".equals(userRole) ? "HR" : "EMPLOYEE";
        String recipientId = "HR".equals(recipientType) ? "HR_SYSTEM" : userId;

        // Use getUnreadCount for backward compatibility
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(recipientId, recipientType)));
    }

    @GetMapping("/unread-persistent-count")
    public ResponseEntity<Map<String, Long>> getUnreadPersistentCount(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("userRole");

        if (userId == null) return ResponseEntity.status(401).build();

        String recipientType = "SUPER_ADMIN".equals(userRole) || "HR".equals(userRole) ? "HR" : "EMPLOYEE";
        String recipientId = "HR".equals(recipientType) ? "HR_SYSTEM" : userId;

        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadPersistentCount(recipientId, recipientType)));
    }

    @PostMapping("/{id}/mark-read")
    public ResponseEntity<Map<String, Boolean>> markAsRead(@PathVariable Long id, HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("userRole");

        if (userId == null) return ResponseEntity.status(401).build();

        String recipientType = "SUPER_ADMIN".equals(userRole) || "HR".equals(userRole) ? "HR" : "EMPLOYEE";
        String recipientId = "HR".equals(recipientType) ? "HR_SYSTEM" : userId;

        notificationService.markAsRead(id, recipientId, recipientType);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/mark-all-read")
    public ResponseEntity<Map<String, Object>> markAllAsRead(HttpSession session) {
        String userId = (String) session.getAttribute("userId");
        String userRole = (String) session.getAttribute("userRole");

        if (userId == null) return ResponseEntity.status(401).build();

        String recipientType = "SUPER_ADMIN".equals(userRole) || "HR".equals(userRole) ? "HR" : "EMPLOYEE";
        String recipientId = "HR".equals(recipientType) ? "HR_SYSTEM" : userId;

        notificationService.markAllAsRead(recipientId, recipientType);

        // Get remaining unread count after marking all as read
        long remainingCount = notificationService.getUnreadCount(recipientId, recipientType);
        return ResponseEntity.ok(Map.of("success", true, "markedCount", remainingCount));
    }
}