package com.priceiq.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_offers")
public class ProductOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;

    @ManyToOne
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "price_uzs", nullable = false)
    private Long priceUzs;

    @Column(name = "old_price_uzs")
    private Long oldPriceUzs;

    @Column(name = "is_available")
    private Boolean isAvailable;

    @Column(name = "offer_url", length = 1000)
    private String offerUrl;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ProductOffer() {}

    public ProductOffer(Long id, Product product, Store store, Long priceUzs, Long oldPriceUzs, Boolean isAvailable, String offerUrl) {
        this.id = id;
        this.product = product;
        this.store = store;
        this.priceUzs = priceUzs;
        this.oldPriceUzs = oldPriceUzs;
        this.isAvailable = isAvailable;
        this.offerUrl = offerUrl;
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }
    public Long getPriceUzs() { return priceUzs; }
    public void setPriceUzs(Long priceUzs) { this.priceUzs = priceUzs; }
    public Long getOldPriceUzs() { return oldPriceUzs; }
    public void setOldPriceUzs(Long oldPriceUzs) { this.oldPriceUzs = oldPriceUzs; }
    public Boolean getIsAvailable() { return isAvailable; }
    public void setIsAvailable(Boolean isAvailable) { this.isAvailable = isAvailable; }
    public String getOfferUrl() { return offerUrl; }
    public void setOfferUrl(String offerUrl) { this.offerUrl = offerUrl; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
