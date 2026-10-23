-- ============================================================
-- AIRPORT LOST & FOUND MANAGEMENT SYSTEM
-- Phase 7: SQL and Relational Algebra Lab
-- All queries in this file are read-only.
-- ============================================================


-- ------------------------------------------------------------
-- QUERY 1: JOIN
-- Question: What lost items have been matched with found items?
-- Concepts: INNER JOIN, foreign keys, relational join.
-- ------------------------------------------------------------

SELECT
    lr.lost_report_id,
    p.full_name AS passenger,
    lost_item.item_name AS lost_item,
    found_item.item_name AS found_item,
    m.match_score,
    m.verification_status AS match_status
FROM app.lost_report AS lr
JOIN app.passenger AS p
    ON p.passenger_id = lr.passenger_id
JOIN app.item AS lost_item
    ON lost_item.item_id = lr.item_id
JOIN app.item_match AS m
    ON m.lost_report_id = lr.lost_report_id
JOIN app.found_item AS f
    ON f.found_item_id = m.found_item_id
JOIN app.item AS found_item
    ON found_item.item_id = f.item_id
ORDER BY m.match_score DESC;


-- ------------------------------------------------------------
-- QUERY 2: LEFT JOIN
-- Question: Show every lost report, even if no match exists.
-- Concepts: LEFT OUTER JOIN, NULL values.
-- ------------------------------------------------------------

SELECT
    lr.lost_report_id,
    p.full_name AS passenger,
    i.item_name,
    lr.report_status,
    m.match_id,
    m.match_score,
    m.verification_status AS match_status
FROM app.lost_report AS lr
JOIN app.passenger AS p
    ON p.passenger_id = lr.passenger_id
JOIN app.item AS i
    ON i.item_id = lr.item_id
LEFT JOIN app.item_match AS m
    ON m.lost_report_id = lr.lost_report_id
ORDER BY lr.lost_report_id, m.match_score DESC;


-- ------------------------------------------------------------
-- QUERY 3: SELECTION AND PROJECTION
-- Question: Which matches have a score of at least 80?
-- Concepts: WHERE, selected columns, relational algebra.
-- ------------------------------------------------------------

SELECT
    lost_report_id,
    passenger_name,
    lost_item_name,
    found_item_name,
    match_score,
    match_status
FROM app.v_match_overview
WHERE match_score >= 80
ORDER BY match_score DESC;


-- ------------------------------------------------------------
-- QUERY 4: AGGREGATION
-- Question: How many catalogue items belong to each category?
-- Concepts: LEFT JOIN, COUNT, GROUP BY.
-- ------------------------------------------------------------

SELECT
    c.category_name,
    COUNT(i.item_id) AS total_items
FROM app.item_category AS c
LEFT JOIN app.item AS i
    ON i.category_id = c.category_id
GROUP BY
    c.category_id,
    c.category_name
ORDER BY total_items DESC, c.category_name;


-- ------------------------------------------------------------
-- QUERY 5: COUNT REPORTS PER PASSENGER
-- Question: How many lost reports has each passenger filed?
-- Concepts: LEFT JOIN, COUNT, GROUP BY.
-- ------------------------------------------------------------

SELECT
    p.passenger_id,
    p.full_name,
    COUNT(lr.lost_report_id) AS total_lost_reports
FROM app.passenger AS p
LEFT JOIN app.lost_report AS lr
    ON lr.passenger_id = p.passenger_id
GROUP BY
    p.passenger_id,
    p.full_name
ORDER BY total_lost_reports DESC, p.full_name;


-- ------------------------------------------------------------
-- QUERY 6: FILTERED AGGREGATION
-- Question: How many candidate, verified and rejected matches
--           exist in the database?
-- Concepts: Aggregate FILTER, COUNT.
-- ------------------------------------------------------------

SELECT
    COUNT(*) AS total_matches,
    COUNT(*) FILTER (
        WHERE verification_status = 'candidate'
    ) AS candidate_matches,
    COUNT(*) FILTER (
        WHERE verification_status = 'verified'
    ) AS verified_matches,
    COUNT(*) FILTER (
        WHERE verification_status = 'rejected'
    ) AS rejected_matches
FROM app.item_match;


-- ------------------------------------------------------------
-- QUERY 7: SUBQUERY WITH EXISTS
-- Question: Which passengers have at least one lost report?
-- Concepts: EXISTS, correlated subqueries.
-- ------------------------------------------------------------

SELECT
    p.passenger_id,
    p.full_name,
    p.email
FROM app.passenger AS p
WHERE EXISTS (
    SELECT 1
    FROM app.lost_report AS lr
    WHERE lr.passenger_id = p.passenger_id
)
ORDER BY p.full_name;


-- ------------------------------------------------------------
-- QUERY 8: CLAIM WORKFLOW REPORT
-- Question: What is the current state of every claim?
-- Concepts: Views, joins, denormalized reporting output.
-- ------------------------------------------------------------

SELECT
    claim_id,
    passenger_name,
    item_name,
    match_status,
    claim_status,
    verification_result,
    return_status
FROM app.v_claim_workflow
ORDER BY claim_id;


-- ------------------------------------------------------------
-- QUERY 9: CTE AND CONDITIONAL AGGREGATION
-- Question: How many candidate and verified matches are linked
--           to each lost report?
-- Concepts: WITH, common table expressions, FILTER, aggregation.
-- ------------------------------------------------------------

WITH report_match_counts AS (
    SELECT
        lr.lost_report_id,
        p.full_name AS passenger,
        i.item_name,
        lr.report_status,
        COUNT(m.match_id) FILTER (
            WHERE m.verification_status = 'candidate'
        ) AS candidate_count,
        COUNT(m.match_id) FILTER (
            WHERE m.verification_status = 'verified'
        ) AS verified_count
    FROM app.lost_report AS lr
    JOIN app.passenger AS p
        ON p.passenger_id = lr.passenger_id
    JOIN app.item AS i
        ON i.item_id = lr.item_id
    LEFT JOIN app.item_match AS m
        ON m.lost_report_id = lr.lost_report_id
    GROUP BY
        lr.lost_report_id,
        p.full_name,
        i.item_name,
        lr.report_status
)
SELECT
    lost_report_id,
    passenger,
    item_name,
    report_status,
    candidate_count,
    verified_count
FROM report_match_counts
ORDER BY lost_report_id;


-- ------------------------------------------------------------
-- QUERY 10: STORAGE CAPACITY
-- Question: How many storage slots are occupied and free?
-- Concepts: JOIN, FILTER, conditional aggregation.
-- ------------------------------------------------------------

SELECT
    s.storage_name,
    COUNT(ss.slot_number) AS total_slots,

    COUNT(ss.slot_number) FILTER (
        WHERE ss.slot_status = 'maintenance'
    ) AS maintenance_slots,

    COUNT(ss.slot_number) FILTER (
        WHERE ss.slot_status = 'available'
          AND sa.assignment_id IS NOT NULL
    ) AS occupied_serviceable_slots,

    COUNT(ss.slot_number) FILTER (
        WHERE ss.slot_status = 'available'
          AND sa.assignment_id IS NULL
    ) AS free_serviceable_slots

FROM app.storage_location AS s
LEFT JOIN app.storage_slot AS ss
    ON ss.storage_id = s.storage_id
LEFT JOIN app.storage_assignment AS sa
    ON sa.storage_id = ss.storage_id
   AND sa.slot_number = ss.slot_number
   AND sa.released_at IS NULL
GROUP BY
    s.storage_id,
    s.storage_name
ORDER BY s.storage_name;


-- ------------------------------------------------------------
-- QUERY 11: STORAGE HISTORY
-- Question: Which items have been stored, and for how long?
-- Concepts: Views, timestamp arithmetic, interval values.
-- ------------------------------------------------------------

SELECT
    item_name,
    storage_name,
    slot_number,
    assigned_at,
    released_at,
    is_active_assignment,
    storage_duration
FROM app.v_storage_assignment_history
ORDER BY storage_duration DESC;


-- ------------------------------------------------------------
-- QUERY 12: AUDIT HISTORY
-- Question: Which item-return events have been recorded?
-- Concepts: Filtering, ordering, JSONB data.
-- ------------------------------------------------------------

SELECT
    audit_id,
    action_type,
    entity_name,
    entity_id,
    details ->> 'claim_id' AS claim_id,
    details ->> 'found_item_id' AS found_item_id,
    occurred_at
FROM app.audit_log
WHERE action_type = 'ITEM_RETURNED'
ORDER BY occurred_at DESC;


-- ------------------------------------------------------------
-- QUERY 13: QUERY PLAN
-- Question: How does PostgreSQL retrieve candidate matches?
-- Concepts: Indexes, cost estimates, execution plans.
-- ------------------------------------------------------------

EXPLAIN (ANALYZE, BUFFERS)
SELECT
    match_id,
    lost_report_id,
    found_item_id,
    match_score
FROM app.item_match
WHERE verification_status = 'candidate'
ORDER BY match_score DESC;


-- End of lab.
