package org.example.controller;

import org.example.service.ReportsService;
import org.example.service.FileStorageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reports")
public class ReportsController {

    private final ReportsService reportsService;
    private final FileStorageService storage;

    public ReportsController(ReportsService reportsService, FileStorageService storage) {
        this.reportsService = reportsService;
        this.storage = storage;
    }

    @GetMapping("/list")
    public List<String> list() {
        try {
            Path base = storage.getReportsDirForToday().getParent(); // artifacts/reports
            if (base == null) return List.of();
            if (!Files.exists(base)) return List.of();
            List<Path> files = new ArrayList<>();
            try (var stream = Files.walk(base, 2)) {
                stream.filter(Files::isRegularFile).forEach(files::add);
            }
            return files.stream().map(p -> base.relativize(p).toString()).sorted().collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    @GetMapping("/download/{filename:.+}")
    public ResponseEntity<FileSystemResource> download(@PathVariable String filename) {
        try {
            Path base = storage.getReportsDirForToday().getParent();
            if (!Files.exists(base)) return ResponseEntity.notFound().build();
            // search for filename under artifacts/reports recursively (depth 2)
            try (var stream = Files.walk(base, 2)) {
                for (Path p : (Iterable<Path>) stream::iterator) {
                    if (Files.isRegularFile(p) && p.getFileName().toString().equals(filename)) {
                        FileSystemResource resource = new FileSystemResource(p.toFile());
                        MediaType mediaType = filename.endsWith(".png") ? MediaType.IMAGE_PNG : (filename.endsWith(".json") ? MediaType.APPLICATION_JSON : MediaType.parseMediaType("text/csv"));
                        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename).contentType(mediaType).body(resource);
                    }
                }
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
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
