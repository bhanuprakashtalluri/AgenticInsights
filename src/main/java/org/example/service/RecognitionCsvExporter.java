package org.example.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.example.model.Recognition;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class RecognitionCsvExporter {
    /**
     * Export a list of recognitions to CSV as bytes.
     */
    public byte[] export(List<Recognition> list) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (CSVPrinter printer = CSVFormat.DEFAULT.withHeader(
                "id", "uuid", "recognitionTypeId", "recognitionTypeName", "category", "level", "recipientId", "recipientUuid", "recipientName", "recipientRole", "senderId", "senderUuid", "senderName", "senderRole", "sentAt", "awardPoints", "approvalStatus", "rejectionReason", "message", "createdAt"
        ).print(new OutputStreamWriter(baos, StandardCharsets.UTF_8))) {
            for (Recognition r : list) {
                printer.printRecord(
                        r.getId(),
                        r.getUuid(),
                        r.getRecognitionType() == null ? null : r.getRecognitionType().getId(),
                        r.getRecognitionType() == null ? "" : r.getRecognitionType().getTypeName(),
                        r.getCategory() == null ? "" : r.getCategory(),
                        r.getLevel() == null ? "" : r.getLevel(),
                        r.getRecipientId() == null ? "" : r.getRecipientId(),
                        r.getRecipient() == null ? "" : r.getRecipient().getUuid(),
                        r.getRecipient() == null ? "" : (r.getRecipient().getFirstName() + " " + r.getRecipient().getLastName()),
                        r.getRecipient() == null ? "" : r.getRecipient().getRole(),
                        r.getSenderId() == null ? "" : r.getSenderId(),
                        r.getSender() == null ? "" : r.getSender().getUuid(),
                        r.getSender() == null ? "" : (r.getSender().getFirstName() + " " + r.getSender().getLastName()),
                        r.getSender() == null ? "" : r.getSender().getRole(),
                        r.getSentAt() == null ? "" : r.getSentAt(),
                        r.getAwardPoints() == null ? "" : r.getAwardPoints(),
                        r.getApprovalStatus() == null ? "" : r.getApprovalStatus(),
                        r.getRejectionReason() == null ? "" : r.getRejectionReason(),
                        r.getMessage() == null ? "" : r.getMessage(),
                        r.getCreatedAt() == null ? "" : r.getCreatedAt()
                );
            }
        }
        return baos.toByteArray();
    }
}
