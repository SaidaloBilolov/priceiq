package com.priceiq.service;

import com.priceiq.dto.PriceHistoryDto;
import com.priceiq.dto.ProductDto;
import com.priceiq.entity.*;
import com.priceiq.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductOfferRepository offerRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final FavoriteRepository favoriteRepository;
    private final PriceAlertRepository priceAlertRepository;

    public ProductService(ProductRepository productRepository,
                          ProductOfferRepository offerRepository,
                          PriceHistoryRepository priceHistoryRepository,
                          CategoryRepository categoryRepository,
                          StoreRepository storeRepository,
                          FavoriteRepository favoriteRepository,
                          PriceAlertRepository priceAlertRepository) {
        this.productRepository = productRepository;
        this.offerRepository = offerRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.categoryRepository = categoryRepository;
        this.storeRepository = storeRepository;
        this.favoriteRepository = favoriteRepository;
        this.priceAlertRepository = priceAlertRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ProductDto> getProductById(Long id) {
        return productRepository.findById(id).map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> searchProducts(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllProducts();
        }
        return productRepository.searchProducts(query.trim()).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductDto createProduct(ProductDto dto) {
        Product product = new Product();
        product.setTitleUz(dto.getTitleUz() != null && !dto.getTitleUz().trim().isEmpty() ? dto.getTitleUz().trim() : "Mahsulot");
        product.setTitleRu(dto.getTitleRu() != null && !dto.getTitleRu().trim().isEmpty() ? dto.getTitleRu().trim() : product.getTitleUz());
        product.setTitleEn(dto.getTitleEn() != null && !dto.getTitleEn().trim().isEmpty() ? dto.getTitleEn().trim() : product.getTitleUz());
        product.setDescriptionUz(dto.getDescriptionUz() != null ? dto.getDescriptionUz().trim() : "");
        product.setDescriptionRu(dto.getDescriptionRu() != null ? dto.getDescriptionRu().trim() : product.getDescriptionUz());
        product.setDescriptionEn(dto.getDescriptionEn() != null ? dto.getDescriptionEn().trim() : product.getDescriptionUz());
        product.setBrand(dto.getBrand() != null && !dto.getBrand().trim().isEmpty() ? dto.getBrand().trim() : "General");
        product.setModel(dto.getModel() != null ? dto.getModel().trim() : "");
        product.setStorage(dto.getStorage() != null ? dto.getStorage().trim() : "");
        product.setRam(dto.getRam() != null ? dto.getRam().trim() : "");
        product.setColor(dto.getColor() != null ? dto.getColor().trim() : "");
        product.setImageUrl(dto.getImageUrl() != null && !dto.getImageUrl().trim().isEmpty() 
                ? dto.getImageUrl().trim() 
                : "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80");

        // Flexible Category Handling
        Category category = null;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId()).orElse(null);
        }
        if (category == null && dto.getCategory() != null && dto.getCategory().getNameUz() != null && !dto.getCategory().getNameUz().trim().isEmpty()) {
            String catName = dto.getCategory().getNameUz().trim();
            category = categoryRepository.findAll().stream()
                    .filter(c -> c.getNameUz().equalsIgnoreCase(catName))
                    .findFirst()
                    .orElseGet(() -> categoryRepository.save(new Category(null, catName, catName, catName, "General")));
        }
        if (category == null) {
            category = categoryRepository.findAll().stream().findFirst()
                    .orElseGet(() -> categoryRepository.save(new Category(null, "Boshqa", "Другое", "Other", "General")));
        }
        product.setCategory(category);

        Product savedProduct = productRepository.save(product);

        // Store Offer handling
        String storeName = (dto.getStoreName() != null && !dto.getStoreName().trim().isEmpty()) ? dto.getStoreName().trim() : "Uzum Market";
        Long priceUzs = (dto.getPriceUzs() != null && dto.getPriceUzs() > 0) ? dto.getPriceUzs() : 100000L;
        String offerUrl = (dto.getStoreOfferUrl() != null && !dto.getStoreOfferUrl().trim().isEmpty()) ? dto.getStoreOfferUrl().trim() : "https://uzum.uz";

        Store store = storeRepository.findAll().stream()
                .filter(s -> s.getName().equalsIgnoreCase(storeName))
                .findFirst()
                .orElseGet(() -> storeRepository.save(new Store(null, storeName,
                        "https://images.unsplash.com/photo-1512499617640-c74ae3a79d37?auto=format&fit=crop&w=120&q=80",
                        offerUrl, 4.8)));

        ProductOffer offer = new ProductOffer(null, savedProduct, store, priceUzs, Math.round(priceUzs * 1.05), true, offerUrl);
        offerRepository.save(offer);

        // Initial Price History
        priceHistoryRepository.save(new PriceHistory(null, savedProduct, priceUzs, LocalDateTime.now()));

        return mapToDto(savedProduct);
    }

    @Transactional
    public ProductDto updateProduct(Long id, ProductDto dto) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        if (dto.getTitleUz() != null) product.setTitleUz(dto.getTitleUz());
        if (dto.getTitleRu() != null) product.setTitleRu(dto.getTitleRu());
        if (dto.getTitleEn() != null) product.setTitleEn(dto.getTitleEn());
        if (dto.getDescriptionUz() != null) product.setDescriptionUz(dto.getDescriptionUz());
        if (dto.getDescriptionRu() != null) product.setDescriptionRu(dto.getDescriptionRu());
        if (dto.getDescriptionEn() != null) product.setDescriptionEn(dto.getDescriptionEn());
        if (dto.getBrand() != null) product.setBrand(dto.getBrand());
        if (dto.getModel() != null) product.setModel(dto.getModel());
        if (dto.getStorage() != null) product.setStorage(dto.getStorage());
        if (dto.getRam() != null) product.setRam(dto.getRam());
        if (dto.getColor() != null) product.setColor(dto.getColor());
        if (dto.getImageUrl() != null && !dto.getImageUrl().isEmpty()) product.setImageUrl(dto.getImageUrl());

        if (dto.getCategoryId() != null) {
            categoryRepository.findById(dto.getCategoryId()).ifPresent(product::setCategory);
        }

        Product saved = productRepository.save(product);

        // Update main store offer if price/store is updated
        List<ProductOffer> existingOffers = offerRepository.findByProductIdOrderByPriceUzsAsc(saved.getId());
        if (!existingOffers.isEmpty() && dto.getPriceUzs() != null && dto.getPriceUzs() > 0) {
            ProductOffer offer = existingOffers.get(0);
            offer.setPriceUzs(dto.getPriceUzs());
            if (dto.getStoreOfferUrl() != null) offer.setOfferUrl(dto.getStoreOfferUrl());
            if (dto.getStoreName() != null) {
                Store store = storeRepository.findAll().stream()
                        .filter(s -> s.getName().equalsIgnoreCase(dto.getStoreName()))
                        .findFirst()
                        .orElseGet(() -> storeRepository.save(new Store(null, dto.getStoreName(),
                                "https://images.unsplash.com/photo-1512499617640-c74ae3a79d37?auto=format&fit=crop&w=120&q=80",
                                dto.getStoreOfferUrl(), 4.8)));
                offer.setStore(store);
            }
            offerRepository.save(offer);
            priceHistoryRepository.save(new PriceHistory(null, saved, dto.getPriceUzs(), LocalDateTime.now()));
        }

        return mapToDto(saved);
    }

    @Transactional
    public void deleteProduct(Long id) {
        try {
            favoriteRepository.deleteByProductId(id);
            priceAlertRepository.deleteByProductId(id);
            priceHistoryRepository.deleteByProductId(id);
            offerRepository.deleteByProductId(id);
            productRepository.deleteById(id);
        } catch (Exception e) {
            productRepository.deleteById(id);
        }
    }

    public ProductDto mapToDto(Product product) {
        List<ProductOffer> sortedOffers = offerRepository.findByProductIdOrderByPriceUzsAsc(product.getId());
        List<PriceHistory> history = priceHistoryRepository.findByProductIdOrderByRecordedAtAsc(product.getId());

        Long lowest = sortedOffers.isEmpty() ? 0L : sortedOffers.get(0).getPriceUzs();
        Long highest = sortedOffers.isEmpty() ? 0L : sortedOffers.get(sortedOffers.size() - 1).getPriceUzs();
        double avg = sortedOffers.isEmpty() ? 0 : sortedOffers.stream().mapToLong(ProductOffer::getPriceUzs).average().orElse(0);

        // Deal Score Calculation (0 - 100)
        int score = 85;
        if (avg > 0 && lowest > 0) {
            double ratio = (double) lowest / avg;
            if (ratio <= 0.90) {
                score = 96;
            } else if (ratio <= 0.95) {
                score = 91;
            } else if (ratio <= 1.02) {
                score = 84;
            } else {
                score = 72;
            }
        }

        String badgeUz = score + "/100 - " + (score >= 90 ? "JUDA YAXSHI TAKLIF" : (score >= 80 ? "BOZOR NARXI" : "QIMMATROQ"));
        String badgeRu = score + "/100 - " + (score >= 90 ? "ВЫГОДНОЕ ПРЕДЛОЖЕНИЕ" : (score >= 80 ? "ХОРОШАЯ ЦЕНА" : "ДОРОГО"));
        String badgeEn = score + "/100 - " + (score >= 90 ? "GREAT DEAL" : (score >= 80 ? "FAIR PRICE" : "EXPENSIVE"));

        List<PriceHistoryDto> historyDtos = history.stream()
                .map(h -> new PriceHistoryDto(h.getId(), h.getPriceUzs(), h.getRecordedAt()))
                .collect(Collectors.toList());

        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setTitleUz(product.getTitleUz());
        dto.setTitleRu(product.getTitleRu());
        dto.setTitleEn(product.getTitleEn());
        dto.setDescriptionUz(product.getDescriptionUz());
        dto.setDescriptionRu(product.getDescriptionRu());
        dto.setDescriptionEn(product.getDescriptionEn());
        dto.setBrand(product.getBrand());
        dto.setModel(product.getModel());
        dto.setStorage(product.getStorage());
        dto.setRam(product.getRam());
        dto.setColor(product.getColor());
        dto.setImageUrl(product.getImageUrl());
        dto.setCategory(product.getCategory());
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
        }
        dto.setLowestPriceUzs(lowest);
        dto.setAveragePriceUzs(Math.round(avg));
        dto.setHighestPriceUzs(highest);
        dto.setDealScore(score);
        dto.setDealBadgeUz(badgeUz);
        dto.setDealBadgeRu(badgeRu);
        dto.setDealBadgeEn(badgeEn);
        dto.setOffers(sortedOffers);
        dto.setPriceHistory(historyDtos);

        if (!sortedOffers.isEmpty()) {
            dto.setStoreName(sortedOffers.get(0).getStore() != null ? sortedOffers.get(0).getStore().getName() : "Store");
            dto.setStoreOfferUrl(sortedOffers.get(0).getOfferUrl());
            dto.setPriceUzs(sortedOffers.get(0).getPriceUzs());
        }

        return dto;
    }
}
