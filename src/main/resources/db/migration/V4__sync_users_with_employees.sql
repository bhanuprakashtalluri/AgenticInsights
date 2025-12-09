-- V4__sync_users_with_employees.sql
-- This migration ensures the user table is always in sync with the employee table.
-- Each user will have the employee's email as username, a default password based on their role (e.g., Admin@123), and the correct role.
-- The password is stored as a BCrypt hash for security and must be updated by the user via the UI (forgot password page).

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS "user" (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(255) NOT NULL UNIQUE, -- employee email
  password VARCHAR(255) NOT NULL,         -- BCrypt hash, to be updated by user
  roles TEXT[] NOT NULL,                  -- e.g. ['ADMIN']
  refresh_token VARCHAR(255)
);

-- Remove all old users and re-insert from employee table
TRUNCATE TABLE "user" RESTART IDENTITY;

-- Insert users based on employee table (username is employee email, password is BCrypt hash of Role@123, role is from employee)
DO $$
DECLARE
    emp RECORD;
    temp_password TEXT;
    hashed_password TEXT;
BEGIN
    FOR emp IN SELECT email, role FROM employee WHERE email IS NOT NULL AND role IS NOT NULL LOOP
        temp_password :=
            CASE LOWER(emp.role)
                WHEN 'admin' THEN 'Admin@123'
                WHEN 'manager' THEN 'Manager@123'
                WHEN 'teamlead' THEN 'Teamlead@123'
                WHEN 'employee' THEN 'Employee@123'
                ELSE 'User@123'
            END;
        hashed_password := crypt(temp_password, gen_salt('bf', 10));
        INSERT INTO "user" (username, password, roles)
        VALUES (emp.email, hashed_password, ARRAY[UPPER(emp.role)])
        ON CONFLICT (username) DO UPDATE SET roles = EXCLUDED.roles;
    END LOOP;
END $$;
