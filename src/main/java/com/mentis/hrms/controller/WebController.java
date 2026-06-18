package com.mentis.hrms.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    private static final Logger logger = LoggerFactory.getLogger(WebController.class);

    @GetMapping("/announcement/create")
    public String getManualCreatePage() {
        logger.info("=== GET /announcement/create (Manual Form) ===");
        return "announcement-manual";
    }

    @GetMapping("/announcement/ai-popup")
    public String getAIPopup() {
        logger.info("=== GET /announcement/ai-popup ===");
        return "ai-popup";
    }

    @GetMapping("/announcement/generated-form")
    public String getGeneratedForm() {
        logger.info("=== GET /announcement/generated-form ===");
        return "generated-announcement-form";
    }
}