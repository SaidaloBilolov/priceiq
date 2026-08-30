package com.priceiq.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "support_tickets")
public class SupportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_chat_id", nullable = false)
    private Long userChatId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "user_phone")
    private String userPhone;

    @Column(name = "user_role")
    private String userRole;

    @Column(name = "message_text", columnDefinition = "TEXT")
    private String messageText;

    @Column(name = "media_type")
    private String mediaType;

    @Column(name = "media_file_id")
    private String mediaFileId;

    @Column(name = "operator_name")
    private String operatorName;

    @Column(name = "operator_chat_id")
    private Long operatorChatId;

    @Column(name = "reply_text", columnDefinition = "TEXT")
    private String replyText;

    @Column(name = "status", nullable = false)
    private String status = "PENDING"; // PENDING, ANSWERED

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    public SupportTicket() {}

    public SupportTicket(Long userChatId, String userName, String userPhone, String userRole, String messageText, String mediaType) {
        this.userChatId = userChatId;
        this.userName = userName;
        this.userPhone = userPhone;
        this.userRole = userRole;
        this.messageText = messageText;
        this.mediaType = mediaType;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = "PENDING";
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserChatId() { return userChatId; }
    public void setUserChatId(Long userChatId) { this.userChatId = userChatId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }

    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    public String getMediaFileId() { return mediaFileId; }
    public void setMediaFileId(String mediaFileId) { this.mediaFileId = mediaFileId; }

    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }

    public Long getOperatorChatId() { return operatorChatId; }
    public void setOperatorChatId(Long operatorChatId) { this.operatorChatId = operatorChatId; }

    public String getReplyText() { return replyText; }
    public void setReplyText(String replyText) { this.replyText = replyText; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getRepliedAt() { return repliedAt; }
    public void setRepliedAt(LocalDateTime repliedAt) { this.repliedAt = repliedAt; }
}
