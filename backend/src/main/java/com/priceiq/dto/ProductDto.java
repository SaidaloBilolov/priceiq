package com.priceiq.dto;

import com.priceiq.entity.Category;
import com.priceiq.entity.ProductOffer;
import java.util.List;

public class ProductDto {
    private Long id;
    private String titleUz;
    private String titleRu;
    private String titleEn;
    private String descriptionUz;
    private String descriptionRu;
    private String descriptionEn;
    private String brand;
    private String model;
    private String storage;
    private String ram;
    private String color;
    private String imageUrl;
    private Long categoryId;
    private Category category;

    // Helper fields for Admin Product Creation
    private String storeName;
    private String storeOfferUrl;
    private Long priceUzs;

    private Long lowestPriceUzs;
    private Long averagePriceUzs;
    private Long highestPriceUzs;
    
    private Integer dealScore;
    private String dealBadgeUz;
    private String dealBadgeRu;
    private String dealBadgeEn;

    private List<ProductOffer> offers;
    private List<PriceHistoryDto> priceHistory;

    public ProductDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitleUz() { return titleUz; }
    public void setTitleUz(String titleUz) { this.titleUz = titleUz; }
    public String getTitleRu() { return titleRu; }
    public void setTitleRu(String titleRu) { this.titleRu = titleRu; }
    public String getTitleEn() { return titleEn; }
    public void setTitleEn(String titleEn) { this.titleEn = titleEn; }
    public String getDescriptionUz() { return descriptionUz; }
    public void setDescriptionUz(String descriptionUz) { this.descriptionUz = descriptionUz; }
    public String getDescriptionRu() { return descriptionRu; }
    public void setDescriptionRu(String descriptionRu) { this.descriptionRu = descriptionRu; }
    public String getDescriptionEn() { return descriptionEn; }
    public void setDescriptionEn(String descriptionEn) { this.descriptionEn = descriptionEn; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getStorage() { return storage; }
    public void setStorage(String storage) { this.storage = storage; }
    public String getRam() { return ram; }
    public void setRam(String ram) { this.ram = ram; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public String getStoreOfferUrl() { return storeOfferUrl; }
    public void setStoreOfferUrl(String storeOfferUrl) { this.storeOfferUrl = storeOfferUrl; }
    public Long getPriceUzs() { return priceUzs; }
    public void setPriceUzs(Long priceUzs) { this.priceUzs = priceUzs; }
    public Long getLowestPriceUzs() { return lowestPriceUzs; }
    public void setLowestPriceUzs(Long lowestPriceUzs) { this.lowestPriceUzs = lowestPriceUzs; }
    public Long getAveragePriceUzs() { return averagePriceUzs; }
    public void setAveragePriceUzs(Long averagePriceUzs) { this.averagePriceUzs = averagePriceUzs; }
    public Long getHighestPriceUzs() { return highestPriceUzs; }
    public void setHighestPriceUzs(Long highestPriceUzs) { this.highestPriceUzs = highestPriceUzs; }
    public Integer getDealScore() { return dealScore; }
    public void setDealScore(Integer dealScore) { this.dealScore = dealScore; }
    public String getDealBadgeUz() { return dealBadgeUz; }
    public void setDealBadgeUz(String dealBadgeUz) { this.dealBadgeUz = dealBadgeUz; }
    public String getDealBadgeRu() { return dealBadgeRu; }
    public void setDealBadgeRu(String dealBadgeRu) { this.dealBadgeRu = dealBadgeRu; }
    public String getDealBadgeEn() { return dealBadgeEn; }
    public void setDealBadgeEn(String dealBadgeEn) { this.dealBadgeEn = dealBadgeEn; }
    public List<ProductOffer> getOffers() { return offers; }
    public void setOffers(List<ProductOffer> offers) { this.offers = offers; }
    public List<PriceHistoryDto> getPriceHistory() { return priceHistory; }
    public void setPriceHistory(List<PriceHistoryDto> priceHistory) { this.priceHistory = priceHistory; }
}
