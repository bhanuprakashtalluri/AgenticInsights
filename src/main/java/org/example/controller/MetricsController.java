package org.example.controller;

import org.example.service.AIInsightsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/metrics")
public class MetricsController {

    private final AIInsightsService insightsService;

    public MetricsController(AIInsightsService insightsService) {
        this.insightsService = insightsService;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(@RequestParam(required = false) Long days) {
        Instant to = Instant.now();
        Instant from = to.minusSeconds(days == null ? 30L*24*3600 : days*24*3600);
        return insightsService.generateInsights(from, to);
    }
}
