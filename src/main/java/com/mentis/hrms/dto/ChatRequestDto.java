package com.mentis.hrms.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChatRequestDto {
    private String employeeId;
    private String message;
    private Long sessionId;           // null = new session
    private boolean temporary = false; // true = don't save to DB
    private List<MessageDto> history; // client-side history for temp chats

    @Data
    public static class MessageDto {
        private String role;
        private String content;
    }
}