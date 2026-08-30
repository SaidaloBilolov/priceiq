package com.priceiq.controller;

import com.priceiq.entity.SupportOperator;
import com.priceiq.repository.SupportOperatorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/support-operators")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "Support Operators", description = "Endpoints for managing support operator profiles and Telegram chat bindings")
public class SupportOperatorController {

    private final SupportOperatorRepository supportOperatorRepository;

    public SupportOperatorController(SupportOperatorRepository supportOperatorRepository) {
        this.supportOperatorRepository = supportOperatorRepository;
    }

    @GetMapping
    @Operation(summary = "List all support operators")
    public ResponseEntity<List<SupportOperator>> getAllOperators() {
        return ResponseEntity.ok(supportOperatorRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get operator by ID")
    public ResponseEntity<SupportOperator> getOperatorById(@PathVariable Long id) {
        return supportOperatorRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create new support operator profile")
    public ResponseEntity<SupportOperator> createOperator(@RequestBody SupportOperator operator) {
        if (operator.getFullName() == null || operator.getFullName().trim().isEmpty() ||
            operator.getPhoneNumber() == null || operator.getPhoneNumber().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (operator.getIsActive() == null) {
            operator.setIsActive(true);
        }
        SupportOperator saved = supportOperatorRepository.save(operator);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update support operator status or details")
    public ResponseEntity<SupportOperator> updateOperator(@PathVariable Long id, @RequestBody SupportOperator details) {
        return supportOperatorRepository.findById(id).map(op -> {
            if (details.getFullName() != null) op.setFullName(details.getFullName());
            if (details.getPhoneNumber() != null) op.setPhoneNumber(details.getPhoneNumber());
            if (details.getTelegramChatId() != null) op.setTelegramChatId(details.getTelegramChatId());
            if (details.getIsActive() != null) op.setIsActive(details.getIsActive());
            SupportOperator updated = supportOperatorRepository.save(op);
            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete support operator")
    public ResponseEntity<Map<String, Boolean>> deleteOperator(@PathVariable Long id) {
        if (supportOperatorRepository.existsById(id)) {
            supportOperatorRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("deleted", true));
        }
        return ResponseEntity.notFound().build();
    }
}
