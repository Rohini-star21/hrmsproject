package com.mentis.hrms.service;

import com.mentis.hrms.dto.ChatRequestDto;
import com.mentis.hrms.dto.ChatResponseDto;
import com.mentis.hrms.model.Attendance;
import com.mentis.hrms.model.ChatMessage;
import com.mentis.hrms.model.ChatSession;
import com.mentis.hrms.repository.ChatMessageRepository;
import com.mentis.hrms.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mentis.hrms.service.OpenRouterService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.mentis.hrms.model.LeaveRequest;
import java.util.stream.Collectors;
@Service
@Transactional
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
    private static final int MAX_HISTORY_PAIRS = 8;

    @Autowired
    private ChatSessionRepository sessionRepository;

    @Autowired
    private ChatMessageRepository messageRepository;

    @Autowired
    private OpenRouterService openRouterService;

    // Services for rich real-time data
    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private LeaveBalanceService leaveBalanceService;
    @Autowired
    private com.mentis.hrms.repository.LeaveRequestRepository leaveRequestRepository;

    public ChatResponseDto sendMessage(ChatRequestDto request) {
        ChatResponseDto response = new ChatResponseDto();
        try {
            String employeeContext = buildRichEmployeeContext(request.getEmployeeId());
            log.debug("Built rich context for employee: {}", request.getEmployeeId());

            if (request.isTemporary()) {
                List<OpenRouterService.MessagePair> history = buildHistoryFromClient(request.getHistory());
                String reply = openRouterService.chat(request.getMessage(), history, employeeContext);

                response.setSuccess(true);
                response.setReply(reply);
                response.setSessionId(-1L);
            } else {
                ChatSession session = getOrCreateSession(request);
                List<ChatMessage> existing = messageRepository.findBySessionIdOrderByCreatedAtAsc(session.getId());
                List<OpenRouterService.MessagePair> history = buildHistoryFromDB(existing);

                String reply = openRouterService.chat(request.getMessage(), history, employeeContext);

                saveMessage(session, "user", request.getMessage());
                saveMessage(session, "assistant", reply);

                session.setUpdatedAt(LocalDateTime.now());
                if (session.getSessionName() == null || session.getSessionName().isEmpty()) {
                    session.setSessionName(generateSessionName(request.getMessage()));
                }
                sessionRepository.save(session);

                response.setSuccess(true);
                response.setReply(reply);
                response.setSessionId(session.getId());
                response.setSessionName(session.getSessionName());
            }
        } catch (Exception e) {
            log.error("Chat error for employee {}", request.getEmployeeId(), e);
            response.setSuccess(false);
            response.setError("Failed to process your request. Please try again.");
        }
        return response;
    }

    @SuppressWarnings("unchecked")
    private String buildRichEmployeeContext(String employeeId) {
        StringBuilder sb = new StringBuilder();
        LocalDate today = LocalDate.now();
        int year = today.getYear();
        int month = today.getMonthValue();

        sb.append("=== EMPLOYEE PROFILE ===\n");
        sb.append("Employee ID: ").append(employeeId).append("\n");
        try {
            Optional<com.mentis.hrms.model.Employee> empOpt = employeeService.getEmployeeByEmployeeId(employeeId);
            if (empOpt.isPresent()) {
                var emp = empOpt.get();
                sb.append("Name: ").append(emp.getFirstName() != null ? emp.getFirstName() : "")
                        .append(" ").append(emp.getLastName() != null ? emp.getLastName() : "").append("\n");
                sb.append("Department: ").append(emp.getDepartment() != null ? emp.getDepartment() : "N/A").append("\n");
                sb.append("Designation: ").append(emp.getDesignation() != null ? emp.getDesignation() : "N/A").append("\n");
                sb.append("Join Date: ").append(emp.getCreatedDate() != null ? emp.getCreatedDate().toLocalDate() : "N/A").append("\n");
            }
        } catch (Exception e) {
            log.warn("Profile context failed for {}: {}", employeeId, e.getMessage());
            sb.append("Profile data unavailable right now.\n");
        }

        sb.append("\n=== TODAY'S ATTENDANCE (").append(today).append(") ===\n");
        try {
            List<Attendance> todayAtt = attendanceService.getTodayAttendanceForEmployee(employeeId);
            if (!todayAtt.isEmpty()) {
                Attendance latest = todayAtt.get(0);
                sb.append("Check-in: ").append(latest.getCheckInTime() != null
                                ? latest.getCheckInTime().format(DateTimeFormatter.ofPattern("hh:mm a")) : "Not checked in yet")
                        .append("\nCheck-out: ").append(latest.getCheckOutTime() != null
                                ? latest.getCheckOutTime().format(DateTimeFormatter.ofPattern("hh:mm a")) : "Not checked out yet")
                        .append("\nStatus: ").append(latest.getStatus() != null ? latest.getStatus() : "N/A")
                        .append("\nWorking hours so far: ").append(latest.getTotalWorkingHours() != null
                                ? latest.getTotalWorkingHours() : "still being calculated")
                        .append("\n");
            } else {
                sb.append("Not checked in yet today.\n");
            }
        } catch (Exception e) {
            log.warn("Today attendance context failed for {}: {}", employeeId, e.getMessage());
            sb.append("Today's attendance data unavailable right now.\n");
        }

        sb.append("\n=== THIS MONTH'S ATTENDANCE SUMMARY (").append(month).append("/").append(year).append(") ===\n");
        try {
            Map<String, Object> summary = attendanceService.getMonthlySummaryForEmployee(employeeId, year, month);
            sb.append("Working days counted so far: ").append(summary.getOrDefault("totalDays", "N/A")).append("\n");
            sb.append("Present days: ").append(summary.getOrDefault("presentDays", "N/A")).append("\n");
            sb.append("Absent days: ").append(summary.getOrDefault("absentDays", "N/A")).append("\n");
            sb.append("Leave days: ").append(summary.getOrDefault("leaveDays", "N/A")).append("\n");
            sb.append("Attendance percentage: ").append(summary.getOrDefault("attendancePercentage", "N/A")).append("%\n");
            sb.append("Late arrivals: ").append(summary.getOrDefault("lateDays", "N/A")).append("\n");
            sb.append("Early departures: ").append(summary.getOrDefault("earlyDepartures", "N/A")).append("\n");
            sb.append("Total working hours: ").append(summary.getOrDefault("totalWorkingHours", "N/A")).append("\n");
        } catch (Exception e) {
            log.warn("Monthly summary context failed for {}: {}", employeeId, e.getMessage());
            sb.append("Monthly attendance summary unavailable right now.\n");
        }

        sb.append("\n=== LAST 7 DAYS ATTENDANCE LOG ===\n");
        try {
            List<Map<String, Object>> recent = attendanceService.getRecentAttendanceForEmployee(employeeId, 7);
            if (recent.isEmpty()) {
                sb.append("No attendance records in the last 7 days.\n");
            } else {
                for (Map<String, Object> day : recent) {
                    sb.append(day.getOrDefault("dateFormatted", "")).append(" (").append(day.getOrDefault("day", "")).append("): ")
                            .append(day.getOrDefault("statusText", day.getOrDefault("status", "")))
                            .append(", in ").append(day.getOrDefault("checkInFormatted", "--:--"))
                            .append(", out ").append(day.getOrDefault("checkOutFormatted", "--:--"))
                            .append(", worked ").append(day.getOrDefault("currentWorkingTime", "0h 00m"));
                    if (day.get("remarks") != null) {
                        sb.append(" — ").append(day.get("remarks"));
                    }
                    sb.append("\n");
                }
            }
        } catch (Exception e) {
            log.warn("Recent attendance context failed for {}: {}", employeeId, e.getMessage());
            sb.append("Recent attendance log unavailable right now.\n");
        }

        sb.append("\n=== LEAVE BALANCE ===\n");
        try {
            Map<String, Object> leaveData = leaveBalanceService.getEmployeeLeaveBalance(employeeId);
            Object balancesObj = leaveData.get("balances");

            if (balancesObj instanceof Map) {
                Map<String, Object> balances = (Map<String, Object>) balancesObj;
                appendLeaveLine(sb, "Sick", balances.get("sick"));
                appendLeaveLine(sb, "Casual", balances.get("casual"));
                appendLeaveLine(sb, "Earned", balances.get("earned"));
                sb.append("Total leave used this year: ").append(leaveData.getOrDefault("usedLeaves", "N/A")).append("\n");
                sb.append("Total leave allotted: ").append(leaveData.getOrDefault("totalLeaves", "N/A")).append("\n");
                sb.append("Total leave available: ").append(leaveData.getOrDefault("availableLeaves", "N/A")).append("\n");
            } else if (leaveData.containsKey("availableSickCasual")) {
                sb.append("Sick + Casual available: ").append(leaveData.get("availableSickCasual")).append(" days\n");
                sb.append("Earned Leave: unlimited accrual\n");
            } else {
                leaveData.forEach((k, v) -> sb.append(k).append(": ").append(v).append("\n"));
            }
        } catch (Exception e) {
            log.warn("Leave balance context failed for {}: {}", employeeId, e.getMessage());
            sb.append("Leave balance data unavailable right now.\n");
        }

        sb.append("\n=== LEAVE REQUESTS ===\n");
        try {
            List<LeaveRequest> leaves = leaveRequestRepository.findByEmployeeIdOrderByCreatedAtDesc(employeeId);

            List<LeaveRequest> pending = leaves.stream()
                    .filter(l -> "PENDING".equalsIgnoreCase(l.getStatus()))
                    .limit(5)
                    .collect(Collectors.toList());

            if (pending.isEmpty()) {
                sb.append("Pending requests: none.\n");
            } else {
                sb.append("Pending requests:\n");
                for (LeaveRequest l : pending) {
                    sb.append("- ").append(l.getLeaveType()).append(": ")
                            .append(l.getStartDate()).append(" to ").append(l.getEndDate())
                            .append(" (").append(l.getTotalDays()).append(" day(s)), reason: ")
                            .append(l.getReason()).append("\n");
                }
            }

            List<LeaveRequest> history = leaves.stream()
                    .filter(l -> "APPROVED".equalsIgnoreCase(l.getStatus()) || "REJECTED".equalsIgnoreCase(l.getStatus()))
                    .limit(5)
                    .collect(Collectors.toList());

            if (!history.isEmpty()) {
                sb.append("Recent history:\n");
                for (LeaveRequest l : history) {
                    sb.append("- ").append(l.getLeaveType()).append(": ")
                            .append(l.getStartDate()).append(" to ").append(l.getEndDate())
                            .append(" — ").append(l.getStatus());
                    if ("REJECTED".equalsIgnoreCase(l.getStatus()) && l.getRejectionReason() != null) {
                        sb.append(" (").append(l.getRejectionReason()).append(")");
                    }
                    sb.append("\n");
                }
            }
        } catch (Exception e) {
            log.warn("Leave requests context failed for {}: {}", employeeId, e.getMessage());
            sb.append("Leave request history unavailable right now.\n");
        }

        sb.append("\nUse the figures above directly — don't tell the employee to check another page for any of this.\n");

        return sb.toString();
    }

    private void appendLeaveLine(StringBuilder sb, String label, Object balanceObj) {
        if (balanceObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> bal = (Map<String, Object>) balanceObj;
            sb.append(label).append(" Leave — used: ").append(bal.getOrDefault("used", "N/A"))
                    .append(", balance: ").append(bal.getOrDefault("balance", "N/A")).append("\n");
        }
    }

    private ChatSession getOrCreateSession(ChatRequestDto request) {
        if (request.getSessionId() != null && request.getSessionId() > 0) {
            return sessionRepository.findById(request.getSessionId())
                    .orElseGet(() -> createNewSession(request.getEmployeeId(), false));
        }
        return createNewSession(request.getEmployeeId(), false);
    }

    private ChatSession createNewSession(String employeeId, boolean temporary) {
        ChatSession session = new ChatSession();
        session.setEmployeeId(employeeId);
        session.setIsTemporary(temporary);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    private void saveMessage(ChatSession session, String role, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setSession(session);
        msg.setRole(role);
        msg.setContent(content);
        msg.setCreatedAt(LocalDateTime.now());
        messageRepository.save(msg);
    }

    private List<OpenRouterService.MessagePair> buildHistoryFromDB(List<ChatMessage> messages) {
        List<OpenRouterService.MessagePair> pairs = new ArrayList<>();
        String userMsg = null;
        int start = Math.max(0, messages.size() - (MAX_HISTORY_PAIRS * 2));
        List<ChatMessage> recent = messages.subList(start, messages.size());

        for (ChatMessage msg : recent) {
            if ("user".equals(msg.getRole())) {
                userMsg = msg.getContent();
            } else if ("assistant".equals(msg.getRole()) && userMsg != null) {
                pairs.add(new OpenRouterService.MessagePair(userMsg, msg.getContent()));
                userMsg = null;
            }
        }
        return pairs;
    }

    private List<OpenRouterService.MessagePair> buildHistoryFromClient(List<ChatRequestDto.MessageDto> history) {
        List<OpenRouterService.MessagePair> pairs = new ArrayList<>();
        if (history == null || history.isEmpty()) return pairs;

        String userMsg = null;
        int start = Math.max(0, history.size() - (MAX_HISTORY_PAIRS * 2));

        for (int i = start; i < history.size(); i++) {
            ChatRequestDto.MessageDto msg = history.get(i);
            if ("user".equals(msg.getRole())) {
                userMsg = msg.getContent();
            } else if ("assistant".equals(msg.getRole()) && userMsg != null) {
                pairs.add(new OpenRouterService.MessagePair(userMsg, msg.getContent()));
                userMsg = null;
            }
        }
        return pairs;
    }

    private String generateSessionName(String firstMessage) {
        if (firstMessage == null || firstMessage.trim().isEmpty()) return "New Chat";
        return firstMessage.length() > 50
                ? firstMessage.substring(0, 47) + "..."
                : firstMessage;
    }

    private ChatResponseDto.MessageInfo toMessageInfo(ChatMessage msg) {
        ChatResponseDto.MessageInfo info = new ChatResponseDto.MessageInfo();
        info.setId(msg.getId());
        info.setRole(msg.getRole());
        info.setContent(msg.getContent());
        info.setCreatedAt(msg.getCreatedAt().format(FORMATTER));
        return info;
    }

    @Transactional(readOnly = true)
    public ChatResponseDto loadSessionHistory(Long sessionId, String employeeId) {
        ChatResponseDto response = new ChatResponseDto();
        try {
            ChatSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found"));

            if (!session.getEmployeeId().equals(employeeId)) {
                response.setSuccess(false);
                response.setError("Unauthorized access");
                return response;
            }

            List<ChatMessage> messages = messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
            List<ChatResponseDto.MessageInfo> messageInfos = messages.stream()
                    .map(this::toMessageInfo)
                    .collect(java.util.stream.Collectors.toList());

            response.setSuccess(true);
            response.setSessionId(sessionId);
            response.setSessionName(session.getSessionName());
            response.setMessages(messageInfos);
        } catch (Exception e) {
            log.error("Error loading session", e);
            response.setSuccess(false);
            response.setError(e.getMessage());
        }
        return response;
    }

    @Transactional(readOnly = true)
    public ChatResponseDto getAllSessions(String employeeId) {
        ChatResponseDto response = new ChatResponseDto();
        try {
            List<ChatSession> sessions = sessionRepository
                    .findByEmployeeIdAndIsTemporaryFalseOrderByUpdatedAtDesc(employeeId);

            List<ChatResponseDto.SessionInfo> sessionInfos = sessions.stream()
                    .map(s -> {
                        ChatResponseDto.SessionInfo info = new ChatResponseDto.SessionInfo();
                        info.setId(s.getId());
                        info.setSessionName(s.getSessionName() != null ? s.getSessionName() : "Chat " + s.getId());
                        info.setUpdatedAt(s.getUpdatedAt().format(FORMATTER));
                        return info;
                    })
                    .collect(java.util.stream.Collectors.toList());

            response.setSuccess(true);
            response.setSessions(sessionInfos);
        } catch (Exception e) {
            log.error("Error getting sessions", e);
            response.setSuccess(false);
            response.setError(e.getMessage());
        }
        return response;
    }

    public ChatResponseDto deleteSession(Long sessionId, String employeeId) {
        ChatResponseDto response = new ChatResponseDto();
        try {
            ChatSession session = sessionRepository.findById(sessionId)
                    .orElseThrow(() -> new RuntimeException("Session not found"));

            if (!session.getEmployeeId().equals(employeeId)) {
                response.setSuccess(false);
                response.setError("Unauthorized");
                return response;
            }

            messageRepository.deleteBySessionId(sessionId);
            sessionRepository.delete(session);
            response.setSuccess(true);
        } catch (Exception e) {
            log.error("Error deleting session", e);
            response.setSuccess(false);
            response.setError(e.getMessage());
        }
        return response;
    }
}