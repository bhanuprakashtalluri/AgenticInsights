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
-- list types
SELECT id, type_name, uuid FROM recognition_type ORDER BY id;
-- counts
SELECT count(*) AS total_recognitions, count(distinct recognition_type_id) AS distinct_types, count(*) FILTER (WHERE recognition_type_id IS NULL) AS null_types FROM recognitions;
-- distribution
SELECT t.type_name, count(*) FROM recognitions r JOIN recognition_type t ON r.recognition_type_id = t.id GROUP BY t.type_name ORDER BY count(*) DESC;

