package org.example.controller;

import org.example.service.DevModeService;
import org.example.service.DataImportService;
import org.example.service.DataExportService;
import org.example.service.FileStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.flywaydb.core.Flyway;

@RestController
@RequestMapping("/admin")
public class AdminController {
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final DevModeService devModeService;
    private final DataImportService dataImportService;
    private final DataExportService dataExportService;
    private final FileStorageService fileStorageService;

    private static final String CSV_DIR = "artifacts/exports/csv/";
    private static final String JSON_DIR = "artifacts/exports/json/";
    private static final String TOON_DIR = "artifacts/exports/toon/";
    private static String timestampedName(String base, String ext) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd-MM-yyyy-HH.mm");
        String ts = LocalDateTime.now().format(fmt);
        return base + "-" + ts + (ext != null ? "." + ext : "");
    }

    public AdminController(DevModeService devModeService,
                           DataImportService dataImportService,
                           DataExportService dataExportService,
                           FileStorageService fileStorageService) {
        this.devModeService = devModeService;
        this.dataImportService = dataImportService;
        this.dataExportService = dataExportService;
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/seed/run")
    public ResponseEntity<?> runSeed() {
        if (!devModeService.isEnabled()) return ResponseEntity.status(403).body(Map.of(
                "status", 403,
                "error", "DEV_DISABLED",
                "message", "dev seed disabled"
        ));
        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "Run SQL seed manually or enable dev seed to run programmatically"
        ));
    }

    @GetMapping("/dev-mode")
    public ResponseEntity<?> getDevMode() {
        return ResponseEntity.ok(Map.of(
                "enabled", devModeService.isEnabled(),
                "mode", devModeService.getMode()
        ));
    }

    @PatchMapping("/dev-mode")
    public ResponseEntity<?> setDevMode(@RequestBody Map<String, Object> body) {
        Object val = body.get("enabled");
        if (val == null) return ResponseEntity.badRequest().body(Map.of(
                "status", 400,
                "error", "MISSING_FIELD",
                "message", "Body must include 'enabled' boolean"
        ));
        if (!(val instanceof Boolean)) return ResponseEntity.badRequest().body(Map.of(
                "status", 400,
                "error", "INVALID_FIELD_TYPE",
                "message", "'enabled' must be boolean"
        ));
        boolean newVal = (Boolean) val;
        devModeService.setEnabled(newVal);
        return ResponseEntity.ok(Map.of(
                "status", 200,
                "enabled", newVal,
                "mode", devModeService.getMode()
        ));
    }

    @GetMapping(value = "/export")
    public ResponseEntity<?> exportFile(@RequestParam("format") String format) {
        try {
            byte[] out;
            String contentType;
            String ext;
            switch (format.toLowerCase(java.util.Locale.ROOT)) {
                case "csv":
                    out = dataExportService.exportCombinedCsv();
                    contentType = "text/csv";
                    ext = "csv";
                    break;
                case "json":
                    out = dataExportService.exportCombinedJson();
                    contentType = "application/json";
                    ext = "json";
                    break;
                case "toon":
                    out = dataExportService.exportCombinedToon();
                    contentType = "text/plain";
                    ext = "toon";
                    break;
                default:
                    return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", "Unsupported format: " + format));
            }
            String fname = "data_combined_export-" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy-HH.mm")) + "." + ext;
            return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=" + fname)
                .header("Content-Type", contentType)
                .body(out);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importFile(@RequestParam("file") MultipartFile file,
                                        @RequestParam("format") String format) {
        try {
            Map<String, Object> result;
            switch (format.toLowerCase(java.util.Locale.ROOT)) {
                case "csv":
                    result = dataImportService.importCombinedCsv(file);
                    break;
                case "json":
                    String json = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    result = dataImportService.importCombinedJson(json);
                    break;
                case "toon":
                    String toon = new String(file.getBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    result = dataImportService.importCombinedToon(toon);
                    break;
                default:
                    return ResponseEntity.badRequest().body(Map.of("status", "ERROR", "error", "Unsupported format: " + format));
            }
            return ResponseEntity.ok(Map.of("status", "OK", "result", result));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status", "ERROR", "error", e.getMessage()));
        }
    }
}
