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

    public DataInitializer(CategoryRepository categoryRepository,
                           StoreRepository storeRepository,
                           ProductRepository productRepository,
                           ProductOfferRepository offerRepository,
                           PriceHistoryRepository priceHistoryRepository) {
        this.categoryRepository = categoryRepository;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
        this.offerRepository = offerRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. Ensure universal categories exist
        Category smartphonesCat = ensureCategory("Smartfonlar va Gadjetlar", "Смартфоны и гаджеты", "Smartphones & Gadgets", "Smartphone");
        Category laptopsCat = ensureCategory("Noutbuklar va Kompyuterlar", "Ноутбуки и ПК", "Laptops & Computers", "Laptop");
        Category tvCat = ensureCategory("Televizor va Audio", "ТВ и Аудио", "TV & Audio", "Tv");
        ensureCategory("Maishiy Texnika", "Бытовая техника", "Appliances", "Appliance");
        ensureCategory("Kiyim va Poyabzal", "Одежда и обувь", "Clothing & Shoes", "Clothing");
        ensureCategory("Go'zallik va Parvarish", "Красота и уход", "Beauty & Care", "Heart");
        ensureCategory("Uy va Oshxona", "Дом и кухня", "Home & Kitchen", "Home");
        ensureCategory("Avtotovarlar", "Автотовары", "Auto goods", "Car");
        ensureCategory("Sport va Salomatlik", "Спорт и здоровье", "Sport & Health", "Activity");
        ensureCategory("Bolalar Mahsulotlari", "Детские товары", "Kids & Baby", "Smile");
        ensureCategory("Kitoblar va Kanselyariya", "Книги и канцелярия", "Books & Stationery", "Book");
        ensureCategory("Boshqa Mahsulotlar", "Другие товары", "Other Products", "Box");

        // 2. Ensure default stores with owner phone numbers
        Store techStore = ensureStore("TechStore Pro (Rasmiy Sotuvchi)", "https://images.unsplash.com/photo-1526738549149-8e07eca6c147?auto=format&fit=crop&w=120&q=80", "https://techstore.uz", 4.9, "+998956233923");
        Store uzumStore = ensureStore("Uzum Market", "https://images.unsplash.com/photo-1512499617640-c74ae3a79d37?auto=format&fit=crop&w=120&q=80", "https://uzum.uz", 4.8, "+998901234567");
        Store olchaStore = ensureStore("Olcha.uz", "https://images.unsplash.com/photo-1526738549149-8e07eca6c147?auto=format&fit=crop&w=120&q=80", "https://olcha.uz", 4.8, "+998935000000");
        Store texnomartStore = ensureStore("Texnomart", "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=120&q=80", "https://texnomart.uz", 4.7, "+998971234567");
        Store asaxiyStore = ensureStore("Asaxiy", "https://images.unsplash.com/photo-1550009158-9ebf69173e03?auto=format&fit=crop&w=120&q=80", "https://asaxiy.uz", 4.9, "+998991234567");

        // 3. Ensure products in PostgreSQL database if empty
        if (productRepository.count() == 0) {
            // Product 1: iPhone 16 Pro Max
            Product iphone = new Product(
                    null,
                    "Apple iPhone 16 Pro Max 256GB Tabiiy Titan",
                    "Apple iPhone 16 Pro Max 256GB Натуральный Титан",
                    "Apple iPhone 16 Pro Max 256GB Natural Titanium",
                    "Apple",
                    "iPhone 16 Pro Max",
                    "256GB",
                    "8GB",
                    "Tabiiy Titan",
                    "https://images.unsplash.com/photo-1695048133142-1a20484d2569?auto=format&fit=crop&w=600&q=80",
                    smartphonesCat
            );
            iphone.setDescriptionUz("A18 Pro protsessor, 48MP asosiy kamera va titan korpusli eng so'nggi flagman smartfon");
            iphone.setDescriptionRu("Флагманский процессор A18 Pro, камера 48МП и титановый корпус");
            iphone = productRepository.save(iphone);

            offerRepository.save(new ProductOffer(null, iphone, uzumStore, 17800000L, 18900000L, true, "https://uzum.uz"));
            offerRepository.save(new ProductOffer(null, iphone, texnomartStore, 18200000L, 19000000L, true, "https://texnomart.uz"));
            offerRepository.save(new ProductOffer(null, iphone, olchaStore, 18500000L, 19200000L, true, "https://olcha.uz"));
            priceHistoryRepository.save(new PriceHistory(null, iphone, 17800000L, LocalDateTime.now()));

            // Product 2: Samsung Galaxy S24 Ultra
            Product samsung = new Product(
                    null,
                    "Samsung Galaxy S24 Ultra 512GB Titanium Gray",
                    "Samsung Galaxy S24 Ultra 512GB Titanium Gray",
                    "Samsung Galaxy S24 Ultra 512GB Titanium Gray",
                    "Samsung",
                    "Galaxy S24 Ultra",
                    "512GB",
                    "12GB",
                    "Titanium Gray",
                    "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?auto=format&fit=crop&w=600&q=80",
                    smartphonesCat
            );
            samsung.setDescriptionUz("Galaxy AI sun'iy intellekt, 200MP kamera va o'rnatilgan S-Pen stilus");
            samsung.setDescriptionRu("Искусственный интеллект Galaxy AI, 200МП камера и встроенный S-Pen");
            samsung = productRepository.save(samsung);

            offerRepository.save(new ProductOffer(null, samsung, olchaStore, 15400000L, 16500000L, true, "https://olcha.uz"));
            offerRepository.save(new ProductOffer(null, samsung, uzumStore, 15900000L, 16800000L, true, "https://uzum.uz"));
            priceHistoryRepository.save(new PriceHistory(null, samsung, 15400000L, LocalDateTime.now()));

            // Product 3: MacBook Air M3
            Product macbook = new Product(
                    null,
                    "MacBook Air 13 M3 16GB 512GB Midnight",
                    "MacBook Air 13 M3 16GB 512GB Midnight",
                    "MacBook Air 13 M3 16GB 512GB Midnight",
                    "Apple",
                    "MacBook Air 13 M3",
                    "512GB",
                    "16GB",
                    "Midnight",
                    "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=600&q=80",
                    laptopsCat
            );
            macbook.setDescriptionUz("Apple M3 chip, 18 soat batareya quvvati va Liquid Retina ekrani");
            macbook = productRepository.save(macbook);

            offerRepository.save(new ProductOffer(null, macbook, techStore, 14800000L, 15500000L, true, "https://techstore.uz"));
            offerRepository.save(new ProductOffer(null, macbook, asaxiyStore, 15200000L, 16000000L, true, "https://asaxiy.uz"));
            priceHistoryRepository.save(new PriceHistory(null, macbook, 14800000L, LocalDateTime.now()));
        }
    }

    private Category ensureCategory(String uz, String ru, String en, String icon) {
        return categoryRepository.findAll().stream()
                .filter(c -> c.getNameUz().equalsIgnoreCase(uz))
                .findFirst()
                .orElseGet(() -> categoryRepository.save(new Category(null, uz, ru, en, icon)));
    }

    private Store ensureStore(String name, String logo, String url, Double rating, String ownerPhone) {
        Store store = storeRepository.findByNameIgnoreCase(name).orElse(null);
        if (store == null) {
            return storeRepository.save(new Store(null, name, logo, url, rating, ownerPhone, null));
        } else {
            if (store.getOwnerPhone() == null || store.getOwnerPhone().isEmpty()) {
                store.setOwnerPhone(ownerPhone);
                return storeRepository.save(store);
            }
            return store;
        }
    }
}
