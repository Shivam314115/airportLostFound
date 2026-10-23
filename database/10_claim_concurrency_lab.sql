BEGIN;

INSERT INTO app.isolation_demo (demo_id, demo_value)
VALUES
    (1, 'pending'),
    (2, 'pending'),
    (3, 'pending')
ON CONFLICT (demo_id)
DO UPDATE SET demo_value = EXCLUDED.demo_value;

COMMIT;
