package com.medvault.controller;

import com.medvault.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TestController {
    
    private final EmailService emailService;
    
    @GetMapping("/send-test-email")
    public String sendTestEmail(@RequestParam String email) {
        try {
            emailService.sendSetPasswordEmail(
                email,
                "Test Doctor",
                "test-token-12345"
            );
            return "Test email sent to: " + email;
        } catch (Exception e) {
            return "Failed to send email: " + e.getMessage();
        }
    }
}