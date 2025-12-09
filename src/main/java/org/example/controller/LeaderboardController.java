package org.example.controller;

import org.example.dto.LeaderboardEntry;
import org.example.dto.LeaderboardPage;
import org.example.repository.EmployeeRepository;
import org.example.repository.RecognitionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/leaderboard")
public class LeaderboardController {

    private final EmployeeRepository employeeRepository;
    private final RecognitionRepository recognitionRepository;
    private static final Logger log = LoggerFactory.getLogger(LeaderboardController.class);

    public LeaderboardController(EmployeeRepository employeeRepository, RecognitionRepository recognitionRepository) {
        this.employeeRepository = employeeRepository;
        this.recognitionRepository = recognitionRepository;
    }

    @GetMapping("/top-senders")
    public LeaderboardPage topSenders(@RequestParam(defaultValue = "10") int size,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(required = false) Long days,
                                      @RequestParam(required = false) String role,
                                      @RequestParam(required = false) Long unitId,
                                      @RequestParam(required = false) Long managerId) {
        Instant to = Instant.now();
        Instant from = (days == null) ? Instant.EPOCH : to.minus(days, ChronoUnit.DAYS);
        String roleFilter = (role == null || role.equalsIgnoreCase("all")) ? null : role;
        Long unitIdFilter = (unitId == null) ? null : unitId;
        Long managerIdFilter = (managerId == null) ? null : managerId;
        int offset = page * size;
        log.info("Leaderboard/top-senders params: from={}, to={}, size={}, page={}, role={}, unitId={}, managerId={}, offset={}", from, to, size, page, roleFilter, unitIdFilter, managerIdFilter, offset);
        List<Object[]> raw = null;
        try {
            raw = recognitionRepository.topSendersNativePaged(from, to, roleFilter, unitIdFilter, managerIdFilter, size, offset);
            log.info("Leaderboard/top-senders raw SQL result: {} rows", raw != null ? raw.size() : 0);
        } catch (Exception e) {
            log.error("Leaderboard/top-senders SQL error: {}", e.getMessage(), e);
            throw e;
        }
        List<LeaderboardEntry> entries = raw.stream().map(arr -> {
            Long id = arr[0] == null ? null : ((Number) arr[0]).longValue();
            Long count = arr[1] == null ? 0L : ((Number) arr[1]).longValue();
            Integer points = arr[2] == null ? 0 : ((Number) arr[2]).intValue();
            String name = id == null ? "Unknown" : employeeRepository.findById(id)
                .map(e -> (e.getFirstName() != null ? e.getFirstName() : "") + (e.getLastName() != null ? (" " + e.getLastName()) : ""))
                .orElse("Unknown");
            return new LeaderboardEntry(id, name.trim(), count, points);
        }).toList();
        long total = recognitionRepository.countTopSendersNativeFiltered(from, to, roleFilter, unitIdFilter, managerIdFilter);
        log.info("Leaderboard/top-senders total: {}", total);
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
        Instant from = (days == null) ? Instant.EPOCH : to.minus(days, ChronoUnit.DAYS);
        String roleFilter = (role == null || role.equalsIgnoreCase("all")) ? null : role;
        Long unitIdFilter = (unitId == null) ? null : unitId;
        Long managerIdFilter = (managerId == null) ? null : managerId;
        int offset = page * size;
        log.info("Leaderboard/top-recipients params: from={}, to={}, size={}, page={}, role={}, unitId={}, managerId={}, offset={}", from, to, size, page, roleFilter, unitIdFilter, managerIdFilter, offset);
        List<Object[]> raw = null;
        try {
            raw = recognitionRepository.topRecipientsNativePaged(from, to, roleFilter, unitIdFilter, managerIdFilter, size, offset);
            log.info("Leaderboard/top-recipients raw SQL result: {} rows", raw != null ? raw.size() : 0);
        } catch (Exception e) {
            log.error("Leaderboard/top-recipients SQL error: {}", e.getMessage(), e);
            throw e;
        }
        List<LeaderboardEntry> entries = raw.stream().map(arr -> {
            Long id = arr[0] == null ? null : ((Number) arr[0]).longValue();
            Long count = arr[1] == null ? 0L : ((Number) arr[1]).longValue();
            Integer points = arr[2] == null ? 0 : ((Number) arr[2]).intValue();
            String name = id == null ? "Unknown" : employeeRepository.findById(id)
                .map(e -> (e.getFirstName() != null ? e.getFirstName() : "") + (e.getLastName() != null ? (" " + e.getLastName()) : ""))
                .orElse("Unknown");
            return new LeaderboardEntry(id, name.trim(), count, points);
        }).toList();
        long total = recognitionRepository.countTopRecipientsNativeFiltered(from, to, roleFilter, unitIdFilter, managerIdFilter);
        log.info("Leaderboard/top-recipients total: {}", total);
        return new LeaderboardPage(entries, page, size, total);
    }
}
