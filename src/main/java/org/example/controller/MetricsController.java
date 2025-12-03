package org.example.controller;

import org.example.model.Recognition;
import org.example.repository.RecognitionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/metrics")
public class MetricsController {

    private final RecognitionRepository recognitionRepository;
    private static final Logger log = LoggerFactory.getLogger(MetricsController.class);

    public MetricsController(RecognitionRepository recognitionRepository) {
        this.recognitionRepository = recognitionRepository;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(@RequestParam(required = false) Long days) {
        Instant to = Instant.now();
        Instant from = (days == null) ? Instant.EPOCH : to.minusSeconds(days*24*3600);
        List<Recognition> recognitions = recognitionRepository.findAllBetween(from, to);
        log.info("Fetched {} recognitions for metrics window from {} to {}", recognitions.size(), from, to);
        if (!recognitions.isEmpty()) {
            recognitions.stream().limit(5).forEach(r -> log.info("Recognition: id={}, sentAt={}, status={}, points={}", r.getId(), r.getSentAt(), r.getApprovalStatus(), r.getAwardPoints()));
        }
        // Totals
        int totalRecognitions = recognitions.size();
        int totalAwardPoints = recognitions.stream().mapToInt(r -> r.getAwardPoints() != null ? r.getAwardPoints() : 0).sum();
        // Statuses
        long approvedCount = recognitions.stream().filter(r -> "APPROVED".equalsIgnoreCase(r.getApprovalStatus())).count();
        long rejectedCount = recognitions.stream().filter(r -> "REJECTED".equalsIgnoreCase(r.getApprovalStatus())).count();
        long pendingCount = recognitions.stream().filter(r -> "PENDING".equalsIgnoreCase(r.getApprovalStatus())).count();
        double approvalRate = totalRecognitions == 0 ? 0.0 : (double) approvedCount / totalRecognitions * 100.0;
        // Series (daily)
        java.util.Map<String, Integer> timeSeriesByDay = new java.util.HashMap<>();
        java.time.ZoneId zone = java.time.ZoneOffset.UTC;
        for (Recognition r : recognitions) {
            if (r.getSentAt() != null) {
                String day = r.getSentAt().atZone(zone).toLocalDate().toString();
                timeSeriesByDay.put(day, timeSeriesByDay.getOrDefault(day, 0) + 1);
            }
        }
        // Leaderboards
        java.util.Map<Long, Integer> topSenders = new java.util.HashMap<>();
        java.util.Map<Long, Integer> topRecipients = new java.util.HashMap<>();
        for (Recognition r : recognitions) {
            if (r.getSenderId() != null) topSenders.put(r.getSenderId(), topSenders.getOrDefault(r.getSenderId(), 0) + 1);
            if (r.getRecipientId() != null) topRecipients.put(r.getRecipientId(), topRecipients.getOrDefault(r.getRecipientId(), 0) + 1);
        }
        // Points distribution
        java.util.Map<Integer, Integer> pointsDistribution = new java.util.HashMap<>();
        for (Recognition r : recognitions) {
            int pts = r.getAwardPoints() != null ? r.getAwardPoints() : 0;
            pointsDistribution.put(pts, pointsDistribution.getOrDefault(pts, 0) + 1);
        }
        // Role aggregates
        java.util.Map<String, Integer> sendersByRole = new java.util.HashMap<>();
        java.util.Map<String, Integer> recipientsByRole = new java.util.HashMap<>();
        for (Recognition r : recognitions) {
            if (r.getSender() != null && r.getSender().getRole() != null)
                sendersByRole.put(r.getSender().getRole(), sendersByRole.getOrDefault(r.getSender().getRole(), 0) + 1);
            if (r.getRecipient() != null && r.getRecipient().getRole() != null)
                recipientsByRole.put(r.getRecipient().getRole(), recipientsByRole.getOrDefault(r.getRecipient().getRole(), 0) + 1);
        }
        // Manager summaries
        java.util.Map<Long, Integer> managerSummaries = new java.util.HashMap<>();
        for (Recognition r : recognitions) {
            if (r.getRecipient() != null && r.getRecipient().getManagerId() != null)
                managerSummaries.put(r.getRecipient().getManagerId(), managerSummaries.getOrDefault(r.getRecipient().getManagerId(), 0) + 1);
        }
        // Build structured response
        Map<String, Object> resp = new java.util.LinkedHashMap<>();
        resp.put("window", java.util.Map.of("from", from.toString(), "to", to.toString()));
        resp.put("totals", java.util.Map.of("count", totalRecognitions, "points", totalAwardPoints));
        resp.put("statuses", java.util.Map.of(
            "approved", approvedCount,
            "rejected", rejectedCount,
            "pending", pendingCount,
            "approvalRatePercent", approvalRate
        ));
        resp.put("series", java.util.Map.of("daily", timeSeriesByDay));
        resp.put("leaderboards", java.util.Map.of(
            "topSenders", topSenders,
            "topRecipients", topRecipients
        ));
        resp.put("pointsDistribution", pointsDistribution);
        resp.put("roles", java.util.Map.of(
            "sendersByRole", sendersByRole,
            "recipientsByRole", recipientsByRole
        ));
        resp.put("managers", managerSummaries);
        return resp;
    }

    @GetMapping("/status")
    public Map<String, Object> statusUp() {
        return Map.of("status", "UP", "timestamp", java.time.Instant.now().toString());
    }

    @GetMapping("/verify-script")
    public Map<String, Object> verifyScript() {
        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/bash", "./docs/curl_examples.sh");
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            java.io.InputStream is = proc.getInputStream();
            StringBuilder sb = new StringBuilder();
            try (java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A")) {
                while (s.hasNext()) sb.append(s.next());
            }
            int exit = proc.waitFor();
            return Map.of(
                "status", exit == 0 ? "OK" : "ERROR",
                "exitCode", exit,
                "output", sb.toString()
            );
        } catch (Exception e) {
            return Map.of(
                "status", "ERROR",
                "error", e.getMessage()
            );
        }
    }
}
