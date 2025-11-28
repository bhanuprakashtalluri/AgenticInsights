-- V4: Staging table and import job/error tables for bulk imports

-- Staging table for fast COPY loads
CREATE TABLE IF NOT EXISTS staging_recognitions (
  recognition_uuid UUID,
  recognition_type_uuid UUID,
  award_name TEXT,
  level TEXT,
  recipient_uuid UUID,
  sender_uuid UUID,
  sent_at TIMESTAMPTZ,
  message TEXT,
  award_points INT,
  approval_status TEXT,
  rejection_reason TEXT
);

-- Import job tracking
CREATE TABLE IF NOT EXISTS import_job (
  id BIGSERIAL PRIMARY KEY,
  filename TEXT,
  status VARCHAR(20) DEFAULT 'QUEUED', -- QUEUED, RUNNING, SUCCESS, PARTIAL, FAILED
  total_rows INT DEFAULT 0,
  success_count INT DEFAULT 0,
  failed_count INT DEFAULT 0,
  created_at TIMESTAMPTZ DEFAULT now(),
  finished_at TIMESTAMPTZ
);

-- Import row-level errors
CREATE TABLE IF NOT EXISTS import_error (
  id BIGSERIAL PRIMARY KEY,
  import_job_id BIGINT REFERENCES import_job(id) ON DELETE CASCADE,
  row_num INT,
  raw_data TEXT,
  error_message TEXT,
  created_at TIMESTAMPTZ DEFAULT now()
);

-- Small indexes to help queries
CREATE INDEX IF NOT EXISTS idx_import_error_job ON import_error(import_job_id);
CREATE INDEX IF NOT EXISTS idx_staging_recipient_uuid ON staging_recognitions(recipient_uuid);
CREATE INDEX IF NOT EXISTS idx_staging_sender_uuid ON staging_recognitions(sender_uuid);

