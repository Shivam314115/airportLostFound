
BEGIN;

-- 1. Create a Lost & Found Office location if it does not exist.

INSERT INTO app.airport_location (
    airport_id,
    terminal_id,
    location_name,
    location_type,
    description
)
SELECT
    a.airport_id,
    t.terminal_id,
    'Lost & Found Office',
    'other',
    'Designated office for recovered items and claim processing.'
FROM app.airport AS a
JOIN app.terminal AS t
    ON t.airport_id = a.airport_id
WHERE a.airport_code = 'IDR'
  AND t.terminal_code = 'T1'
  AND NOT EXISTS (
      SELECT 1
      FROM app.airport_location AS l
      WHERE l.airport_id = a.airport_id
        AND l.terminal_id = t.terminal_id
        AND l.location_name = 'Lost & Found Office'
  );


-- 2. Create a storage cabinet at that office.

INSERT INTO app.storage_location (
    location_id,
    storage_name,
    description,
    capacity
)
SELECT
    l.location_id,
    'IDR Lost & Found Cabinet A',
    'Demonstration cabinet for recovered airport items.',
    10
FROM app.airport_location AS l
JOIN app.airport AS a
    ON a.airport_id = l.airport_id
WHERE a.airport_code = 'IDR'
  AND l.location_name = 'Lost & Found Office'
  AND NOT EXISTS (
      SELECT 1
      FROM app.storage_location AS s
      WHERE s.location_id = l.location_id
        AND s.storage_name = 'IDR Lost & Found Cabinet A'
  );


-- 3. Generate the cabinet's ten storage slots.

INSERT INTO app.storage_slot (
    storage_id,
    slot_number,
    slot_status
)
SELECT
    s.storage_id,
    n.slot_number,
    'available'
FROM app.storage_location AS s
CROSS JOIN generate_series(1, 10) AS n(slot_number)
WHERE s.storage_name = 'IDR Lost & Found Cabinet A'
ON CONFLICT (storage_id, slot_number) DO NOTHING;


-- 4. Assign the recovered earbuds to slot 1.

INSERT INTO app.storage_assignment (
    found_item_id,
    storage_id,
    slot_number,
    assigned_by_staff_id
)
SELECT
    f.found_item_id,
    s.storage_id,
    slot.slot_number,
    staff.staff_id
FROM app.found_item AS f
JOIN app.item AS i
    ON i.item_id = f.item_id
JOIN app.staff AS staff
    ON staff.email = 'aarav.officer@example.test'
JOIN app.storage_location AS s
    ON s.storage_name = 'IDR Lost & Found Cabinet A'
JOIN app.storage_slot AS slot
    ON slot.storage_id = s.storage_id
   AND slot.slot_number = 1
WHERE f.item_status = 'stored'
  AND i.description =
      'Found item description: black charging case with a small scratch; recovered near Gate 4.'
  AND NOT EXISTS (
      SELECT 1
      FROM app.storage_assignment AS sa
      WHERE sa.found_item_id = f.found_item_id
        AND sa.released_at IS NULL
  )
  AND NOT EXISTS (
      SELECT 1
      FROM app.storage_assignment AS sa
      WHERE sa.storage_id = s.storage_id
        AND sa.slot_number = slot.slot_number
        AND sa.released_at IS NULL
  );


-- 5. Have an officer verify the strong candidate match.
-- This verifies the match, not the passenger's identity.

WITH target_match AS (
    SELECT
        m.match_id,
        reviewer.staff_id AS reviewer_id
    FROM app.item_match AS m
    JOIN app.lost_report AS lr
        ON lr.lost_report_id = m.lost_report_id
    JOIN app.passenger AS p
        ON p.passenger_id = lr.passenger_id
    JOIN app.item AS lost_i
        ON lost_i.item_id = lr.item_id
    JOIN app.found_item AS f
        ON f.found_item_id = m.found_item_id
    JOIN app.item AS found_i
        ON found_i.item_id = f.item_id
    JOIN app.staff AS reviewer
        ON reviewer.email = 'aarav.officer@example.test'
    WHERE p.email = 'riya.patel@example.test'
      AND lost_i.description =
          'Passenger description: black charging case with a small scratch.'
      AND found_i.description =
          'Found item description: black charging case with a small scratch; recovered near Gate 4.'
      AND m.verification_status = 'candidate'
)
UPDATE app.item_match AS m
SET
    verification_status = 'verified',
    reviewed_by_staff_id = tm.reviewer_id,
    reviewed_at = CURRENT_TIMESTAMP,
    remarks = 'Officer reviewed the item description and location; identity verification remains pending.'
FROM target_match AS tm
WHERE m.match_id = tm.match_id;


-- 6. Register a claim for the reviewed match.

INSERT INTO app.claim (
    match_id,
    claim_status
)
SELECT
    m.match_id,
    'submitted'
FROM app.item_match AS m
JOIN app.lost_report AS lr
    ON lr.lost_report_id = m.lost_report_id
JOIN app.passenger AS p
    ON p.passenger_id = lr.passenger_id
JOIN app.item AS i
    ON i.item_id = lr.item_id
WHERE p.email = 'riya.patel@example.test'
  AND i.description =
      'Passenger description: black charging case with a small scratch.'
  AND m.verification_status = 'verified'
  AND NOT EXISTS (
      SELECT 1
      FROM app.claim AS c
      WHERE c.match_id = m.match_id
  );


-- 7. Record that the claimant's identity check is pending.

INSERT INTO app.identity_verification (
    claim_id,
    verification_method,
    verification_result,
    verification_notes
)
SELECT
    c.claim_id,
    'in_person_document_check',
    'pending',
    'Identity check has not yet been completed.'
FROM app.claim AS c
JOIN app.item_match AS m
    ON m.match_id = c.match_id
JOIN app.lost_report AS lr
    ON lr.lost_report_id = m.lost_report_id
JOIN app.passenger AS p
    ON p.passenger_id = lr.passenger_id
JOIN app.item AS i
    ON i.item_id = lr.item_id
WHERE p.email = 'riya.patel@example.test'
  AND i.description =
      'Passenger description: black charging case with a small scratch.'
  AND NOT EXISTS (
      SELECT 1
      FROM app.identity_verification AS iv
      WHERE iv.claim_id = c.claim_id
        AND iv.verification_method = 'in_person_document_check'
  );


-- 8. Notify the passenger whose claim was registered.

INSERT INTO app.notification (
    passenger_id,
    notification_type,
    subject,
    message
)
SELECT
    p.passenger_id,
    'claim',
    'Claim received',
    'Your claim has been registered. Identity verification is still pending.'
FROM app.passenger AS p
WHERE p.email = 'riya.patel@example.test'
  AND EXISTS (
      SELECT 1
      FROM app.claim AS c
      JOIN app.item_match AS m
          ON m.match_id = c.match_id
      JOIN app.lost_report AS lr
          ON lr.lost_report_id = m.lost_report_id
      WHERE lr.passenger_id = p.passenger_id
        AND m.verification_status = 'verified'
  )
  AND NOT EXISTS (
      SELECT 1
      FROM app.notification AS n
      WHERE n.passenger_id = p.passenger_id
        AND n.subject = 'Claim received'
  );


-- 9. Notify the other passenger about a potential match.

INSERT INTO app.notification (
    passenger_id,
    notification_type,
    subject,
    message
)
SELECT
    p.passenger_id,
    'match',
    'Potential match identified',
    'A possible match has been identified for your report. It has not yet been verified.'
FROM app.passenger AS p
JOIN app.lost_report AS lr
    ON lr.passenger_id = p.passenger_id
JOIN app.item_match AS m
    ON m.lost_report_id = lr.lost_report_id
WHERE p.email = 'kabir.singh@example.test'
  AND m.verification_status = 'candidate'
  AND NOT EXISTS (
      SELECT 1
      FROM app.notification AS n
      WHERE n.passenger_id = p.passenger_id
        AND n.subject = 'Potential match identified'
  );

COMMIT;

