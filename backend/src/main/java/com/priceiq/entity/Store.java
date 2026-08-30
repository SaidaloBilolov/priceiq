package com.priceiq.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "stores")
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "logo_url", length = 1000)
    private String logoUrl;

    @Column(name = "website_url", length = 1000)
    private String websiteUrl;

    @Column(name = "rating")
    private Double rating;

    @Column(name = "owner_phone")
    private String ownerPhone;

    @Column(name = "owner_chat_id")
    private Long ownerChatId;

    public Store() {}

    public Store(Long id, String name, String logoUrl, String websiteUrl, Double rating) {
        this.id = id;
        this.name = name;
        this.logoUrl = logoUrl;
        this.websiteUrl = websiteUrl;
        this.rating = rating;
    }

    public Store(Long id, String name, String logoUrl, String websiteUrl, Double rating, String ownerPhone, Long ownerChatId) {
        this.id = id;
        this.name = name;
        this.logoUrl = logoUrl;
        this.websiteUrl = websiteUrl;
        this.rating = rating;
        this.ownerPhone = ownerPhone;
        this.ownerChatId = ownerChatId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public String getOwnerPhone() { return ownerPhone; }
    public void setOwnerPhone(String ownerPhone) { this.ownerPhone = ownerPhone; }
    public Long getOwnerChatId() { return ownerChatId; }
    public void setOwnerChatId(Long ownerChatId) { this.ownerChatId = ownerChatId; }
}
