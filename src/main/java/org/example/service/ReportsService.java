package org.example.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class ReportsService {

    private final AIInsightsService insightsService;
    private final ChartService chartService;
    private final RecognitionCsvExporter csvExporter;
    private final org.example.repository.RecognitionRepository recognitionRepository;

    private final String reportsPath;

    public ReportsService(AIInsightsService insightsService,
                          ChartService chartService,
                          RecognitionCsvExporter csvExporter,
                          org.example.repository.RecognitionRepository recognitionRepository,
                          @Value("${app.reports.path:./reports}") String reportsPath) {
        this.insightsService = insightsService;
        this.chartService = chartService;
        this.csvExporter = csvExporter;
        this.recognitionRepository = recognitionRepository;
        this.reportsPath = reportsPath;
    }

    private void ensureReportsDir() throws Exception {
        File dir = new File(reportsPath);
        if (!dir.exists()) Files.createDirectories(dir.toPath());
    }

    @Scheduled(cron = "${app.reports.daily-cron}")
    public void dailyReport() {
        try {
            generateReportNow(Instant.now().minusSeconds(24*3600), Instant.now(), "daily");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Scheduled(cron = "${app.reports.monthly-cron}")
    public void monthlyReport() {
        try {
            generateReportNow(Instant.now().minusSeconds(30L*24*3600), Instant.now(), "monthly");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String generateReportNow(Instant from, Instant to, String label) throws Exception {
        ensureReportsDir();
        Map<String, Object> insights = insightsService.generateInsights(from, to);
        String ts = LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).replace(':','-');
        String base = label + "-" + ts;
        File jsonFile = new File(reportsPath, base + ".json");
        Files.writeString(jsonFile.toPath(), new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(insights));

        java.util.List<org.example.model.Recognition> recs = recognitionRepository.findAllBetween(from, to);
        try (FileOutputStream fos = new FileOutputStream(new File(reportsPath, base + ".csv"))) {
            csvExporter.exportToStream(fos, recs);
        }

        @SuppressWarnings("unchecked")
        java.util.Map<String, Integer> perDay = (java.util.Map<String, Integer>) insights.get("timeSeriesByDay");
        byte[] png = chartService.renderTimeSeriesChart(perDay, "Recognitions", "day", "count");
        try (FileOutputStream fos = new FileOutputStream(new File(reportsPath, base + ".png"))) {
            fos.write(png);
        }

        return base;
    }

}
