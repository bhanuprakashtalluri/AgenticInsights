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
END $$;
