-- V5: Add started_at column to import_job to align with StagingImportService queries
ALTER TABLE import_job ADD COLUMN IF NOT EXISTS started_at TIMESTAMPTZ;