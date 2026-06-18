package com.mentis.hrms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * WebSocket controller for attendance-related messaging.
 * Note: Broadcasting is now handled directly by AttendanceService to avoid circular dependencies.
 * This controller can be used for handling incoming WebSocket messages from clients if needed.
 */
@Controller
public class AttendanceWebSocketController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * This controller is kept for potential future use (e.g., handling messages from HR clients).
     * Currently, all broadcasting is done via SimpMessagingTemplate in AttendanceService.
     */

    // All broadcasting methods have been removed and moved to AttendanceService
    // to resolve the circular dependency issue.

}