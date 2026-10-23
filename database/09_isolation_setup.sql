BEGIN;

SET ROLE airport_owner;

CREATE TABLE IF NOT EXISTS app.isolation_demo (
    demo_id INTEGER PRIMARY KEY,
    demo_value TEXT NOT NULL
);

INSERT INTO app.isolation_demo (demo_id, demo_value)
VALUES (1, 'original')
ON CONFLICT (demo_id)
DO UPDATE SET demo_value = EXCLUDED.demo_value;

GRANT SELECT, INSERT, UPDATE, DELETE
ON app.isolation_demo
TO airport_app;

RESET ROLE;

COMMIT;
