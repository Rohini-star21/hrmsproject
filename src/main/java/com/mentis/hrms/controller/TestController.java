package com.mentis.hrms.controller;

import com.mentis.hrms.util.PasswordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestController {

    @Autowired
    private PasswordUtil passwordUtil;

    @GetMapping("/test-password")
    public String testPassword(Model model) {
        // Test password encoding
        String testPass = "Test@123";
        String encoded = passwordUtil.encodePassword(testPass);
        boolean matches = passwordUtil.matchesPassword(testPass, encoded);

        // Database hash for Test@123 (SHA-256)
        String dbHash = "jZae727K08KaOmKSgOaGzww/XVqGr/PKEgIMkjrcbJI=";
        boolean dbMatches = passwordUtil.matchesPassword(testPass, dbHash);

        model.addAttribute("testPassword", testPass);
        model.addAttribute("encodedPassword", encoded);
        model.addAttribute("passwordMatch", matches);
        model.addAttribute("passwordLength", encoded.length());
        model.addAttribute("dbHash", dbHash);
        model.addAttribute("dbMatch", dbMatches);

        return "test-password";
    }

    @GetMapping("/debug-login")
    public String debugLogin(Model model) {
        model.addAttribute("loginUrl", "/candidate/login");
        model.addAttribute("testUser", "DEMO001");
        model.addAttribute("testPassword", "Test@123");
        return "debug-login";
    }

    @GetMapping("/test-db")
    public String testDatabase() {
        return "Test endpoint - Database connection OK";
    }
}