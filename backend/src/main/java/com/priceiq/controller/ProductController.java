package com.priceiq.controller;

import com.priceiq.dto.ProductDto;
import com.priceiq.dto.UzumProductDto;
import com.priceiq.service.FileStorageService;
import com.priceiq.service.ProductService;
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
@RequestMapping("/api/products")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "Product Catalog", description = "Endpoints for Smartphone search, deal scores, price comparison, and Admin CRUD")
public class ProductController {

    private final ProductService productService;
    private final FileStorageService fileStorageService;
    private final UzumMarketService uzumMarketService;

    public ProductController(ProductService productService, FileStorageService fileStorageService, UzumMarketService uzumMarketService) {
        this.productService = productService;
        this.fileStorageService = fileStorageService;
        this.uzumMarketService = uzumMarketService;
    }

    @GetMapping
    @Operation(summary = "List all products or search by query / category")
    public ResponseEntity<List<ProductDto>> getProducts(@RequestParam(required = false) String search,
                                                        @RequestParam(required = false) Long categoryId) {
        if (categoryId != null) {
            return ResponseEntity.ok(productService.getProductsByCategory(categoryId));
        }
        if (search != null && !search.trim().isEmpty()) {
            return ResponseEntity.ok(productService.searchProducts(search));
        }
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/search")
    @Operation(summary = "Search live products from Uzum Market API")
    public ResponseEntity<List<UzumProductDto>> searchUzumProducts(@RequestParam(required = false, defaultValue = "") String query,
                                                                   @RequestParam(required = false) String q) {
        String searchQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : (q != null ? q.trim() : "");
        List<UzumProductDto> results = uzumMarketService.searchProducts(searchQuery);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed smartphone information with sorted store offers and price history")
    public ResponseEntity<ProductDto> getProductById(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload image file from PC for product creation")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
        }
        String imageUrl = fileStorageService.storeFile(file);
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

    @PostMapping
    @Operation(summary = "Create new smartphone with store offer")
    public ResponseEntity<ProductDto> createProduct(@RequestBody ProductDto productDto) {
        ProductDto created = productService.createProduct(productDto);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update existing smartphone details and offer")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id, @RequestBody ProductDto productDto) {
        ProductDto updated = productService.updateProduct(id, productDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete smartphone product by ID")
    public ResponseEntity<Map<String, Boolean>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(Map.of("deleted", true));
    }
}
