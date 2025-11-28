package org.example.controller;

import org.example.dto.LeaderboardEntry;
import org.example.dto.LeaderboardPage;
import org.example.repository.EmployeeRepository;
import org.example.service.AIInsightsService;
import org.example.util.EntityMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/leaderboard")
public class LeaderboardController {

    private final AIInsightsService insightsService;
    private final EmployeeRepository employeeRepository;

    public LeaderboardController(AIInsightsService insightsService, EmployeeRepository employeeRepository) {
        this.insightsService = insightsService;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/top-senders")
    public LeaderboardPage topSenders(@RequestParam(defaultValue = "10") int size,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(required = false) Long days,
                                      @RequestParam(required = false) String role,
                                      @RequestParam(required = false) Long unitId,
                                      @RequestParam(required = false) Long managerId) {
        Instant to = Instant.now();
        Instant from = to.minusSeconds(days == null ? 30L * 24 * 3600 : days * 24 * 3600);
        Map<String, Object> raw = insightsService.getTopSendersPaged(page, size, from, to, role, unitId, managerId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) raw.get("items");
        long total = ((Number) raw.get("totalElements")).longValue();
        List<LeaderboardEntry> entries = items.stream().map(m -> {
            Long id = ((Number) m.get("id")).longValue();
            Long count = ((Number) m.get("count")).longValue();
            Integer points = m.get("points") == null ? 0 : ((Number) m.get("points")).intValue();
            String first = (String) m.get("firstName");
            String last = (String) m.get("lastName");
            String name = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
            if (name.isEmpty()) name = "(unknown)";
            return EntityMapper.toLeaderboardEntry(id, name, count, points);
        }).collect(Collectors.toList());
        return new LeaderboardPage(entries, page, size, total);
    }

    @GetMapping("/top-recipients")
    public LeaderboardPage topRecipients(@RequestParam(defaultValue = "10") int size,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(required = false) Long days,
                                         @RequestParam(required = false) String role,
                                         @RequestParam(required = false) Long unitId,
                                         @RequestParam(required = false) Long managerId) {
        Instant to = Instant.now();
        Instant from = to.minusSeconds(days == null ? 30L * 24 * 3600 : days * 24 * 3600);
        Map<String, Object> raw = insightsService.getTopRecipientsPaged(page, size, from, to, role, unitId, managerId);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) raw.get("items");
        long total = ((Number) raw.get("totalElements")).longValue();
        List<LeaderboardEntry> entries = items.stream().map(m -> {
            Long id = ((Number) m.get("id")).longValue();
            Long count = ((Number) m.get("count")).longValue();
            Integer points = m.get("points") == null ? 0 : ((Number) m.get("points")).intValue();
            String first = (String) m.get("firstName");
            String last = (String) m.get("lastName");
            String name = ((first == null ? "" : first) + " " + (last == null ? "" : last)).trim();
            if (name.isEmpty()) name = "(unknown)";
            return EntityMapper.toLeaderboardEntry(id, name, count, points);
        }).collect(Collectors.toList());
        return new LeaderboardPage(entries, page, size, total);
    }
}
