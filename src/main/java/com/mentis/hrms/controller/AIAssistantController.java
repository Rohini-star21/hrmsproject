package com.mentis.hrms.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AIAssistantController {

    private static final Logger logger = LoggerFactory.getLogger(AIAssistantController.class);

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.model}")
    private String model;

    // Test endpoint to verify Groq API key
    @GetMapping("/test-key")
    public ResponseEntity<Map<String, Object>> testApiKey() {
        Map<String, Object> result = new HashMap<>();
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                    Map.of("role", "user", "content", "Say 'API is working!'")
            ));
            requestBody.put("max_tokens", 20);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, Map.class);

            result.put("success", true);
            result.put("data", response.getBody());
            result.put("message", "✅ Groq API Key is WORKING!");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("API test failed: {}", e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("message", "❌ API test failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PostMapping("/generate-announcement")
    public ResponseEntity<Map<String, Object>> generateAnnouncement(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");

        if (prompt == null || prompt.trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Prompt is required");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        logger.info("🤖 Groq AI Generation Request: {}", prompt);

        // Try Groq API first
        Map<String, Object> aiResult = callGroqAPI(prompt);
        if (aiResult != null) {
            logger.info("✅ Groq AI generation successful");
            return ResponseEntity.ok(aiResult);
        }

        // If Groq fails, use intelligent context-aware response
        logger.info("Using intelligent context-aware response");
        Map<String, Object> contextResult = getContextAwareResponse(prompt);
        return ResponseEntity.ok(contextResult);
    }

    private Map<String, Object> callGroqAPI(String prompt) {
        // Groq supports multiple models - try fallbacks if primary fails[citation:3]
        String[] modelsToTry = {
                model,
                "llama-3.1-70b-versatile",
                "llama-3.1-8b-instant",
                "mixtral-8x7b-32768",
                "gemma2-9b-it"
        };

        for (String modelName : modelsToTry) {
            try {
                logger.info("🔄 Trying Groq model: {}", modelName);

                RestTemplate restTemplate = new RestTemplate();

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + apiKey);

                String systemPrompt = "You are an expert HR manager. Generate a professional company announcement. " +
                        "Return ONLY valid JSON. No markdown. Format: {\"title\": \"title\", \"content\": \"announcement with \\n for line breaks\"}";

                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", modelName);
                requestBody.put("messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", "Write a professional company announcement about: " + prompt)
                ));
                requestBody.put("temperature", 0.7);
                requestBody.put("max_tokens", 500);
                requestBody.put("top_p", 0.9);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<Map> response = restTemplate.exchange(
                        apiUrl,
                        HttpMethod.POST,
                        entity,
                        Map.class
                );

                if (response.getBody() != null && response.getBody().containsKey("choices")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                    if (!choices.isEmpty()) {
                        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                        String aiResponse = (String) message.get("content");

                        logger.info("✅ Groq model {} succeeded!", modelName);
                        return parseAIResponse(aiResponse, prompt);
                    }
                }
            } catch (Exception e) {
                logger.warn("Groq model {} failed: {}", modelName, e.getMessage());
            }
        }

        logger.error("All Groq models failed!");
        return null;
    }

    private Map<String, Object> parseAIResponse(String aiResponse, String originalPrompt) {
        try {
            int start = aiResponse.indexOf("{");
            int end = aiResponse.lastIndexOf("}") + 1;
            if (start >= 0 && end > start) {
                String jsonStr = aiResponse.substring(start, end);
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> data = mapper.readValue(jsonStr, Map.class);

                Map<String, Object> result = new HashMap<>();
                result.put("title", data.getOrDefault("title", getSmartTitle(originalPrompt)));
                result.put("content", data.getOrDefault("content", getSmartContent(originalPrompt)));
                result.put("priority", getPriority(originalPrompt));
                result.put("type", getType(originalPrompt));
                result.put("category", getCategory(originalPrompt));
                result.put("pinned", false);
                return result;
            }
        } catch (Exception e) {
            logger.error("JSON parse error: {}", e.getMessage());
        }
        return null;
    }

    // ========== CONTEXT-AWARE RESPONSE GENERATION (FALLBACK) ==========

    private Map<String, Object> getContextAwareResponse(String prompt) {
        Map<String, Object> result = new HashMap<>();
        String lowerPrompt = prompt.toLowerCase();

        // Weather/Heavy Rain Holiday
        if ((lowerPrompt.contains("rain") || lowerPrompt.contains("flood") || lowerPrompt.contains("weather")) &&
                (lowerPrompt.contains("holiday") || lowerPrompt.contains("close") || lowerPrompt.contains("off"))) {

            result.put("title", "⛈️ Office Closure Due to Heavy Rains");
            result.put("content", "Dear Team,\n\n" +
                    "Due to the ongoing heavy rainfall and waterlogging, the management has decided to close the office today.\n\n" +
                    "📌 Important Instructions:\n" +
                    "• Please stay indoors and avoid unnecessary travel\n" +
                    "• Work from home if your role permits\n" +
                    "• In case of emergency, contact HR\n\n" +
                    "Your safety is our top priority.\n\n" +
                    "Best regards,\nHR Department");
            result.put("priority", "URGENT");
            result.put("type", "TEMPORARY");
            result.put("category", "HR");
        }
        // New Employee Joining
        else if (lowerPrompt.contains("appointed") || lowerPrompt.contains("new employee") ||
                lowerPrompt.contains("joining") || lowerPrompt.contains("welcome")) {

            result.put("title", "👋 Welcome Our New Team Member!");
            result.put("content", "Dear Team,\n\n" +
                    prompt + "\n\n" +
                    "Please join us in giving a warm welcome to our new colleague!\n\n" +
                    "Best regards,\nHR Department");
            result.put("priority", "NORMAL");
            result.put("type", "PERMANENT");
            result.put("category", "HR");
        }
        // Festival Holiday
        else if (lowerPrompt.contains("holiday") && !lowerPrompt.contains("rain")) {
            result.put("title", "🎉 Holiday Announcement");
            result.put("content", "Dear Team,\n\n" +
                    prompt + "\n\n" +
                    "Wishing you and your family a wonderful celebration!\n\n" +
                    "Best regards,\nHR Department");
            result.put("priority", "NORMAL");
            result.put("type", "TEMPORARY");
            result.put("category", "HR");
        }
        // Promotion
        else if (lowerPrompt.contains("promotion")) {
            result.put("title", "🎉 Promotion Announcement");
            result.put("content", "Dear Team,\n\n" +
                    prompt + "\n\n" +
                    "Please join us in congratulating our colleague on this well-deserved achievement!\n\n" +
                    "Best regards,\nManagement Team");
            result.put("priority", "HIGH");
            result.put("type", "PERMANENT");
            result.put("category", "Corporate");
        }
        // Meeting
        else if (lowerPrompt.contains("meeting")) {
            result.put("title", "📅 Meeting Schedule");
            result.put("content", "Hello Team,\n\n" +
                    prompt + "\n\n" +
                    "📌 Meeting Details:\n" +
                    "• Platform: Google Meet / Microsoft Teams\n" +
                    "• Link will be shared before the meeting\n\n" +
                    "Please join on time.\n\n" +
                    "Best regards,\nOrganizer");
            result.put("priority", "NORMAL");
            result.put("type", "TEMPORARY");
            result.put("category", "Corporate");
        }
        // Maintenance
        else if (lowerPrompt.contains("maintenance")) {
            result.put("title", "⚠️ System Maintenance Notice");
            result.put("content", "Dear Team,\n\n" +
                    prompt + "\n\n" +
                    "We appreciate your understanding and cooperation.\n\n" +
                    "Regards,\nIT Department");
            result.put("priority", "URGENT");
            result.put("type", "TEMPORARY");
            result.put("category", "Maintenance");
        }
        // Policy Update
        else if (lowerPrompt.contains("policy")) {
            result.put("title", "📋 Policy Update Announcement");
            result.put("content", "Dear Team,\n\n" +
                    prompt + "\n\n" +
                    "For questions, contact HR.\n\n" +
                    "Best regards,\nHR Department");
            result.put("priority", "HIGH");
            result.put("type", "PERMANENT");
            result.put("category", "Policy");
        }
        // Achievement
        else if (lowerPrompt.contains("achievement") || lowerPrompt.contains("milestone")) {
            result.put("title", "🏆 Achievement Milestone");
            result.put("content", "Dear Team,\n\n" +
                    prompt + "\n\n" +
                    "Congratulations to the entire team!\n\n" +
                    "Best regards,\nManagement Team");
            result.put("priority", "HIGH");
            result.put("type", "PERMANENT");
            result.put("category", "Corporate");
        }
        // Default
        else {
            result.put("title", "📢 Official Announcement");
            result.put("content", "Dear Team,\n\n" + prompt + "\n\n" +
                    "For questions, please reach out to HR.\n\n" +
                    "Best regards,\nManagement Team");
            result.put("priority", "NORMAL");
            result.put("type", "TEMPORARY");
            result.put("category", "Corporate");
        }

        result.put("pinned", result.get("priority").equals("URGENT"));
        return result;
    }

    // ========== HELPER METHODS ==========

    private String getSmartTitle(String prompt) {
        String lower = prompt.toLowerCase();
        if (lower.contains("rain") || lower.contains("flood")) return "⛈️ Emergency Closure";
        if (lower.contains("appointed") || lower.contains("new employee")) return "👋 New Team Member";
        if (lower.contains("promotion")) return "🎉 Promotion Announcement";
        if (lower.contains("holiday")) return "🎉 Holiday Announcement";
        if (lower.contains("meeting")) return "📅 Meeting Schedule";
        if (lower.contains("maintenance")) return "⚠️ Maintenance Notice";
        if (lower.contains("policy")) return "📋 Policy Update";
        if (lower.contains("achievement")) return "🏆 Achievement";
        return "📢 " + (prompt.length() > 50 ? prompt.substring(0, 47) + "..." : prompt);
    }

    private String getSmartContent(String prompt) {
        String lower = prompt.toLowerCase();
        if (lower.contains("rain") || lower.contains("flood")) {
            return "Dear Team,\n\nDue to heavy rains, the office will remain closed. Stay safe!\n\nBest regards,\nHR Department";
        }
        if (lower.contains("appointed") || lower.contains("new employee")) {
            return "Dear Team,\n\n" + prompt + "\n\nPlease welcome our new colleague!\n\nBest regards,\nHR Department";
        }
        return "Dear Team,\n\n📢 " + prompt + "\n\nBest regards,\nHR Department";
    }

    private String getPriority(String prompt) {
        String lower = prompt.toLowerCase();
        if (lower.contains("urgent") || lower.contains("rain") || lower.contains("emergency")) return "URGENT";
        if (lower.contains("important") || lower.contains("achievement") || lower.contains("promotion")) return "HIGH";
        return "NORMAL";
    }

    private String getType(String prompt) {
        String lower = prompt.toLowerCase();
        if (lower.contains("policy") || lower.contains("permanent") || lower.contains("promotion") || lower.contains("achievement")) {
            return "PERMANENT";
        }
        return "TEMPORARY";
    }

    private String getCategory(String prompt) {
        String lower = prompt.toLowerCase();
        if (lower.contains("hr") || lower.contains("policy") || lower.contains("holiday") || lower.contains("appointed")) return "HR";
        if (lower.contains("maintenance")) return "Maintenance";
        return "Corporate";
    }
}