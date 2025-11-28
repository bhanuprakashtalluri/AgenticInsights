package org.example.dto;

import java.util.UUID;

public class RecognitionTypeResponse {
    public Long id;
    public UUID uuid;
    public String typeName;

    public RecognitionTypeResponse() {}

    public RecognitionTypeResponse(Long id, UUID uuid, String typeName) {
        this.id = id;
        this.uuid = uuid;
        this.typeName = typeName;
    }
}

