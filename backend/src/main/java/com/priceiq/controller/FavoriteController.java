package com.priceiq.controller;

import com.priceiq.dto.ProductDto;
import com.priceiq.entity.Favorite;
import com.priceiq.entity.Product;
import com.priceiq.entity.User;
import com.priceiq.repository.FavoriteRepository;
import com.priceiq.repository.ProductRepository;
import com.priceiq.repository.UserRepository;
import com.priceiq.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favorites")
@CrossOrigin(origins = "*")
@Tag(name = "Favorites", description = "Endpoints for user favorite smartphones")
public class FavoriteController {

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;

    public FavoriteController(FavoriteRepository favoriteRepository, UserRepository userRepository, ProductRepository productRepository, ProductService productService) {
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Get user favorite products by Telegram ID")
    public ResponseEntity<List<ProductDto>> getFavorites(@RequestParam Long telegramId) {
        Optional<User> userOpt = userRepository.findByTelegramId(telegramId);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        List<Favorite> favorites = favoriteRepository.findByUserId(userOpt.get().getId());
        List<ProductDto> dtos = favorites.stream()
                .map(f -> productService.mapToDto(f.getProduct()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    @Transactional
    @Operation(summary = "Add or toggle product favorite status")
    public ResponseEntity<?> addFavorite(@RequestBody Map<String, Long> payload) {
        Long telegramId = payload.get("telegramId");
        Long productId = payload.get("productId");

        if (telegramId == null || productId == null) {
            return ResponseEntity.badRequest().body("telegramId and productId are required");
        }

        User user = userRepository.findByTelegramId(telegramId)
                .orElseGet(() -> {
                    User u = new User();
                    u.setTelegramId(telegramId);
                    return userRepository.save(u);
                });

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        Optional<Favorite> existing = favoriteRepository.findByUserIdAndProductId(user.getId(), product.getId());
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return ResponseEntity.ok(Map.of("favorited", false));
        } else {
            Favorite fav = new Favorite();
            fav.setUser(user);
            fav.setProduct(product);
            favoriteRepository.save(fav);
            return ResponseEntity.ok(Map.of("favorited", true));
        }
    }
}
