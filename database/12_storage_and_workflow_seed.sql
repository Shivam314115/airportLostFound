BEGIN;

-- 1. Ensure airport locations exist for storage cabinets
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
JOIN app.terminal AS t ON t.airport_id = a.airport_id
WHERE a.airport_code = 'IDR' AND t.terminal_code = 'T1'
  AND NOT EXISTS (
      SELECT 1 FROM app.airport_location l
      WHERE l.airport_id = a.airport_id AND l.location_name = 'Lost & Found Office'
  );

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
    'Terminal 1 Secure Vault Area',
    'security',
    'Restricted high-security vault storage room in Terminal 1.'
FROM app.airport AS a
JOIN app.terminal AS t ON t.airport_id = a.airport_id
WHERE a.airport_code = 'IDR' AND t.terminal_code = 'T1'
  AND NOT EXISTS (
      SELECT 1 FROM app.airport_location l
      WHERE l.airport_id = a.airport_id AND l.location_name = 'Terminal 1 Secure Vault Area'
  );

-- 2. Create Storage Locations
INSERT INTO app.storage_location (
    location_id,
    storage_name,
    description,
    capacity,
    room_number,
    cabinet_number,
    is_active
)
SELECT
    l.location_id,
    'IDR Lost & Found Cabinet A',
    'Terminal 1 primary custodial cabinet for recovered electronics and valuables.',
    10,
    'Room 104-A',
    'CAB-SEC-01',
    true
FROM app.airport_location l
WHERE l.location_name = 'Lost & Found Office'
  AND NOT EXISTS (
      SELECT 1 FROM app.storage_location s WHERE s.storage_name = 'IDR Lost & Found Cabinet A'
  );

INSERT INTO app.storage_location (
    location_id,
    storage_name,
    description,
    capacity,
    room_number,
    cabinet_number,
    is_active
)
SELECT
    l.location_id,
    'IDR Secure High-Value Vault',
    'Climate-controlled high-security locker bank for jewelry, passports, and smartphones.',
    12,
    'Room 208-B',
    'CAB-SEC-02',
    true
FROM app.airport_location l
WHERE l.location_name = 'Terminal 1 Secure Vault Area'
  AND NOT EXISTS (
      SELECT 1 FROM app.storage_location s WHERE s.storage_name = 'IDR Secure High-Value Vault'
  );

-- 3. Generate Storage Slots (1 to capacity)
INSERT INTO app.storage_slot (storage_id, slot_number, slot_status)
SELECT s.storage_id, n.slot_number, 'available'
FROM app.storage_location s
CROSS JOIN generate_series(1, 10) AS n(slot_number)
WHERE s.storage_name = 'IDR Lost & Found Cabinet A'
ON CONFLICT (storage_id, slot_number) DO NOTHING;

INSERT INTO app.storage_slot (storage_id, slot_number, slot_status)
SELECT s.storage_id, n.slot_number, 'available'
FROM app.storage_location s
CROSS JOIN generate_series(1, 12) AS n(slot_number)
WHERE s.storage_name = 'IDR Secure High-Value Vault'
ON CONFLICT (storage_id, slot_number) DO NOTHING;

-- 4. Initial slot assignment for item 1 (Earbuds)
INSERT INTO app.storage_assignment (
    found_item_id,
    storage_id,
    slot_number,
    assigned_by_staff_id,
    assigned_at
)
SELECT
    1,
    s.storage_id,
    1,
    COALESCE((SELECT staff_id FROM app.staff WHERE email = 'pooja.inventory@airport.test' LIMIT 1), 1),
    CURRENT_TIMESTAMP
FROM app.storage_location s
WHERE s.storage_name = 'IDR Lost & Found Cabinet A'
  AND EXISTS (SELECT 1 FROM app.found_item WHERE found_item_id = 1)
  AND NOT EXISTS (SELECT 1 FROM app.storage_assignment WHERE found_item_id = 1 AND released_at IS NULL)
  AND NOT EXISTS (SELECT 1 FROM app.storage_assignment WHERE storage_id = s.storage_id AND slot_number = 1 AND released_at IS NULL);

UPDATE app.found_item f
SET storage_id = s.storage_id,
    slot_number = 1,
    item_status = 'stored',
    stored_at = COALESCE(stored_at, CURRENT_TIMESTAMP)
FROM app.storage_location s
WHERE f.found_item_id = 1 AND s.storage_name = 'IDR Lost & Found Cabinet A';

-- 5. Demonstration claim for workflow verification
INSERT INTO app.item_match (
    lost_report_id,
    found_item_id,
    match_score,
    verification_status,
    remarks
)
SELECT 1, 1, 96.50, 'candidate', 'Strong correlation on brand, colour, and discovery checkpoint.'
WHERE EXISTS (SELECT 1 FROM app.lost_report WHERE lost_report_id = 1)
  AND EXISTS (SELECT 1 FROM app.found_item WHERE found_item_id = 1)
  AND NOT EXISTS (
      SELECT 1 FROM app.item_match WHERE lost_report_id = 1 AND found_item_id = 1
  );

INSERT INTO app.claim (
    match_id,
    claim_status,
    created_at
)
SELECT
    m.match_id,
    'submitted',
    CURRENT_TIMESTAMP - INTERVAL '2 hours'
FROM app.item_match m
WHERE m.lost_report_id = 1 AND m.found_item_id = 1
  AND NOT EXISTS (
      SELECT 1 FROM app.claim c WHERE c.match_id = m.match_id
  );

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
    'Awaiting government-issued photo ID check at custody desk.'
FROM app.claim c
JOIN app.item_match m ON m.match_id = c.match_id
WHERE m.lost_report_id = 1 AND m.found_item_id = 1
  AND NOT EXISTS (
      SELECT 1 FROM app.identity_verification iv WHERE iv.claim_id = c.claim_id
  );

COMMIT;
