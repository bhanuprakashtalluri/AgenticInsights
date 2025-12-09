package org.example.service;

import org.example.model.Employee;
import org.example.model.Recognition;
import org.example.model.RecognitionType;
import org.example.repository.EmployeeRepository;
import org.example.repository.RecognitionRepository;
import org.example.repository.RecognitionTypeRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class DataExportService {
    private final EmployeeRepository employeeRepo;
    private final RecognitionRepository recognitionRepo;
    private final RecognitionTypeRepository typeRepo;
    private final FileStorageService storage;

    public DataExportService(EmployeeRepository employeeRepo, RecognitionRepository recognitionRepo, RecognitionTypeRepository typeRepo, FileStorageService storage) {
        this.employeeRepo = employeeRepo;
        this.recognitionRepo = recognitionRepo;
        this.typeRepo = typeRepo;
        this.storage = storage;
    }

    public byte[] exportCombinedCsv() throws java.io.IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("table,fields...\n");
        for (Employee e : employeeRepo.findAll()) {
            sb.append("employees,")
              .append(n(e.getFirstName())).append(',')
              .append(n(e.getLastName())).append(',')
              .append(n(e.getUnitId())).append(',')
              .append(n(e.getManagerId())).append(',')
              .append(n(e.getEmail())).append(',')
              .append(n(e.getJoiningDate())).append(',')
              .append(n(e.getRole()))
              .append('\n');
        }
        for (RecognitionType t : typeRepo.findAll()) {
            sb.append("recognition_types,")
              .append(n(t.getTypeName()))
              .append('\n');
        }
        for (Recognition r : recognitionRepo.findAll()) {
            sb.append("recognitions,")
              .append(n(r.getRecognitionType()==null?null:r.getRecognitionType().getId())).append(',')
              .append(n(r.getRecipientId())).append(',')
              .append(n(r.getSenderId())).append(',')
              .append(n(r.getSentAt())).append(',')
              .append(n(r.getMessage())).append(',')
              .append(n(r.getAwardPoints())).append(',')
              .append(n(r.getApprovalStatus()))
              .append('\n');
        }
        byte[] out = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        // store in exports/csv with timestamped name using storage timestamp
        String filename = "data_combined_" + storage.nowTimestamp() + ".csv";
        storage.storeExportCsv(filename, out);
        return out;
    }

    public byte[] exportEmployeesCsv() throws java.io.IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("firstName,lastName,unitId,managerId,email,joiningDate,role\n");
        for (Employee e : employeeRepo.findAll()) {
            sb.append(n(e.getFirstName())).append(',')
              .append(n(e.getLastName())).append(',')
              .append(n(e.getUnitId())).append(',')
              .append(n(e.getManagerId())).append(',')
              .append(n(e.getEmail())).append(',')
              .append(n(e.getJoiningDate())).append(',')
              .append(n(e.getRole()))
              .append('\n');
        }
        byte[] out = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String filename = "employees_" + storage.nowTimestamp() + ".csv";
        storage.storeExportCsv(filename, out);
        return out;
    }

    public byte[] exportRecognitionTypesCsv() throws java.io.IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("typeName\n");
        for (RecognitionType t : typeRepo.findAll()) {
            sb.append(n(t.getTypeName())).append('\n');
        }
        byte[] out = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String filename = "recognition_types_" + storage.nowTimestamp() + ".csv";
        storage.storeExportCsv(filename, out);
        return out;
    }

    public byte[] exportRecognitionsCsv() throws java.io.IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("recognitionTypeId,recipientId,senderId,sentAt,message,awardPoints,approvalStatus\n");
        for (Recognition r : recognitionRepo.findAll()) {
            sb.append(n(r.getRecognitionType()==null?null:r.getRecognitionType().getId())).append(',')
              .append(n(r.getRecipientId())).append(',')
              .append(n(r.getSenderId())).append(',')
              .append(n(r.getSentAt())).append(',')
              .append(n(r.getMessage())).append(',')
              .append(n(r.getAwardPoints())).append(',')
              .append(n(r.getApprovalStatus()))
              .append('\n');
        }
        byte[] out = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String filename = "recognitions_" + storage.nowTimestamp() + ".csv";
        storage.storeExportCsv(filename, out);
        return out;
    }

    public byte[] exportCombinedJson() throws java.io.IOException {
        // Export all data as a single JSON object
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("employees", employeeRepo.findAll());
        data.put("recognition_types", typeRepo.findAll());
        data.put("recognitions", recognitionRepo.findAll());
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        byte[] out = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(data);
        String filename = "data_combined_" + storage.nowTimestamp() + ".json";
        storage.storeExportJson(filename, out);
        return out;
    }

    public byte[] exportCombinedToon() throws java.io.IOException {
        // Export all data as a TOON (custom text) format
        StringBuilder sb = new StringBuilder();
        sb.append("# Employees\n");
        for (Employee e : employeeRepo.findAll()) {
            sb.append(e.getFirstName()).append(' ').append(e.getLastName()).append(" | unit:").append(e.getUnitId()).append(" | manager:").append(e.getManagerId()).append(" | email:").append(e.getEmail()).append(" | joined:").append(e.getJoiningDate()).append(" | role:").append(e.getRole()).append('\n');
        }
        sb.append("\n# Recognition Types\n");
        for (RecognitionType t : typeRepo.findAll()) {
            sb.append(t.getTypeName()).append('\n');
        }
        sb.append("\n# Recognitions\n");
        for (Recognition r : recognitionRepo.findAll()) {
            sb.append("type:").append(r.getRecognitionType()==null?null:r.getRecognitionType().getId())
              .append(" | recipient:").append(r.getRecipientId())
              .append(" | sender:").append(r.getSenderId())
              .append(" | sentAt:").append(r.getSentAt())
              .append(" | message:").append(r.getMessage())
              .append(" | points:").append(r.getAwardPoints())
              .append(" | status:").append(r.getApprovalStatus())
              .append('\n');
        }
        byte[] out = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String filename = "data_combined_" + storage.nowTimestamp() + ".toon";
        storage.storeExportToon(filename, out);
        return out;
    }

    private static String n(Object o) {
        if (o == null) return "";
        String s = String.valueOf(o);
        // naive escaping
        if (s.contains(",") || s.contains("\n") || s.contains("\"")) {
            s = '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}
