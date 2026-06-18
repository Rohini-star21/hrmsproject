package com.mentis.hrms.dto;

import lombok.Data;
import java.util.List;

@Data
public class ChatResponseDto {
    private boolean success;
    private String reply;
    private Long sessionId;
    private String sessionName;
    private String error;

    // For history loading
    private List<MessageInfo> messages;
    private List<SessionInfo> sessions;

    @Data
    public static class MessageInfo {
        private Long id;
        private String role;
        private String content;
        private String createdAt;
    }

    @Data
    public static class SessionInfo {
        private Long id;
        private String sessionName;
        private String updatedAt;
        private int messageCount;
    }
}