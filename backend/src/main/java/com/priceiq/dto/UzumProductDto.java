package com.priceiq.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UzumProductDto {

    private Long productId;
    private String title;
    private Long price;
    private Long fullPrice;
    private Double rating;
    private String mainImage;
    private String productUrl;

    public UzumProductDto() {}

    public UzumProductDto(Long productId, String title, Long price, Long fullPrice, Double rating, String mainImage) {
        this.productId = productId;
        this.title = title;
        this.price = price;
        this.fullPrice = fullPrice;
        this.rating = rating;
        this.mainImage = mainImage;
        if (productId != null) {
            this.productUrl = "https://uzum.uz/product/" + productId;
        }
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
        if (productId != null && (this.productUrl == null || this.productUrl.isEmpty())) {
            this.productUrl = "https://uzum.uz/product/" + productId;
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getPrice() {
        return price;
    }

    public void setPrice(Long price) {
        this.price = price;
    }

    public Long getFullPrice() {
        return fullPrice;
    }

    public void setFullPrice(Long fullPrice) {
        this.fullPrice = fullPrice;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public String getMainImage() {
        return mainImage;
    }

    public void setMainImage(String mainImage) {
        if (mainImage != null && !mainImage.startsWith("http") && !mainImage.startsWith("/")) {
            this.mainImage = "https://images.uzum.uz/" + mainImage + "/original.jpg";
        } else {
            this.mainImage = mainImage;
        }
    }

    public String getProductUrl() {
        if (productUrl == null && productId != null) {
            return "https://uzum.uz/product/" + productId;
        }
        return productUrl;
    }

    public void setProductUrl(String productUrl) {
        this.productUrl = productUrl;
    }
}
