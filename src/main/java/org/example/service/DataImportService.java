package org.example.service;

import org.example.model.Employee;
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

@Service
public class DataImportService {
    private final EmployeeRepository employeeRepo;
    private final RecognitionRepository recognitionRepo;
    private final RecognitionTypeRepository typeRepo;

    public DataImportService(EmployeeRepository employeeRepo, RecognitionRepository recognitionRepo, RecognitionTypeRepository typeRepo) {
        this.employeeRepo = employeeRepo;
        this.recognitionRepo = recognitionRepo;
        this.typeRepo = typeRepo;
    }

    public Map<String,Object> importCombinedCsv(MultipartFile file) throws Exception {
        // Combined CSV format: first column 'table' with values employees|recognitions|recognition_types
        // followed by headers specific to that table
        int insertedEmp=0, insertedRec=0, insertedType=0;
        List<Map<String,String>> errors = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = br.readLine();
            if (header == null) throw new IllegalArgumentException("Empty CSV");
            // Expect 'table,...'
            String line;
            while ((line = br.readLine()) != null) {
                String[] cols = parseCsvLine(line);
                if (cols.length == 0) continue;
                String table = cols[0].trim().toLowerCase(Locale.ROOT);
                try {
                    switch (table) {
                        case "employees": {
                            // columns: table,firstName,lastName,unitId,managerId,email,joiningDate,role,uuid
                            Employee e = new Employee();
                            e.setFirstName(safe(cols,1));
                            e.setLastName(safe(cols,2));
                            e.setUnitId(parseLong(safe(cols,3)));
                            e.setManagerId(parseLong(safe(cols,4)));
                            e.setEmail(safe(cols,5));
                            String jd = safe(cols,6);
                            if (jd!=null && !jd.isBlank()) e.setJoiningDate(java.time.LocalDate.parse(jd));
                            e.setRole(safe(cols,7));
                            // uuid optional; if provided, set via EntityMapper after save (assuming model has uuid)
                            employeeRepo.save(e);
                            insertedEmp++;
                            break;
                        }
                        case "recognition_types": {
                            // columns: table,typeName,uuid
                            RecognitionType t = new RecognitionType();
                            t.setTypeName(safe(cols,1));
                            typeRepo.save(t);
                            insertedType++;
                            break;
                        }
                        case "recognitions": {
                            // columns: table,recognitionTypeId,recipientId,senderId,sentAtISO,message,awardPoints,approvalStatus
                            Recognition r = new Recognition();
                            Long typeId = parseLong(safe(cols,1));
                            if (typeId != null) typeRepo.findById(typeId).ifPresent(r::setRecognitionType);
                            Long recipientId = parseLong(safe(cols,2));
                            if (recipientId != null) employeeRepo.findById(recipientId).ifPresent(r::setRecipient);
                            Long senderId = parseLong(safe(cols,3));
                            if (senderId != null) employeeRepo.findById(senderId).ifPresent(r::setSender);
                            String sent = safe(cols,4);
                            if (sent!=null && !sent.isBlank()) r.setSentAt(Instant.parse(sent));
                            r.setMessage(safe(cols,5));
                            Integer pts = parseInt(safe(cols,6));
                            r.setAwardPoints(pts==null?0:pts);
                            String status = safe(cols,7);
                            r.setApprovalStatus(status==null?"PENDING":status);
                            recognitionRepo.save(r);
                            insertedRec++;
                            break;
                        }
                        default:
                            errors.add(Map.of("row", line, "error", "Unknown table: "+table));
                    }
                } catch (Exception ex) {
                    errors.add(Map.of("row", line, "error", ex.getMessage()==null?ex.getClass().getSimpleName():ex.getMessage()));
                }
            }
        }
        return Map.of(
                "insertedEmployees", insertedEmp,
                "insertedRecognitions", insertedRec,
                "insertedTypes", insertedType,
                "errors", errors
        );
    }

    public Map<String,Object> importEmployeesCsv(MultipartFile file) throws Exception {
        int inserted=0; List<Map<String,String>> errors=new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    String[] c = parseCsvLine(line);
                    Employee e = new Employee();
                    e.setFirstName(safe(c,0));
                    e.setLastName(safe(c,1));
                    e.setUnitId(parseLong(safe(c,2)));
                    e.setManagerId(parseLong(safe(c,3)));
                    e.setEmail(safe(c,4));
                    String jd = safe(c,5);
                    if (jd!=null && !jd.isBlank()) e.setJoiningDate(java.time.LocalDate.parse(jd));
                    e.setRole(safe(c,6));
                    employeeRepo.save(e); inserted++;
                } catch (Exception ex) { errors.add(Map.of("row", line, "error", ex.getMessage())); }
            }
        }
        return Map.of("inserted", inserted, "errors", errors);
    }

    public Map<String,Object> importRecognitionTypesCsv(MultipartFile file) throws Exception {
        int inserted=0; List<Map<String,String>> errors=new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = br.readLine(); String line;
            while ((line = br.readLine()) != null) {
                try {
                    String[] c = parseCsvLine(line);
                    RecognitionType t = new RecognitionType();
                    t.setTypeName(safe(c,0));
                    typeRepo.save(t); inserted++;
                } catch (Exception ex) { errors.add(Map.of("row", line, "error", ex.getMessage())); }
            }
        }
        return Map.of("inserted", inserted, "errors", errors);
    }

    public Map<String,Object> importRecognitionsCsv(MultipartFile file) throws Exception {
        int inserted=0; List<Map<String,String>> errors=new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = br.readLine(); String line;
            while ((line = br.readLine()) != null) {
                try {
                    String[] c = parseCsvLine(line);
                    Recognition r = new Recognition();
                    Long typeId = parseLong(safe(c,0));
                    if (typeId != null) typeRepo.findById(typeId).ifPresent(r::setRecognitionType);
                    Long recipientId = parseLong(safe(c,1));
                    if (recipientId != null) employeeRepo.findById(recipientId).ifPresent(r::setRecipient);
                    Long senderId = parseLong(safe(c,2));
                    if (senderId != null) employeeRepo.findById(senderId).ifPresent(r::setSender);
                    String sent = safe(c,3);
                    if (sent!=null && !sent.isBlank()) r.setSentAt(Instant.parse(sent));
                    r.setMessage(safe(c,4));
                    Integer pts = parseInt(safe(c,5));
                    r.setAwardPoints(pts==null?0:pts);
                    String status = safe(c,6);
                    r.setApprovalStatus(status==null?"PENDING":status);
                    recognitionRepo.save(r); inserted++;
                } catch (Exception ex) { errors.add(Map.of("row", line, "error", ex.getMessage())); }
            }
        }
        return Map.of("inserted", inserted, "errors", errors);
    }

    public Map<String, Object> importCombinedJson(String json) {
        // TODO: Implement actual JSON import logic
        return java.util.Map.of("message", "JSON import not implemented");
    }

    public Map<String, Object> importCombinedToon(String toon) {
        // TODO: Implement actual TOON import logic
        return java.util.Map.of("message", "TOON import not implemented");
    }

    private static String safe(String[] arr, int idx) { return idx < arr.length ? emptyToNull(arr[idx]) : null; }
    private static String emptyToNull(String s) { return (s==null || s.isBlank())?null:s; }
    private static Long parseLong(String s) { try { return s==null?null:Long.parseLong(s); } catch (Exception e) { return null; } }
    private static Integer parseInt(String s) { try { return s==null?null:Integer.parseInt(s); } catch (Exception e) { return null; } }

    private static String[] parseCsvLine(String line) {
        // Minimal CSV split honoring quoted values
        List<String> out = new ArrayList<>();
        boolean inQ=false; StringBuilder sb=new StringBuilder();
        for (int i=0;i<line.length();i++) {
            char ch=line.charAt(i);
            if (ch=='"') { inQ=!inQ; continue; }
            if (ch==',' && !inQ) { out.add(sb.toString()); sb.setLength(0); }
            else sb.append(ch);
        }
        out.add(sb.toString());
        return out.toArray(new String[0]);
    }
}
