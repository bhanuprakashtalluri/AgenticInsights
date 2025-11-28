package org.example.controller;

import org.example.service.BulkImportService;
import org.example.service.ReportsService;
import org.example.service.StagingImportService;
import org.example.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final ReportsService reportsService;
    private final EmployeeRepository employeeRepository;
    private final BulkImportService bulkImportService;
    private final StagingImportService stagingImportService;
    private final boolean devEnabled;

    public AdminController(ReportsService reportsService, EmployeeRepository employeeRepository, BulkImportService bulkImportService, StagingImportService stagingImportService, @Value("${app.dev.enabled:false}") boolean devEnabled) {
        this.reportsService = reportsService;
        this.employeeRepository = employeeRepository;
        this.bulkImportService = bulkImportService;
        this.stagingImportService = stagingImportService;
        this.devEnabled = devEnabled;
    }

    @PostMapping("/seed/run")
    public ResponseEntity<?> runSeed() {
        if (!devEnabled) return ResponseEntity.status(403).body("dev seed disabled");
        // instruct user to run SQL V2 manually; we can also execute SQL here if needed.
        return ResponseEntity.ok("Run SQL seed manually or enable dev seed to run programmatically");
    }

    @PostMapping(value = "/recognitions/bulk-upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> bulkUpload(@RequestParam(value = "file", required = false) MultipartFile file,
                                        HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            java.util.List<String> partNames = new java.util.ArrayList<>();
            int partCount = 0;
            try {
                for (Part p : request.getParts()) { partNames.add(p.getName()); partCount++; }
            } catch (Exception ex) {
                log.warn("Could not enumerate parts: {}", ex.getMessage());
            }
            log.warn("Missing file part. contentType={}, paramNames={}, partNames={}", request.getContentType(), request.getParameterMap().keySet(), partNames);
            return ResponseEntity.status(400).body(java.util.Map.of(
                    "timestamp", java.time.Instant.now().toString(),
                    "status", 400,
                    "error", "MISSING_MULTIPART_PART",
                    "message", "Required multipart part 'file' is missing",
                    "path", "/admin/recognitions/bulk-upload",
                    "hint", "In Postman set Body=form-data, key 'file', Type=File, choose CSV. In curl: curl -F file=@tmp/sample_import.csv http://localhost:8080/admin/recognitions/bulk-upload",
                    "contentType", request.getContentType(),
                    "receivedParts", partNames,
                    "partCount", partCount,
                    "parameters", request.getParameterMap().keySet()
            ));
        }
        try {
            BulkImportService.ImportResult res = bulkImportService.importCsv(file);
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping(value = "/recognitions/bulk-import-copy", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> bulkImportCopy(@RequestParam(value = "file", required = false) MultipartFile file,
                                            HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            java.util.List<String> partNames = new java.util.ArrayList<>();
            int partCount = 0;
            try { for (Part p : request.getParts()) { partNames.add(p.getName()); partCount++; } } catch (Exception ex) { log.warn("Could not enumerate parts: {}", ex.getMessage()); }
            log.warn("Missing file part bulk-import-copy. contentType={}, paramNames={}, partNames={}", request.getContentType(), request.getParameterMap().keySet(), partNames);
            return ResponseEntity.status(400).body(java.util.Map.of(
                    "timestamp", java.time.Instant.now().toString(),
                    "status", 400,
                    "error", "MISSING_MULTIPART_PART",
                    "message", "Required multipart part 'file' is missing",
                    "path", "/admin/recognitions/bulk-import-copy",
                    "hint", "Use form-data key 'file' (Type=File). curl example: curl -F file=@tmp/sample_import.csv http://localhost:8080/admin/recognitions/bulk-import-copy",
                    "contentType", request.getContentType(),
                    "receivedParts", partNames,
                    "partCount", partCount,
                    "parameters", request.getParameterMap().keySet()
            ));
        }
        try {
            Map<String, Object> res = new java.util.HashMap<>(stagingImportService.importCsvViaCopy(file, file.getOriginalFilename()));
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @PostMapping(value = "/imports", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> startImport(@RequestParam(value = "file", required = false) MultipartFile file,
                                         HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            java.util.List<String> partNames = new java.util.ArrayList<>();
            int partCount = 0;
            try { for (Part p : request.getParts()) { partNames.add(p.getName()); partCount++; } } catch (Exception ex) { log.warn("Could not enumerate parts: {}", ex.getMessage()); }
            log.warn("Missing file part async import. contentType={}, paramNames={}, partNames={}", request.getContentType(), request.getParameterMap().keySet(), partNames);
            return ResponseEntity.status(400).body(java.util.Map.of(
                    "timestamp", java.time.Instant.now().toString(),
                    "status", 400,
                    "error", "MISSING_MULTIPART_PART",
                    "message", "Required multipart part 'file' is missing",
                    "path", "/admin/imports",
                    "hint", "Use form-data key 'file'. curl: curl -F file=@tmp/sample_import.csv http://localhost:8080/admin/imports",
                    "contentType", request.getContentType(),
                    "receivedParts", partNames,
                    "partCount", partCount,
                    "parameters", request.getParameterMap().keySet()
            ));
        }
        try {
            Map<String, Object> res = stagingImportService.startImportViaCopyAsync(file, file.getOriginalFilename());
            // Return 202 Accepted with jobId
            return ResponseEntity.accepted().body(res);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    // New endpoint: get import job status by jobId
    @GetMapping("/imports/{jobId}")
    public ResponseEntity<?> getImportJob(@PathVariable Long jobId) {
        try {
            return ResponseEntity.ok(stagingImportService.getImportJobStatus(jobId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/imports/{jobId}/errors")
    public ResponseEntity<?> getImportErrors(@PathVariable Long jobId,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "50") int size) {
        try {
            return ResponseEntity.ok(stagingImportService.getImportErrorsPaged(jobId, page, size));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @GetMapping("/imports/{jobId}/errors/csv")
    public ResponseEntity<?> downloadImportErrorsCsv(@PathVariable Long jobId) {
        try {
            Path tmp = stagingImportService.exportImportErrorsCsvToTemp(jobId);
            org.springframework.core.io.Resource res = new org.springframework.core.io.PathResource(tmp);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=import_errors_" + jobId + ".csv")
                    .body(res);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
