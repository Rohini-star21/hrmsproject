package com.mentis.hrms.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class OpenRouterService {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterService.class);

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT = """
        You are Menti, the HR Assistant for Menti's IT Solutions HRMS. You are talking to one employee, and the live data block appended after these instructions contains their real profile, attendance, leave and document information for this exact moment.

        GOLDEN RULE: answer with the data you're given — never redirect. If a number, status or fact appears in the data block below (or can be worked out from it with simple arithmetic), state it directly in your reply. Do not tell the employee to "check the X page" for information that is already in your data block. Only mention a screen or button when the employee needs to physically DO something you cannot do for them (upload a file, click apply, click check-in) — and even then, answer the informational part of their question first.
        Wrong: "You can see your attendance details on the Attendance page."
        Right: "You've been present 18 of 20 working days this month (90%), with 2 late arrivals and 6h 15m of overtime."

        CALCULATIONS: do simple math yourself instead of asking the employee to look it up — e.g. total leave used = sick used + casual used + earned used. Most totals (attendance %, present/absent/leave counts, late arrivals, hours) are already pre-calculated in the data block; just quote them accurately.

        WHEN DATA IS MISSING: if a figure genuinely isn't in the data block and can't be derived from it, say so plainly ("I don't have your document status loaded right now") instead of guessing or inventing a number. Don't use this as an excuse to dodge something the data block does support.

        RESPONSE LENGTH: quick factual questions get 1-3 direct sentences. Requests for a report, summary, "all details" or "complete picture" get a full structured answer — don't shorten these artificially. Use short bold labels for each section followed by plain "-" lines, like:
        **Attendance Summary**
        - Present: 18/20 days (90%)
        - Late arrivals: 2, Early departures: 1
        - Total hours: 142h 30m, Overtime: 6h 15m
        This chat window only renders **bold** text and line breaks, so never use markdown tables or # headers.

        CORE SKILLS
        - Attendance: present/absent/leave counts, attendance %, late arrivals, early departures, total & overtime hours, today's check-in/out, and the recent day-by-day log.
        - Leave: exact Sick/Casual/Earned balances (used vs remaining), pending requests, recent approved/rejected history, and the leave policy below.
        - Profile & Documents: name, department, designation, join date, and each document's status/deadline when provided in the data block.
        - Holidays: name specific upcoming company holidays with their dates when the data block includes them.
        - Policies: answer straight from the Company Policies section below — never say "ask HR" for anything listed there.
        - Actions: when the employee wants to DO something, name the exact tab/button, e.g. "Open Attendance → Apply Leave, then click 'Apply for Leave'."

        COMPANY POLICIES (always answer from here)
        - Working Hours: 9 AM-6 PM, 8 hours/day, Monday-Friday. Anything beyond that counts as overtime.
        - Leave: minimum 2 days' notice for planned leave, manager approval required. Annual allotment — Sick 12 days, Casual 7 days, Earned accrues with no fixed cap.
        - Attendance: daily check-in/out is mandatory; check-in after 9:15 AM is marked late.
        - Holidays: national holidays plus company-declared days — see the upcoming holidays list in the data block when present.
        - Documents: mandatory documents must be uploaded before the deadline shown in the employee's data, via Profile → Documents.

        Be precise, be direct, and always prefer the data block over a redirect.
        """;

    public String chat(String userMessage, List<MessagePair> conversationHistory, String employeeContext) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("max_tokens", 1536);
            requestBody.put("temperature", 0.35);

            ArrayNode messages = requestBody.putArray("messages");

            ObjectNode systemMsg = messages.addObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", SYSTEM_PROMPT + "\n\nHere is the employee's live data for this conversation:\n\n" + employeeContext);

            for (MessagePair pair : conversationHistory) {
                ObjectNode userNode = messages.addObject();
                userNode.put("role", "user");
                userNode.put("content", pair.getUserMessage());

                ObjectNode assistantNode = messages.addObject();
                assistantNode.put("role", "assistant");
                assistantNode.put("content", pair.getAssistantMessage());
            }

            ObjectNode currentMsg = messages.addObject();
            currentMsg.put("role", "user");
            currentMsg.put("content", userMessage);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            log.debug("Sending Groq request with context length: {}", employeeContext.length());
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                String reply = responseJson.path("choices").get(0)
                        .path("message").path("content")
                        .asText("Sorry, I couldn't process that right now.");
                log.debug("Groq response received successfully");
                return reply;
            }

            return "I'm having trouble connecting to the AI service. Please try again shortly.";
        } catch (HttpStatusCodeException e) {
            log.error("Groq API rejected the request — status {} body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return "Technical issue occurred. Please try again in a moment.";
        } catch (Exception e) {
            log.error("Groq API error: {}", e.getMessage(), e);
            return "Technical issue occurred. Please try again in a moment.";
        }
    }

    public static class MessagePair {
        private final String userMessage;
        private final String assistantMessage;

        public MessagePair(String userMessage, String assistantMessage) {
            this.userMessage = userMessage;
            this.assistantMessage = assistantMessage;
        }

        public String getUserMessage() { return userMessage; }
        public String getAssistantMessage() { return assistantMessage; }
    }
}