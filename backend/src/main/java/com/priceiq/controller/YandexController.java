package com.priceiq.controller;

import com.priceiq.service.YandexMarketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/yandex")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "Yandex.Market API Integration", description = "Endpoints for Yandex.Market partner integration and API testing")
public class YandexController {

    private final YandexMarketService yandexMarketService;

    public YandexController(YandexMarketService yandexMarketService) {
        this.yandexMarketService = yandexMarketService;
    }

    @GetMapping("/health")
    @Operation(summary = "Test Yandex.Market API connection with Api-Key header")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> healthResult = yandexMarketService.testConnection();
        return ResponseEntity.ok(healthResult);
    }

    @GetMapping("/campaigns")
    @Operation(summary = "Fetch Yandex.Market partner campaigns")
    public ResponseEntity<String> getCampaigns() {
        try {
            return yandexMarketService.getCampaigns();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/search")
    @Operation(summary = "Search products on Yandex.Market")
    public ResponseEntity<String> searchProducts(@RequestParam(required = false, defaultValue = "") String query,
                                                 @RequestParam(required = false, defaultValue = "1") Integer page,
                                                 @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        try {
            return yandexMarketService.searchProducts(query, page, pageSize);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
