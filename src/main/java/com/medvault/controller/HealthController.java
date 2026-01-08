package com.medvault.controller;

import org.springframework.http. ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework. web.bind.annotation.RequestMapping;
import org.springframework. web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class HealthController {
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health. put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "MedVault API");
        health.put("version", "1.0.0");
        return ResponseEntity.ok(health);
    }
}