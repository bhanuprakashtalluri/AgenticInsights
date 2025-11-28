package org.example.dto;

public class ApprovalRequest {
    public String reason;
    public Long approverId;

    public ApprovalRequest() {}

    public ApprovalRequest(String reason, Long approverId) {
        this.reason = reason;
        this.approverId = approverId;
    }
}

