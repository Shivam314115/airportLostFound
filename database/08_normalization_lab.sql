-- ============================================================
-- Airport Lost & Found Management System
-- Phase 8: Normalization Lab
--
-- This exercise creates a TEMPORARY, deliberately denormalized
-- table. It does not modify the project's permanent tables.
-- ============================================================


-- 1. Create an intentionally denormalized relation.

CREATE TEMP TABLE lab_denormalized_match (
    lost_report_id BIGINT NOT NULL,
    passenger_id BIGINT NOT NULL,
    passenger_name VARCHAR(150) NOT NULL,
    passenger_email VARCHAR(254) NOT NULL,

    lost_item_id BIGINT NOT NULL,
    lost_item_name VARCHAR(150) NOT NULL,
    category_id BIGINT NOT NULL,
    category_name VARCHAR(80) NOT NULL,

    found_item_id BIGINT NOT NULL,
    found_item_name VARCHAR(150) NOT NULL,

    match_score NUMERIC(5, 2),
    match_status VARCHAR(25) NOT NULL,

    PRIMARY KEY (lost_report_id, found_item_id)
);


-- 2. Insert illustrative practice data.
-- The third row represents an additional hypothetical candidate.
-- These rows exist only in the temporary lab table.

INSERT INTO lab_denormalized_match VALUES
(
    1, 1, 'Airport Passenger', 'riya.patel@example.test',
    101, 'Black wireless earbuds', 1, 'Electronics',
    201, 'Black wireless earbuds',
    96.50, 'verified'
),
(
    1, 1, 'Airport Passenger', 'riya.patel@example.test',
    101, 'Black wireless earbuds', 1, 'Electronics',
    202, 'Similar black earbuds',
    82.00, 'candidate'
),
(
    2, 2, 'Kabir Singh', 'kabir.singh@example.test',
    102, 'Blue document folder', 2, 'Documents',
    203, 'Blue document folder',
    72.00, 'candidate'
);


-- 3. Inspect the denormalized relation.

SELECT *
FROM lab_denormalized_match
ORDER BY lost_report_id, found_item_id;


-- 4. Demonstrate repeated passenger information.

SELECT
    passenger_id,
    passenger_name,
    passenger_email,
    COUNT(*) AS repeated_rows
FROM lab_denormalized_match
GROUP BY
    passenger_id,
    passenger_name,
    passenger_email
HAVING COUNT(*) > 1
ORDER BY passenger_id;


-- 5. Demonstrate repeated lost-item information.

SELECT
    lost_report_id,
    lost_item_id,
    lost_item_name,
    category_name,
    COUNT(*) AS repeated_rows
FROM lab_denormalized_match
GROUP BY
    lost_report_id,
    lost_item_id,
    lost_item_name,
    category_name
HAVING COUNT(*) > 1
ORDER BY lost_report_id;


-- 6. Inspect the functional dependencies in the example.
--
-- Assumptions for this illustrative design:
--
-- passenger_id -> passenger_name, passenger_email
-- lost_item_id -> lost_item_name, category_id
-- category_id -> category_name
-- found_item_id -> found_item_name
-- (lost_report_id, found_item_id) -> match_score, match_status
--
-- lost_report_id also determines passenger_id and lost_item_id.
-- These assumptions describe the intended business model;
-- they are not inferred automatically by PostgreSQL.


-- 7. Show how the real project separates these facts.

SELECT
    p.passenger_id,
    p.full_name AS passenger_name,
    lr.lost_report_id,
    lost_i.item_id AS lost_item_id,
    lost_i.item_name AS lost_item_name,
    cat.category_name,
    f.found_item_id,
    found_i.item_name AS found_item_name,
    m.match_score,
    m.verification_status
FROM app.lost_report AS lr
JOIN app.passenger AS p
    ON p.passenger_id = lr.passenger_id
JOIN app.item AS lost_i
    ON lost_i.item_id = lr.item_id
JOIN app.item_category AS cat
    ON cat.category_id = lost_i.category_id
JOIN app.item_match AS m
    ON m.lost_report_id = lr.lost_report_id
JOIN app.found_item AS f
    ON f.found_item_id = m.found_item_id
JOIN app.item AS found_i
    ON found_i.item_id = f.item_id
ORDER BY lr.lost_report_id, m.match_score DESC;
