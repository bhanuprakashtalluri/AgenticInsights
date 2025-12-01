package org.example.controller;

import org.example.service.AIInsightsService;
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

@RestController
@RequestMapping("/insights")
public class InsightsController {

    private final AIInsightsService insightsService;
    private final ChartService chartService;
    private final FileStorageService fileStorageService;

    public InsightsController(AIInsightsService insightsService, ChartService chartService, FileStorageService fileStorageService) {
        this.insightsService = insightsService;
        this.chartService = chartService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public Map<String, Object> globalInsights(@RequestParam(required = false) Long days) {
        Instant to = Instant.now();
        Instant from = to.minusSeconds(days == null || days <= 0 ? 30L*24*3600 : days*24*3600);
        return insightsService.generateInsights(from, to);
    }

    @GetMapping(value = "/graph.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> globalGraph(@RequestParam(required = false) Long days) throws Exception {
        long effectiveDays = (days == null || days <= 0) ? 30 : days;
        Instant to = Instant.now();
        Instant from = to.minusSeconds(effectiveDays * 24 * 3600);
        Map<String, Object> insights = insightsService.generateInsights(from, to);
        @SuppressWarnings("unchecked")
        Map<String, Integer> series = (Map<String, Integer>) insights.get("timeSeriesByDay");
        byte[] png = chartService.renderTimeSeriesChart(series, "Recognitions over time", "day", "count");
        // store graph
        String ts = fileStorageService.nowTimestamp();
        String filename = "graph_" + from.toString().replace(':','-') + "_to_" + to.toString().replace(':','-') + "_" + ts + ".png";
        fileStorageService.storeGraph(filename, png);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }

    @GetMapping("/employee/{employeeId}")
    public Map<String, Object> employeeInsights(@PathVariable Long employeeId, @RequestParam(required = false) Long days) {
        Instant to = Instant.now();
        Instant from = to.minusSeconds(days == null ? 30L*24*3600 : days*24*3600);
        return insightsService.generateInsightsForEmployee(employeeId, from, to);
    }

    @GetMapping("/unit/{unitId}")
    public Map<String, Object> unitInsights(@PathVariable Long unitId, @RequestParam(required = false) Long days) {
        Instant to = Instant.now();
        Instant from = to.minusSeconds(days == null ? 30L*24*3600 : days*24*3600);
        return insightsService.generateInsightsForUnit(unitId, from, to);
    }

    @GetMapping("/role")
    public Map<String, Object> insightsByRole(@RequestParam String role, @RequestParam(required = false) Long days) {
        long effectiveDays = (days == null || days <= 0) ? 30 : days;
        Instant to = Instant.now();
        Instant from = to.minusSeconds(effectiveDays * 24 * 3600);
        return insightsService.generateInsightsForRole(role, from, to);
    }

    @GetMapping(value = "/role/graph.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> graphByRole(@RequestParam String role, @RequestParam(required = false) Long days) throws Exception {
        long effectiveDays = (days == null || days <= 0) ? 30 : days;
        Instant to = Instant.now();
        Instant from = to.minusSeconds(effectiveDays * 24 * 3600);
        Map<String, Object> insights = insightsService.generateInsightsForRole(role, from, to);
        @SuppressWarnings("unchecked")
        Map<String, Integer> series = (Map<String, Integer>) insights.get("timeSeriesByDay");
        byte[] png = chartService.renderTimeSeriesChart(series, "Recognitions (" + role + ")", "day", "count");
        String ts = fileStorageService.nowTimestamp();
        String filename = "graph_role_" + role + "_" + from.toString().replace(':','-') + "_to_" + to.toString().replace(':','-') + "_" + ts + ".png";
        fileStorageService.storeGraph(filename, png);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }

    @GetMapping("/manager/{managerId}")
    public Map<String, Object> insightsByManager(@PathVariable Long managerId, @RequestParam(required = false) Long days) {
        long effectiveDays = (days == null || days <= 0) ? 30 : days;
        Instant to = Instant.now();
        Instant from = to.minusSeconds(effectiveDays * 24 * 3600);
        return insightsService.generateInsightsForManager(managerId, from, to);
    }

    @GetMapping(value = "/manager/{managerId}/graph.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> graphByManager(@PathVariable Long managerId, @RequestParam(required = false) Long days) throws Exception {
        long effectiveDays = (days == null || days <= 0) ? 30 : days;
        Instant to = Instant.now();
        Instant from = to.minusSeconds(effectiveDays * 24 * 3600);
        Map<String, Object> insights = insightsService.generateInsightsForManager(managerId, from, to);
        @SuppressWarnings("unchecked")
        Map<String, Integer> series = (Map<String, Integer>) insights.get("timeSeriesByDay");
        byte[] png = chartService.renderTimeSeriesChart(series, "Recognitions (manager:" + managerId + ")", "day", "count");
        String ts = fileStorageService.nowTimestamp();
        String filename = "graph_manager_" + managerId + "_" + from.toString().replace(':','-') + "_to_" + to.toString().replace(':','-') + "_" + ts + ".png";
        fileStorageService.storeGraph(filename, png);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }
}
