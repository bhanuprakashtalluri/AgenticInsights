package org.example.dto;

import java.util.UUID;

public class StatusUpdateRequest {
    public String status;
    public String rejectionReason;
    public UUID approverUuid;
    public Long approverId;

    public StatusUpdateRequest() {}

    public StatusUpdateRequest(String status, String rejectionReason, UUID approverUuid, Long approverId) {
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.approverUuid = approverUuid;
        this.approverId = approverId;
    }
}

