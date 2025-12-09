-- Minimal, precise seed data for recognitions app
-- Recognition Types (explicit IDs for FK integrity)
INSERT INTO recognition_type (id, type_name, created_by, created_at) VALUES
  (1, 'ecard', 1, now()),
  (2, 'ecard_with_points', 1, now()),
  (3, 'award', 1, now())
ON CONFLICT (id) DO NOTHING;

-- Employees: 2 managers, 2 teamleads under each, 4 employees under each teamlead
-- Managers
INSERT INTO employee (id, first_name, last_name, unit_id, manager_id, email, joining_date, role) VALUES
  (1, 'Alice', 'Smith', 101, NULL, 'alice.smith@company.com', '2022-01-10', 'manager'),
  (2, 'Bob', 'Johnson', 102, NULL, 'bob.johnson@company.com', '2022-01-15', 'manager');
-- Teamleads under Alice (manager_id=1)
INSERT INTO employee (id, first_name, last_name, unit_id, manager_id, email, joining_date, role) VALUES
  (3, 'Carol', 'Williams', 101, 1, 'carol.williams@company.com', '2022-02-01', 'teamlead'),
  (4, 'David', 'Brown', 101, 1, 'david.brown@company.com', '2022-02-02', 'teamlead');
-- Teamleads under Bob (manager_id=2)
INSERT INTO employee (id, first_name, last_name, unit_id, manager_id, email, joining_date, role) VALUES
  (5, 'Eve', 'Davis', 102, 2, 'eve.davis@company.com', '2022-02-03', 'teamlead'),
  (6, 'Frank', 'Miller', 102, 2, 'frank.miller@company.com', '2022-02-04', 'teamlead');
-- Employees under each teamlead (4 per teamlead)
-- Under Carol (3)
INSERT INTO employee (id, first_name, last_name, unit_id, manager_id, email, joining_date, role) VALUES
  (7, 'Grace', 'Moore', 101, 3, 'grace.moore@company.com', '2022-03-01', 'employee'),
  (8, 'Hank', 'Taylor', 101, 3, 'hank.taylor@company.com', '2022-03-02', 'employee'),
  (9, 'Ivy', 'Anderson', 101, 3, 'ivy.anderson@company.com', '2022-03-03', 'employee'),
  (10, 'Jack', 'Thomas', 101, 3, 'jack.thomas@company.com', '2022-03-04', 'employee');
-- Under David (4)
INSERT INTO employee (id, first_name, last_name, unit_id, manager_id, email, joining_date, role) VALUES
  (11, 'Kate', 'Jackson', 101, 4, 'kate.jackson@company.com', '2022-03-05', 'employee'),
  (12, 'Leo', 'White', 101, 4, 'leo.white@company.com', '2022-03-06', 'employee'),
  (13, 'Mia', 'Harris', 101, 4, 'mia.harris@company.com', '2022-03-07', 'employee'),
  (14, 'Nina', 'Martin', 101, 4, 'nina.martin@company.com', '2022-03-08', 'employee');
-- Under Eve (5)
INSERT INTO employee (id, first_name, last_name, unit_id, manager_id, email, joining_date, role) VALUES
  (15, 'Oscar', 'Lee', 102, 5, 'oscar.lee@company.com', '2022-03-09', 'employee'),
  (16, 'Pam', 'Walker', 102, 5, 'pam.walker@company.com', '2022-03-10', 'employee'),
  (17, 'Quinn', 'Hall', 102, 5, 'quinn.hall@company.com', '2022-03-11', 'employee'),
  (18, 'Rita', 'Young', 102, 5, 'rita.young@company.com', '2022-03-12', 'employee');
-- Under Frank (6)
INSERT INTO employee (id, first_name, last_name, unit_id, manager_id, email, joining_date, role) VALUES
  (19, 'Sam', 'King', 102, 6, 'sam.king@company.com', '2022-03-13', 'employee'),
  (20, 'Tina', 'Scott', 102, 6, 'tina.scott@company.com', '2022-03-14', 'employee'),
  (21, 'Uma', 'Green', 102, 6, 'uma.green@company.com', '2022-03-15', 'employee'),
  (22, 'Vera', 'Baker', 102, 6, 'vera.baker@company.com', '2022-03-16', 'employee');

-- Recognitions: each employee sends and receives a recognition from every other employee (no self-recognition)
DO $$
DECLARE
  e1 INT;
  e2 INT;
  categories TEXT[] := ARRAY['Great Job','Awesome Work','Milestone Achieved','Team Player','Innovation'];
  levels TEXT[] := ARRAY['gold','silver','bronze','diamond'];
  type_ids INT[] := ARRAY[1,2,3];
  idx INT := 0;
  points INT;
  status TEXT;
  level TEXT;
  type_id INT;
  rejection_reason TEXT;
BEGIN
  FOR e1 IN 1..22 LOOP
    FOR e2 IN 1..22 LOOP
      IF e1 != e2 THEN
        idx := idx + 1;
        type_id := type_ids[(idx % 3) + 1];
        level := CASE WHEN type_id = 3 THEN levels[(idx % 4) + 1] ELSE NULL END;
        status := CASE WHEN (idx % 3) = 0 THEN 'APPROVED' WHEN (idx % 3) = 1 THEN 'PENDING' ELSE 'REJECTED' END;
        -- Points logic: only 0, 5, 10, 20, 25, 30 allowed
        IF type_id = 1 THEN -- ecard
          points := 0;
        ELSIF type_id = 2 THEN -- ecard_with_points
          points := CASE WHEN (idx % 2) = 0 THEN 5 ELSE 10 END;
        ELSIF type_id = 3 THEN -- award
          IF level = 'bronze' OR level = 'diamond' THEN points := 20;
          ELSIF level = 'silver' THEN points := 25;
          ELSIF level = 'gold' THEN points := 30;
          ELSE points := 20;
          END IF;
        END IF;
        -- Rejection reason logic
        IF status = 'REJECTED' THEN
          rejection_reason := 'Policy mismatch';
        ELSE
          rejection_reason := NULL;
        END IF;
        INSERT INTO recognitions (recognition_type_id, category, level, recipient_id, sender_id, sent_at, message, award_points, approval_status, rejection_reason)
        VALUES (
          type_id,
          categories[(idx % 5) + 1],
          level,
          e1, e2,
          '2023-01-01'::timestamp + ((idx % 365) || ' days')::interval,
          'Recognition from ' || e2 || ' to ' || e1,
          points,
          status,
          rejection_reason
        );
      END IF;
    END LOOP;
  END LOOP;
END$$;
