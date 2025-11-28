package org.example.dto;

import java.time.Instant;
import java.util.UUID;

public class RecognitionResponse {
    public Long id;
    public UUID uuid;
    public Long recognitionTypeId;
    public String recognitionTypeName;
    public String awardName;
    public String level;
    public Long recipientId;
    public UUID recipientUuid;
    public String recipientName;
    public String recipientRole;
    public Long senderId;
    public UUID senderUuid;
    public String senderName;
    public String senderRole;
    public Instant sentAt;
    public String message;
    public Integer awardPoints;
    public String approvalStatus;
    public String rejectionReason;
    public Instant createdAt;

    public RecognitionResponse() {}
}

