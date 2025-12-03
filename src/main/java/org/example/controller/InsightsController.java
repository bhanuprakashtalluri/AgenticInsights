package org.example.controller;

import org.example.service.ChartService;
import org.example.service.FileStorageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/insights")
public class InsightsController {

    private final ChartService chartService;
    private final FileStorageService fileStorageService;

    public InsightsController(ChartService chartService, FileStorageService fileStorageService) {
        this.chartService = chartService;
        this.fileStorageService = fileStorageService;
    }

    // Helper method for 'all' check
    private boolean isAll(Object param) {
        if (param == null) return false;
        if (param instanceof String s) return s.equalsIgnoreCase("all");
        return false;
    }

    @GetMapping
    public Map<String, Object> globalInsights(@RequestParam(required = false) Long days) {
        return java.util.Collections.emptyMap();
    }

    @GetMapping(value = "/graph.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> globalGraph(
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
            @RequestParam(required = false) String timeframe, // days, weeks, months, years
            @RequestParam(required = false) Integer iterations // e.g. 10 weeks
    ) throws Exception {
        // 1. Query recognitions (simulate with empty data for now)
        java.util.Map<String, Integer> timeSeries = new java.util.LinkedHashMap<>();
        // TODO: Replace with real DB query and aggregation logic
        // Example: timeSeries.put("2025-11-01", 5); timeSeries.put("2025-11-08", 8);
        // 2. Generate chart
        String title = "Recognitions";
        String xLabel = timeframe == null ? "date" : timeframe;
        String yLabel = "count";
        byte[] png = chartService.renderTimeSeriesChart(timeSeries, title, xLabel, yLabel);
        // 3. Store the file
        String fname = "recognition_graph-" + java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy-HH.mm")) + ".png";
        fileStorageService.storeGraph(fname, png);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }

    @GetMapping("/employee/{employeeId}")
    public Map<String, Object> employeeInsights(@PathVariable Long employeeId, @RequestParam(required = false) Long days) {
        return java.util.Collections.emptyMap();
    }

    @GetMapping("/unit/{unitId}")
    public Map<String, Object> unitInsights(@PathVariable Long unitId, @RequestParam(required = false) Long days) {
        return java.util.Collections.emptyMap();
    }

    @GetMapping("/role")
    public Map<String, Object> insightsByRole(@RequestParam String role, @RequestParam(required = false) Long days) {
        return java.util.Collections.emptyMap();
    }

    @GetMapping(value = "/role/graph.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> graphByRole(@RequestParam String role, @RequestParam(required = false) Long days) throws Exception {
        byte[] png = new byte[0];
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }

    @GetMapping("/manager/{managerId}")
    public Map<String, Object> insightsByManager(@PathVariable Long managerId, @RequestParam(required = false) Long days) {
        return java.util.Collections.emptyMap();
    }

    @GetMapping(value = "/manager/{managerId}/graph.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> graphByManager(@PathVariable Long managerId, @RequestParam(required = false) Long days) throws Exception {
        byte[] png = new byte[0];
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }
}
