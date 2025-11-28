-- V2: Seed recognition types, 100 employees, and ~1000 recognitions
-- Insert recognition types (ecard, ecard_with_points, award) - idempotent insert without relying on named constraint
INSERT INTO recognition_type (type_name, created_by)
SELECT v.type_name, v.created_by
FROM (VALUES
  ('ecard', 1),
  ('ecard_with_points', 1),
  ('award', 1)
) AS v(type_name, created_by)
WHERE NOT EXISTS (
  SELECT 1 FROM recognition_type rt WHERE lower(rt.type_name) = lower(v.type_name)
);

-- Create 100 employees with improved data quality
DO $$
DECLARE
  names TEXT[] := array[
    'Alex Smith','Jordan Brown','Taylor Johnson','Casey Lee','Morgan Davis','Riley Martinez','Parker Wilson','Drew Anderson','Cameron Thomas','Avery Jackson',
    'Sam Harris','Jamie King','Robin Green','Chris Hall','Dana Scott','Evan Young','Frank Wright','Gaby Wood','Hayden Baker','Ira Bell',
    'Jesse Cox','Kris Ward','Lane Price','Max Stone','Noel Gray','Owen Ross','Quinn Perry','Reese Long','Shawn Fox','Toby Reed',
    'Uma Lane','Vera Hale','Wes Neal','Yara Cruz','Zane Lowe','Ivy Hill','Bea Lane','Cal Moss','Dax Cole','Elsa Moss',
    'Finn Gale','Gus Dale','Hugo Ford','Iris North','Jude Snow','Kara West','Lana Moss','Mia Hart','Nia Starr','Ola Beck',
    'Paul Cain','Quin Ray','Rae Lynn','Shae Morn','Tia Vale','Uli Moss','Val Joy','Wyn Price','Xan Hale','Yol Hart',
    'Zoe Lane','Ana Reed','Ben Holt','Cia Nash','Dev Roy','Eli Moss','Fay Dunn','Gia Reed','Hal Moss','Ian Ford',
    'Joy Finn','Ken Stern','Lee Voss','Moe Dale','Ned Finn','Orr Hale','Pam West','Que Lee','Ron Vale','Sid Shaw',
    'Taj Wynn','Una Dune','Vic Miles','Will Pace','Xia Fu','Yen Tao','Zed Park','Ada Bell','Bo Kent','Cy Pace',
    'Dee Knox','Eon Rice','Fia West','Gio Park','Hua Lin','Ina Snow','Jax Cole','Kip Moss','Lio Frost','Moe Lake'
  ];
  i INT;
  nm TEXT;
  fn TEXT;
  ln TEXT;
BEGIN
  IF (SELECT count(*) FROM employee) < 100 THEN
    FOR i IN 1..100 LOOP
      nm := names[i];
      fn := split_part(nm, ' ', 1);
      ln := split_part(nm, ' ', 2);
      INSERT INTO employee (first_name, last_name, unit_id, manager_id, email, joining_date, role, terminated_at)
      VALUES (
        fn,
        ln,
        (1 + ((i-1) % 10)),
        CASE WHEN i <= 10 THEN NULL WHEN i <= 30 THEN ((i - 1) % 10) + 1 ELSE ((1 + ((i - 1) % 30)) ) END,
        lower(fn || '.' || ln || i || '@example.com'),
        current_date - ((i % 100) + ((random()*365)::int))::int,
        CASE WHEN i <= 10 THEN 'manager' WHEN i <= 30 THEN 'teamlead' ELSE 'employee' END,
        -- about 10% of seeded employees are terminated at a random date in the past 2 years
        CASE WHEN random() < 0.10 THEN (now() - ((365*2 * random())::int || ' days')::interval) ELSE NULL END
      );
    END LOOP;
  END IF;
END$$;

-- Insert recognitions: 10 per employee, random types and levels (gold/silver/bronze/diamond), spread over 3 years
DO $$
DECLARE
  emp RECORD;
  types INTEGER[] := array(SELECT id FROM recognition_type);
  tcount INT := array_length(types,1);
  i INT;
  points INT;
  status TEXT;
  levels TEXT[] := array['gold','silver','bronze','diamond'];
  days_offset INT;
BEGIN
  FOR emp IN SELECT id FROM employee LOOP
    FOR i IN 1..10 LOOP
      points := CASE WHEN random() < 0.5 THEN (1 + floor(random()*10))::int
                     WHEN random() < 0.85 THEN (11 + floor(random()*40))::int
                     WHEN random() < 0.98 THEN (51 + floor(random()*50))::int
                     ELSE (101 + floor(random()*400))::int END;
      -- increase PENDING/REJECTED rates for testing: 70% APPROVED, 20% PENDING, 10% REJECTED
      status := CASE WHEN random() < 0.65 THEN 'APPROVED' WHEN random() < 0.93 THEN 'PENDING' ELSE 'REJECTED' END;
      -- spread across ~3 years to avoid clustering
      days_offset := floor(random()* (365*3))::int;
      INSERT INTO recognitions (recognition_type_id, award_name, level, recipient_id, sender_id, sent_at, message, award_points, approval_status, rejection_reason)
      VALUES (
        types[1 + (floor(random()*tcount))::int],
        CASE WHEN random() < 0.5 THEN 'Great Job' ELSE 'Awesome Work' END,
        levels[1 + (floor(random()*array_length(levels,1)))::int],
        emp.id,
        (SELECT id FROM employee WHERE id != emp.id ORDER BY random() LIMIT 1),
        now() - (days_offset || ' days')::interval,
        'Thanks for your contribution!',
        points,
        status,
        CASE WHEN status = 'REJECTED' THEN (array['Policy mismatch','Insufficient details','Out of scope','Duplicate entry'])[1 + floor(random()*4)::int] ELSE NULL END
      );
    END LOOP;
  END LOOP;
END$$;

-- Create indexes used by insights (also safe if already present)
CREATE INDEX IF NOT EXISTS idx_recog_level ON recognitions(level);
CREATE INDEX IF NOT EXISTS idx_recog_approval ON recognitions(approval_status);
