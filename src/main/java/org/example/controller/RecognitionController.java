package org.example.controller;

import org.example.dto.RecognitionResponse;
import org.example.dto.RecognitionCreateRequest;
import org.example.model.Recognition;
import org.example.model.Employee;
import org.example.repository.EmployeeRepository;
import org.example.repository.RecognitionRepository;
import org.example.repository.RecognitionTypeRepository;
import org.example.service.RecognitionCsvExporter;
import org.example.service.RecognitionToonExporter;
import org.example.service.ChartService;
import org.example.service.FileStorageService;
import org.example.util.EntityMapper;
import org.example.dto.RecognitionChartDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/recognitions")
public class RecognitionController {
    private final RecognitionRepository recognitionRepository;
    private final RecognitionTypeRepository recognitionTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final RecognitionCsvExporter csvExporter;
    private final RecognitionToonExporter toonExporter;
    private final ChartService chartService;
    private final FileStorageService fileStorageService;

    private static final String CSV_DIR = "artifacts/exports/csv/";
    private static final String JSON_DIR = "artifacts/exports/json/";
    private static final String GRAPHS_DIR = "artifacts/graphs/";
    private static String timestampedName(String base, String ext) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH.mm");
        String ts = LocalDateTime.now().format(fmt);
        return base + "-" + ts + (ext != null ? "." + ext : "");
    }

    public RecognitionController(RecognitionRepository recognitionRepository,
                                 RecognitionTypeRepository recognitionTypeRepository,
                                 EmployeeRepository employeeRepository,
                                 RecognitionCsvExporter csvExporter,
                                 RecognitionToonExporter toonExporter,
                                 ChartService chartService,
                                 FileStorageService fileStorageService) {
        this.recognitionRepository = recognitionRepository;
        this.recognitionTypeRepository = recognitionTypeRepository;
        this.employeeRepository = employeeRepository;
        this.csvExporter = csvExporter;
        this.toonExporter = toonExporter;
        this.chartService = chartService;
        this.fileStorageService = fileStorageService;
    }

    // --- CRUD ---
    @PostMapping
    public ResponseEntity<?> create(@RequestBody RecognitionCreateRequest req) {
        try {
            Recognition r = new Recognition();
            if (req.recognitionTypeId != null) {
                recognitionTypeRepository.findById(req.recognitionTypeId).ifPresent(r::setRecognitionType);
            } else if (req.recognitionTypeUuid != null) {
                recognitionTypeRepository.findByUuid(req.recognitionTypeUuid).ifPresent(r::setRecognitionType);
            }
            if (req.recipientId != null) {
                employeeRepository.findById(req.recipientId).ifPresent(r::setRecipient);
            } else if (req.recipientUuid != null) {
                employeeRepository.findByUuid(req.recipientUuid).ifPresent(r::setRecipient);
            }
            if (req.senderId != null) {
                employeeRepository.findById(req.senderId).ifPresent(r::setSender);
            } else if (req.senderUuid != null) {
                employeeRepository.findByUuid(req.senderUuid).ifPresent(r::setSender);
            }
            r.setCategory(req.category);
            r.setLevel(req.level);
            r.setMessage(req.message);
            r.setAwardPoints(req.awardPoints == null ? 0 : req.awardPoints);
            if (req.sentAt != null) r.setSentAt(Instant.parse(req.sentAt));
            r.setApprovalStatus("PENDING"); // always uppercase
            Recognition saved = recognitionRepository.save(r);
            Recognition reloaded = recognitionRepository.findByIdWithRelations(saved.getId()).orElse(saved);
            return ResponseEntity.status(201).body(EntityMapper.toRecognitionResponse(reloaded));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public Page<RecognitionResponse> list(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(required = false) Long senderId,
                               @RequestParam(required = false) Long recipientId,
                               Authentication authentication) {
        Pageable p = PageRequest.of(page, size);
        List<Recognition> all = recognitionRepository.findAllWithRelations(p).getContent();
        Page<Recognition> pageResult;
        String role = authentication.getAuthorities().stream().findFirst().map(a -> a.getAuthority()).orElse("");
        String email = authentication.getName();
        if (role.equals("ROLE_EMPLOYEE")) {
            Long employeeId = employeeRepository.findByEmail(email).map(Employee::getId).orElse(-1L);
            all = all.stream().filter(r -> (r.getSender() != null && r.getSender().getId().equals(employeeId)) || (r.getRecipient() != null && r.getRecipient().getId().equals(employeeId))).toList();
            pageResult = new org.springframework.data.domain.PageImpl<>(all, p, all.size());
        } else if (role.equals("ROLE_TEAMLEAD")) {
            Long managerId = employeeRepository.findByEmail(email).map(Employee::getId).orElse(-1L);
            all = all.stream().filter(r -> r.getRecipient() != null && managerId.equals(r.getRecipient().getManagerId())).toList();
            pageResult = new org.springframework.data.domain.PageImpl<>(all, p, all.size());
        } else if (role.equals("ROLE_MANAGER")) {
            Long unitId = employeeRepository.findByEmail(email).map(Employee::getUnitId).orElse(-1L);
            all = all.stream().filter(r -> r.getRecipient() != null && unitId.equals(r.getRecipient().getUnitId())).toList();
            pageResult = new org.springframework.data.domain.PageImpl<>(all, p, all.size());
        } else {
            pageResult = recognitionRepository.findAllWithRelations(p);
        }
        return pageResult.map(EntityMapper::toRecognitionResponse);
    }

    private Long getEmployeeIdByEmail(String email) {
        return employeeRepository.findByEmail(email).map(Employee::getId).orElse(-1L);
    }

    // Unified get by ID or UUID (as request parameters)
    @GetMapping("/single")
    public ResponseEntity<?> getByIdOrUuid(@RequestParam(required = false) Long id, @RequestParam(required = false) UUID uuid, HttpServletRequest request) {
        Optional<Recognition> opt = Optional.empty();
        if (id != null) opt = recognitionRepository.findByIdWithRelations(id);
        else if (uuid != null) opt = recognitionRepository.findByUuidWithRelations(uuid);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Recognition r = opt.get();
        return ResponseEntity.ok(EntityMapper.toRecognitionResponse(r));
    }

    // Unified update by ID or UUID (as request parameters)
    @PutMapping("/single")
    public ResponseEntity<?> update(@RequestParam(required = false) Long id, @RequestParam(required = false) UUID uuid, @RequestBody Recognition req, HttpServletRequest request) {
        Optional<Recognition> opt = Optional.empty();
        if (id != null) opt = recognitionRepository.findByIdWithRelations(id);
        else if (uuid != null) opt = recognitionRepository.findByUuidWithRelations(uuid);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Recognition r = opt.get();
        // Update fields
        if (req.getCategory() != null) r.setCategory(req.getCategory());
        if (req.getLevel() != null) r.setLevel(req.getLevel());
        if (req.getMessage() != null) r.setMessage(req.getMessage());
        if (req.getSentAt() != null) r.setSentAt(req.getSentAt());
        if (req.getRecognitionType() != null) r.setRecognitionType(req.getRecognitionType());
        // Points logic
        String typeName = r.getRecognitionType() != null ? r.getRecognitionType().getTypeName() : null;
        String level = r.getLevel();
        if (typeName != null) {
            if (typeName.equalsIgnoreCase("ecard")) {
                r.setAwardPoints(0);
            } else if (typeName.equalsIgnoreCase("ecard_with_points")) {
                Integer pts = req.getAwardPoints();
                if (pts != null && (pts == 5 || pts == 10)) {
                    r.setAwardPoints(pts);
                } else {
                    r.setAwardPoints(5); // default to 5 if not valid
                }
            } else if (typeName.equalsIgnoreCase("award")) {
                if (level != null) {
                    switch (level.toLowerCase()) {
                        case "bronze" -> r.setAwardPoints(20);
                        case "silver" -> r.setAwardPoints(25);
                        case "gold" -> r.setAwardPoints(30);
                        default -> r.setAwardPoints(0);
                    }
                } else {
                    r.setAwardPoints(0);
                }
            }
        }
        // Status and rejection reason logic
        String prevStatus = r.getApprovalStatus();
        String newStatus = req.getApprovalStatus();
        if (newStatus != null) {
            r.setApprovalStatus(newStatus);
            if (!"REJECTED".equalsIgnoreCase(newStatus)) {
                r.setRejectionReason(null); // clear reason if not rejected
            } else {
                r.setRejectionReason(req.getRejectionReason());
            }
        }
        recognitionRepository.save(r);
        return ResponseEntity.ok(EntityMapper.toRecognitionResponse(r));
    }

    // Unified delete by ID or UUID (as request parameters)
    @DeleteMapping("/single")
    public ResponseEntity<?> delete(@RequestParam(required = false) Long id, @RequestParam(required = false) UUID uuid, HttpServletRequest request) {
        Optional<Recognition> opt = Optional.empty();
        if (id != null) opt = recognitionRepository.findByIdWithRelations(id);
        else if (uuid != null) opt = recognitionRepository.findByUuidWithRelations(uuid);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Recognition r = opt.get();
        recognitionRepository.deleteById(r.getId());
        return ResponseEntity.noContent().build();
    }

    // Unified approve by ID or UUID (as request parameters)
    @PatchMapping("/approve")
    public ResponseEntity<?> approve(@RequestParam(required = false) Long id, @RequestParam(required = false) UUID uuid, @RequestBody(required = false) org.example.dto.ApprovalRequest body, @RequestParam(required = false) Long approverId, HttpServletRequest request) {
        Optional<Recognition> opt = Optional.empty();
        if (id != null) opt = recognitionRepository.findByIdWithRelations(id);
        else if (uuid != null) opt = recognitionRepository.findByUuidWithRelations(uuid);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Recognition r = opt.get();
        r.setApprovalStatus("APPROVED");
        r.setRejectionReason(null);
        recognitionRepository.save(r);
        Recognition reloaded = recognitionRepository.findByIdWithRelations(r.getId()).orElse(r);
        return ResponseEntity.ok(EntityMapper.toRecognitionResponse(reloaded));
    }

    // Unified reject by ID or UUID (as request parameters)
    @PatchMapping("/reject")
    public ResponseEntity<?> reject(@RequestParam(required = false) Long id, @RequestParam(required = false) UUID uuid, @RequestBody(required = false) org.example.dto.ApprovalRequest body, @RequestParam(required = false) String reason, @RequestParam(required = false) Long approverId, HttpServletRequest request) {
        Optional<Recognition> opt = Optional.empty();
        if (id != null) opt = recognitionRepository.findByIdWithRelations(id);
        else if (uuid != null) opt = recognitionRepository.findByUuidWithRelations(uuid);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Recognition r = opt.get();
        String usedReason = (body != null && body.reason != null) ? body.reason : reason;
        if (usedReason == null || usedReason.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "reason is required to reject a recognition"));
        }
        r.setApprovalStatus("REJECTED");
        r.setRejectionReason(usedReason);
        recognitionRepository.save(r);
        Recognition reloaded = recognitionRepository.findByIdWithRelations(r.getId()).orElse(r);
        return ResponseEntity.ok(EntityMapper.toRecognitionResponse(reloaded));
    }

    // --- Search ---
    @GetMapping("/search")
    public Page<RecognitionResponse> search(@RequestParam(required = false) Long id,
                                        @RequestParam(required = false) UUID uuid,
                                        @RequestParam(required = false) String name,
                                        @RequestParam(required = false) Long unitId,
                                        @RequestParam(required = false) Long typeId,
                                        @RequestParam(required = false) Integer points,
                                        @RequestParam(required = false) String role,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) String category,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size,
                                        HttpServletRequest request) {
    Pageable p = PageRequest.of(page, size);
    List<Recognition> filtered = recognitionRepository.findAllWithRelations(Pageable.unpaged()).getContent();
        if (name != null && !name.isBlank()) filtered = filtered.stream().filter(r -> r.getCategory() != null && r.getCategory().toLowerCase().contains(name.toLowerCase())).toList();
        if (unitId != null && !isAll(unitId)) filtered = filtered.stream().filter(r -> r.getRecipient() != null && unitId.equals(r.getRecipient().getUnitId())).toList();
        if (typeId != null && !isAll(typeId)) filtered = filtered.stream().filter(r -> r.getRecognitionType() != null && typeId.equals(r.getRecognitionType().getId())).toList();
        if (points != null && !isAll(points)) filtered = filtered.stream().filter(r -> points.equals(r.getAwardPoints())).toList();
        if (role != null && !role.isBlank() && !role.equalsIgnoreCase("all")) filtered = filtered.stream().filter(r -> r.getRecipient() != null && role.equalsIgnoreCase(r.getRecipient().getRole())).toList();
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("all")) {
            String statusNorm = status.trim().toUpperCase();
            filtered = filtered.stream().filter(r -> {
                String s = r.getApprovalStatus();
                return s != null && statusNorm.equals(s.trim().toUpperCase());
            }).toList();
        }
        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("all")) {
            String categoryNorm = category.trim().toLowerCase();
            filtered = filtered.stream().filter(r -> r.getCategory() != null && r.getCategory().toLowerCase().contains(categoryNorm)).toList();
        }
        final List<Recognition> finalFiltered = filtered;
        return finalFiltered.stream().skip((long) page * size).limit(size).map(EntityMapper::toRecognitionResponse).collect(Collectors.collectingAndThen(Collectors.toList(), l -> new org.springframework.data.domain.PageImpl<>(l, p, finalFiltered.size())));
    }

    // --- Export ---
    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportCsv(@RequestParam(required = false) Long recipientId,
                                             @RequestParam(required = false) Long senderId,
                                             @RequestParam(required = false) String role,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String category,
                                             @RequestParam(required = false) Long managerId,
                                             @RequestParam(required = false) Long days) {
        List<Recognition> list;
        boolean applyWindow = (role != null) || (managerId != null) || (days != null) || (status != null) || (category != null);
        if (applyWindow) {
            long effectiveDays = (days == null || days <= 0) ? 30 : days;
            Instant to = Instant.now();
            Instant from = to.minus(effectiveDays, ChronoUnit.DAYS);
            list = recognitionRepository.findAllBetweenWithRelations(from, to);
        } else {
            list = recognitionRepository.findAllWithRelations(Pageable.unpaged()).getContent();
        }
        if (recipientId != null && !isAll(recipientId)) list = list.stream().filter(r -> recipientId.equals(r.getRecipientId())).toList();
        if (senderId != null && !isAll(senderId)) list = list.stream().filter(r -> senderId.equals(r.getSenderId())).toList();
        if (role != null && !role.isBlank() && !role.equalsIgnoreCase("all")) list = list.stream().filter(r -> r.getRecipient() != null && role.equalsIgnoreCase(r.getRecipient().getRole())).toList();
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("all")) {
            String statusNorm = status.trim().toUpperCase();
            list = list.stream().filter(r -> {
                String s = r.getApprovalStatus();
                return s != null && statusNorm.equals(s.trim().toUpperCase());
            }).toList();
        }
        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("all")) {
            String categoryNorm = category.trim().toLowerCase();
            list = list.stream().filter(r -> r.getCategory() != null && r.getCategory().toLowerCase().contains(categoryNorm)).toList();
        }
        if (managerId != null && !isAll(managerId)) list = list.stream().filter(r -> r.getRecipient() != null && managerId.equals(r.getRecipient().getManagerId())).toList();
        if (list.isEmpty()) {
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=empty.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body("".getBytes());
        }
        byte[] bytes;
        String fname = timestampedName("recognitions_export", "csv");
        try {
            bytes = csvExporter.export(list);
            Files.createDirectories(Paths.get(CSV_DIR));
            Files.write(Paths.get(CSV_DIR, fname), bytes);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(("CSV export failed: " + e.getMessage()).getBytes());
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fname)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    @GetMapping("/export.json")
    public ResponseEntity<List<RecognitionResponse>> exportJson(@RequestParam(required = false) Long recipientId,
                                                                 @RequestParam(required = false) Long senderId,
                                                                 @RequestParam(required = false) String role,
                                                                 @RequestParam(required = false) String status,
                                                                 @RequestParam(required = false) String category,
                                                                 @RequestParam(required = false) Long managerId,
                                                                 @RequestParam(required = false) Long days) {
        List<Recognition> list;
        boolean applyWindow = (role != null) || (managerId != null) || (days != null) || (status != null) || (category != null);
        if (applyWindow) {
            long effectiveDays = (days == null || days <= 0) ? 30 : days;
            Instant to = Instant.now();
            Instant from = to.minus(effectiveDays, ChronoUnit.DAYS);
            list = recognitionRepository.findAllBetweenWithRelations(from, to);
        } else {
            list = recognitionRepository.findAllWithRelations(Pageable.unpaged()).getContent();
        }
        if (recipientId != null && !isAll(recipientId)) list = list.stream().filter(r -> recipientId.equals(r.getRecipientId())).toList();
        if (senderId != null && !isAll(senderId)) list = list.stream().filter(r -> senderId.equals(r.getSenderId())).toList();
        if (role != null && !role.isBlank() && !role.equalsIgnoreCase("all")) list = list.stream().filter(r -> r.getRecipient() != null && role.equalsIgnoreCase(r.getRecipient().getRole())).toList();
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("all")) {
            String statusNorm = status.trim().toUpperCase();
            list = list.stream().filter(r -> {
                String s = r.getApprovalStatus();
                return s != null && statusNorm.equals(s.trim().toUpperCase());
            }).toList();
        }
        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("all")) {
            String categoryNorm = category.trim().toLowerCase();
            list = list.stream().filter(r -> r.getCategory() != null && r.getCategory().toLowerCase().contains(categoryNorm)).toList();
        }
        if (managerId != null && !isAll(managerId)) list = list.stream().filter(r -> r.getRecipient() != null && managerId.equals(r.getRecipient().getManagerId())).toList();
        if (list.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        List<RecognitionResponse> resp = list.stream().map(EntityMapper::toRecognitionResponse).collect(Collectors.toList());
        // Store JSON in json folder
        try {
            Files.createDirectories(Paths.get(JSON_DIR));
            String fname = timestampedName("recognitions_export", "json");
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(resp);
            Files.write(Paths.get(JSON_DIR, fname), json.getBytes());
        } catch (Exception e) {
            // Log or ignore
        }
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/export.toon")
    public ResponseEntity<byte[]> exportToon(@RequestParam(required = false) Long recipientId,
                                             @RequestParam(required = false) Long senderId,
                                             @RequestParam(required = false) String role,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String category,
                                             @RequestParam(required = false) Long managerId,
                                             @RequestParam(required = false) Long days) {
        List<Recognition> list;
        boolean applyWindow = (role != null) || (managerId != null) || (days != null) || (status != null) || (category != null);
        if (applyWindow) {
            long effectiveDays = (days == null || days <= 0) ? 30 : days;
            Instant to = Instant.now();
            Instant from = to.minus(effectiveDays, ChronoUnit.DAYS);
            list = recognitionRepository.findAllBetweenWithRelations(from, to);
        } else {
            list = recognitionRepository.findAllWithRelations(Pageable.unpaged()).getContent();
        }
        if (recipientId != null && !isAll(recipientId)) list = list.stream().filter(r -> recipientId.equals(r.getRecipientId())).toList();
        if (senderId != null && !isAll(senderId)) list = list.stream().filter(r -> senderId.equals(r.getSenderId())).toList();
        if (role != null && !role.isBlank() && !role.equalsIgnoreCase("all")) list = list.stream().filter(r -> r.getRecipient() != null && role.equalsIgnoreCase(r.getRecipient().getRole())).toList();
        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("all")) {
            String statusNorm = status.trim().toUpperCase();
            list = list.stream().filter(r -> {
                String s = r.getApprovalStatus();
                return s != null && statusNorm.equals(s.trim().toUpperCase());
            }).toList();
        }
        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("all")) {
            String categoryNorm = category.trim().toLowerCase();
            list = list.stream().filter(r -> r.getCategory() != null && r.getCategory().toLowerCase().contains(categoryNorm)).toList();
        }
        if (managerId != null && !isAll(managerId)) list = list.stream().filter(r -> r.getRecipient() != null && managerId.equals(r.getRecipient().getManagerId())).toList();
        if (list.isEmpty()) {
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=empty.toon")
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(new byte[0]);
        }
        byte[] bytes;
        String fname = timestampedName("recognitions_export", "toon");
        try {
            bytes = toonExporter.export(list);
            Files.createDirectories(Paths.get("artifacts/exports/toon/"));
            Files.write(Paths.get("artifacts/exports/toon/", fname), bytes);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(("TOON export failed: " + e.getMessage()).getBytes());
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fname)
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(bytes);
    }

    // Unified export endpoint
    @GetMapping("/export")
    public ResponseEntity<?> export(@RequestParam String format,
                               @RequestParam(required = false) Long recipientId,
                               @RequestParam(required = false) Long senderId,
                               @RequestParam(required = false) String role,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) String category,
                               @RequestParam(required = false) Long managerId,
                               @RequestParam(required = false) Long days,
                               HttpServletRequest request) {
    List<Recognition> filteredList;
    filteredList = recognitionRepository.findAllWithRelations(Pageable.unpaged()).getContent();
    switch (format.toLowerCase()) {
        case "csv" -> {
            byte[] bytes;
            try {
                bytes = csvExporter.export(filteredList);
            } catch (Exception e) {
                return ResponseEntity.status(500).body(("CSV export failed: " + e.getMessage()).getBytes());
            }
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recognitions_export.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
        }
        case "json" -> {
            List<RecognitionResponse> resp = filteredList.stream().map(EntityMapper::toRecognitionResponse).toList();
            return ResponseEntity.ok(resp);
        }
        case "toon" -> {
            byte[] bytes = toonExporter.export(filteredList);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recognitions_export.toon")
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(bytes);
        }
        default -> {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid format: " + format));
        }
    }
}

    // --- Graph ---
    @GetMapping(value = "/graph", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> graph(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) UUID uuid,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long unitId,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String sender,
            @RequestParam(required = false) String receiver,
            @RequestParam(required = false) String manager,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer points,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false, defaultValue = "days") String groupBy,
            @RequestParam(required = false, defaultValue = "10") Integer iterations,
            HttpServletRequest request) throws Exception {
        List<Recognition> allRecognitions = recognitionRepository.findAllWithRelations(Pageable.unpaged()).getContent();
        // Filter by params
        List<Recognition> filtered = allRecognitions.stream()
            .filter(r -> id == null || r.getRecipientId() != null && r.getRecipientId().equals(id))
            .filter(r -> unitId == null || unitId.toString().equalsIgnoreCase("all") || r.getRecipient() != null && unitId.equals(r.getRecipient().getUnitId()))
            .filter(r -> role == null || role.equalsIgnoreCase("all") || (r.getLevel() != null && r.getLevel().equalsIgnoreCase(role)))
            .filter(r -> points == null || points.toString().equalsIgnoreCase("all") || (r.getAwardPoints() != null && r.getAwardPoints().equals(points)))
            .filter(r -> status == null || status.equalsIgnoreCase("all") || (r.getApprovalStatus() != null && r.getApprovalStatus().equalsIgnoreCase(status)))
            .toList();
        // Map to DTOs
        List<RecognitionChartDTO> chartData = filtered.stream()
            .map(r -> new RecognitionChartDTO(r.getSentAt(), r.getAwardPoints(), r.getRecipientId(), r.getSenderId(), r.getApprovalStatus(), r.getLevel()))
            .toList();
        // Group by time bucket
        java.util.Map<String, Integer> timeSeries = new java.util.LinkedHashMap<>();
        for (int i = iterations - 1; i >= 0; i--) {
            String label;
            java.time.LocalDate bucketDate;
            switch (groupBy.toLowerCase()) {
                case "weeks" -> {
                    bucketDate = java.time.LocalDate.now().minusWeeks(i);
                    label = bucketDate.with(java.time.DayOfWeek.MONDAY).toString();
                }
                case "months" -> {
                    bucketDate = java.time.LocalDate.now().minusMonths(i).withDayOfMonth(1);
                    label = bucketDate.toString();
                }
                case "years" -> {
                    bucketDate = java.time.LocalDate.now().minusYears(i).withDayOfYear(1);
                    label = String.valueOf(bucketDate.getYear());
                }
                default -> {
                    bucketDate = java.time.LocalDate.now().minusDays(i);
                    label = bucketDate.toString();
                }
            }
            timeSeries.put(label, 0);
        }
        for (RecognitionChartDTO r : chartData) {
            java.time.Instant sent = r.getSentAt();
            if (sent == null) continue;
            java.time.LocalDate sentDate = sent.atZone(java.time.ZoneOffset.UTC).toLocalDate();
            String label;
            switch (groupBy.toLowerCase()) {
                case "weeks" -> label = sentDate.with(java.time.DayOfWeek.MONDAY).toString();
                case "months" -> label = sentDate.withDayOfMonth(1).toString();
                case "years" -> label = String.valueOf(sentDate.getYear());
                default -> label = sentDate.toString();
            }
            timeSeries.computeIfPresent(label, (k, v) -> v + 1);
        }
        String title = "Recognitions";
        String yLabel = "count";
        byte[] png = chartService.renderTimeSeriesChart(timeSeries, title, groupBy, yLabel);
        String fname = "recognition_graph-" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy-HH.mm")) + ".png";
        fileStorageService.storeGraph(fname, png);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }

    // Helper method for 'all' check
    private boolean isAll(Object param) {
        if (param == null) return false;
        if (param instanceof String s) return s.equalsIgnoreCase("all");
        return false;
    }
}
