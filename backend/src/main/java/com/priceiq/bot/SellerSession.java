package com.priceiq.bot;

import com.priceiq.entity.Store;

public class SellerSession {
    private Long chatId;
    private Long telegramUserId;
    private String phoneNumber;
    private Store store;
    private SellerState state = SellerState.START;

    // Temporary data for Add Product Flow
    private String tempPhotoFileId;
    private String tempPhotoUrl;
    private String tempTitle;
    private Long tempPriceUzs;
    private String tempDescription;

    // Temporary data for Price Update Flow
    private Long tempSelectedProductId;

    // Temporary data for Operator Reply Flow
    private Long tempReplyToChatId;
    private String tempReplyToUserName;

    public SellerSession() {}

    public SellerSession(Long chatId, Long telegramUserId) {
        this.chatId = chatId;
        this.telegramUserId = telegramUserId;
    }

    public void clearTempProductData() {
        this.tempPhotoFileId = null;
        this.tempPhotoUrl = null;
        this.tempTitle = null;
        this.tempPriceUzs = null;
        this.tempDescription = null;
        this.tempSelectedProductId = null;
    }

    public Long getChatId() { return chatId; }
    public void setChatId(Long chatId) { this.chatId = chatId; }

    public Long getTelegramUserId() { return telegramUserId; }
    public void setTelegramUserId(Long telegramUserId) { this.telegramUserId = telegramUserId; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Store getStore() { return store; }
    public void setStore(Store store) { this.store = store; }

    public SellerState getState() { return state; }
    public void setState(SellerState state) { this.state = state; }

    public String getTempPhotoFileId() { return tempPhotoFileId; }
    public void setTempPhotoFileId(String tempPhotoFileId) { this.tempPhotoFileId = tempPhotoFileId; }

    public String getTempPhotoUrl() { return tempPhotoUrl; }
    public void setTempPhotoUrl(String tempPhotoUrl) { this.tempPhotoUrl = tempPhotoUrl; }

    public String getTempTitle() { return tempTitle; }
    public void setTempTitle(String tempTitle) { this.tempTitle = tempTitle; }

    public Long getTempPriceUzs() { return tempPriceUzs; }
    public void setTempPriceUzs(Long tempPriceUzs) { this.tempPriceUzs = tempPriceUzs; }

    public String getTempDescription() { return tempDescription; }
    public void setTempDescription(String tempDescription) { this.tempDescription = tempDescription; }

    public Long getTempSelectedProductId() { return tempSelectedProductId; }
    public void setTempSelectedProductId(Long tempSelectedProductId) { this.tempSelectedProductId = tempSelectedProductId; }

    public Long getTempReplyToChatId() { return tempReplyToChatId; }
    public void setTempReplyToChatId(Long tempReplyToChatId) { this.tempReplyToChatId = tempReplyToChatId; }

    public String getTempReplyToUserName() { return tempReplyToUserName; }
    public void setTempReplyToUserName(String tempReplyToUserName) { this.tempReplyToUserName = tempReplyToUserName; }
}
