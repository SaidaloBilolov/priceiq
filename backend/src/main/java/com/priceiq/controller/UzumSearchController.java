package com.priceiq.controller;

import com.priceiq.dto.UzumProductDto;
import com.priceiq.service.UzumMarketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "Uzum Market Search API v1", description = "Endpoints for live Uzum Market search for Web & Mini App")
public class UzumSearchController {

    private final UzumMarketService uzumMarketService;

    public UzumSearchController(UzumMarketService uzumMarketService) {
        this.uzumMarketService = uzumMarketService;
    }

    @GetMapping("/search")
    @Operation(summary = "Search live products from Uzum Market API")
    public ResponseEntity<List<UzumProductDto>> searchUzumProducts(@RequestParam(required = false, defaultValue = "") String query,
                                                                   @RequestParam(required = false) String q) {
        String searchQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : (q != null ? q.trim() : "");
        List<UzumProductDto> results = uzumMarketService.searchProducts(searchQuery);
        return ResponseEntity.ok(results);
    }
}
