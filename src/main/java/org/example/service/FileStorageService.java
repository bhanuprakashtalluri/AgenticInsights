package org.example.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class FileStorageService {
    private final Path base;
    // Use MM-dd-yyyy to match shell scripts and existing logs
    private final DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private final DateTimeFormatter tsFmt = DateTimeFormatter.ofPattern("MM-dd-yyyy-HH.mm.ss");

    public FileStorageService(@Value("${app.storage.path:./artifacts}") String basePath) {
        this.base = Path.of(basePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(base);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create storage base path: " + base, e);
        }
    }

    public Path getBase() { return base; }

    public Path getReportsDirForToday() {
        return base.resolve("reports").resolve(LocalDate.now().format(dayFmt));
    }

    public Path getGraphsDirForToday() {
        // graphs are part of exports under the new layout
        return base.resolve("exports").resolve("graphs").resolve(LocalDate.now().format(dayFmt));
    }

    public Path getExportsCsvDirForToday() {
        return base.resolve("exports").resolve("csv").resolve(LocalDate.now().format(dayFmt));
    }

    public Path getExportsJsonDirForToday() {
        return base.resolve("exports").resolve("json").resolve(LocalDate.now().format(dayFmt));
    }

    public Path getSamplesDirForNow() {
        String ts = LocalDateTime.now().format(tsFmt);
        Path dir = base.resolve("samples").resolve(ts);
        return dir;
    }

    public Path getLogsDirForToday() {
        // verification logs moved to verification_logs
        return base.resolve("verification_logs").resolve(LocalDate.now().format(dayFmt));
    }

    public Path storeReport(String filename, byte[] bytes) throws IOException {
        Path dir = getReportsDirForToday();
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return target;
    }

    public Path storeGraph(String filename, byte[] bytes) throws IOException {
        Path dir = getGraphsDirForToday();
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return target;
    }

    public Path storeExportCsv(String filename, byte[] bytes) throws IOException {
        Path dir = getExportsCsvDirForToday();
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return target;
    }

    public Path storeExportJson(String filename, byte[] bytes) throws IOException {
        Path dir = getExportsJsonDirForToday();
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return target;
    }

    public Path storeSample(String filename, byte[] bytes) throws IOException {
        Path dir = getSamplesDirForNow();
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
        return target;
    }

    public Path storeLog(String filename, byte[] bytes) throws IOException {
        Path dir = getLogsDirForToday();
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        return target;
    }

    // helper to get current timestamp string in configured format
    public String nowTimestamp() {
        return LocalDateTime.now().format(tsFmt);
    }
}