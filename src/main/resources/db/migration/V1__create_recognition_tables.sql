-- V1: Create extensions and tables for recognitions app
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Employees table
CREATE TABLE IF NOT EXISTS employee (
  id BIGSERIAL PRIMARY KEY,
  uuid UUID NOT NULL DEFAULT gen_random_uuid(),
  first_name VARCHAR(100) NOT NULL,
  last_name VARCHAR(100) NOT NULL,
  unit_id BIGINT, -- team/unit id reference if needed
  manager_id BIGINT,
  email VARCHAR(255),
  joining_date DATE,
  role VARCHAR(50) DEFAULT 'employee',
  terminated_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  CONSTRAINT uq_employee_uuid UNIQUE (uuid)
);

CREATE INDEX IF NOT EXISTS idx_employee_manager ON employee(manager_id);
CREATE INDEX IF NOT EXISTS idx_employee_unit ON employee(unit_id);

-- Recognition types
CREATE TABLE IF NOT EXISTS recognition_type (
  id BIGSERIAL PRIMARY KEY,
  uuid UUID NOT NULL DEFAULT gen_random_uuid(),
  type_name VARCHAR(150) NOT NULL,
  created_by BIGINT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  CONSTRAINT uq_recognition_type_uuid UNIQUE (uuid)
);

-- Enforce uniqueness on type_name (case-insensitive) to prevent duplicates
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint c
    JOIN pg_class t ON c.conrelid = t.oid
    WHERE c.conname = 'uq_recognition_type_name' AND t.relname = 'recognition_type'
  ) THEN
    ALTER TABLE recognition_type ADD CONSTRAINT uq_recognition_type_name UNIQUE (lower(type_name));
  END IF;
EXCEPTION WHEN duplicate_object THEN NULL; END$$;

-- Recognitions
CREATE TABLE IF NOT EXISTS recognitions (
  id BIGSERIAL PRIMARY KEY,
  uuid UUID NOT NULL DEFAULT gen_random_uuid(),
  recognition_type_id BIGINT REFERENCES recognition_type(id) ON DELETE SET NULL,
  award_name VARCHAR(255),
  level VARCHAR(100),
  recipient_id BIGINT REFERENCES employee(id) ON DELETE CASCADE,
  sender_id BIGINT REFERENCES employee(id) ON DELETE SET NULL,
  sent_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  message TEXT,
  award_points INTEGER DEFAULT 0,
  approval_status VARCHAR(50) DEFAULT 'PENDING', -- e.g., PENDING/APPROVED/REJECTED
  rejection_reason TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  CONSTRAINT uq_recognitions_uuid UNIQUE (uuid)
);

CREATE INDEX IF NOT EXISTS idx_recipient ON recognitions(recipient_id);
CREATE INDEX IF NOT EXISTS idx_sender ON recognitions(sender_id);
CREATE INDEX IF NOT EXISTS idx_recog_type ON recognitions(recognition_type_id);
CREATE INDEX IF NOT EXISTS idx_sent_at ON recognitions(sent_at);
