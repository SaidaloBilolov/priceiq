package com.priceiq.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title_uz", nullable = false)
    private String titleUz;

    @Column(name = "title_ru")
    private String titleRu;

    @Column(name = "title_en")
    private String titleEn;

    @Column(name = "description_uz", length = 2000)
    private String descriptionUz;

    @Column(name = "description_ru", length = 2000)
    private String descriptionRu;

    @Column(name = "description_en", length = 2000)
    private String descriptionEn;

    @Column(name = "brand")
    private String brand;

    @Column(name = "model")
    private String model;

    @Column(name = "storage")
    private String storage;

    @Column(name = "ram")
    private String ram;

    @Column(name = "color")
    private String color;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductOffer> offers = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Product() {}

    public Product(Long id, String titleUz, String titleRu, String titleEn, String brand, String model, String storage, String ram, String color, String imageUrl, Category category) {
        this.id = id;
        this.titleUz = titleUz;
        this.titleRu = titleRu;
        this.titleEn = titleEn;
        this.brand = brand;
        this.model = model;
        this.storage = storage;
        this.ram = ram;
        this.color = color;
        this.imageUrl = imageUrl;
        this.category = category;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

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
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public List<ProductOffer> getOffers() { return offers; }
    public void setOffers(List<ProductOffer> offers) { this.offers = offers; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
