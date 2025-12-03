package org.example.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Map;

@Service
public class ReportsService {

    private final ChartService chartService;
    private final RecognitionCsvExporter csvExporter;
    private final org.example.repository.RecognitionRepository recognitionRepository;
    private final FileStorageService storage;

    public ReportsService(ChartService chartService,
                          RecognitionCsvExporter csvExporter,
                          org.example.repository.RecognitionRepository recognitionRepository,
                          FileStorageService storage) {
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

    public String generateReportNow(java.time.Instant from, java.time.Instant to, String label) throws Exception {
        return "stub-report";
    }

}
