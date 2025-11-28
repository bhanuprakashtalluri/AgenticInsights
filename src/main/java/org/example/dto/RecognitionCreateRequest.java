package org.example.dto;

import java.util.UUID;

public class RecognitionCreateRequest {
    public Long recognitionTypeId;
    public UUID recognitionTypeUuid;
    public String awardName;
    public String level;
    public Long recipientId;
    public UUID recipientUuid;
    public Long senderId;
    public UUID senderUuid;
    public String sentAt; // ISO string optional
    public String message;
    public Integer awardPoints;
}

