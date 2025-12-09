package org.example.service;

import org.example.model.Recognition;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class RecognitionToonExporter {
    public byte[] export(List<Recognition> list) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (Recognition r : list) {
            // Token-Oriented Object Notation (TOON) - simple key:value; pairs, one per line, blank line between records
            baos.writeBytes(("id:" + r.getId() + ";\n").getBytes(StandardCharsets.UTF_8));
            baos.writeBytes(("uuid:" + r.getUuid() + ";\n").getBytes(StandardCharsets.UTF_8));
            baos.writeBytes(("recognitionTypeId:" + (r.getRecognitionType() == null ? "" : r.getRecognitionType().getId()) + ";\n").getBytes(StandardCharsets.UTF_8));
            baos.writeBytes(("recognitionTypeName:" + (r.getRecognitionType() == null ? "" : r.getRecognitionType().getTypeName()) + ";\n").getBytes(StandardCharsets.UTF_8));
            baos.writeBytes(("category:" + (r.getCategory() == null ? "" : r.getCategory()) + ";\n").getBytes(StandardCharsets.UTF_8));
            baos.writeBytes(("level:" + (r.getLevel() == null ? "" : r.getLevel()) + ";\n").getBytes(StandardCharsets.UTF_8));
            baos.writeBytes(("recipientId:" + (r.getRecipientId() == null ? "" : r.getRecipientId()) + ";\n").getBytes(StandardCharsets.UTF_8));
            baos.writeBytes(("recipientUuid:" + (r.getRecipient() == null ? "" : r.getRecipient().getUuid()) + ";\n").getBytes(StandardCharsets.UTF_8));
            baos.writeBytes(("recipientName:" + (r.getRecipient() == null ? "" : r.getRecipient().getFirstName() + " " + r.getRecipient().getLastName()) + ";\n").getBytes(StandardCharsets.UTF_8));
            baos.writeBytes(("recipientRole:" + (r.getRecipient() == null ? "" : r.getRecipient().getRole()) + ";\n").getBytes(StandardCharsets.UTF_8));
            baos.writeBytes(("senderId:" + (r.getSenderId() == null ? "" : r.getSenderId()) + ";\n").getBytes(StandardCharsets.UTF_8));
            baos.writeBytes(("senderUuid:" + (r.getSender() == null ? "" : r.getSender().getUuid()) + ";\n").getBytes(StandardCharsets.UTF_8));
            baos.writeBytes(("senderName:" + (r.getSender() == null ? "" : r.getSender().getFirstName() + " " + r.getSender().getLastName()) + ";\n").getBytes(StandardCharsets.UTF_8));
            baos.writeBytes(("senderRole:" + (r.getSender() == null ? "" : r.getSender().getRole()) + ";\n").getBytes(StandardCharsets.UTF_8));
            baos.writeBytes(("sentAt:" + (r.getSentAt() == null ? "" : r.getSentAt()) + ";\n").getBytes(StandardCharsets.UTF_8));
            baos.writeBytes(("awardPoints:" + (r.getAwardPoints() == null ? "" : r.getAwardPoints()) + ";\n").getBytes(StandardCharsets.UTF_8));
            baos.writeBytes(("approvalStatus:" + (r.getApprovalStatus() == null ? "" : r.getApprovalStatus()) + ";\n").getBytes(StandardCharsets.UTF_8));
            baos.writeBytes(("rejectionReason:" + (r.getRejectionReason() == null ? "" : r.getRejectionReason()) + ";\n").getBytes(StandardCharsets.UTF_8));
            baos.writeBytes(("message:" + (r.getMessage() == null ? "" : r.getMessage()) + ";\n").getBytes(StandardCharsets.UTF_8));
            baos.writeBytes(("createdAt:" + (r.getCreatedAt() == null ? "" : r.getCreatedAt()) + ";\n").getBytes(StandardCharsets.UTF_8));
            baos.writeBytes("\n".getBytes(StandardCharsets.UTF_8)); // blank line between records
        }
        return baos.toByteArray();
    }
}

