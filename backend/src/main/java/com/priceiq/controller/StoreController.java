package com.priceiq.controller;

import com.priceiq.entity.Store;
import com.priceiq.repository.StoreRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stores")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@Tag(name = "Store Management", description = "Endpoints for creating shops and assigning seller phone numbers")
public class StoreController {

    private final StoreRepository storeRepository;

    public StoreController(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @GetMapping
    @Operation(summary = "Get all registered stores")
    public ResponseEntity<List<Store>> getAllStores() {
        return ResponseEntity.ok(storeRepository.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get store by ID")
    public ResponseEntity<Store> getStoreById(@PathVariable Long id) {
        return storeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create new store and assign seller phone number")
    public ResponseEntity<Store> createStore(@RequestBody Store store) {
        if (store.getName() == null || store.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (store.getRating() == null) {
            store.setRating(4.8);
        }
        if (store.getLogoUrl() == null || store.getLogoUrl().trim().isEmpty()) {
            store.setLogoUrl("https://images.unsplash.com/photo-1526738549149-8e07eca6c147?auto=format&fit=crop&w=120&q=80");
        }
        Store saved = storeRepository.save(store);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update store and seller phone number")
    public ResponseEntity<Store> updateStore(@PathVariable Long id, @RequestBody Store storeDetails) {
        return storeRepository.findById(id).map(store -> {
            if (storeDetails.getName() != null) store.setName(storeDetails.getName());
            if (storeDetails.getOwnerPhone() != null) store.setOwnerPhone(storeDetails.getOwnerPhone());
            if (storeDetails.getLogoUrl() != null) store.setLogoUrl(storeDetails.getLogoUrl());
            if (storeDetails.getWebsiteUrl() != null) store.setWebsiteUrl(storeDetails.getWebsiteUrl());
            if (storeDetails.getRating() != null) store.setRating(storeDetails.getRating());
            Store updated = storeRepository.save(store);
            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete store")
    public ResponseEntity<Map<String, Boolean>> deleteStore(@PathVariable Long id) {
        if (storeRepository.existsById(id)) {
            storeRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("deleted", true));
        }
        return ResponseEntity.notFound().build();
    }
}
