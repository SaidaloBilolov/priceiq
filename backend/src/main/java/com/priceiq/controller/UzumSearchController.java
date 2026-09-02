package com.priceiq.controller;

import com.priceiq.dto.UzumProductDto;
import com.priceiq.service.ProductImportService;
import com.priceiq.service.UzumMarketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "Uzum Market Search API v1", description = "Endpoints for live Uzum Market search and Excel bulk import")
public class UzumSearchController {

    private final UzumMarketService uzumMarketService;
    private final ProductImportService productImportService;

    public UzumSearchController(UzumMarketService uzumMarketService, ProductImportService productImportService) {
        this.uzumMarketService = uzumMarketService;
        this.productImportService = productImportService;
    }

    @GetMapping("/search")
    @Operation(summary = "Search live products from Uzum Market API")
    public ResponseEntity<List<UzumProductDto>> searchUzumProducts(@RequestParam(required = false, defaultValue = "") String query,
                                                                   @RequestParam(required = false) String q) {
        String searchQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : (q != null ? q.trim() : "");
        List<UzumProductDto> results = uzumMarketService.searchProducts(searchQuery);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/trending")
    @Operation(summary = "Get live trending best deals from Uzum Market")
    public ResponseEntity<List<UzumProductDto>> getTrendingProducts() {
        List<UzumProductDto> trending = uzumMarketService.searchProducts("smartfon");
        if (trending.isEmpty()) {
            trending = uzumMarketService.searchProducts("televizor");
        }
        return ResponseEntity.ok(trending);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Bulk import products from Excel (.xlsx) file")
    public ResponseEntity<Map<String, Object>> importProducts(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Fayl yuborilmadi yoki u bo'sh", "success", false));
        }
        try {
            int count = productImportService.importProductsFromExcel(file);
            return ResponseEntity.ok(Map.of(
                    "message", "Excel faylidan " + count + " ta mahsulot muvaffaqiyatli import qilindi",
                    "count", count,
                    "success", true
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Import qilishda xatolik: " + e.getMessage(), "success", false));
        }
    }
}
