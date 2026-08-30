package com.priceiq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdatePhoneRequest {

    @NotNull(message = "Telegram ID is required")
    private Long telegramId;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    private String languageCode;

    public UpdatePhoneRequest() {}

    public UpdatePhoneRequest(Long telegramId, String phoneNumber, String languageCode) {
        this.telegramId = telegramId;
        this.phoneNumber = phoneNumber;
        this.languageCode = languageCode;
    }

    public Long getTelegramId() { return telegramId; }
    public void setTelegramId(Long telegramId) { this.telegramId = telegramId; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getLanguageCode() { return languageCode; }
    public void setLanguageCode(String languageCode) { this.languageCode = languageCode; }
}
