-- Clean, precise schema for recognitions app
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS employee (
  id BIGSERIAL PRIMARY KEY,
  uuid UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
  first_name VARCHAR(100) NOT NULL,
  last_name VARCHAR(100) NOT NULL,
  unit_id BIGINT,
  manager_id BIGINT,
  email VARCHAR(255),
  joining_date DATE,
  role VARCHAR(50) DEFAULT 'employee',
  terminated_at TIMESTAMP WITH TIME ZONE,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_employee_manager ON employee(manager_id);
CREATE INDEX IF NOT EXISTS idx_employee_unit ON employee(unit_id);
CREATE INDEX IF NOT EXISTS idx_employee_role ON employee(role);

CREATE TABLE IF NOT EXISTS recognition_type (
  id BIGSERIAL PRIMARY KEY,
  uuid UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
  type_name VARCHAR(150) NOT NULL UNIQUE,
  created_by BIGINT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE IF NOT EXISTS recognitions (
  id BIGSERIAL PRIMARY KEY,
  uuid UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
  recognition_type_id BIGINT REFERENCES recognition_type(id) ON DELETE SET NULL,
  category VARCHAR(255), -- was award_name
  level VARCHAR(100), -- only for recognition type 'award'
  recipient_id BIGINT REFERENCES employee(id) ON DELETE CASCADE,
  sender_id BIGINT REFERENCES employee(id) ON DELETE SET NULL,
  sent_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  message TEXT,
  award_points INTEGER DEFAULT 0,
  approval_status VARCHAR(50) DEFAULT 'PENDING',
  rejection_reason TEXT,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_recipient ON recognitions(recipient_id);
CREATE INDEX IF NOT EXISTS idx_sender ON recognitions(sender_id);
CREATE INDEX IF NOT EXISTS idx_recog_type ON recognitions(recognition_type_id);
CREATE INDEX IF NOT EXISTS idx_sent_at ON recognitions(sent_at);
CREATE INDEX IF NOT EXISTS idx_recog_level ON recognitions(level);
CREATE INDEX IF NOT EXISTS idx_recog_approval ON recognitions(approval_status);
CREATE INDEX IF NOT EXISTS idx_recognitions_sent_at ON recognitions (sent_at);
CREATE INDEX IF NOT EXISTS idx_recognitions_sender_sent_at ON recognitions (sender_id, sent_at);
CREATE INDEX IF NOT EXISTS idx_recognitions_recipient_sent_at ON recognitions (recipient_id, sent_at);
