package org.example.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.example.model.Recognition;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class RecognitionCsvExporter {

    public byte[] export(List<Recognition> list) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        exportToStream(baos, list);
        return baos.toByteArray();
    }

    public void exportToStream(OutputStream out, List<Recognition> list) throws Exception {
        OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
        CSVPrinter printer = CSVFormat.DEFAULT.withHeader(
                "id","uuid","recognitionTypeId","recognitionTypeName","awardName","level","recipientId","recipientUuid","recipientName","recipientRole","senderId","senderUuid","senderName","senderRole","sentAt","awardPoints","approvalStatus","rejectionReason","message","createdAt"
        ).print(writer);

        for (Recognition r : list) {
            String recogTypeName = r.getRecognitionType() == null ? "" : r.getRecognitionType().getTypeName();
            String recipientName = r.getRecipient() == null ? "" : (r.getRecipient().getFirstName() + " " + r.getRecipient().getLastName());
            String senderName = r.getSender() == null ? "" : (r.getSender().getFirstName() + " " + r.getSender().getLastName());
            String recipientRole = r.getRecipient() == null ? "" : r.getRecipient().getRole();
            String senderRole = r.getSender() == null ? "" : r.getSender().getRole();

            printer.printRecord(
                    r.getId(), r.getUuid(),
                    r.getRecognitionType() == null ? null : r.getRecognitionType().getId(), recogTypeName,
                    r.getAwardName(), r.getLevel(), r.getRecipientId(),
                    r.getRecipient() == null ? null : r.getRecipient().getUuid(), recipientName, recipientRole,
                    r.getSenderId(), r.getSender() == null ? null : r.getSender().getUuid(), senderName, senderRole,
                    r.getSentAt(), r.getAwardPoints(), r.getApprovalStatus(), r.getRejectionReason(), r.getMessage(), r.getCreatedAt()
            );
        }

        printer.flush();
    }
}
