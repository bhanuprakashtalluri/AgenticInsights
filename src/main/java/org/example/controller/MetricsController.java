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
        Instant from = to.minusSeconds(days == null || days <= 0 ? 30L*24*3600 : days*24*3600);
        Map<String, Object> raw = insightsService.generateInsights(from, to);
        // Build structured response
        Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("window", java.util.Map.of("from", from.toString(), "to", to.toString()));
        // Totals
        Map<String, Object> totals = new java.util.LinkedHashMap<>();
        totals.put("count", raw.getOrDefault("totalRecognitions", 0));
        totals.put("points", raw.getOrDefault("totalAwardPoints", 0));
        resp.put("totals", totals);
        // Statuses
        Map<String, Object> statuses = new java.util.LinkedHashMap<>();
        statuses.put("approved", raw.getOrDefault("approvedCount", 0));
        statuses.put("rejected", raw.getOrDefault("rejectedCount", 0));
        statuses.put("pending", raw.getOrDefault("pendingCount", 0));
        statuses.put("approvalRatePercent", raw.getOrDefault("approvalRate", 0.0));
        resp.put("statuses", statuses);
        // Series
        @SuppressWarnings("unchecked")
        Map<String, Integer> perDay = (Map<String, Integer>) raw.getOrDefault("timeSeriesByDay", java.util.Collections.emptyMap());
        resp.put("series", java.util.Map.of("daily", perDay));
        // Leaderboards
        Map<String, Object> boards = new java.util.LinkedHashMap<>();
        boards.put("topSenders", raw.getOrDefault("topSenders", java.util.Collections.emptyMap()));
        boards.put("topRecipients", raw.getOrDefault("topRecipients", java.util.Collections.emptyMap()));
        resp.put("leaderboards", boards);
        // Points distribution
        resp.put("pointsDistribution", raw.getOrDefault("pointsDistribution", java.util.Collections.emptyMap()));
        // Role aggregates
        Map<String, Object> roles = new java.util.LinkedHashMap<>();
        roles.put("sendersByRole", raw.getOrDefault("sendersByRole", java.util.Collections.emptyMap()));
        roles.put("recipientsByRole", raw.getOrDefault("recipientsByRole", java.util.Collections.emptyMap()));
        resp.put("roles", roles);
        // Manager summaries
        resp.put("managers", raw.getOrDefault("managerSummaries", java.util.Collections.emptyMap()));
        return resp;
    }
}
