package com.priceiq.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_history")
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "price_uzs", nullable = false)
    private Long priceUzs;

    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    public PriceHistory() {}

    public PriceHistory(Long id, Product product, Long priceUzs, LocalDateTime recordedAt) {
        this.id = id;
        this.product = product;
        this.priceUzs = priceUzs;
        this.recordedAt = recordedAt;
    }

    @PrePersist
    protected void onCreate() {
        if (this.recordedAt == null) {
            this.recordedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Long getPriceUzs() { return priceUzs; }
    public void setPriceUzs(Long priceUzs) { this.priceUzs = priceUzs; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
