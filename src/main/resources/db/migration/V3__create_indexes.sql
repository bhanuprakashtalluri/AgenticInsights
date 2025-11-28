-- V3: Indexes to accelerate leaderboard and time-series queries

CREATE INDEX IF NOT EXISTS idx_recognitions_sent_at ON recognitions (sent_at);
CREATE INDEX IF NOT EXISTS idx_recognitions_sender_sent_at ON recognitions (sender_id, sent_at);
CREATE INDEX IF NOT EXISTS idx_recognitions_recipient_sent_at ON recognitions (recipient_id, sent_at);

CREATE INDEX IF NOT EXISTS idx_employee_role ON employee (role);
CREATE INDEX IF NOT EXISTS idx_employee_unit ON employee (unit_id);
CREATE INDEX IF NOT EXISTS idx_employee_manager ON employee (manager_id);

-- Note: run this migration manually if Flyway is disabled (app is configured for manual migrations).

