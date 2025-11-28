# Database migration & manual setup guide

This document shows exactly how to create the database, apply the repository SQL migrations, fix duplicate recognition types, seed data, and run common manual commands for inserting, deleting and truncating rows. All commands assume a local Postgres server and the credentials used in `application.yml` by default:

- host: localhost
- port: 5432
- db: recognitions
- user: postgres
- password: rmkec

If your environment differs, replace host/user/password/database with your values.

---

## Quick checklist

- [ ] Create the `recognitions` database (if missing)
- [ ] Run V1 (schema), V2 (seed), V3 (indexes) SQL migration files
- [ ] Verify recognition types deduped and unique index exists
- [ ] Run verification queries
- [ ] Use examples to insert/delete/truncate rows or run COPY for bulk import

---

## 0. Prerequisites

- psql CLI installed and reachable in your PATH
- Postgres running locally or reachable from this machine
- Repository `src/main/resources/db/migration/` contains SQL files:
  - `V1__create_recognition_tables.sql`
  - `V2__seed_sample_data.sql`
  - `V3__create_indexes.sql`

Set an environment variable for convenience (zsh/bash):

```bash
export PGPASSWORD=rmkec
```

(Or set PGPASSWORD inline for each command.)

---

## 1. Create the database if missing

This will create the `recognitions` DB only if it doesn't already exist.

```bash
export PGPASSWORD=rmkec
psql -h localhost -U postgres -tc "SELECT 1 FROM pg_database WHERE datname='recognitions'" | grep -q 1 || psql -h localhost -U postgres -c "CREATE DATABASE recognitions;"
```

---

## 2. Apply migrations (manual)

Apply the V1 (schema), V2 (seed) and V3 (indexes) migrations in order. These commands run the SQL files located under `src/main/resources/db/migration/`.

```bash
export PGPASSWORD=rmkec
psql -h localhost -U postgres -d recognitions -f src/main/resources/db/migration/V1__create_recognition_tables.sql
psql -h localhost -U postgres -d recognitions -f src/main/resources/db/migration/V2__seed_sample_data.sql
psql -h localhost -U postgres -d recognitions -f src/main/resources/db/migration/V3__create_indexes.sql
```

Notes:
- `V1` creates the schema and a case-insensitive uniqueness constraint on `recognition_type` names.
- `V2` seeds sample employees, recognition types, and recognitions (improved seeding spreads recognitions across ~3 years).
- `V3` adds helpful indexes for analytics and leaderboard queries.

If a script fails (syntax error), inspect the SQL file and the error message; most seed scripts use PL/pgSQL DO blocks and can be re-run after fixes.

---

## 3. Deduplicate `recognition_type` safely (if needed)

If you find duplicate recognition types (same name but different rows), run this safe transaction which:

1. picks a canonical id per lower(type_name) (smallest id),
2. repoints `recognitions.recognition_type_id` to canonical ids,
3. deletes duplicate rows,
4. creates a unique index on `lower(type_name)`.

Run the following (idempotent) block:

```bash
export PGPASSWORD=rmkec
psql -h localhost -U postgres -d recognitions -v ON_ERROR_STOP=1 -c "
BEGIN;
CREATE TEMP TABLE tmp_duplicates AS
  SELECT rt.id AS dup_id, c.keep_id
  FROM recognition_type rt
  JOIN (SELECT min(id) AS keep_id, lower(type_name) AS lname FROM recognition_type GROUP BY lower(type_name)) c
    ON lower(rt.type_name) = c.lname
  WHERE rt.id <> c.keep_id;

-- re-point recognitions to canonical ids
UPDATE recognitions r
  SET recognition_type_id = td.keep_id
  FROM tmp_duplicates td
  WHERE r.recognition_type_id = td.dup_id;

-- remove duplicate rows
DELETE FROM recognition_type rt
  USING tmp_duplicates td
  WHERE rt.id = td.dup_id;

DROP TABLE tmp_duplicates;
COMMIT;
"

# Create case-insensitive unique index to prevent new duplicates
psql -h localhost -U postgres -d recognitions -c "CREATE UNIQUE INDEX IF NOT EXISTS uq_recognition_type_name_idx ON recognition_type (lower(type_name));"
```

Verify duplicates are gone:

```bash
psql -h localhost -U postgres -d recognitions -c "SELECT lower(type_name) as name, count(*) FROM recognition_type GROUP BY lower(type_name) HAVING count(*)>1;"
```

If the result is empty, dedupe succeeded.

---

## 4. Useful verification queries

- Number of employees, types and recognitions:

```bash
psql -h localhost -U postgres -d recognitions -c "SELECT 'employees' AS kind, count(*) FROM employee;"
psql -h localhost -U postgres -d recognitions -c "SELECT 'types' AS kind, count(*) FROM recognition_type;"
psql -h localhost -U postgres -d recognitions -c "SELECT 'recognitions' AS kind, count(*) FROM recognitions;"
```

- Sample recognitions joined to types:

```bash
psql -h localhost -U postgres -d recognitions -c "SELECT r.id, r.uuid, r.sent_at, r.award_points, r.approval_status, rt.id AS type_id, rt.type_name FROM recognitions r LEFT JOIN recognition_type rt ON r.recognition_type_id = rt.id ORDER BY r.sent_at DESC LIMIT 10;"
```

- Time series (daily count last 30 days):

```bash
psql -h localhost -U postgres -d recognitions -c "SELECT date_trunc('day', sent_at) AS day, count(*) FROM recognitions WHERE sent_at > now() - interval '30 days' GROUP BY day ORDER BY day;"
```

---

## 5. Manual single-row operations

### Insert a single employee (psql)

```sql
INSERT INTO employee (first_name, last_name, unit_id, manager_id, email, joining_date, role)
VALUES ('Sam', 'Example', 1, NULL, 'sam.example@example.com', '2020-06-01', 'manager') RETURNING id, uuid;
```

Run via psql:

```bash
psql -h localhost -U postgres -d recognitions -c "INSERT INTO employee (first_name, last_name, unit_id, manager_id, email, joining_date, role) VALUES ('Sam', 'Example', 1, NULL, 'sam.example@example.com', '2020-06-01', 'manager') RETURNING id, uuid;"
```

### Insert a single recognition (psql)

Replace `TYPE_ID`, `RECIPIENT_ID`, `SENDER_ID` with actual ids from your DB:

```sql
INSERT INTO recognitions (recognition_type_id, award_name, level, recipient_id, sender_id, sent_at, message, award_points, approval_status)
VALUES (TYPE_ID, 'Great Job', 'gold', RECIPIENT_ID, SENDER_ID, '2025-11-26T12:00:00Z', 'Thanks!', 25, 'APPROVED') RETURNING id, uuid;
```

### Delete a single recognition by id

```bash
psql -h localhost -U postgres -d recognitions -c "DELETE FROM recognitions WHERE id = 123;"
```

### Truncate tables (CAREFUL)

Truncating will remove all data. Use only in dev/test. Example: clear recognitions and reset sequences.

```bash
psql -h localhost -U postgres -d recognitions -c "TRUNCATE TABLE recognitions RESTART IDENTITY CASCADE;"
# optionally truncate employees too (this will cascade and remove recognitions with FK CASCADE)
psql -h localhost -U postgres -d recognitions -c "TRUNCATE TABLE employee RESTART IDENTITY CASCADE;"
```

Note: `CASCADE` will remove dependent rows. Always back up (pg_dump) before truncating in non-dev environments.

---

## 6. Bulk CSV import (simple approach using COPY)

Recommended CSV header (UTF-8, one header row):

```
recognition_uuid,recognition_type_uuid,award_name,level,recipient_uuid,sender_uuid,sent_at,message,award_points,approval_status,rejection_reason
```

Example `COPY` command to import into a staging table then insert to production (fast path):

1) Create a simple staging table (one-time):

```sql
CREATE TABLE IF NOT EXISTS staging_recognitions (
  recognition_uuid UUID,
  recognition_type_uuid UUID,
  award_name TEXT,
  level TEXT,
  recipient_uuid UUID,
  sender_uuid UUID,
  sent_at TIMESTAMPTZ,
  message TEXT,
  award_points INT,
  approval_status TEXT,
  rejection_reason TEXT
);
```

2) Load CSV into staging using psql's COPY:

```bash
psql -h localhost -U postgres -d recognitions -c "\copy staging_recognitions FROM 'path/to/file.csv' WITH (FORMAT csv, HEADER true, ENCODING 'utf-8');"
```

3) Insert from staging into `recognitions` with joins to resolve UUID -> id and capture rows that fail mapping into `import_errors` if needed. Example insert (adjust column names):

```sql
INSERT INTO recognitions (uuid, recognition_type_id, award_name, level, recipient_id, sender_id, sent_at, message, award_points, approval_status, rejection_reason)
SELECT s.recognition_uuid,
       rt.id,
       s.award_name,
       s.level,
       e_rec.id,
       e_sndr.id,
       s.sent_at,
       s.message,
       s.award_points,
       s.approval_status,
       s.rejection_reason
FROM staging_recognitions s
LEFT JOIN recognition_type rt ON s.recognition_type_uuid = rt.uuid
LEFT JOIN employee e_rec ON s.recipient_uuid = e_rec.uuid
LEFT JOIN employee e_sndr ON s.sender_uuid = e_sndr.uuid
WHERE rt.id IS NOT NULL AND e_rec.id IS NOT NULL;
```

4) Capture failed rows (where rt or e_rec or e_sndr are NULL) for manual inspection:

```sql
SELECT * FROM staging_recognitions s
LEFT JOIN recognition_type rt ON s.recognition_type_uuid = rt.uuid
LEFT JOIN employee e_rec ON s.recipient_uuid = e_rec.uuid
LEFT JOIN employee e_sndr ON s.sender_uuid = e_sndr.uuid
WHERE rt.id IS NULL OR e_rec.id IS NULL OR e_sndr.id IS NULL;
```

5) Cleanup staging after import:

```bash
psql -h localhost -U postgres -d recognitions -c "TRUNCATE TABLE staging_recognitions;"
```

Notes:
- Using COPY + set-based INSERT with joins is fast and avoids N+1 lookups.
- For idempotency, include `recognition_uuid` and use `ON CONFLICT (uuid) DO NOTHING` or DO UPDATE.

---

## 7. Backups and safety

- Create a SQL dump before destructive operations:

```bash
export PGPASSWORD=rmkec
pg_dump -h localhost -U postgres -d recognitions -F c -b -v -f recognitions_preop.dump
```

- Restore from dump (if needed):

```bash
pg_restore -h localhost -U postgres -d recognitions -v recognitions_preop.dump
```

---

## 8. Troubleshooting

- If `psql` reports syntax errors in a DO block, open the SQL and check for stray array/ indexing patterns (some older psql versions may not accept certain array index constructs). We already replaced a fragile pattern in V2 with CASE.
- If a migration fails because of existing objects (indexes/constraints), re-run the migration after inspecting the DB; `CREATE INDEX IF NOT EXISTS` is safe and used in the migrations.
- If the app cannot connect, verify `application.yml` settings and that Postgres is accepting TCP connections on the specified host/port.

---

## 9. Quick reference commands (copy-paste)

Create DB if missing:

```bash
export PGPASSWORD=rmkec
psql -h localhost -U postgres -tc "SELECT 1 FROM pg_database WHERE datname='recognitions'" | grep -q 1 || psql -h localhost -U postgres -c "CREATE DATABASE recognitions;"
```

Apply migrations:

```bash
psql -h localhost -U postgres -d recognitions -f src/main/resources/db/migration/V1__create_recognition_tables.sql
psql -h localhost -U postgres -d recognitions -f src/main/resources/db/migration/V2__seed_sample_data.sql
psql -h localhost -U postgres -d recognitions -f src/main/resources/db/migration/V3__create_indexes.sql
```

Dedupe types (safe):

```bash
export PGPASSWORD=rmkec
psql -h localhost -U postgres -d recognitions -v ON_ERROR_STOP=1 -c "
BEGIN;
CREATE TEMP TABLE tmp_duplicates AS
  SELECT rt.id AS dup_id, c.keep_id
  FROM recognition_type rt
  JOIN (SELECT min(id) AS keep_id, lower(type_name) AS lname FROM recognition_type GROUP BY lower(type_name)) c
    ON lower(rt.type_name) = c.lname
  WHERE rt.id <> c.keep_id;
UPDATE recognitions r
  SET recognition_type_id = td.keep_id
  FROM tmp_duplicates td
  WHERE r.recognition_type_id = td.dup_id;
DELETE FROM recognition_type rt
  USING tmp_duplicates td
  WHERE rt.id = td.dup_id;
DROP TABLE tmp_duplicates;
COMMIT;
"
psql -h localhost -U postgres -d recognitions -c "CREATE UNIQUE INDEX IF NOT EXISTS uq_recognition_type_name_idx ON recognition_type (lower(type_name));"
```

Truncate recognitions (dev):

```bash
psql -h localhost -U postgres -d recognitions -c "TRUNCATE TABLE recognitions RESTART IDENTITY CASCADE;"
```

Bulk COPY import (staging -> insert):

```bash
psql -h localhost -U postgres -d recognitions -c "\copy staging_recognitions FROM 'path/to/file.csv' WITH (FORMAT csv, HEADER true, ENCODING 'utf-8');"
psql -h localhost -U postgres -d recognitions -c "INSERT INTO recognitions (uuid, recognition_type_id, award_name, level, recipient_id, sender_id, sent_at, message, award_points, approval_status, rejection_reason) SELECT s.recognition_uuid, rt.id, s.award_name, s.level, e_rec.id, e_sndr.id, s.sent_at, s.message, s.award_points, s.approval_status, s.rejection_reason FROM staging_recognitions s LEFT JOIN recognition_type rt ON s.recognition_type_uuid = rt.uuid LEFT JOIN employee e_rec ON s.recipient_uuid = e_rec.uuid LEFT JOIN employee e_sndr ON s.sender_uuid = e_sndr.uuid WHERE rt.id IS NOT NULL AND e_rec.id IS NOT NULL AND e_sndr.id IS NOT NULL;"
```

---

If you'd like, I can:

- Re-run the dedupe step now and show the verification outputs and top-of-table samples (I can run the commands for you),
- Re-seed the DB (truncate & run V2) to fully refresh data (only if you confirm this is safe for your environment), or
- Add a small `docs/DB_QUICKSTART.md` with a one-line script that runs all non-destructive checks.

Tell me which of the three you want me to do next.

## Normalize recognition types and remap recognitions (commands)

If you find duplicate or inconsistent recognition type names (for example "ecard with points" vs "ecard_with_points"), run the following commands to canonicalize the set to `ecard`, `ecard_with_points`, and `award`, dedupe duplicate rows, remap existing recognitions to canonical ids, and verify results.

Option A — run the normalization script file (recommended)

1. Create a script file (one-liner here-doc) and run it:

```bash
export PGPASSWORD=rmkec
cat > tmp/normalize_recognition_types.sql <<'SQL'
BEGIN;

-- 1) Ensure canonical recognition_type rows exist
INSERT INTO recognition_type(type_name, created_by)
SELECT v.t, 1
FROM (VALUES ('ecard'), ('ecard_with_points'), ('award')) AS v(t)
WHERE NOT EXISTS (SELECT 1 FROM recognition_type rt WHERE lower(rt.type_name) = lower(v.t));

-- 2) Build mapping of existing rows to canonical names
CREATE TEMP TABLE tmp_map AS
SELECT id AS src_id,
       type_name AS src_name,
       CASE
         WHEN lower(type_name) LIKE '%point%' THEN 'ecard_with_points'
         WHEN lower(type_name) LIKE '%ecard%' THEN 'ecard'
         WHEN lower(type_name) LIKE 'award%' THEN 'award'
         ELSE lower(type_name)
       END AS canonical_name
FROM recognition_type;

-- 3) Resolve canonical keep_id for each canonical_name
CREATE TEMP TABLE tmp_keep AS
SELECT DISTINCT m.canonical_name,
       (SELECT id FROM recognition_type rt WHERE lower(rt.type_name) = m.canonical_name LIMIT 1) AS keep_id
FROM tmp_map m;

-- 4) Identify duplicate rows (src_id != keep_id)
CREATE TEMP TABLE tmp_dups AS
SELECT m.src_id AS dup_id, k.keep_id
FROM tmp_map m
JOIN tmp_keep k ON m.canonical_name = k.canonical_name
WHERE m.src_id <> k.keep_id;

-- 5) Remap recognitions that point to duplicate type ids -> canonical keep_id
UPDATE recognitions r
SET recognition_type_id = td.keep_id
FROM tmp_dups td
WHERE r.recognition_type_id = td.dup_id;

-- 6) Delete duplicate type rows
DELETE FROM recognition_type rt
USING tmp_dups td
WHERE rt.id = td.dup_id;

-- 7) Normalize canonical spellings explicitly
UPDATE recognition_type SET type_name = 'ecard_with_points' WHERE lower(type_name) = 'ecard_with_points';
UPDATE recognition_type SET type_name = 'ecard' WHERE lower(type_name) = 'ecard';
UPDATE recognition_type SET type_name = 'award' WHERE lower(type_name) = 'award';

COMMIT;

-- 8) Create case-insensitive unique index to prevent future duplicates
CREATE UNIQUE INDEX IF NOT EXISTS idx_recognition_type_lower ON recognition_type (lower(type_name));

-- 9) Verification output
SELECT id, type_name, uuid FROM recognition_type ORDER BY id;
SELECT count(*) AS total_recognitions, count(distinct recognition_type_id) AS distinct_types, count(*) FILTER (WHERE recognition_type_id IS NULL) AS null_types FROM recognitions;
SELECT t.type_name, count(*) FROM recognitions r JOIN recognition_type t ON r.recognition_type_id = t.id GROUP BY t.type_name ORDER BY count(*) DESC;
SQL

# run the script
psql -h localhost -U postgres -d recognitions -f tmp/normalize_recognition_types.sql
```

Option B — inline psql commands (single-shot)

```bash
export PGPASSWORD=rmkec
psql -h localhost -U postgres -d recognitions -v ON_ERROR_STOP=1 -c "\
BEGIN;\
INSERT INTO recognition_type(type_name, created_by) SELECT v.t,1 FROM (VALUES ('ecard'),('ecard_with_points'),('award')) v(t) WHERE NOT EXISTS (SELECT 1 FROM recognition_type rt WHERE lower(rt.type_name)=lower(v.t));\
-- normalize names\
UPDATE recognition_type SET type_name = 'ecard_with_points' WHERE lower(type_name) LIKE '%point%';\
UPDATE recognition_type SET type_name = 'ecard' WHERE lower(type_name) LIKE 'ecard' AND lower(type_name) NOT LIKE '%point%';\
UPDATE recognition_type SET type_name = 'award' WHERE lower(type_name) LIKE 'award%';\
-- dedupe and remap\
CREATE TEMP TABLE tmp_map AS SELECT id AS src_id, lower(type_name) AS lname FROM recognition_type;\
CREATE TEMP TABLE tmp_keep2 AS SELECT lname, min(src_id) AS keep_id FROM tmp_map GROUP BY lname;\
CREATE TEMP TABLE tmp_dups AS SELECT m.src_id AS dup_id, k.keep_id FROM tmp_map m JOIN tmp_keep2 k ON m.lname = k.lname WHERE m.src_id <> k.keep_id;\
UPDATE recognitions r SET recognition_type_id = td.keep_id FROM tmp_dups td WHERE r.recognition_type_id = td.dup_id;\
DELETE FROM recognition_type rt USING tmp_dups td WHERE rt.id = td.dup_id;\
DROP TABLE tmp_dups; DROP TABLE tmp_keep2; DROP TABLE tmp_map;\
-- assign any remaining NULL recognition_type_id by heuristic (award_points >=100 -> award, >0 -> points, else ecard)\
UPDATE recognitions SET recognition_type_id = (SELECT id FROM recognition_type WHERE lower(type_name)='award' LIMIT 1) WHERE recognition_type_id IS NULL AND COALESCE(award_points,0) >= 100;\
UPDATE recognitions SET recognition_type_id = (SELECT id FROM recognition_type WHERE lower(type_name)='ecard_with_points' LIMIT 1) WHERE recognition_type_id IS NULL AND COALESCE(award_points,0) > 0;\
UPDATE recognitions SET recognition_type_id = (SELECT id FROM recognition_type WHERE lower(type_name)='ecard' LIMIT 1) WHERE recognition_type_id IS NULL;\
COMMIT;"

# then create index and verify
psql -h localhost -U postgres -d recognitions -c "CREATE UNIQUE INDEX IF NOT EXISTS idx_recognition_type_lower ON recognition_type (lower(type_name));"
psql -h localhost -U postgres -d recognitions -c "SELECT id,type_name FROM recognition_type ORDER BY id;"
psql -h localhost -U postgres -d recognitions -c "SELECT t.type_name, count(*) FROM recognitions r JOIN recognition_type t ON r.recognition_type_id = t.id GROUP BY t.type_name ORDER BY count(*) DESC;"
```

Notes
- Option A writes a script file to `tmp/normalize_recognition_types.sql` and runs it; use this if you prefer reproducible runs and inspection.
- Option B runs inline in psql; it's convenient but careful escaping is required for multi-line statements in some shells.
- Both options include a heuristic to assign a recognition type for any recognition that lacks one (based on `award_points`). Adjust the heuristic to suit your data.

---

## Add terminated employees and increase PENDING/REJECTED recognitions

For testing analytics you may want some employees to be terminated and a healthy mix of APPROVED/PENDING/REJECTED recognitions. The following SQL will:

- mark approximately 10% of employees as terminated at a random date within the last 2 years (only employees with NULL `terminated_at` are affected),
- set ~10% of recognitions to `REJECTED` (with a random rejection reason),
- set ~20% of recognitions (remaining APPROVED ones) to `PENDING`.

Run as a single transaction (safe & idempotent for already-set fields):

```bash
export PGPASSWORD=rmkec
psql -h localhost -U postgres -d recognitions -v ON_ERROR_STOP=1 -c "\
BEGIN;\
-- mark ~10% of employees as terminated at a random date in the last 2 years (only unset terminated_at)\
UPDATE employee SET terminated_at = now() - ((floor(random()*730))::int || ' days')::interval\
  WHERE terminated_at IS NULL AND random() < 0.10;\
\
-- set ~10% of recognitions to REJECTED (add a random rejection_reason)\
UPDATE recognitions SET approval_status = 'REJECTED',\
  rejection_reason = (ARRAY['Policy mismatch','Insufficient details','Out of scope','Duplicate entry'])[(floor(random()*4)::int + 1)]\
  WHERE random() < 0.10;\
\
-- set ~20% of (remaining) APPROVED recognitions to PENDING (leave REJECTED alone)\
UPDATE recognitions SET approval_status = 'PENDING', rejection_reason = NULL\
  WHERE approval_status = 'APPROVED' AND random() < 0.20;\
\
COMMIT;"
```

Verification queries

```bash
# number of terminated employees
psql -h localhost -U postgres -d recognitions -c "SELECT count(*) AS terminated_count FROM employee WHERE terminated_at IS NOT NULL;"

# distribution of recognition approval statuses
psql -h localhost -U postgres -d recognitions -c "SELECT approval_status, count(*) FROM recognitions GROUP BY approval_status ORDER BY count(*) DESC;"

# sample rejected recognitions with reasons
psql -h localhost -U postgres -d recognitions -c "SELECT id, recipient_id, sender_id, award_points, approval_status, rejection_reason, sent_at FROM recognitions WHERE approval_status = 'REJECTED' LIMIT 10;"
```

Notes
- These updates are non-reversible automatically — back up the DB before running if you may want to revert. Use `pg_dump` as shown in the Backups section.
- The random selection is non-deterministic; run again to get a different sample.
