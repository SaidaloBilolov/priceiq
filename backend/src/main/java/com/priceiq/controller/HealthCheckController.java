package com.priceiq.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "Health Check", description = "Endpoints for UptimeRobot monitoring and system health check")
public class HealthCheckController {

    @GetMapping(value = {"/", "/health"})
    @Operation(summary = "Root and health check endpoint for UptimeRobot monitoring")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("PriceIQ Backend is Active");
    }
}
