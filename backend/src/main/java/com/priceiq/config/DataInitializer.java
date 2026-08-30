package com.priceiq.config;

import com.priceiq.entity.*;
import com.priceiq.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final ProductOfferRepository offerRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    public DataInitializer(CategoryRepository categoryRepository, StoreRepository storeRepository, ProductRepository productRepository, ProductOfferRepository offerRepository, PriceHistoryRepository priceHistoryRepository) {
        this.categoryRepository = categoryRepository;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.offerRepository = offerRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Ensure universal categories exist
        ensureCategory("Smartfonlar", "Смартфоны", "Smartphones", "Smartphone");
        ensureCategory("Noutbuklar", "Ноутбуки", "Laptops", "Laptop");
        ensureCategory("Maishiy Texnika", "Бытовая техника", "Appliances", "Appliance");
        ensureCategory("Kiyim-kechak", "Одежда", "Clothing", "Clothing");
        ensureCategory("Aksessuarlar", "Аксессуары", "Accessories", "Accessory");
        ensureCategory("Boshqa", "Другое", "Other", "General");

        if (productRepository.count() > 0) {
            return;
        }

        Category catSmartphones = categoryRepository.findAll().stream()
                .filter(c -> c.getNameUz().equalsIgnoreCase("Smartfonlar"))
                .findFirst()
                .orElseGet(() -> categoryRepository.findAll().get(0));

        // Stores
        Store olcha = storeRepository.save(new Store(null, "Olcha.uz",
                "https://images.unsplash.com/photo-1526738549149-8e07eca6c147?auto=format&fit=crop&w=120&q=80",
                "https://olcha.uz", 4.8));

        Store texnomart = storeRepository.save(new Store(null, "Texnomart",
                "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=120&q=80",
                "https://texnomart.uz", 4.7));

        Store asaxiy = storeRepository.save(new Store(null, "Asaxiy",
                "https://images.unsplash.com/photo-1550009158-9ebf69173e03?auto=format&fit=crop&w=120&q=80",
                "https://asaxiy.uz", 4.9));

        Store uzum = storeRepository.save(new Store(null, "Uzum Nasiya / Market",
                "https://images.unsplash.com/photo-1512499617640-c74ae3a79d37?auto=format&fit=crop&w=120&q=80",
                "https://uzum.uz", 4.6));

        // Initial Smartphone Data
        Product p1 = productRepository.save(new Product(null, "Apple iPhone 16 Pro Max 256GB Tabiiy Titan", "Apple iPhone 16 Pro Max 256GB Натуральный Титан", "Apple iPhone 16 Pro Max 256GB Natural Titanium", "Apple", "iPhone 16 Pro Max", "256GB", "8GB", "Tabiiy Titan", "https://images.unsplash.com/photo-1695048133142-1a20484d2569?auto=format&fit=crop&w=600&q=80", catSmartphones));
        offerRepository.save(new ProductOffer(null, p1, uzum, 17800000L, 18500000L, true, "https://uzum.uz/product/16-pro-max"));
        offerRepository.save(new ProductOffer(null, p1, texnomart, 18200000L, 19000000L, true, "https://texnomart.uz/product/16-pro-max"));
        priceHistoryRepository.save(new PriceHistory(null, p1, 17800000L, LocalDateTime.now().minusDays(5)));

        Product p2 = productRepository.save(new Product(null, "Samsung Galaxy S24 Ultra 512GB Titanium Gray", "Samsung Galaxy S24 Ultra 512GB Серый Титан", "Samsung Galaxy S24 Ultra 512GB Titanium Gray", "Samsung", "Galaxy S24 Ultra", "512GB", "12GB", "Titanium Gray", "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?auto=format&fit=crop&w=600&q=80", catSmartphones));
        offerRepository.save(new ProductOffer(null, p2, olcha, 15400000L, 16200000L, true, "https://olcha.uz/product/s24-ultra"));
        offerRepository.save(new ProductOffer(null, p2, asaxiy, 15900000L, 16500000L, true, "https://asaxiy.uz/product/s24-ultra"));
        priceHistoryRepository.save(new PriceHistory(null, p2, 15400000L, LocalDateTime.now().minusDays(3)));
    }

    private void ensureCategory(String uz, String ru, String en, String icon) {
        boolean exists = categoryRepository.findAll().stream().anyMatch(c -> c.getNameUz().equalsIgnoreCase(uz));
        if (!exists) {
            categoryRepository.save(new Category(null, uz, ru, en, icon));
        }
    }
}
