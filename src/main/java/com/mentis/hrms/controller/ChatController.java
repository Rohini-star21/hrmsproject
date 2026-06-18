package com.mentis.hrms.controller;

import com.mentis.hrms.dto.ChatRequestDto;
import com.mentis.hrms.dto.ChatResponseDto;
import com.mentis.hrms.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    // Send a message (handles both temporary and permanent chats)
    @PostMapping("/send")
    public ResponseEntity<ChatResponseDto> sendMessage(@RequestBody ChatRequestDto request) {
        return ResponseEntity.ok(chatService.sendMessage(request));
    }

    // Load history for a specific session
    @GetMapping("/session/{sessionId}/history")
    public ResponseEntity<ChatResponseDto> getSessionHistory(
            @PathVariable Long sessionId,
            @RequestParam String employeeId) {
        return ResponseEntity.ok(chatService.loadSessionHistory(sessionId, employeeId));
    }

    // Get all saved sessions for an employee
    @GetMapping("/sessions")
    public ResponseEntity<ChatResponseDto> getAllSessions(@RequestParam String employeeId) {
        return ResponseEntity.ok(chatService.getAllSessions(employeeId));
    }

    // Delete a session
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<ChatResponseDto> deleteSession(
            @PathVariable Long sessionId,
            @RequestParam String employeeId) {
        return ResponseEntity.ok(chatService.deleteSession(sessionId, employeeId));
    }
}