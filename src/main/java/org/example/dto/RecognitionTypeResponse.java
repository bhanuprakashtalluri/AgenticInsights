package org.example.dto;

import java.util.UUID;

public class RecognitionTypeResponse {
    public Long id;
    public UUID uuid;
    public String typeName;
    public String createdBy;
    public String createdAt;

    public RecognitionTypeResponse() {}

    public RecognitionTypeResponse(Long id, UUID uuid, String typeName, String createdBy, String createdAt) {
        this.id = id;
        this.uuid = uuid;
        this.typeName = typeName;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }
}
