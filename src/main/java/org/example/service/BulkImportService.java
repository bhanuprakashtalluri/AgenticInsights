package org.example.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.example.model.Recognition;
import org.example.model.RecognitionType;
import org.example.repository.EmployeeRepository;
import org.example.repository.RecognitionRepository;
import org.example.repository.RecognitionTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BulkImportService {

    private final EmployeeRepository employeeRepository;
    private final RecognitionTypeRepository recognitionTypeRepository;
    private final RecognitionRepository recognitionRepository;

    public BulkImportService(EmployeeRepository employeeRepository, RecognitionTypeRepository recognitionTypeRepository, RecognitionRepository recognitionRepository) {
        this.employeeRepository = employeeRepository;
        this.recognitionTypeRepository = recognitionTypeRepository;
        this.recognitionRepository = recognitionRepository;
    }

    public static class ImportResult {
        public int totalRows;
        public int successCount;
        public int failedCount;
        public List<String> errors = new ArrayList<>();
    }

    public ImportResult importCsv(MultipartFile file) throws Exception {
        ImportResult result = new ImportResult();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().withTrim().parse(br);
            Map<String,Integer> headerMap = parser.getHeaderMap();
            List<CSVRecord> records = parser.getRecords();
            result.totalRows = records.size();

            // Bulk-collect UUIDs to resolve employees and recognition types in one pass
            Set<java.util.UUID> employeeUuids = new HashSet<>();
            Set<java.util.UUID> typeUuids = new HashSet<>();
            for (CSVRecord r : records) {
                String ru = r.isMapped("recipient_uuid") ? r.get("recipient_uuid") : null;
                String su = r.isMapped("sender_uuid") ? r.get("sender_uuid") : null;
                String tu = r.isMapped("recognition_type_uuid") ? r.get("recognition_type_uuid") : null;
                if (ru != null && !ru.isBlank()) employeeUuids.add(UUID.fromString(ru));
                if (su != null && !su.isBlank()) employeeUuids.add(UUID.fromString(su));
                if (tu != null && !tu.isBlank()) typeUuids.add(UUID.fromString(tu));
            }

            Map<UUID, Long> empUuidToId = new HashMap<>();
            for (var e : employeeRepository.findAllByUuidIn(new ArrayList<>(employeeUuids))) {
                if (e.getUuid() != null && e.getId() != null) empUuidToId.put(e.getUuid(), e.getId());
            }
            Map<UUID, Long> typeUuidToId = new HashMap<>();
            for (var t : recognitionTypeRepository.findAllByUuidIn(new ArrayList<>(typeUuids))) {
                if (t.getUuid() != null && t.getId() != null) typeUuidToId.put(t.getUuid(), t.getId());
            }

            // Batch insert
            List<Recognition> toInsert = new ArrayList<>();
            int rowNo = 0;
            for (CSVRecord r : records) {
                rowNo++;
                try {
                    String typeUuid = r.isMapped("recognition_type_uuid") ? r.get("recognition_type_uuid") : null;
                    Long typeId = null;
                    if (typeUuid != null && !typeUuid.isBlank()) typeId = typeUuidToId.get(UUID.fromString(typeUuid));
                    if (typeId == null && r.isMapped("recognition_type_id") && !r.get("recognition_type_id").isBlank()) typeId = Long.parseLong(r.get("recognition_type_id"));
                    if (typeId == null) throw new IllegalArgumentException("unknown recognition type");

                    String recipientUuid = r.isMapped("recipient_uuid") ? r.get("recipient_uuid") : null;
                    Long recipientId = null;
                    if (recipientUuid != null && !recipientUuid.isBlank()) recipientId = empUuidToId.get(UUID.fromString(recipientUuid));
                    if (recipientId == null && r.isMapped("recipient_id") && !r.get("recipient_id").isBlank()) recipientId = Long.parseLong(r.get("recipient_id"));
                    if (recipientId == null) throw new IllegalArgumentException("unknown recipient");

                    String senderUuid = r.isMapped("sender_uuid") ? r.get("sender_uuid") : null;
                    Long senderId = null;
                    if (senderUuid != null && !senderUuid.isBlank()) senderId = empUuidToId.get(UUID.fromString(senderUuid));
                    if (senderId == null && r.isMapped("sender_id") && !r.get("sender_id").isBlank()) senderId = Long.parseLong(r.get("sender_id"));
                    if (senderId == null) throw new IllegalArgumentException("unknown sender");

                    Recognition rec = new Recognition();
                    rec.setRecognitionType(recognitionTypeRepository.findById(typeId).orElse(null));
                    rec.setAwardName(r.isMapped("award_name")?r.get("award_name"):null);
                    rec.setLevel(r.isMapped("level")?r.get("level"):null);
                    rec.setRecipientId(recipientId);
                    rec.setSenderId(senderId);
                    if (r.isMapped("sent_at") && !r.get("sent_at").isBlank()) rec.setSentAt(Instant.parse(r.get("sent_at")));
                    rec.setMessage(r.isMapped("message")?r.get("message"):null);
                    if (r.isMapped("award_points") && !r.get("award_points").isBlank()) rec.setAwardPoints(Integer.parseInt(r.get("award_points")));
                    rec.setApprovalStatus(r.isMapped("approval_status")?r.get("approval_status"):"PENDING");
                    if (r.isMapped("rejection_reason") && !r.get("rejection_reason").isBlank()) rec.setRejectionReason(r.get("rejection_reason"));
                    toInsert.add(rec);
                } catch (Exception ex) {
                    result.failedCount++;
                    result.errors.add("row " + rowNo + ": " + ex.getMessage());
                }

                if (toInsert.size() >= 500) {
                    recognitionRepository.saveAll(toInsert);
                    result.successCount += toInsert.size();
                    toInsert.clear();
                }
            }

            if (!toInsert.isEmpty()) {
                recognitionRepository.saveAll(toInsert);
                result.successCount += toInsert.size();
            }
        }

        return result;
    }
}
