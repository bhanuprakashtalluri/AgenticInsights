package org.example.controller;

import org.example.service.DevModeService;
import org.example.service.DataImportService;
import org.example.service.DataExportService;
import org.example.service.FileStorageService;
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

    private final DevModeService devModeService;
    private final DataImportService dataImportService;
    private final DataExportService dataExportService;
    private final FileStorageService fileStorageService;

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

    @PostMapping(value = "/data/upload", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadData(@RequestParam(required = false) MultipartFile combined,
                                        @RequestParam(required = false) MultipartFile employees,
                                        @RequestParam(required = false) MultipartFile recognition_types,
                                        @RequestParam(required = false) MultipartFile recognitions,
                                        HttpServletRequest request) {
        try {
            if (combined == null && employees == null && recognition_types == null && recognitions == null) {
                return ResponseEntity.badRequest().body(Map.of("error","MISSING_FILE","message","Provide 'combined' or any of 'employees','recognition_types','recognitions' multipart files"));
            }
            Map<String,Object> result = new java.util.LinkedHashMap<>();
            if (combined != null && !combined.isEmpty()) {
                result.put("combined", dataImportService.importCombinedCsv(combined));
                // store sample copy
                fileStorageService.storeSample(combined.getOriginalFilename() == null ? "combined.csv" : combined.getOriginalFilename(), combined.getBytes());
            }
            if (employees != null && !employees.isEmpty()) {
                result.put("employees", dataImportService.importEmployeesCsv(employees));
                fileStorageService.storeSample(employees.getOriginalFilename() == null ? "employees.csv" : employees.getOriginalFilename(), employees.getBytes());
            }
            if (recognition_types != null && !recognition_types.isEmpty()) {
                result.put("recognition_types", dataImportService.importRecognitionTypesCsv(recognition_types));
                fileStorageService.storeSample(recognition_types.getOriginalFilename() == null ? "recognition_types.csv" : recognition_types.getOriginalFilename(), recognition_types.getBytes());
            }
            if (recognitions != null && !recognitions.isEmpty()) {
                result.put("recognitions", dataImportService.importRecognitionsCsv(recognitions));
                fileStorageService.storeSample(recognitions.getOriginalFilename() == null ? "recognitions.csv" : recognitions.getOriginalFilename(), recognitions.getBytes());
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error","UPLOAD_FAILED","message", e.getMessage()));
        }
    }

    @GetMapping(value = "/data/download", produces = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<?> downloadData(@RequestParam(defaultValue = "combined") String format) {
        try {
            byte[] bytes;
            String filename;
            switch (format.toLowerCase(java.util.Locale.ROOT)) {
                case "employees": bytes = dataExportService.exportEmployeesCsv(); filename = "employees.csv"; break;
                case "recognition_types": bytes = dataExportService.exportRecognitionTypesCsv(); filename = "recognition_types.csv"; break;
                case "recognitions": bytes = dataExportService.exportRecognitionsCsv(); filename = "recognitions.csv"; break;
                default: bytes = dataExportService.exportCombinedCsv(); filename = "data_combined.csv";
            }
            return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=" + filename).body(bytes);
        } catch (java.io.IOException e) {
            return ResponseEntity.status(500).body(Map.of("error","DOWNLOAD_FAILED","message", e.getMessage()));
        }
    }

    @PostMapping("/snapshot")
    public ResponseEntity<?> runSnapshot() {
        if (!devModeService.isEnabled()) return ResponseEntity.status(403).body(Map.of(
                "status", 403,
                "error", "DEV_DISABLED",
                "message", "dev mode must be enabled to run snapshots"
        ));
        try {
            String script = "./scripts/snapshot_db.sh";
            ProcessBuilder pb = new ProcessBuilder(script);
            pb.environment().putAll(System.getenv());
            pb.directory(new java.io.File(System.getProperty("user.dir")));
            pb.redirectErrorStream(true);
            Process p = pb.start();
            java.io.InputStream is = p.getInputStream();
            java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
            String out = s.hasNext() ? s.next() : "";
            int code = p.waitFor();
            if (code != 0) {
                return ResponseEntity.status(500).body(Map.of(
                        "status", 500,
                        "error", "SNAPSHOT_FAILED",
                        "message", "script exited with code " + code,
                        "detail", out
                ));
            }
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "snapshot completed",
                    "output", out
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status",500,"error","SNAPSHOT_ERROR","message",e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> runVerification() {
        if (!devModeService.isEnabled()) return ResponseEntity.status(403).body(Map.of(
                "status", 403,
                "error", "DEV_DISABLED",
                "message", "dev mode must be enabled to run verification"
        ));
        try {
            String script = "./scripts/verify_all_endpoints.sh";
            ProcessBuilder pb = new ProcessBuilder(script);
            pb.environment().putAll(System.getenv());
            pb.directory(new java.io.File(System.getProperty("user.dir")));
            pb.redirectErrorStream(true);
            Process p = pb.start();
            java.io.InputStream is = p.getInputStream();
            java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
            String out = s.hasNext() ? s.next() : "";
            int code = p.waitFor();
            if (code != 0) {
                return ResponseEntity.status(500).body(Map.of(
                        "status", 500,
                        "error", "VERIFY_FAILED",
                        "message", "script exited with code " + code,
                        "detail", out
                ));
            }
            return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "verification completed",
                    "output", out
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("status",500,"error","VERIFY_ERROR","message",e.getMessage()));
        }
    }
}
