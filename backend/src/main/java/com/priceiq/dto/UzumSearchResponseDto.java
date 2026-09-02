package com.priceiq.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UzumSearchResponseDto {

    private Payload payload;

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Payload {
        private List<ProductItem> itemList;
        private List<ProductItem> products;

        public List<ProductItem> getItemList() {
            return itemList != null ? itemList : products;
        }

        public void setItemList(List<ProductItem> itemList) {
            this.itemList = itemList;
        }

        public List<ProductItem> getProducts() {
            return products;
        }

        public void setProducts(List<ProductItem> products) {
            this.products = products;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductItem {
        private Long productId;
        private String title;
        private Long minSellPrice;
        private Long minFullPrice;
        private Long price;
        private Long fullPrice;
        private Double rating;
        private String imageKey;
        private String photoKey;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Long getPrice() {
            if (minSellPrice != null && minSellPrice > 0) return minSellPrice;
            if (price != null && price > 0) return price;
            return 0L;
        }

        public Long getFullPrice() {
            if (minFullPrice != null && minFullPrice > 0) return minFullPrice;
            if (fullPrice != null && fullPrice > 0) return fullPrice;
            return getPrice();
        }

        public Double getRating() {
            return rating != null ? rating : 4.8;
        }

        public void setRating(Double rating) {
            this.rating = rating;
        }

        public String getImageKey() {
            return imageKey != null ? imageKey : photoKey;
        }

        public void setImageKey(String imageKey) {
            this.imageKey = imageKey;
        }

        public void setPhotoKey(String photoKey) {
            this.photoKey = photoKey;
        }

        public UzumProductDto toUzumProductDto() {
            String key = getImageKey();
            String imgUrl = (key != null && !key.isEmpty()) ? "https://images.uzum.uz/" + key + "/original.jpg" : "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80";
            return new UzumProductDto(productId, title, getPrice(), getFullPrice(), getRating(), imgUrl);
        }
    }
}
