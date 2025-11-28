package org.example.repository;

import java.time.Instant;

/**
 * Projection for lightweight recognition scans (time-series + aggregate metrics) without fetching full entity graphs.
 */
public interface RecognitionLite {
    Instant getSentAt();
    Integer getAwardPoints();
    Long getRecipientId();
    Long getSenderId();
    String getApprovalStatus();
    String getLevel();
}

