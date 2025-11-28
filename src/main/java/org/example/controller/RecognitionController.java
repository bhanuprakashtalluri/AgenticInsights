package org.example.controller;

import org.example.dto.RecognitionResponse;
import org.example.dto.RecognitionCreateRequest;
import org.example.dto.RecognitionUpdateRequest;
import org.example.model.Recognition;
import org.example.model.RecognitionType;
import org.example.repository.EmployeeRepository;
import org.example.repository.RecognitionRepository;
import org.example.repository.RecognitionTypeRepository;
import org.example.service.AIInsightsService;
import org.example.service.ChartService;
import org.example.service.RecognitionCsvExporter;
import org.example.util.EntityMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
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

    private static final Logger log = LoggerFactory.getLogger(RecognitionController.class);

    public RecognitionController(RecognitionRepository recognitionRepository,
                                 RecognitionTypeRepository recognitionTypeRepository,
                                 EmployeeRepository employeeRepository,
                                 RecognitionCsvExporter csvExporter,
                                 AIInsightsService insightsService,
                                 ChartService chartService) {
        this.recognitionRepository = recognitionRepository;
        this.recognitionTypeRepository = recognitionTypeRepository;
        this.employeeRepository = employeeRepository;
        this.csvExporter = csvExporter;
        this.insightsService = insightsService;
        this.chartService = chartService;
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

    @GetMapping("/{id}")
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
    public ResponseEntity<byte[]> exportCsv(@RequestParam(required = false) Long recipientId) throws Exception {
        List<Recognition> list;
        if (recipientId != null) list = recognitionRepository.findAllByRecipientId(recipientId, Pageable.unpaged()).getContent();
        else list = recognitionRepository.findAllWithRelations(Pageable.unpaged()).getContent();
        byte[] bytes = csvExporter.export(list);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recognitions.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    @GetMapping("/export.json")
    public ResponseEntity<List<RecognitionResponse>> exportJson(@RequestParam(required = false) Long recipientId) {
        List<Recognition> list;
        if (recipientId != null) list = recognitionRepository.findAllByRecipientId(recipientId, Pageable.unpaged()).getContent();
        else list = recognitionRepository.findAllWithRelations(Pageable.unpaged()).getContent();
        List<RecognitionResponse> resp = list.stream().map(EntityMapper::toRecognitionResponse).collect(Collectors.toList());
        return ResponseEntity.ok(resp);
    }

    @GetMapping(value = "/export-stream.csv")
    public ResponseEntity<StreamingResponseBody> exportStreamCsv(@RequestParam(required = false) Long recipientId) throws Exception {
        List<Recognition> list;
        if (recipientId != null) list = recognitionRepository.findAllByRecipientId(recipientId, Pageable.unpaged()).getContent();
        else list = recognitionRepository.findAllWithRelations(Pageable.unpaged()).getContent();

        StreamingResponseBody stream = (OutputStream os) -> {
            try {
                csvExporter.exportToStream(os, list);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recognitions-stream.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(stream);
    }

    @GetMapping("/insights")
    public Map<String, Object> insights(@RequestParam(required = false) Long days) {
        long effectiveDays = (days == null || days <= 0) ? 30 : days;
        Instant to = Instant.now();
        Instant from = to.minus(effectiveDays, ChronoUnit.DAYS);
        return insightsService.generateInsights(from, to);
    }

    @GetMapping(value = "/graph.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> graph(@RequestParam(required = false) Long days) throws Exception {
        long effectiveDays = (days == null || days <= 0) ? 30 : days;
        Instant to = Instant.now();
        Instant from = to.minus(effectiveDays, ChronoUnit.DAYS);
        Map<String, Object> insights = insightsService.generateInsights(from, to);
        @SuppressWarnings("unchecked")
        Map<String, Integer> series = (Map<String, Integer>) insights.get("timeSeriesByDay");
        byte[] png = chartService.renderTimeSeriesChart(series, "Recognitions over time", "day", "count");
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }

    // New endpoints: role-based insights and exports
    @GetMapping("/insights/role")
    public Map<String, Object> insightsByRole(@RequestParam String role, @RequestParam(required = false) Long days) {
        long effectiveDays = (days == null || days <= 0) ? 30 : days;
        Instant to = Instant.now();
        Instant from = to.minus(effectiveDays, ChronoUnit.DAYS);
        return insightsService.generateInsightsForRole(role, from, to);
    }

    @GetMapping(value = "/insights/role/graph.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> graphByRole(@RequestParam String role, @RequestParam(required = false) Long days) throws Exception {
        long effectiveDays = (days == null || days <= 0) ? 30 : days;
        Instant to = Instant.now();
        Instant from = to.minus(effectiveDays, ChronoUnit.DAYS);
        Map<String, Object> insights = insightsService.generateInsightsForRole(role, from, to);
        @SuppressWarnings("unchecked")
        Map<String, Integer> series = (Map<String, Integer>) insights.get("timeSeriesByDay");
        byte[] png = chartService.renderTimeSeriesChart(series, "Recognitions (" + role + ")", "day", "count");
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }

    @GetMapping("/export-role.csv")
    public ResponseEntity<byte[]> exportCsvByRole(@RequestParam String role, @RequestParam(required = false) Long days) throws Exception {
        long effectiveDays = (days == null || days <= 0) ? 30 : days;
        Instant to = Instant.now();
        Instant from = to.minus(effectiveDays, ChronoUnit.DAYS);
        List<Recognition> list = recognitionRepository.findAllBetween(from, to).stream().filter(r -> {
            if (r.getRecipient() == null) return false;
            return role.equalsIgnoreCase(r.getRecipient().getRole());
        }).toList();
        byte[] bytes = csvExporter.export(list);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recognitions-" + role + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    // Manager scoped insights
    @GetMapping("/insights/manager/{managerId}")
    public Map<String, Object> insightsByManager(@PathVariable Long managerId, @RequestParam(required = false) Long days) {
        long effectiveDays = (days == null || days <= 0) ? 30 : days;
        Instant to = Instant.now();
        Instant from = to.minus(effectiveDays, ChronoUnit.DAYS);
        return insightsService.generateInsightsForManager(managerId, from, to);
    }

    @GetMapping(value = "/insights/manager/{managerId}/graph.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> graphByManager(@PathVariable Long managerId, @RequestParam(required = false) Long days) throws Exception {
        long effectiveDays = (days == null || days <= 0) ? 30 : days;
        Instant to = Instant.now();
        Instant from = to.minus(effectiveDays, ChronoUnit.DAYS);
        Map<String, Object> insights = insightsService.generateInsightsForManager(managerId, from, to);
        @SuppressWarnings("unchecked")
        Map<String, Integer> series = (Map<String, Integer>) insights.get("timeSeriesByDay");
        byte[] png = chartService.renderTimeSeriesChart(series, "Recognitions (manager:" + managerId + ")", "day", "count");
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }

    @GetMapping("/export-manager/{managerId}.csv")
    public ResponseEntity<byte[]> exportCsvByManager(@PathVariable Long managerId, @RequestParam(required = false) Long days) throws Exception {
        long effectiveDays = (days == null || days <= 0) ? 30 : days;
        Instant to = Instant.now();
        Instant from = to.minus(effectiveDays, ChronoUnit.DAYS);
        // find direct reports
        List<Long> reports = recognitionRepository.findAll().stream().filter(r -> r.getRecipient() != null && managerId.equals(r.getRecipient().getManagerId())).map(r -> r.getRecipientId()).distinct().toList();
        List<Recognition> list = recognitionRepository.findAllBetween(from, to).stream().filter(r -> reports.contains(r.getRecipientId())).toList();
        byte[] bytes = csvExporter.export(list);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recognitions-manager-" + managerId + ".csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(bytes);
    }

    @GetMapping(value = "/graph-advanced.png", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<byte[]> graphAdvanced(@RequestParam(defaultValue = "daily") String series,
                                                @RequestParam(required = false) String role,
                                                @RequestParam(required = false) Long managerId,
                                                @RequestParam(required = false) Long days,
                                                @RequestParam(required = false, defaultValue = "png") String format) throws Exception {
        try {
            String normalized = series == null ? "daily" : series.toLowerCase(Locale.ROOT);
            if ("weakly".equals(normalized)) normalized = "weekly"; // alias
            java.util.Set<String> allowed = java.util.Set.of("daily","weekly","monthly","quarterly","yearly");
            if (!allowed.contains(normalized)) {
                return ResponseEntity.badRequest().body(("Unsupported series: " + series + ". Allowed: " + allowed).getBytes());
            }
            Instant to = Instant.now();
            Instant from;
            // Clamp user-specified days to prevent unbounded scans (per granularity sensible maximum)
            java.util.Map<String, Long> maxDays = java.util.Map.of(
                    "daily", 400L,          // ~13 months
                    "weekly", 3L * 365L,     // ~3 years
                    "monthly", 7L * 365L,    // ~7 years
                    "quarterly", 7L * 365L,  // treat similar to monthly for raw scans
                    "yearly", 12L * 365L     // cap ~12 years
            );
            boolean truncated = false;
            if (days != null && days > 0) {
                long requested = days;
                long cap = maxDays.getOrDefault(normalized, 365L);
                if (requested > cap) {
                    truncated = true;
                    requested = cap;
                }
                from = to.minus(requested, ChronoUnit.DAYS);
            } else {
                switch (normalized) {
                    case "daily": from = to.minus(30, ChronoUnit.DAYS); break;
                    case "weekly": from = to.minus(8 * 7L, ChronoUnit.DAYS); break; // ~8 weeks
                    case "monthly": from = to.minus(12, ChronoUnit.MONTHS); break; // 12 months
                    case "quarterly": from = to.minus(8 * 3L, ChronoUnit.MONTHS); break; // 8 quarters (~2y)
                    case "yearly": from = to.minus(5, ChronoUnit.YEARS); break; // 5 years
                    default: from = to.minus(30, ChronoUnit.DAYS);
                }
            }

            // Attempt optimized daily counts via native aggregation (recipient role/manager filters)
            java.util.Map<String,Integer> dailySeries = new java.util.LinkedHashMap<>();
            try {
                java.util.List<Object[]> rows = recognitionRepository.countByDayFiltered(from, to, role, managerId);
                for (Object[] row : rows) {
                    String day = row[0] == null ? null : row[0].toString();
                    Number cntN = (Number) row[1];
                    if (day != null) dailySeries.put(day, cntN == null ? 0 : cntN.intValue());
                }
            } catch (Exception aggEx) {
                log.warn("Fallback to in-memory dailySeries aggregation due to error: {}", aggEx.getMessage());
            }

            // Fallback if native query returned nothing (e.g., no recognitions or error)
            if (dailySeries.isEmpty()) {
                Map<String, Object> insights = managerId != null ?
                        insightsService.generateInsightsForManager(managerId, from, to) :
                        (role != null ? insightsService.generateInsightsForRole(role, from, to) : insightsService.generateInsights(from, to));
                if (insights != null) {
                    @SuppressWarnings("unchecked")
                    Map<String,Integer> tmp = (Map<String,Integer>) insights.get("timeSeriesByDay");
                    if (tmp != null) dailySeries.putAll(tmp);
                }
                if (dailySeries.isEmpty()) {
                    // final fallback raw scan
                    java.time.ZoneId zid = java.time.ZoneId.of("UTC");
                    java.util.List<org.example.model.Recognition> recs = recognitionRepository.findAllBetween(from, to);
                    for (org.example.model.Recognition r : recs) {
                        Instant sent = r.getSentAt();
                        if (sent != null) {
                            String day = sent.atZone(zid).toLocalDate().toString();
                            dailySeries.put(day, dailySeries.getOrDefault(day, 0) + 1);
                        }
                    }
                }
            }

            // Gap fill daily series so downstream weekly/monthly etc. produce continuous buckets
            if (!dailySeries.isEmpty()) {
                java.time.LocalDate start = java.time.LocalDate.parse(dailySeries.keySet().iterator().next());
                java.time.LocalDate end = java.time.LocalDate.parse(dailySeries.keySet().stream().reduce((first, second) -> second).orElse(start.toString()));
                // ensure coverage from computed 'from' instant, not only existing first key
                java.time.LocalDate requestedStart = from.atZone(java.time.ZoneId.of("UTC")).toLocalDate();
                if (requestedStart.isBefore(start)) start = requestedStart;
                java.time.LocalDate requestedEnd = to.atZone(java.time.ZoneId.of("UTC")).toLocalDate();
                if (requestedEnd.isAfter(end)) end = requestedEnd;
                for (java.time.LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                    dailySeries.putIfAbsent(d.toString(), 0);
                }
                // Re-sort after gap fill
                dailySeries = dailySeries.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).collect(Collectors.toMap(java.util.Map.Entry::getKey, java.util.Map.Entry::getValue, (a,b)->a, java.util.LinkedHashMap::new));
            }

            dailySeries = sanitizeSeries(dailySeries);
            Map<String, Integer> seriesMap = aggregateSeries(dailySeries, normalized);
            seriesMap = sanitizeSeries(seriesMap);

            if (seriesMap.isEmpty()) {
                seriesMap.put(java.time.LocalDate.now().toString(), 0);
            }

            if ("json".equalsIgnoreCase(format)) {
                java.util.List<java.util.Map<String,Object>> data = new java.util.ArrayList<>();
                for (Map.Entry<String,Integer> e : seriesMap.entrySet()) {
                    java.util.Map<String,Object> point = new java.util.LinkedHashMap<>();
                    point.put("bucket", e.getKey());
                    point.put("count", e.getValue());
                    data.add(point);
                }
                int total = 0; for (java.util.Map<String,Object> m : data) total += (int) m.get("count");
                java.util.Map<String,Object> body = new java.util.LinkedHashMap<>();
                body.put("series", normalized);
                body.put("from", from.toString());
                body.put("to", to.toString());
                body.put("points", data);
                body.put("total", total);
                String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body);
                ResponseEntity.BodyBuilder builder = ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON);
                if (truncated) builder.header("X-Range-Truncated", "true");
                return builder.body(json.getBytes());
            }
            String xLabel = switch (normalized) {
                case "daily" -> "day";
                case "weekly" -> "week";
                case "monthly" -> "month";
                case "quarterly" -> "quarter";
                case "yearly" -> "year";
                default -> "day";
            };
            byte[] png = chartService.renderTimeSeriesChart(seriesMap, "Recognitions (" + normalized + ")", xLabel, "count");
            ResponseEntity.BodyBuilder builder = ResponseEntity.ok().contentType(MediaType.IMAGE_PNG);
            if (truncated) builder.header("X-Range-Truncated", "true");
            return builder.body(png);
        } catch (Exception ex) {
            String json = "{\"status\":500,\"error\":\"GRAPH_ERROR\",\"message\":\"" + ex.getClass().getSimpleName() + ": " + ex.getMessage() + "\"}";
            return ResponseEntity.status(500).contentType(MediaType.APPLICATION_JSON).body(json.getBytes());
        }
    }

    private Map<String,Integer> sanitizeSeries(Map<String,Integer> in) {
        java.util.Map<String,Integer> out = new java.util.LinkedHashMap<>();
        if (in == null) return out;
        for (Map.Entry<String,Integer> e : in.entrySet()) {
            if (e.getKey() == null) continue; // drop null key
            out.put(e.getKey(), e.getValue() == null ? 0 : e.getValue());
        }
        return out;
    }

    // Helper to aggregate daily counts into requested granularity
    private Map<String, Integer> aggregateSeries(Map<String, Integer> daily, String series) {
        if (daily == null) return java.util.Collections.emptyMap();
        String s = series.toLowerCase(Locale.ROOT);
        if ("daily".equals(s)) return daily; // already daily
        java.util.Map<String, Integer> out = new java.util.LinkedHashMap<>();
        WeekFields wf = WeekFields.ISO;
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (Map.Entry<String, Integer> e : daily.entrySet()) {
            try {
                LocalDate d = LocalDate.parse(e.getKey());
                String bucket;
                switch (s) {
                    case "weekly": {
                        int week = d.get(wf.weekOfWeekBasedYear());
                        bucket = d.getYear() + "-W" + String.format("%02d", week);
                        break;
                    }
                    case "monthly": {
                        bucket = d.format(monthFmt); // yyyy-MM
                        break;
                    }
                    case "quarterly": {
                        int q = (d.getMonthValue() - 1) / 3 + 1;
                        bucket = d.getYear() + "-Q" + q;
                        break;
                    }
                    case "yearly": {
                        bucket = String.valueOf(d.getYear());
                        break;
                    }
                    default: {
                        // unsupported series -> return daily unmodified
                        return daily;
                    }
                }
                out.put(bucket, out.getOrDefault(bucket, 0) + e.getValue());
            } catch (Exception ex) {
                // skip malformed date keys
            }
        }
        return out;
    }
}
