package com.priceiq.controller;

import com.priceiq.entity.PriceAlert;
import com.priceiq.entity.Product;
import com.priceiq.entity.User;
import com.priceiq.repository.PriceAlertRepository;
import com.priceiq.repository.ProductRepository;
import com.priceiq.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/alerts")
@CrossOrigin(origins = "*")
@Tag(name = "Price Alerts", description = "Endpoints for setting custom target UZS price alerts")
public class PriceAlertController {

    private final PriceAlertRepository alertRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public PriceAlertController(PriceAlertRepository alertRepository, UserRepository userRepository, ProductRepository productRepository) {
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @GetMapping
    @Operation(summary = "Get user price alerts")
    public ResponseEntity<List<PriceAlert>> getAlerts(@RequestParam Long telegramId) {
        Optional<User> userOpt = userRepository.findByTelegramId(telegramId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }
        return ResponseEntity.ok(alertRepository.findByUserId(userOpt.get().getId()));
    }

    @PostMapping
    @Operation(summary = "Create custom target price alert in UZS")
    public ResponseEntity<PriceAlert> createAlert(@RequestBody Map<String, Object> payload) {
        Long telegramId = Long.valueOf(payload.get("telegramId").toString());
        Long productId = Long.valueOf(payload.get("productId").toString());
        Long targetPriceUzs = Long.valueOf(payload.get("targetPriceUzs").toString());

        User user = userRepository.findByTelegramId(telegramId)
                .orElseGet(() -> {
                    User u = new User();
                    u.setTelegramId(telegramId);
                    return userRepository.save(u);
                });

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        PriceAlert alert = new PriceAlert();
        alert.setUser(user);
        alert.setProduct(product);
        alert.setTargetPriceUzs(targetPriceUzs);
        alert.setIsActive(true);

        return ResponseEntity.ok(alertRepository.save(alert));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete price alert by ID")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        alertRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
