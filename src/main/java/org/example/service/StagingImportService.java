package org.example.service;

import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.springframework.core.io.InputStreamResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StagingImportService {

    private final JdbcTemplate jdbcTemplate;

    public StagingImportService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> importCsvViaCopy(MultipartFile file, String filename) throws Exception {
        // Create import_job
        long jobId = jdbcTemplate.queryForObject("INSERT INTO import_job(filename,status,created_at) VALUES (?, 'RUNNING', now()) RETURNING id", Long.class, filename);

        // write file to temp and run COPY FROM STDIN
        Path tmp = Files.createTempFile("recognitions-upload", ".csv");
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        try (Connection c = jdbcTemplate.getDataSource().getConnection()) {
            PGConnection pg = c.unwrap(PGConnection.class);
            CopyManager cm = pg.getCopyAPI();
            String sql = "COPY staging_recognitions(recognition_uuid,recognition_type_uuid,award_name,level,recipient_uuid,sender_uuid,sent_at,message,award_points,approval_status,rejection_reason) FROM STDIN WITH (FORMAT csv, HEADER true, ENCODING 'utf-8')";
            try (InputStream fis = Files.newInputStream(tmp)) {
                long rows = cm.copyIn(sql, fis);
                jdbcTemplate.update("UPDATE import_job SET total_rows = ? WHERE id = ?", rows, jobId);
            }

            // Now move valid rows into recognitions while recording failures
            // Insert errors: rows that don't map to employee/type
            jdbcTemplate.update("INSERT INTO import_error(import_job_id,row_num,raw_data,error_message) SELECT ?, row_number, (s.*)::text, CASE WHEN rt.id IS NULL THEN 'missing recognition_type' WHEN e_rec.id IS NULL THEN 'missing recipient' WHEN e_s.id IS NULL THEN 'missing sender' ELSE 'unknown' END FROM (SELECT row_number() OVER () as row_number, * FROM staging_recognitions) s LEFT JOIN recognition_type rt ON s.recognition_type_uuid = rt.uuid LEFT JOIN employee e_rec ON s.recipient_uuid = e_rec.uuid LEFT JOIN employee e_s ON s.sender_uuid = e_s.uuid WHERE rt.id IS NULL OR e_rec.id IS NULL OR e_s.id IS NULL", jobId);

            // Insert good rows
            int inserted = jdbcTemplate.update("INSERT INTO recognitions (uuid, recognition_type_id, award_name, level, recipient_id, sender_id, sent_at, message, award_points, approval_status) SELECT COALESCE(s.recognition_uuid, gen_random_uuid()), rt.id, s.award_name, s.level, e_rec.id, e_s.id, s.sent_at, s.message, s.award_points, s.approval_status FROM staging_recognitions s JOIN recognition_type rt ON s.recognition_type_uuid = rt.uuid JOIN employee e_rec ON s.recipient_uuid = e_rec.uuid JOIN employee e_s ON s.sender_uuid = e_s.uuid ON CONFLICT (uuid) DO NOTHING");

            // Update import_job counts
            Integer failed = jdbcTemplate.queryForObject("SELECT count(*) FROM import_error WHERE import_job_id = ?", Integer.class, jobId);
            jdbcTemplate.update("UPDATE import_job SET success_count = ?, failed_count = ?, status = ? , finished_at = now() WHERE id = ?", inserted, failed, failed != null && failed > 0 ? "PARTIAL" : "SUCCESS", jobId);

            // cleanup staging
            jdbcTemplate.update("TRUNCATE TABLE staging_recognitions");

            return Map.of("jobId", jobId, "inserted", inserted, "failed", failed == null ? 0 : failed);
        } catch (Exception ex) {
            jdbcTemplate.update("UPDATE import_job SET status = 'FAILED', finished_at = now() WHERE id = ?", jobId);
            throw ex;
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // Start an async import: create job row (PENDING) and schedule the COPY/import work in background. Returns jobId immediately.
    public Map<String, Object> startImportViaCopyAsync(MultipartFile file, String filename) throws Exception {
        // create job with PENDING status
        long jobId = jdbcTemplate.queryForObject("INSERT INTO import_job(filename,status,created_at) VALUES (?, 'PENDING', now()) RETURNING id", Long.class, filename);

        // copy file to temp for background thread to use
        Path tmp = Files.createTempFile("recognitions-upload-async", ".csv");
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        // schedule background worker
        Thread t = new Thread(() -> {
            try (Connection c = jdbcTemplate.getDataSource().getConnection()) {
                // set status RUNNING
                jdbcTemplate.update("UPDATE import_job SET status = 'RUNNING', started_at = now() WHERE id = ?", jobId);

                PGConnection pg = c.unwrap(PGConnection.class);
                CopyManager cm = pg.getCopyAPI();
                String sql = "COPY staging_recognitions(recognition_uuid,recognition_type_uuid,award_name,level,recipient_uuid,sender_uuid,sent_at,message,award_points,approval_status,rejection_reason) FROM STDIN WITH (FORMAT csv, HEADER true, ENCODING 'utf-8')";
                try (InputStream fis = Files.newInputStream(tmp)) {
                    long rows = cm.copyIn(sql, fis);
                    jdbcTemplate.update("UPDATE import_job SET total_rows = ? WHERE id = ?", rows, jobId);
                }

                // same move logic as synchronous method
                jdbcTemplate.update("INSERT INTO import_error(import_job_id,row_num,raw_data,error_message) SELECT ?, row_number, (s.*)::text, CASE WHEN rt.id IS NULL THEN 'missing recognition_type' WHEN e_rec.id IS NULL THEN 'missing recipient' WHEN e_s.id IS NULL THEN 'missing sender' ELSE 'unknown' END FROM (SELECT row_number() OVER () as row_number, * FROM staging_recognitions) s LEFT JOIN recognition_type rt ON s.recognition_type_uuid = rt.uuid LEFT JOIN employee e_rec ON s.recipient_uuid = e_rec.uuid LEFT JOIN employee e_s ON s.sender_uuid = e_s.uuid WHERE rt.id IS NULL OR e_rec.id IS NULL OR e_s.id IS NULL", jobId);

                int inserted = jdbcTemplate.update("INSERT INTO recognitions (uuid, recognition_type_id, award_name, level, recipient_id, sender_id, sent_at, message, award_points, approval_status) SELECT COALESCE(s.recognition_uuid, gen_random_uuid()), rt.id, s.award_name, s.level, e_rec.id, e_s.id, s.sent_at, s.message, s.award_points, s.approval_status FROM staging_recognitions s JOIN recognition_type rt ON s.recognition_type_uuid = rt.uuid JOIN employee e_rec ON s.recipient_uuid = e_rec.uuid JOIN employee e_s ON s.sender_uuid = e_s.uuid ON CONFLICT (uuid) DO NOTHING");

                Integer failed = jdbcTemplate.queryForObject("SELECT count(*) FROM import_error WHERE import_job_id = ?", Integer.class, jobId);
                jdbcTemplate.update("UPDATE import_job SET success_count = ?, failed_count = ?, status = ? , finished_at = now() WHERE id = ?", inserted, failed, failed != null && failed > 0 ? "PARTIAL" : "SUCCESS", jobId);

                jdbcTemplate.update("TRUNCATE TABLE staging_recognitions");
            } catch (Exception ex) {
                jdbcTemplate.update("UPDATE import_job SET status = 'FAILED', finished_at = now() WHERE id = ?", jobId);
            } finally {
                try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
            }
        }, "import-worker-" + jobId);
        t.setDaemon(true);
        t.start();

        return Map.of("jobId", jobId);
    }

    public Map<String, Object> getImportJobStatus(long jobId) {
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT id, filename, status, total_rows, success_count, failed_count, created_at, started_at, finished_at FROM import_job WHERE id = ?", jobId);
        return row;
    }

    public Map<String, Object> getImportErrorsPaged(long jobId, int page, int size) {
        int offset = page * size;
        Integer total = jdbcTemplate.queryForObject("SELECT count(*) FROM import_error WHERE import_job_id = ?", Integer.class, jobId);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT id, row_num, raw_data, error_message, created_at FROM import_error WHERE import_job_id = ? ORDER BY id LIMIT ? OFFSET ?", jobId, size, offset);
        return Map.of("items", rows, "page", page, "size", size, "totalElements", total == null ? 0 : total);
    }

    public Path exportImportErrorsCsvToTemp(long jobId) throws Exception {
        Path tmp = Files.createTempFile("import-errors-" + jobId + "-", ".csv");
        try (BufferedWriter w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            // header
            w.write("id,row_num,raw_data,error_message,created_at\n");
            jdbcTemplate.query("SELECT id, row_num, raw_data, error_message, created_at FROM import_error WHERE import_job_id = ? ORDER BY id", new Object[]{jobId}, rs -> {
                try {
                    long id = rs.getLong("id");
                    int rowNum = rs.getInt("row_num");
                    String raw = rs.getString("raw_data");
                    String msg = rs.getString("error_message");
                    java.sql.Timestamp created = rs.getTimestamp("created_at");
                    String createdStr = created == null ? "" : created.toInstant().toString();
                    // simple CSV escaping: wrap in quotes and escape existing quotes
                    raw = raw == null ? "" : raw.replace("\"", "\"\"");
                    msg = msg == null ? "" : msg.replace("\"", "\"\"");
                    w.write(id + "," + rowNum + ",\"" + raw + "\",\"" + msg + "\"," + createdStr + "\n");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return tmp;
    }
}
