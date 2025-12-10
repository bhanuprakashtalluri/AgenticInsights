-- Add default admin user
INSERT INTO "user" (username, password, roles) VALUES
  ('admin@company.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '{"ADMIN"}')
ON CONFLICT (username) DO NOTHING; -- Password: password
