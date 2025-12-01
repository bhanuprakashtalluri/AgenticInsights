package org.example.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Map;

@Service
public class ReportsService {

    private final AIInsightsService insightsService;
    private final ChartService chartService;
    private final RecognitionCsvExporter csvExporter;
    private final org.example.repository.RecognitionRepository recognitionRepository;
    private final FileStorageService storage;

    public ReportsService(AIInsightsService insightsService,
                          ChartService chartService,
                          RecognitionCsvExporter csvExporter,
                          org.example.repository.RecognitionRepository recognitionRepository,
                          FileStorageService storage) {
        this.insightsService = insightsService;
        this.chartService = chartService;
        this.csvExporter = csvExporter;
        this.recognitionRepository = recognitionRepository;
        this.storage = storage;
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
        Map<String, Object> insights = insightsService.generateInsights(from, to);
        String ts = storage.nowTimestamp();
        String base = label + "-" + ts;

        byte[] jsonBytes = new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsBytes(insights);
        storage.storeReport(base + ".json", jsonBytes);

        java.util.List<org.example.model.Recognition> recs = recognitionRepository.findAllBetween(from, to);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        csvExporter.exportToStream(baos, recs);
        storage.storeReport(base + ".csv", baos.toByteArray());

        @SuppressWarnings("unchecked")
        java.util.Map<String, Integer> perDay = (java.util.Map<String, Integer>) insights.get("timeSeriesByDay");
        byte[] png = chartService.renderTimeSeriesChart(perDay, "Recognitions", "day", "count");
        storage.storeReport(base + ".png", png);

        return base;
    }

}
