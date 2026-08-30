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
        ensureCategory("Smartfonlar va Gadjetlar", "Смартфоны и гаджеты", "Smartphones & Gadgets", "Smartphone");
        ensureCategory("Noutbuklar va Kompyuterlar", "Ноутбуки и ПК", "Laptops & Computers", "Laptop");
        ensureCategory("Televizor va Audio", "ТВ и Аудио", "TV & Audio", "Tv");
        ensureCategory("Maishiy Texnika", "Бытовая техника", "Appliances", "Appliance");
        ensureCategory("Kiyim va Poyabzal", "Одежда и обувь", "Clothing & Shoes", "Clothing");
        ensureCategory("Go'zallik va Parvarish", "Красота и уход", "Beauty & Care", "Heart");
        ensureCategory("Uy va Oshxona", "Дом и кухня", "Home & Kitchen", "Home");
        ensureCategory("Avtotovarlar", "Автотовары", "Auto goods", "Car");
        ensureCategory("Sport va Salomatlik", "Спорт и здоровье", "Sport & Health", "Activity");
        ensureCategory("Bolalar Mahsulotlari", "Детские товары", "Kids & Baby", "Smile");
        ensureCategory("Kitoblar va Kanselyariya", "Книги и канцелярия", "Books & Stationery", "Book");
        ensureCategory("Boshqa Mahsulotlar", "Другие товары", "Other Products", "Box");

        // Ensure default stores with owner phone numbers
        ensureStore("TechStore Pro (Rasmiy Sotuvchi)", "https://images.unsplash.com/photo-1526738549149-8e07eca6c147?auto=format&fit=crop&w=120&q=80", "https://techstore.uz", 4.9, "+998956233923");
        ensureStore("Uzum Nasiya / Market", "https://images.unsplash.com/photo-1512499617640-c74ae3a79d37?auto=format&fit=crop&w=120&q=80", "https://uzum.uz", 4.8, "+998901234567");
        ensureStore("Olcha.uz", "https://images.unsplash.com/photo-1526738549149-8e07eca6c147?auto=format&fit=crop&w=120&q=80", "https://olcha.uz", 4.8, "+998935000000");
        ensureStore("Texnomart", "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=120&q=80", "https://texnomart.uz", 4.7, "+998971234567");
        ensureStore("Asaxiy", "https://images.unsplash.com/photo-1550009158-9ebf69173e03?auto=format&fit=crop&w=120&q=80", "https://asaxiy.uz", 4.9, "+998991234567");
    }

    private void ensureCategory(String uz, String ru, String en, String icon) {
        boolean exists = categoryRepository.findAll().stream().anyMatch(c -> c.getNameUz().equalsIgnoreCase(uz));
        if (!exists) {
            categoryRepository.save(new Category(null, uz, ru, en, icon));
        }
    }

    private void ensureStore(String name, String logo, String url, Double rating, String ownerPhone) {
        Store store = storeRepository.findByNameIgnoreCase(name).orElse(null);
        if (store == null) {
            storeRepository.save(new Store(null, name, logo, url, rating, ownerPhone, null));
        } else if (store.getOwnerPhone() == null || store.getOwnerPhone().isEmpty()) {
            store.setOwnerPhone(ownerPhone);
            storeRepository.save(store);
        }
    }
}
