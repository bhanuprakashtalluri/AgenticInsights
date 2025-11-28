package org.example.controller;

import org.example.service.AIInsightsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/insights")
public class InsightsController {

    private final AIInsightsService insightsService;

    public InsightsController(AIInsightsService insightsService) {
        this.insightsService = insightsService;
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

    @GetMapping("/cohort")
    public Map<String, Object> cohortInsights(@RequestParam(required = false) Long days) {
        Instant to = Instant.now();
        Instant from = to.minusSeconds(days == null ? 30L*24*3600 : days*24*3600);
        // example buckets: 0-90,91-365,366-9999
        java.util.List<int[]> buckets = java.util.List.of(new int[]{0,90}, new int[]{91,365}, new int[]{366,9999});
        return insightsService.generateCohortInsights(buckets, from, to);
    }
}
