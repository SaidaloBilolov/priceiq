package com.priceiq.dto;

import java.time.LocalDateTime;

public class PriceHistoryDto {
    private Long id;
    private Long priceUzs;
    private LocalDateTime recordedAt;

    public PriceHistoryDto() {}

    public PriceHistoryDto(Long id, Long priceUzs, LocalDateTime recordedAt) {
        this.id = id;
        this.priceUzs = priceUzs;
        this.recordedAt = recordedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPriceUzs() { return priceUzs; }
    public void setPriceUzs(Long priceUzs) { this.priceUzs = priceUzs; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
