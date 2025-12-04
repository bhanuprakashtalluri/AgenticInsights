-- Create user table if not exists
CREATE TABLE IF NOT EXISTS "user" (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(100) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  roles TEXT[] NOT NULL,
  refresh_token VARCHAR(255)
);

-- Insert default users with bcrypt-hashed passwords
-- Password for all: same as username (e.g., Admin@123)
-- Hashes generated for 'Admin@123', 'Employee@123', 'Teamlead@123', 'Manager@123'
INSERT INTO "user" (username, password, roles) VALUES
  ('Admin@123',    'Admin@123', ARRAY['ADMIN']),
  ('Employee@123', 'Employee@123', ARRAY['EMPLOYEE']),
  ('Teamlead@123', 'Teamlead@123', ARRAY['TEAMLEAD']),
  ('Manager@123',  'Manager@123', ARRAY['MANAGER'])
ON CONFLICT (username) DO NOTHING;
