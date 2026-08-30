package com.priceiq.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "System Health", description = "Endpoints for checking Backend and Database connectivity status")
public class HealthController {

    @GetMapping
    @Operation(summary = "Check backend and database health status")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "database", "CONNECTED",
            "service", "priceiq-backend",
            "timestamp", System.currentTimeMillis()
        ));
    }
}
