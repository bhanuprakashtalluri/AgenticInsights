package org.example.dto;

import java.time.Instant;

public class RecognitionChartDTO {
    private Instant sentAt;
    private Integer awardPoints;
    private Long recipientId;
    private Long senderId;
    private String approvalStatus;
    private String level;

    public RecognitionChartDTO(Instant sentAt, Integer awardPoints, Long recipientId, Long senderId, String approvalStatus, String level) {
        this.sentAt = sentAt;
        this.awardPoints = awardPoints;
        this.recipientId = recipientId;
        this.senderId = senderId;
        this.approvalStatus = approvalStatus;
        this.level = level;
    }

    public Instant getSentAt() { return sentAt; }
    public Integer getAwardPoints() { return awardPoints; }
    public Long getRecipientId() { return recipientId; }
    public Long getSenderId() { return senderId; }
    public String getApprovalStatus() { return approvalStatus; }
    public String getLevel() { return level; }
}

