package com.priceiq.controller;

import com.priceiq.dto.UpdatePhoneRequest;
import com.priceiq.entity.User;
import com.priceiq.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "User Management", description = "Endpoints for Telegram User profile & phone number updates")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get user profile by Telegram ID")
    public ResponseEntity<User> getUserProfile(@RequestParam Long telegramId,
                                                @RequestParam(required = false) String firstName,
                                                @RequestParam(required = false) String username,
                                                @RequestParam(required = false) String languageCode) {
        User user = userService.getOrCreateUser(telegramId, firstName, username, languageCode);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/phone")
    @Operation(summary = "Update user primary phone number & language preference")
    public ResponseEntity<User> updatePhoneNumber(@Valid @RequestBody UpdatePhoneRequest request) {
        User updated = userService.updatePhoneNumber(request);
        return ResponseEntity.ok(updated);
    }
}
