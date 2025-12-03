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

    public Path getBase() {
        return base;
    }

    public Path getReportsDirForToday() {
        return base.resolve("reports").resolve(LocalDate.now().format(dayFmt));
    }

    public Path getGraphsDirForToday() {
        return base.resolve("exports").resolve("graphs").resolve(LocalDate.now().format(dayFmt));
    }

    public Path getExportsCsvDirForToday() {
        return base.resolve("exports").resolve("csv").resolve(LocalDate.now().format(dayFmt));
    }

    public Path getExportsJsonDirForToday() {
        return base.resolve("exports").resolve("json").resolve(LocalDate.now().format(dayFmt));
    }

    public Path getExportsToonDirForToday() {
        return base.resolve("exports").resolve("toon").resolve(LocalDate.now().format(dayFmt));
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

    public Path storeExportToon(String filename, byte[] bytes) throws IOException {
        Path dir = getExportsToonDirForToday();
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        return target;
    }

    public String nowTimestamp() {
        return LocalDateTime.now().format(tsFmt);
    }
}
