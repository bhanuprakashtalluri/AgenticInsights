-- Create user table if not exists
CREATE TABLE IF NOT EXISTS "user" (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(100) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  roles TEXT[] NOT NULL,
  refresh_token VARCHAR(255)
);

DO $$
DECLARE
    max_id bigint;
BEGIN
    -- employee_id_seq
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM employee;
    PERFORM setval('employee_id_seq', max_id + 1, false);
    -- recognition_type_id_seq
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM recognition_type;
    PERFORM setval('recognition_type_id_seq', max_id + 1, false);
    -- recognitions_id_seq
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM recognitions;
    PERFORM setval('recognitions_id_seq', max_id + 1, false);
    -- user_id_seq
    SELECT COALESCE(MAX(id), 0) INTO max_id FROM "user";
    PERFORM setval('user_id_seq', max_id + 1, false);
END $$;
