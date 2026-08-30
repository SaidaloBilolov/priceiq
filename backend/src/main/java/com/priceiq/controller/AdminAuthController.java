package com.priceiq.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "Admin Authentication", description = "Endpoints for Admin Portal authentication")
public class AdminAuthController {

    @PostMapping("/login")
    @Operation(summary = "Authenticate Admin user credentials")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (username != null && username.trim().equalsIgnoreCase("admin") &&
            password != null && password.trim().equals("admin123")) {
            
            return ResponseEntity.ok(Map.of(
                "status", "ok",
                "message", "Authentication successful",
                "token", "priceiq-admin-session-token-998877",
                "role", "SUPER_ADMIN",
                "username", "admin"
            ));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Login yoki parol noto'g'ri"));
    }
}
