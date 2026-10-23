BEGIN;

SET ROLE airport_owner;


-- View 1: Lost reports and their associated found-item matches.

CREATE OR REPLACE VIEW app.v_match_overview AS
SELECT
    lr.lost_report_id,
    p.full_name AS passenger_name,
    lost_i.item_name AS lost_item_name,
    lost_cat.category_name AS lost_category,
    lr.report_status,
    lr.lost_at,

    f.found_item_id,
    found_i.item_name AS found_item_name,
    found_cat.category_name AS found_category,
    loc.location_name AS found_location,
    f.found_at,

    m.match_score,
    m.verification_status AS match_status,
    m.remarks AS match_remarks,
    reviewer.full_name AS reviewed_by,
    m.created_at AS matched_at,
    m.reviewed_at

FROM app.item_match AS m
JOIN app.lost_report AS lr
    ON lr.lost_report_id = m.lost_report_id
JOIN app.passenger AS p
    ON p.passenger_id = lr.passenger_id
JOIN app.item AS lost_i
    ON lost_i.item_id = lr.item_id
JOIN app.item_category AS lost_cat
    ON lost_cat.category_id = lost_i.category_id
JOIN app.found_item AS f
    ON f.found_item_id = m.found_item_id
JOIN app.item AS found_i
    ON found_i.item_id = f.item_id
JOIN app.item_category AS found_cat
    ON found_cat.category_id = found_i.category_id
LEFT JOIN app.airport_location AS loc
    ON loc.location_id = f.found_location_id
LEFT JOIN app.staff AS reviewer
    ON reviewer.staff_id = m.reviewed_by_staff_id;


-- View 2: Claim, identity verification, and return status in one result.

CREATE OR REPLACE VIEW app.v_claim_workflow AS
SELECT
    c.claim_id,
    p.full_name AS passenger_name,
    i.item_name,
    m.match_score,
    m.verification_status AS match_status,

    c.claim_status,
    c.submitted_at,
    claim_reviewer.full_name AS claim_reviewed_by,
    c.reviewed_at AS claim_reviewed_at,

    iv.verification_method,
    iv.verification_result,
    iv.verified_at,

    r.return_method,
    r.return_status,
    r.created_at AS return_created_at,
    r.completed_at AS return_completed_at

FROM app.claim AS c
JOIN app.item_match AS m
    ON m.match_id = c.match_id
JOIN app.lost_report AS lr
    ON lr.lost_report_id = m.lost_report_id
JOIN app.passenger AS p
    ON p.passenger_id = lr.passenger_id
JOIN app.item AS i
    ON i.item_id = lr.item_id
LEFT JOIN app.staff AS claim_reviewer
    ON claim_reviewer.staff_id = c.reviewed_by_staff_id

LEFT JOIN LATERAL (
    SELECT
        iv_inner.verification_method,
        iv_inner.verification_result,
        iv_inner.verified_at
    FROM app.identity_verification AS iv_inner
    WHERE iv_inner.claim_id = c.claim_id
    ORDER BY
        iv_inner.verified_at DESC NULLS LAST,
        iv_inner.verification_id DESC
    LIMIT 1
) AS iv ON TRUE

LEFT JOIN app.return_delivery AS r
    ON r.claim_id = c.claim_id;


-- View 3: Storage assignment history, including released assignments.

CREATE OR REPLACE VIEW app.v_storage_assignment_history AS
SELECT
    sa.assignment_id,
    f.found_item_id,
    i.item_name,
    f.item_status,

    s.storage_name,
    slot.slot_number,
    loc.location_name AS storage_location,

    assigned_staff.full_name AS assigned_by,
    sa.assigned_at,
    sa.released_at,

    (sa.released_at IS NULL) AS is_active_assignment,

    COALESCE(sa.released_at, CURRENT_TIMESTAMP)
        - sa.assigned_at AS storage_duration

FROM app.storage_assignment AS sa
JOIN app.found_item AS f
    ON f.found_item_id = sa.found_item_id
JOIN app.item AS i
    ON i.item_id = f.item_id
JOIN app.storage_location AS s
    ON s.storage_id = sa.storage_id
JOIN app.storage_slot AS slot
    ON slot.storage_id = sa.storage_id
   AND slot.slot_number = sa.slot_number
JOIN app.airport_location AS loc
    ON loc.location_id = s.location_id
JOIN app.staff AS assigned_staff
    ON assigned_staff.staff_id = sa.assigned_by_staff_id;


-- Permit the runtime application role to read these views.

GRANT SELECT ON
    app.v_match_overview,
    app.v_claim_workflow,
    app.v_storage_assignment_history
TO airport_app;

COMMIT;

RESET ROLE;
