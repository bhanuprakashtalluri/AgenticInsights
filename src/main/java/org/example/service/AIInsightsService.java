package org.example.service;

import org.example.model.Employee;
import org.example.model.Recognition;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AIInsightsService {

    private final org.example.repository.RecognitionRepository recognitionRepository;
    private final org.example.repository.EmployeeRepository employeeRepository;

    public AIInsightsService(org.example.repository.RecognitionRepository recognitionRepository, org.example.repository.EmployeeRepository employeeRepository) {
        this.recognitionRepository = recognitionRepository;
        this.employeeRepository = employeeRepository;
    }

    /**
     * Generate heuristic insights between two instants (inclusive).
     */
    public Map<String, Object> generateInsights(Instant from, Instant to) {
        Map<String, Object> out = new HashMap<>();
        List<org.example.repository.RecognitionLite> recsLite = recognitionRepository.findAllLiteBetween(from, to);
        out.put("totalRecognitions", recsLite.size());
        int totalPoints = recsLite.stream().mapToInt(r -> r.getAwardPoints() == null ? 0 : r.getAwardPoints()).sum();
        out.put("totalAwardPoints", totalPoints);
        // Top senders and recipients overall (skip null ids to avoid null map keys)
        Map<Long, Long> topSenders = recsLite.stream()
                .filter(r -> r.getSenderId() != null)
                .collect(Collectors.groupingBy(org.example.repository.RecognitionLite::getSenderId, Collectors.counting()));
        Map<Long, Long> topRecipients = recsLite.stream()
                .filter(r -> r.getRecipientId() != null)
                .collect(Collectors.groupingBy(org.example.repository.RecognitionLite::getRecipientId, Collectors.counting()));
        out.put("topSenders", topSenders);
        out.put("topRecipients", topRecipients);
        // Approval stats
        long approved = recsLite.stream().filter(r -> "APPROVED".equalsIgnoreCase(r.getApprovalStatus())).count();
        long rejected = recsLite.stream().filter(r -> "REJECTED".equalsIgnoreCase(r.getApprovalStatus())).count();
        long pending = recsLite.stream().filter(r -> "PENDING".equalsIgnoreCase(r.getApprovalStatus())).count();
        out.put("approvedCount", approved);
        out.put("rejectedCount", rejected);
        out.put("pendingCount", pending);
        out.put("approvalRate", recsLite.isEmpty() ? 0.0 : (approved * 100.0 / recsLite.size()));
        // Points distribution
        Map<String, Integer> buckets = new TreeMap<>();
        buckets.put("0-10", 0);
        buckets.put("11-50", 0);
        buckets.put("51-100", 0);
        buckets.put(">100", 0);
        for (org.example.repository.RecognitionLite r : recsLite) {
            int p = r.getAwardPoints() == null ? 0 : r.getAwardPoints();
            if (p <= 10) buckets.put("0-10", buckets.get("0-10") + 1);
            else if (p <= 50) buckets.put("11-50", buckets.get("11-50") + 1);
            else if (p <= 100) buckets.put("51-100", buckets.get("51-100") + 1);
            else buckets.put(">100", buckets.get(">100") + 1);
        }
        out.put("pointsDistribution", buckets);
        // Time series by day
        Map<String, Integer> perDay = new TreeMap<>();
        java.time.ZoneId zid = java.time.ZoneId.of("UTC");
        for (org.example.repository.RecognitionLite r : recsLite) {
            if (r.getSentAt() == null) continue;
            String day = r.getSentAt().atZone(zid).toLocalDate().toString();
            perDay.put(day, perDay.getOrDefault(day, 0) + 1);
        }
        out.put("timeSeriesByDay", perDay);
        // Aggregates by role
        Map<String, Long> sendersByRole = new HashMap<>();
        Map<String, Long> recipientsByRole = new HashMap<>();
        Map<Long, String> employeeRoles = new HashMap<>();
        for (Employee e : employeeRepository.findAll()) {
            if (e.getId() != null) employeeRoles.put(e.getId(), e.getRole() == null ? "employee" : e.getRole());
        }
        for (org.example.repository.RecognitionLite r : recsLite) {
            Long sid = r.getSenderId();
            Long rid = r.getRecipientId();
            if (sid != null) {
                String srole = employeeRoles.getOrDefault(sid, "employee");
                sendersByRole.put(srole, sendersByRole.getOrDefault(srole, 0L) + 1);
            }
            if (rid != null) {
                String rrole = employeeRoles.getOrDefault(rid, "employee");
                recipientsByRole.put(rrole, recipientsByRole.getOrDefault(rrole, 0L) + 1);
            }
        }
        out.put("sendersByRole", sendersByRole);
        out.put("recipientsByRole", recipientsByRole);
        // Manager summaries
        Map<Long, Map<String, Object>> managerSummaries = new HashMap<>();
        Map<Long, List<Long>> reports = employeeRepository.findAll().stream()
                .collect(Collectors.groupingBy(e -> e.getManagerId() == null ? 0L : e.getManagerId(), Collectors.mapping(Employee::getId, Collectors.toList())));
        for (Map.Entry<Long, List<Long>> entry : reports.entrySet()) {
            Long managerId = entry.getKey();
            if (managerId == 0L) continue;
            List<Long> team = entry.getValue();
            long teamRecCount = recsLite.stream().filter(r -> r.getRecipientId() != null && team.contains(r.getRecipientId())).count();
            int teamPoints = recsLite.stream().filter(r -> r.getRecipientId() != null && team.contains(r.getRecipientId()))
                    .mapToInt(r -> r.getAwardPoints() == null ? 0 : r.getAwardPoints()).sum();
            Map<String, Object> summary = new HashMap<>();
            summary.put("teamSize", team.size());
            summary.put("teamRecognitions", teamRecCount);
            summary.put("teamPoints", teamPoints);
            managerSummaries.put(managerId, summary);
        }
        out.put("managerSummaries", managerSummaries);

        return out;
    }

    public Map<String, Object> generateInsightsForRole(String role, Instant from, Instant to) {
        List<Recognition> recs = recognitionRepository.findAllBetween(from, to).stream()
                .filter(r -> r.getRecipient() != null && role.equalsIgnoreCase(r.getRecipient().getRole()))
                .toList();
        Map<String, Object> base = new HashMap<>();
        base.put("role", role);
        base.put("count", recs.size());
        base.put("totalPoints", recs.stream().mapToInt(r -> r.getAwardPoints() == null ? 0 : r.getAwardPoints()).sum());
        Map<String, Long> byLevel = recs.stream()
                .filter(r -> r.getLevel() != null && !r.getLevel().isBlank())
                .collect(Collectors.groupingBy(Recognition::getLevel, Collectors.counting()));
        base.put("byLevel", byLevel);
        // time series for this role
        Map<String, Integer> perDay = new TreeMap<>();
        java.time.ZoneId zid = java.time.ZoneId.of("UTC");
        for (Recognition r : recs) {
            if (r.getSentAt() == null) continue;
            String day = r.getSentAt().atZone(zid).toLocalDate().toString();
            perDay.put(day, perDay.getOrDefault(day, 0) + 1);
        }
        base.put("timeSeriesByDay", perDay);
        return base;
    }

    public Map<String, Object> generateInsightsForManager(Long managerId, Instant from, Instant to) {
        List<Employee> reports = employeeRepository.findAll().stream().filter(e -> managerId.equals(e.getManagerId())).toList();
        List<Long> reportIds = reports.stream().map(Employee::getId).toList();
        List<Recognition> recs = recognitionRepository.findAllBetween(from, to).stream()
                .filter(r -> reportIds.contains(r.getRecipientId()))
                .toList();
        Map<String, Object> base = new HashMap<>();
        base.put("managerId", managerId);
        base.put("teamSize", reportIds.size());
        base.put("teamRecognitions", recs.size());
        base.put("teamPoints", recs.stream().mapToInt(r -> r.getAwardPoints() == null ? 0 : r.getAwardPoints()).sum());
        Map<String, Long> byLevel = recs.stream()
                .filter(r -> r.getLevel() != null && !r.getLevel().isBlank())
                .collect(Collectors.groupingBy(Recognition::getLevel, Collectors.counting()));
        base.put("byLevel", byLevel);
        Map<String, Integer> perDay = new TreeMap<>();
        java.time.ZoneId zid = java.time.ZoneId.of("UTC");
        for (Recognition r : recs) {
            if (r.getSentAt() == null) continue;
            String day = r.getSentAt().atZone(zid).toLocalDate().toString();
            perDay.put(day, perDay.getOrDefault(day, 0) + 1);
        }
        base.put("timeSeriesByDay", perDay);
        return base;
    }

    public Map<String, Object> generateInsightsForEmployee(Long employeeId, Instant from, Instant to) {
        List<Recognition> recs = recognitionRepository.findAllBetween(from, to).stream()
                .filter(r -> employeeId.equals(r.getRecipientId()) || employeeId.equals(r.getSenderId()))
                .toList();
        Map<String, Object> out = new HashMap<>();
        out.put("employeeId", employeeId);
        out.put("receivedCount", recs.stream().filter(r -> employeeId.equals(r.getRecipientId())).count());
        out.put("sentCount", recs.stream().filter(r -> employeeId.equals(r.getSenderId())).count());
        out.put("pointsReceived", recs.stream().filter(r -> employeeId.equals(r.getRecipientId())).mapToInt(r -> r.getAwardPoints() == null ? 0 : r.getAwardPoints()).sum());
        out.put("pointsSent", recs.stream().filter(r -> employeeId.equals(r.getSenderId())).mapToInt(r -> r.getAwardPoints() == null ? 0 : r.getAwardPoints()).sum());
        Map<String, Long> byLevel = recs.stream()
                .filter(r -> r.getLevel() != null && !r.getLevel().isBlank())
                .collect(Collectors.groupingBy(Recognition::getLevel, Collectors.counting()));
        out.put("byLevel", byLevel);
        Map<String, Integer> perDay = new TreeMap<>();
        java.time.ZoneId zid = java.time.ZoneId.of("UTC");
        for (Recognition r : recs) {
            if (r.getSentAt() == null) continue;
            String day = r.getSentAt().atZone(zid).toLocalDate().toString();
            perDay.put(day, perDay.getOrDefault(day, 0) + 1);
        }
        out.put("timeSeriesByDay", perDay);
        return out;
    }

    public Map<String, Object> generateInsightsForUnit(Long unitId, Instant from, Instant to) {
        List<Long> empIds = employeeRepository.findAllByUnitId(unitId).stream().map(Employee::getId).toList();
        List<Recognition> recs = recognitionRepository.findAllBetween(from, to).stream()
                .filter(r -> empIds.contains(r.getRecipientId()))
                .toList();
        Map<String, Object> out = new HashMap<>();
        out.put("unitId", unitId);
        out.put("count", recs.size());
        out.put("totalPoints", recs.stream().mapToInt(r -> r.getAwardPoints() == null ? 0 : r.getAwardPoints()).sum());
        Map<String, Long> byLevel = recs.stream()
                .filter(r -> r.getLevel() != null && !r.getLevel().isBlank())
                .collect(Collectors.groupingBy(Recognition::getLevel, Collectors.counting()));
        out.put("byLevel", byLevel);
        Map<String, Integer> perDay = new TreeMap<>();
        java.time.ZoneId zid = java.time.ZoneId.of("UTC");
        for (Recognition r : recs) {
            if (r.getSentAt() == null) continue;
            String day = r.getSentAt().atZone(zid).toLocalDate().toString();
            perDay.put(day, perDay.getOrDefault(day, 0) + 1);
        }
        out.put("timeSeriesByDay", perDay);
        return out;
    }

    public List<Map<String, Object>> getTopSenders(int limit, Instant from, Instant to) {
        List<Object[]> rows = recognitionRepository.topSendersNative(from, to, limit);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] row : rows) {
            Number idN = (Number) row[0];
            Number cntN = (Number) row[1];
            Number ptsN = (Number) row[2];
            Map<String, Object> m = new HashMap<>();
            m.put("senderId", idN == null ? null : idN.longValue());
            m.put("count", cntN == null ? 0L : cntN.longValue());
            m.put("points", ptsN == null ? 0 : ptsN.intValue());
            out.add(m);
        }
        return out;
    }

    public List<Map<String, Object>> getTopRecipients(int limit, Instant from, Instant to) {
        List<Object[]> rows = recognitionRepository.topRecipientsNative(from, to, limit);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] row : rows) {
            Number idN = (Number) row[0];
            Number cntN = (Number) row[1];
            Number ptsN = (Number) row[2];
            Map<String, Object> m = new HashMap<>();
            m.put("recipientId", idN == null ? null : idN.longValue());
            m.put("count", cntN == null ? 0L : cntN.longValue());
            m.put("points", ptsN == null ? 0 : ptsN.intValue());
            out.add(m);
        }
        return out;
    }

    // cohort insights simple implementation: bucketing by tenure days
    public Map<String, Object> generateCohortInsights(List<int[]> buckets, Instant from, Instant to) {
        Map<String, Object> out = new HashMap<>();
        List<Employee> emps = employeeRepository.findAll();
        List<Recognition> recs = recognitionRepository.findAllBetween(from, to);
        java.time.ZoneId zid = java.time.ZoneId.of("UTC");
        for (int[] b : buckets) {
            int min = b[0]; int max = b[1];
            long count = emps.stream().filter(e -> {
                if (e.getJoiningDate()==null) return false;
                long days = java.time.Duration.between(e.getJoiningDate().atStartOfDay(zid).toInstant(), Instant.now()).toDays();
                return days >= min && days <= max;
            }).count();
            out.put(min + "-" + max, Map.of("employees", count));
        }
        return out;
    }

    @Cacheable(value = "leaderboardSenders", key = "#limit + '_' + #from.toEpochMilli() + '_' + #to.toEpochMilli() + '_' + (#role==null?''+#unitId+#managerId:#role+'_'+#unitId+'_'+#managerId)")
    public List<Map<String, Object>> getTopSendersCached(int limit, Instant from, Instant to, String role, Long unitId, Long managerId) {
        List<Object[]> rows = recognitionRepository.topSendersNativeFiltered(from, to, role, unitId, managerId, limit);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] row : rows) {
            Number idN = (Number) row[0];
            Number cntN = (Number) row[1];
            Number ptsN = (Number) row[2];
            Map<String, Object> m = new HashMap<>();
            m.put("senderId", idN == null ? null : idN.longValue());
            m.put("count", cntN == null ? 0L : cntN.longValue());
            m.put("points", ptsN == null ? 0 : ptsN.intValue());
            out.add(m);
        }
        return out;
    }

    @Cacheable(value = "leaderboardRecipients", key = "#limit + '_' + #from.toEpochMilli() + '_' + #to.toEpochMilli() + '_' + (#role==null?''+#unitId+#managerId:#role+'_'+#unitId+'_'+#managerId)")
    public List<Map<String, Object>> getTopRecipientsCached(int limit, Instant from, Instant to, String role, Long unitId, Long managerId) {
        List<Object[]> rows = recognitionRepository.topRecipientsNativeFiltered(from, to, role, unitId, managerId, limit);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object[] row : rows) {
            Number idN = (Number) row[0];
            Number cntN = (Number) row[1];
            Number ptsN = (Number) row[2];
            Map<String, Object> m = new HashMap<>();
            m.put("recipientId", idN == null ? null : idN.longValue());
            m.put("count", cntN == null ? 0L : cntN.longValue());
            m.put("points", ptsN == null ? 0 : ptsN.intValue());
            out.add(m);
        }
        return out;
    }

    @Cacheable(value = "leaderboardSendersPaged", key = "#page + '_' + #size + '_' + #from.toEpochMilli() + '_' + #to.toEpochMilli() + '_' + (#role==null?''+#unitId+#managerId:#role+'_'+#unitId+'_'+#managerId))")
    public Map<String, Object> getTopSendersPaged(int page, int size, Instant from, Instant to, String role, Long unitId, Long managerId) {
        int offset = page * size;
        List<Object[]> rows = recognitionRepository.topSendersNativePaged(from, to, role, unitId, managerId, size, offset);
        int total = recognitionRepository.countTopSendersNativeFiltered(from, to, role, unitId, managerId);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object[] row : rows) {
            Number idN = (Number) row[0];
            Number cntN = (Number) row[1];
            Number ptsN = (Number) row[2];
            String first = row[3] == null ? null : row[3].toString();
            String last = row[4] == null ? null : row[4].toString();
            String uuid = row[5] == null ? null : row[5].toString();
            Map<String, Object> m = new HashMap<>();
            m.put("id", idN == null ? null : idN.longValue());
            m.put("count", cntN == null ? 0L : cntN.longValue());
            m.put("points", ptsN == null ? 0 : ptsN.intValue());
            m.put("firstName", first);
            m.put("lastName", last);
            m.put("uuid", uuid);
            items.add(m);
        }
        return Map.of("items", items, "page", page, "size", size, "totalElements", total);
    }

    @Cacheable(value = "leaderboardRecipientsPaged", key = "#page + '_' + #size + '_' + #from.toEpochMilli() + '_' + #to.toEpochMilli() + '_' + (#role==null?''+#unitId+#managerId:#role+'_'+#unitId+'_'+#managerId))")
    public Map<String, Object> getTopRecipientsPaged(int page, int size, Instant from, Instant to, String role, Long unitId, Long managerId) {
        int offset = page * size;
        List<Object[]> rows = recognitionRepository.topRecipientsNativePaged(from, to, role, unitId, managerId, size, offset);
        int total = recognitionRepository.countTopRecipientsNativeFiltered(from, to, role, unitId, managerId);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object[] row : rows) {
            Number idN = (Number) row[0];
            Number cntN = (Number) row[1];
            Number ptsN = (Number) row[2];
            String first = row[3] == null ? null : row[3].toString();
            String last = row[4] == null ? null : row[4].toString();
            String uuid = row[5] == null ? null : row[5].toString();
            Map<String, Object> m = new HashMap<>();
            m.put("id", idN == null ? null : idN.longValue());
            m.put("count", cntN == null ? 0L : cntN.longValue());
            m.put("points", ptsN == null ? 0 : ptsN.intValue());
            m.put("firstName", first);
            m.put("lastName", last);
            m.put("uuid", uuid);
            items.add(m);
        }
        return Map.of("items", items, "page", page, "size", size, "totalElements", total);
    }

}
