package com.priceiq.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UzumSearchResponseDto {

    private Payload payload;
    private Payload data;

    public Payload getPayload() {
        return payload != null ? payload : data;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    public Payload getData() {
        return data;
    }

    public void setData(Payload data) {
        this.data = data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Payload {
        private List<ProductItem> itemList;
        private List<ProductItem> products;
        private List<ProductItem> items;
        private List<ProductItem> data;

        public List<ProductItem> getItemList() {
            if (products != null && !products.isEmpty()) return products;
            if (itemList != null && !itemList.isEmpty()) return itemList;
            if (items != null && !items.isEmpty()) return items;
            if (data != null && !data.isEmpty()) return data;
            return products;
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

        public List<ProductItem> getItems() {
            return items;
        }

        public void setItems(List<ProductItem> items) {
            this.items = items;
        }

        public List<ProductItem> getData() {
            return data;
        }

        public void setData(List<ProductItem> data) {
            this.data = data;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductItem {
        private Long productId;
        private Long id;
        private String title;
        private String name;
        private Long sellPrice;
        private Long minSellPrice;
        private Long price;
        private Long fullPrice;
        private Long minFullPrice;
        private Double rating;
        private String imageKey;
        private String photoKey;
        private List<Photo> photos;

        public Long getProductId() {
            if (productId != null) return productId;
            return id;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getTitle() {
            if (title != null && !title.isEmpty()) return title;
            return name;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Long getPrice() {
            if (sellPrice != null && sellPrice > 0) return sellPrice;
            if (minSellPrice != null && minSellPrice > 0) return minSellPrice;
            if (price != null && price > 0) return price;
            return 0L;
        }

        public void setSellPrice(Long sellPrice) {
            this.sellPrice = sellPrice;
        }

        public Long getFullPrice() {
            if (fullPrice != null && fullPrice > 0) return fullPrice;
            if (minFullPrice != null && minFullPrice > 0) return minFullPrice;
            return getPrice();
        }

        public void setFullPrice(Long fullPrice) {
            this.fullPrice = fullPrice;
        }

        public Double getRating() {
            return rating != null ? rating : 4.8;
        }

        public void setRating(Double rating) {
            this.rating = rating;
        }

        public String getImageKey() {
            if (photoKey != null && !photoKey.isEmpty()) return photoKey;
            if (imageKey != null && !imageKey.isEmpty()) return imageKey;
            if (photos != null && !photos.isEmpty()) {
                String pKey = photos.get(0).getPhotoKey();
                if (pKey != null && !pKey.isEmpty()) return pKey;
            }
            return null;
        }

        public void setImageKey(String imageKey) {
            this.imageKey = imageKey;
        }

        public void setPhotoKey(String photoKey) {
            this.photoKey = photoKey;
        }

        public List<Photo> getPhotos() {
            return photos;
        }

        public void setPhotos(List<Photo> photos) {
            this.photos = photos;
        }

        public UzumProductDto toUzumProductDto() {
            String key = getImageKey();
            String imgUrl = (key != null && !key.isEmpty())
                    ? "https://images.uzum.uz/" + key + "/original.jpg"
                    : "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=600&q=80";
            return new UzumProductDto(getProductId(), getTitle(), getPrice(), getFullPrice(), getRating(), imgUrl);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Photo {
        private String photoKey;
        private String key;

        public String getPhotoKey() {
            return photoKey != null ? photoKey : key;
        }

        public void setPhotoKey(String photoKey) {
            this.photoKey = photoKey;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }
}
