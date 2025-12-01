package org.example.controller;

import org.example.dto.RecognitionResponse;
import org.example.dto.RecognitionCreateRequest;
import org.example.model.Recognition;
import org.example.model.RecognitionType;
import org.example.repository.EmployeeRepository;
import org.example.repository.RecognitionRepository;
import org.example.repository.RecognitionTypeRepository;
import org.example.service.AIInsightsService;
import org.example.service.ChartService;
import org.example.service.FileStorageService;
import org.example.service.RecognitionCsvExporter;
import org.example.util.EntityMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/recognitions")
public class RecognitionController {

    private final RecognitionRepository recognitionRepository;
    private final RecognitionTypeRepository recognitionTypeRepository;
    private final EmployeeRepository employeeRepository;
    private final RecognitionCsvExporter csvExporter;
    private final AIInsightsService insightsService;
    private final ChartService chartService;
    private final FileStorageService fileStorageService;

    private static final Logger log = LoggerFactory.getLogger(RecognitionController.class);

    public RecognitionController(RecognitionRepository recognitionRepository,
                                 RecognitionTypeRepository recognitionTypeRepository,
                                 EmployeeRepository employeeRepository,
                                 RecognitionCsvExporter csvExporter,
                                 AIInsightsService insightsService,
                                 ChartService chartService,
                                 FileStorageService fileStorageService) {
        this.recognitionRepository = recognitionRepository;
        this.recognitionTypeRepository = recognitionTypeRepository;
        this.employeeRepository = employeeRepository;
        this.csvExporter = csvExporter;
        this.insightsService = insightsService;
        this.chartService = chartService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public Page<RecognitionResponse> list(@RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size,
                                  @RequestParam(required = false) Long recipientId,
                                  @RequestParam(required = false) Long senderId,
                                  @RequestParam(required = false) java.util.UUID recipientUuid,
                                  @RequestParam(required = false) java.util.UUID senderUuid) {
        Pageable p = PageRequest.of(page, size);
        Page<Recognition> pageResult;
        // prefer UUID resolution if provided
        if (recipientUuid != null) {
            pageResult = employeeRepository.findByUuid(recipientUuid).map(e -> recognitionRepository.findAllByRecipientId(e.getId(), p)).orElseGet(() -> recognitionRepository.findAllWithRelations(Pageable.unpaged()));
        } else if (senderUuid != null) {
            pageResult = employeeRepository.findByUuid(senderUuid).map(e -> recognitionRepository.findAllBySenderId(e.getId(), p)).orElseGet(() -> recognitionRepository.findAllWithRelations(Pageable.unpaged()));
        } else if (recipientId != null) pageResult = recognitionRepository.findAllByRecipientId(recipientId, p);
        else if (senderId != null) pageResult = recognitionRepository.findAllBySenderId(senderId, p);
         else pageResult = recognitionRepository.findAllWithRelations(p);
         return pageResult.map(EntityMapper::toRecognitionResponse);
     }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody RecognitionCreateRequest req) {
        try {
            Recognition r = new Recognition();
            if (req.recognitionTypeId != null) {
                Optional<RecognitionType> rt = recognitionTypeRepository.findById(req.recognitionTypeId);
                rt.ifPresent(r::setRecognitionType);
            } else if (req.recognitionTypeUuid != null) {
                Optional<RecognitionType> rt = recognitionTypeRepository.findByUuid(req.recognitionTypeUuid);
                rt.ifPresent(r::setRecognitionType);
            }
            // resolve recipient
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
            r.setAwardName(req.awardName);
            r.setLevel(req.level);
            r.setMessage(req.message);
            r.setAwardPoints(req.awardPoints == null ? 0 : req.awardPoints);
            if (req.sentAt != null) r.setSentAt(Instant.parse(req.sentAt));
            r.setApprovalStatus("PENDING");
            Recognition saved = recognitionRepository.save(r);
            // reload with relations to map
            Recognition reloaded = recognitionRepository.findByIdWithRelations(saved.getId()).orElse(saved);
            return ResponseEntity.status(201).body(EntityMapper.toRecognitionResponse(reloaded));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id:\\d+}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        Optional<Recognition> r = recognitionRepository.findByIdWithRelations(id);
        return r.map(rec -> ResponseEntity.ok(EntityMapper.toRecognitionResponse(rec))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/uuid/{uuid}")
    public ResponseEntity<?> getByUuid(@PathVariable UUID uuid) {
        Optional<Recognition> r = recognitionRepository.findByUuidWithRelations(uuid);
        return r.map(rec -> ResponseEntity.ok(EntityMapper.toRecognitionResponse(rec))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody org.example.dto.RecognitionUpdateRequest req) {
        Optional<Recognition> opt = recognitionRepository.findByIdWithRelations(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Recognition r = opt.get();
        if (req.awardName != null) r.setAwardName(req.awardName);
        if (req.level != null) r.setLevel(req.level);
        if (req.message != null) r.setMessage(req.message);
        if (req.awardPoints != null) r.setAwardPoints(req.awardPoints);
        if (req.approvalStatus != null) r.setApprovalStatus(req.approvalStatus);
        if (req.rejectionReason != null) r.setRejectionReason(req.rejectionReason);
        recognitionRepository.save(r);
        Recognition reloaded = recognitionRepository.findByIdWithRelations(r.getId()).orElse(r);
        return ResponseEntity.ok(EntityMapper.toRecognitionResponse(reloaded));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id,
                                     @RequestBody(required = false) org.example.dto.ApprovalRequest body,
                                     @RequestParam(required = false) Long approverId) {
        Optional<Recognition> opt = recognitionRepository.findByIdWithRelations(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Recognition r = opt.get();
        Long usedApprover = (body != null && body.approverId != null) ? body.approverId : approverId;
        // (we don't currently store approver, just accept it)
        r.setApprovalStatus("APPROVED");
        // clear any previous rejection reason when approving
        r.setRejectionReason(null);
        recognitionRepository.save(r);
        Recognition reloaded = recognitionRepository.findByIdWithRelations(r.getId()).orElse(r);
        return ResponseEntity.ok(EntityMapper.toRecognitionResponse(reloaded));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id,
                                    @RequestBody(required = false) org.example.dto.ApprovalRequest body,
                                    @RequestParam(required = false) String reason,
                                    @RequestParam(required = false) Long approverId) {
        Optional<Recognition> opt = recognitionRepository.findByIdWithRelations(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        Recognition r = opt.get();
        String usedReason = (body != null && body.reason != null) ? body.reason : reason;
        Long usedApprover = (body != null && body.approverId != null) ? body.approverId : approverId;
        if (usedReason == null || usedReason.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "reason is required to reject a recognition"));
        }
        r.setApprovalStatus("REJECTED");
        r.setRejectionReason(usedReason);
        recognitionRepository.save(r);
        Recognition reloaded = recognitionRepository.findByIdWithRelations(r.getId()).orElse(r);
        return ResponseEntity.ok(EntityMapper.toRecognitionResponse(reloaded));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!recognitionRepository.existsById(id)) return ResponseEntity.notFound().build();
        recognitionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/export.csv")
    public ResponseEntity<byte[]> exportCsv(@RequestParam(required = false) Long recipientId,
                                             @RequestParam(required = false) Long senderId,
                                             @RequestParam(required = false) String role,
                                             @RequestParam(required = false) Long managerId,
                                             @RequestParam(required = false) Long days) throws Exception {
        List<Recognition> list;
        boolean applyWindow = (role != null) || (managerId != null) || (days != null);
        if (applyWindow) {
            long effectiveDays = (days == null || days <= 0) ? 30 : days;
            Instant to = Instant.now();
            Instant from = to.minus(effectiveDays, ChronoUnit.DAYS);
            list = recognitionRepository.findAllBetweenWithRelations(from, to);
        } else {
            list = recognitionRepository.findAllWithRelations(Pageable.unpaged()).getContent();
        }
        // apply entity filters
        if (recipientId != null) list = list.stream().filter(r -> recipientId.equals(r.getRecipientId())).toList();
        if (senderId != null) list = list.stream().filter(r -> senderId.equals(r.getSenderId())).toList();
        if (role != null && !role.isBlank()) list = list.stream().filter(r -> r.getRecipient() != null && role.equalsIgnoreCase(r.getRecipient().getRole())).toList();
        if (managerId != null) list = list.stream().filter(r -> r.getRecipient() != null && managerId.equals(r.getRecipient().getManagerId())).toList();
        byte[] bytes = csvExporter.export(list);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recognitions.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    @GetMapping("/export.json")
    public ResponseEntity<List<RecognitionResponse>> exportJson(@RequestParam(required = false) Long recipientId,
                                                                 @RequestParam(required = false) Long senderId,
                                                                 @RequestParam(required = false) String role,
                                                                 @RequestParam(required = false) Long managerId,
                                                                 @RequestParam(required = false) Long days) {
        List<Recognition> list;
        boolean applyWindow = (role != null) || (managerId != null) || (days != null);
        if (applyWindow) {
            long effectiveDays = (days == null || days <= 0) ? 30 : days;
            Instant to = Instant.now();
            Instant from = to.minus(effectiveDays, ChronoUnit.DAYS);
            list = recognitionRepository.findAllBetweenWithRelations(from, to);
        } else {
            list = recognitionRepository.findAllWithRelations(Pageable.unpaged()).getContent();
        }
        if (recipientId != null) list = list.stream().filter(r -> recipientId.equals(r.getRecipientId())).toList();
        if (senderId != null) list = list.stream().filter(r -> senderId.equals(r.getSenderId())).toList();
        if (role != null && !role.isBlank()) list = list.stream().filter(r -> r.getRecipient() != null && role.equalsIgnoreCase(r.getRecipient().getRole())).toList();
        if (managerId != null) list = list.stream().filter(r -> r.getRecipient() != null && managerId.equals(r.getRecipient().getManagerId())).toList();
        List<RecognitionResponse> resp = list.stream().map(EntityMapper::toRecognitionResponse).collect(Collectors.toList());
        return ResponseEntity.ok(resp);
    }


    // Deprecated / moved endpoints: redirect to canonical endpoints under /insights or /recognitions/export.csv
    @Deprecated
    @GetMapping("/insights")
    public ResponseEntity<Void> deprecatedInsightsRedirect(@RequestParam(required = false) Long days) {
        String url = "/insights" + (days == null ? "" : "?days=" + days);
        return ResponseEntity.status(301).header(HttpHeaders.LOCATION, url).build();
    }

    @Deprecated
    @GetMapping(value = "/graph.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<Void> deprecatedGraphRedirect(@RequestParam(required = false) Long days) {
        String url = "/insights/graph.png" + (days == null ? "" : "?days=" + days);
        return ResponseEntity.status(301).header(HttpHeaders.LOCATION, url).build();
    }

    @Deprecated
    @GetMapping("/insights/role")
    public ResponseEntity<Void> deprecatedInsightsRoleRedirect(@RequestParam String role, @RequestParam(required = false) Long days) {
        String url = "/insights/role?role=" + role + (days == null ? "" : "&days=" + days);
        return ResponseEntity.status(301).header(HttpHeaders.LOCATION, url).build();
    }

    @Deprecated
    @GetMapping(value = "/insights/role/graph.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<Void> deprecatedGraphByRoleRedirect(@RequestParam String role, @RequestParam(required = false) Long days) {
        String url = "/insights/role/graph.png?role=" + role + (days == null ? "" : "&days=" + days);
        return ResponseEntity.status(301).header(HttpHeaders.LOCATION, url).build();
    }

    @Deprecated
    @GetMapping("/export-role.csv")
    public ResponseEntity<Void> deprecatedExportRoleRedirect(@RequestParam String role, @RequestParam(required = false) Long days) {
        String url = "/recognitions/export.csv?role=" + role + (days == null ? "" : "&days=" + days);
        return ResponseEntity.status(301).header(HttpHeaders.LOCATION, url).build();
    }

    @Deprecated
    @GetMapping("/insights/manager/{managerId}")
    public ResponseEntity<Void> deprecatedInsightsManagerRedirect(@PathVariable Long managerId, @RequestParam(required = false) Long days) {
        String url = "/insights/manager/" + managerId + (days == null ? "" : "?days=" + days);
        return ResponseEntity.status(301).header(HttpHeaders.LOCATION, url).build();
    }

    @Deprecated
    @GetMapping(value = "/insights/manager/{managerId}/graph.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<Void> deprecatedGraphByManagerRedirect(@PathVariable Long managerId, @RequestParam(required = false) Long days) {
        String url = "/insights/manager/" + managerId + "/graph.png" + (days == null ? "" : "?days=" + days);
        return ResponseEntity.status(301).header(HttpHeaders.LOCATION, url).build();
    }

    @Deprecated
    @GetMapping("/export-manager/{managerId}.csv")
    public ResponseEntity<Void> deprecatedExportManagerRedirect(@PathVariable Long managerId, @RequestParam(required = false) Long days) {
        String url = "/recognitions/export.csv?managerId=" + managerId + (days == null ? "" : "&days=" + days);
        return ResponseEntity.status(301).header(HttpHeaders.LOCATION, url).build();
    }

    @GetMapping(value = {"/graph/advanced.png", "/graph/advanced"}, produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<byte[]> graphAdvanced(@RequestParam(defaultValue = "daily") String series,
                                                  @RequestParam(required = false) String role,
                                                  @RequestParam(required = false) Long managerId,
                                                  @RequestParam(required = false) Long days,
                                                  @RequestParam(required = false) Integer buckets,
                                                  @RequestParam(required = false) String iterations,
                                                  @RequestParam(required = false) String iteration,
                                                  @RequestParam(required = false, defaultValue = "png") String format,
                                                  HttpServletRequest request) throws Exception {
        try {
            // Normalize series selection and compute timeframe (keep original defaults/caps)
            String rawSeries = series == null ? "daily" : series;
            java.util.List<String> tokens = java.util.Arrays.stream(rawSeries.split(","))
                    .map(t -> t.trim().toLowerCase(Locale.ROOT))
                    .map(t -> "weakly".equals(t) ? "weekly" : t)
                    .filter(t -> !t.isEmpty())
                    .collect(Collectors.toList());
            java.util.Set<String> allowed = java.util.Set.of("daily","weekly","monthly","quarterly","yearly");
            for (String tok : tokens) {
                if (!allowed.contains(tok)) {
                    return ResponseEntity.badRequest().body(("Unsupported series token: " + tok + ". Allowed: " + allowed).getBytes());
                }
            }
            boolean multiGranularity = tokens.size() > 1;
            String normalized = tokens.get(0);
