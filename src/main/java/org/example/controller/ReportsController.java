package org.example.controller;

import org.example.service.ReportsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reports")
public class ReportsController {

    private final ReportsService reportsService;
    private final String reportsPath;

    public ReportsController(ReportsService reportsService, @Value("${app.reports.path:./reports}") String reportsPath) {
        this.reportsService = reportsService;
        this.reportsPath = reportsPath;
    }

    @GetMapping("/list")
    public List<String> list() {
        File dir = new File(reportsPath);
        if (!dir.exists()) return List.of();
        return Arrays.stream(dir.listFiles()).map(File::getName).sorted().collect(Collectors.toList());
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<FileSystemResource> download(@PathVariable String filename) {
        File file = new File(reportsPath, filename);
        if (!file.exists()) return ResponseEntity.notFound().build();
        FileSystemResource resource = new FileSystemResource(file);
        MediaType mediaType = filename.endsWith(".png") ? MediaType.IMAGE_PNG : (filename.endsWith(".json") ? MediaType.APPLICATION_JSON : MediaType.parseMediaType("text/csv"));
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename).contentType(mediaType).body(resource);
    }

    @PostMapping("/generate-now")
    public ResponseEntity<?> generateNow(@RequestParam(required = false) String from,
                                         @RequestParam(required = false) String to,
                                         @RequestParam(defaultValue = "adhoc") String label) {
        try {
            Instant f = from == null ? Instant.now().minusSeconds(24*3600) : Instant.parse(from);
            Instant t = to == null ? Instant.now() : Instant.parse(to);
            String base = reportsService.generateReportNow(f, t, label);
            return ResponseEntity.ok().body(base);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
