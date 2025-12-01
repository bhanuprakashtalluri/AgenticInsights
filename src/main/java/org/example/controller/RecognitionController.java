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
             String rawSeries = series == null ? "daily" : series;
             java.util.List<String> tokens = java.util.Arrays.stream(rawSeries.split(","))
                     .map(t -> t.trim().toLowerCase(Locale.ROOT))
                     .map(t -> "weakly".equals(t) ? "weekly" : t)
                     .filter(t -> !t.isEmpty())
                     .collect(Collectors.toList());
             java.util.Set<String> allowed = java.util.Set.of("daily","weekly","monthly","quarterly","yearly");
             // validate tokens: they must all be one of the allowed granularities
             for (String tok : tokens) {
                 if (!allowed.contains(tok)) {
                     return ResponseEntity.badRequest().body(("Unsupported series token: " + tok + ". Allowed: " + allowed).getBytes());
                 }
             }
             boolean multiGranularity = tokens.size() > 1;
             String normalized = tokens.get(0); // primary granularity for single-mode PNG

             Instant to = Instant.now();
             Instant from;
             // Clamp user-specified ranges to prevent unbounded scans (per granularity sensible maximum)
             java.util.Map<String, Long> maxDays = java.util.Map.of(
                     "daily", 400L,          // ~13 months
                     "weekly", 3L * 365L,     // ~3 years
                     "monthly", 7L * 365L,    // ~7 years
                     "quarterly", 7L * 365L,  // treat similar to monthly for raw scans
                     "yearly", 12L * 365L     // cap ~12 years
             );
             boolean truncated = false;

             // parse 'iterations' like '3months', '5quarters' — precedence: iterations > buckets > days
             // Support singular 'iteration' param too: prefer 'iterations' if present, else 'iteration'
             if ((iterations == null || iterations.isBlank()) && iteration != null && !iteration.isBlank()) {
                iterations = iteration;
             }
             // Fallback: read raw request param names (in case binding missed 'iteration')
             if ((iterations == null || iterations.isBlank()) && request != null) {
                 String raw = request.getParameter("iteration");
                 if (raw == null || raw.isBlank()) raw = request.getParameter("iter");
                 if (raw != null && !raw.isBlank()) iterations = raw;
             }

             if (iterations != null && !iterations.isBlank()) {
               // Normalize: remove weird trailing chars (e.g., '2;;') and non-alphanumeric except space
               String itRaw = iterations.trim().replaceAll("[;]+$", "").replaceAll("[^A-Za-z0-9 ]", "");
               // Accept bare numeric values as 'buckets' in the requested granularity
               if (itRaw.matches("^\\d+$")) {
                   int qty = Integer.parseInt(itRaw);
                   String unitKey = switch (normalized) {
                       case "daily" -> "d";
                       case "weekly" -> "w";
                       case "monthly" -> "m";
                       case "quarterly" -> "q";
                       case "yearly" -> "y";
                       default -> "d";
                   };
                   long daysEquivalent = switch (unitKey) {
                       case "d" -> (long) qty;
                       case "w" -> (long) qty * 7L;
                       case "m" -> (long) qty * 30L;
                       case "q" -> (long) qty * 90L;
                       case "y" -> (long) qty * 365L;
                       default -> (long) qty;
                   };
                   long cap = maxDays.getOrDefault(normalized, 365L);
                   if (daysEquivalent > cap) {
                       truncated = true;
                       from = to.minus(cap, ChronoUnit.DAYS);
                   } else {
                       ZonedDateTime zto = ZonedDateTime.ofInstant(to, ZoneId.of("UTC"));
                       // Compute start of the current bucket and then go back (qty-1) buckets so we return exactly qty buckets
                       ZonedDateTime zfrom;
                       switch (unitKey) {
                           case "d": {
                               ZonedDateTime startToday = zto.truncatedTo(ChronoUnit.DAYS);
                               zfrom = startToday.minusDays(Math.max(0, qty - 1));
                               break;
                           }
                           case "w": {
                               WeekFields wf = WeekFields.ISO;
                               java.time.LocalDate startOfWeek = zto.toLocalDate().with(wf.dayOfWeek(), 1);
                               zfrom = startOfWeek.atStartOfDay(ZoneId.of("UTC")).minusWeeks(Math.max(0, qty - 1));
                               break;
                           }
                           case "m": {
                               ZonedDateTime startOfMonth = zto.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
                               zfrom = startOfMonth.minusMonths(Math.max(0, qty - 1));
                               break;
                           }
                           case "q": {
                               int month = zto.getMonthValue();
                               int startMonth = ((month - 1) / 3) * 3 + 1;
                               ZonedDateTime startOfQuarter = zto.withMonth(startMonth).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
                               zfrom = startOfQuarter.minusMonths(Math.max(0, (qty - 1) * 3L));
                               break;
                           }
                           case "y": {
                               ZonedDateTime startOfYear = zto.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS);
                               zfrom = startOfYear.minusYears(Math.max(0, qty - 1));
                               break;
                           }
                           default: {
                               ZonedDateTime startToday = zto.truncatedTo(ChronoUnit.DAYS);
                               zfrom = startToday.minusDays(Math.max(0, qty - 1));
                           }
                       }
                       from = zfrom.toInstant();
                   }
                } else {
                   // Accept forms like: 3months, 3 months, 3M, 3mon, 5quarters, 10years, etc.
                    java.util.regex.Pattern p = java.util.regex.Pattern.compile("^\\s*(\\d+)\\s*([A-Za-z]+)\\s*$");
                    java.util.regex.Matcher m = p.matcher(itRaw);
                    if (!m.matches()) {
                        return ResponseEntity.badRequest().body(("Unsupported iterations format: " + iterations + ". Examples: 3months, 5quarters, 10years").getBytes());
                    }
                    int qty = Integer.parseInt(m.group(1));
                    String rawUnit = m.group(2).toLowerCase(Locale.ROOT);
                    // Map by prefix: d=days, w=weeks, m=months, q=quarters, y=years
                    String unitKey;
                    if (rawUnit.startsWith("d")) unitKey = "d";
                    else if (rawUnit.startsWith("w")) unitKey = "w";
                    else if (rawUnit.startsWith("q")) unitKey = "q";
                    else if (rawUnit.startsWith("y")) unitKey = "y";
                    else if (rawUnit.startsWith("m")) unitKey = "m"; // months
                    else return ResponseEntity.badRequest().body(("Unsupported iterations unit: " + rawUnit + ". Use d/w/m/q/y or words like months/quarters/years").getBytes());

                   long daysEquivalent = switch (unitKey) {
                       case "d" -> (long) qty;
                       case "w" -> (long) qty * 7L;
                       case "m" -> (long) qty * 30L;
                       case "q" -> (long) qty * 90L;
                       case "y" -> (long) qty * 365L;
                       default -> (long) qty;
                   };
                   long cap = maxDays.getOrDefault(normalized, 365L);
                   if (daysEquivalent > cap) {
                       truncated = true;
                       from = to.minus(cap, ChronoUnit.DAYS);
                   } else {
                       ZonedDateTime zto = ZonedDateTime.ofInstant(to, ZoneId.of("UTC"));
                       // same logic as numeric branch: start of current bucket minus (qty-1) buckets
                       ZonedDateTime zfrom;
                       switch (unitKey) {
                           case "d":
                               zfrom = zto.truncatedTo(ChronoUnit.DAYS).minusDays(Math.max(0, qty - 1));
                               break;
                           case "w":
                               WeekFields wf = WeekFields.ISO;
                               java.time.LocalDate startOfWeek = zto.toLocalDate().with(wf.dayOfWeek(), 1);
                               zfrom = startOfWeek.atStartOfDay(ZoneId.of("UTC")).minusWeeks(Math.max(0, qty - 1));
                               break;
                           case "m":
                               zfrom = zto.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).minusMonths(Math.max(0, qty - 1));
                               break;
                           case "q": {
                               int month = zto.getMonthValue();
                               int startMonth = ((month - 1) / 3) * 3 + 1;
                               ZonedDateTime startOfQuarter = zto.withMonth(startMonth).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
                               zfrom = startOfQuarter.minusMonths(Math.max(0, (qty - 1) * 3L));
                               break;
                           }
                           case "y":
                               zfrom = zto.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS).minusYears(Math.max(0, qty - 1));
                               break;
                           default:
                               zfrom = zto.truncatedTo(ChronoUnit.DAYS).minusDays(Math.max(0, qty - 1));
                       }
                       from = zfrom.toInstant();
                   }
                }
            } else if (buckets != null && buckets > 0) {
                  // Preferred: compute 'from' using number of buckets in the requested granularity
                  int b = buckets;
                  // approximate days for cap comparison
                 long daysEquivalent = switch (normalized) {
                     case "daily" -> (long) b;
                     case "weekly" -> (long) b * 7L;
                     case "monthly" -> (long) b * 30L;
                     case "quarterly" -> (long) b * 90L; // 3 months
                     case "yearly" -> (long) b * 365L;
                     default -> (long) b;
                 };
                 long cap = maxDays.getOrDefault(normalized, 365L);
                 if (daysEquivalent > cap) {
                     truncated = true;
                     // fall back to days cap as an approximation
                     from = to.minus(cap, ChronoUnit.DAYS);
                 } else {
                     ZonedDateTime zto = ZonedDateTime.ofInstant(to, ZoneId.of("UTC"));
                     // compute start of current bucket then back (b-1) buckets to return exactly b buckets
                     ZonedDateTime zfrom;
                     switch (normalized) {
                         case "daily": zfrom = zto.truncatedTo(ChronoUnit.DAYS).minusDays(Math.max(0, b - 1)); break;
                         case "weekly": {
                             WeekFields wf = WeekFields.ISO;
                             java.time.LocalDate startOfWeek = zto.toLocalDate().with(wf.dayOfWeek(), 1);
                             zfrom = startOfWeek.atStartOfDay(ZoneId.of("UTC")).minusWeeks(Math.max(0, b - 1));
                             break;
                         }
                         case "monthly": zfrom = zto.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS).minusMonths(Math.max(0, b - 1)); break;
                         case "quarterly": {
                             int month = zto.getMonthValue();
                             int startMonth = ((month - 1) / 3) * 3 + 1;
                             ZonedDateTime startOfQuarter = zto.withMonth(startMonth).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
                             zfrom = startOfQuarter.minusMonths(Math.max(0, (b - 1) * 3L));
                             break;
                         }
                         case "yearly": zfrom = zto.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS).minusYears(Math.max(0, b - 1)); break;
                         default: zfrom = zto.truncatedTo(ChronoUnit.DAYS).minusDays(Math.max(0, b - 1));
                     }
                     from = zfrom.toInstant();
                 }
              } else if (days != null && days > 0) {
                 long requested = days;
                 long cap = maxDays.getOrDefault(normalized, 365L);
                 if (requested > cap) {
                     truncated = true;
                     requested = cap;
                 }
                 from = to.minus(requested, ChronoUnit.DAYS);
             } else {
                 // Default timeframe per granularity
                 ZonedDateTime zto = ZonedDateTime.ofInstant(to, ZoneId.of("UTC"));
                 ZonedDateTime zfrom;
                 switch (normalized) {
                     case "daily": zfrom = zto.minusDays(30); break;
                     case "weekly": zfrom = zto.minusWeeks(8); break; // ~8 weeks
                     case "monthly": zfrom = zto.minusMonths(12); break; // 12 months
                     case "quarterly": zfrom = zto.minusMonths(8 * 3L); break; // 8 quarters (~24 months)
                     case "yearly": zfrom = zto.minusYears(5); break; // 5 years
                     default: zfrom = zto.minusDays(30);
                 }
                 from = zfrom.toInstant();
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
                java.time.LocalDate requestedStart = from.atZone(java.time.ZoneId.of("UTC")).toLocalDate();
                java.time.LocalDate requestedEnd = to.atZone(java.time.ZoneId.of("UTC")).toLocalDate();
                for (java.time.LocalDate d = requestedStart; !d.isAfter(requestedEnd); d = d.plusDays(1)) {
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

            // Support JSON output for a single granularity (happy path)
            if ("json".equalsIgnoreCase(format) && !multiGranularity) {
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
                ResponseEntity.BodyBuilder respBuilder = ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON);
                if (truncated) respBuilder.header("X-Range-Truncated", "true");
                return respBuilder.body(json.getBytes());
            }

            // If multiple granularities requested: JSON/CSV will return all; PNG is unsupported
            if (multiGranularity && "png".equalsIgnoreCase(format)) {
                String msg = "PNG output supports a single granularity. Request JSON or CSV for multiple series.";
                return ResponseEntity.badRequest().body(msg.getBytes());
            }

            // Support CSV output for the advanced graph endpoint
            if ("csv".equalsIgnoreCase(format)) {
                StringBuilder sb = new StringBuilder();
                if (multiGranularity) {
                    sb.append("series,bucket,count\n");
                    for (String tok : tokens) {
                        Map<String, Integer> m = sanitizeSeries(aggregateSeries(dailySeries, tok));
                        for (Map.Entry<String, Integer> e : m.entrySet()) {
                            sb.append(tok).append(',').append(e.getKey()).append(',').append(e.getValue()).append('\n');
                        }
                    }
                } else {
                    sb.append("bucket,count\n");
                    for (Map.Entry<String, Integer> e : seriesMap.entrySet()) {
                        sb.append(e.getKey()).append(',').append(e.getValue()).append('\n');
                    }
                }
                byte[] csvBytes = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                ResponseEntity.BodyBuilder csvBuilder = ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv"))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=recognitions-" + String.join("+", tokens) + ".csv");
                if (truncated) csvBuilder.header("X-Range-Truncated", "true");
                return csvBuilder.body(csvBytes);
            }

            // Support JSON output for multiple granularities
            if ("json".equalsIgnoreCase(format) && multiGranularity) {
                java.util.Map<String, Object> body = new java.util.LinkedHashMap<>();
                body.put("series", tokens);
                body.put("from", from.toString());
                body.put("to", to.toString());
                java.util.Map<String, java.util.List<java.util.Map<String,Object>>> data = new java.util.LinkedHashMap<>();
                for (String tok : tokens) {
                    Map<String, Integer> m = sanitizeSeries(aggregateSeries(dailySeries, tok));
                    java.util.List<java.util.Map<String,Object>> pts = new java.util.ArrayList<>();
                    for (Map.Entry<String,Integer> e : m.entrySet()) {
                        java.util.Map<String,Object> point = new java.util.LinkedHashMap<>();
                        point.put("bucket", e.getKey());
                        point.put("count", e.getValue());
                        pts.add(point);
                    }
                    data.put(tok, pts);
                }
                body.put("data", data);
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
            // store advanced graph
            String ts = fileStorageService.nowTimestamp();
            String filename = "graph_advanced_" + normalized + "_" + from.toString().replace(':','-') + "_to_" + to.toString().replace(':','-') + "_" + ts + ".png";
            fileStorageService.storeGraph(filename, png);
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
    private Map<String, Integer> aggregateSeries(Map<String, Integer> daily,
