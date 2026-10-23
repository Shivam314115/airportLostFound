-- ============================================================
-- Airport Lost & Found Management System
-- Phase 11: Comprehensive Domain Alignment Migration
-- ============================================================

SET ROLE airport_owner;

-- 1. Passenger Profile & PassengerPhone Dependent Entity
ALTER TABLE app.passenger
    ADD COLUMN IF NOT EXISTS first_name VARCHAR(75),
    ADD COLUMN IF NOT EXISTS last_name VARCHAR(75),
    ADD COLUMN IF NOT EXISTS passport_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS nationality VARCHAR(60),
    ADD COLUMN IF NOT EXISTS date_of_birth DATE;

-- Populate first_name and last_name from existing full_name where null
UPDATE app.passenger
SET
    first_name = SPLIT_PART(full_name, ' ', 1),
    last_name = CASE
        WHEN POSITION(' ' IN full_name) > 0 THEN SUBSTRING(full_name FROM POSITION(' ' IN full_name) + 1)
        ELSE 'Passenger'
    END
WHERE first_name IS NULL;

-- Create PassengerPhone relation keyed by (passenger_id, phone_number)
CREATE TABLE IF NOT EXISTS app.passenger_phone (
    passenger_id BIGINT NOT NULL
        REFERENCES app.passenger(passenger_id)
        ON DELETE CASCADE,
    phone_number VARCHAR(30) NOT NULL,
    phone_type VARCHAR(20) NOT NULL DEFAULT 'mobile',
    is_primary BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (passenger_id, phone_number),

    CONSTRAINT chk_passenger_phone_number
        CHECK (LENGTH(TRIM(phone_number)) >= 5),

    CONSTRAINT chk_passenger_phone_type
        CHECK (phone_type IN ('mobile', 'home', 'work', 'emergency'))
);

-- Seed existing passenger phone numbers into passenger_phone
INSERT INTO app.passenger_phone (passenger_id, phone_number, phone_type, is_primary)
SELECT passenger_id, phone, 'mobile', true
FROM app.passenger
WHERE phone IS NOT NULL AND LENGTH(TRIM(phone)) > 0
ON CONFLICT (passenger_id, phone_number) DO NOTHING;

-- 2. Terminal Description
ALTER TABLE app.terminal
    ADD COLUMN IF NOT EXISTS description TEXT;

UPDATE app.terminal
SET description = CASE
    WHEN terminal_code = 'T1' THEN 'Domestic operations, departures, arrivals, and primary terminal custody desk.'
    WHEN terminal_code = 'T2' THEN 'International operations, transfer hub, and secondary custody facility.'
    ELSE 'Airport passenger terminal facility.'
END
WHERE description IS NULL;

-- 3. Airport Location Type Expansion
ALTER TABLE app.airport_location
    DROP CONSTRAINT IF EXISTS chk_location_type;

ALTER TABLE app.airport_location
    ADD CONSTRAINT chk_location_type
    CHECK (
        location_type IN (
            'check_in',
            'security',
            'gate',
            'baggage_claim',
            'lounge',
            'restroom',
            'parking',
            'food_court',
            'washroom',
            'immigration',
            'waiting_area',
            'custody_desk',
            'other'
        )
    );

-- 4. Item Category Hierarchy (Parent Category)
ALTER TABLE app.item_category
    ADD COLUMN IF NOT EXISTS parent_category_id BIGINT
        REFERENCES app.item_category(category_id)
        ON DELETE RESTRICT;

-- Insert hierarchical subcategories if they do not already exist
INSERT INTO app.item_category (category_name, description, parent_category_id)
SELECT 'Smartphones & Tablets', 'Mobile phones, cellular devices, and tablets', c.category_id
FROM app.item_category c WHERE c.category_name = 'Electronics'
ON CONFLICT (category_name) DO NOTHING;

INSERT INTO app.item_category (category_name, description, parent_category_id)
SELECT 'Laptops & Computers', 'Portable computers, notebooks, and ultrabooks', c.category_id
FROM app.item_category c WHERE c.category_name = 'Electronics'
ON CONFLICT (category_name) DO NOTHING;

INSERT INTO app.item_category (category_name, description, parent_category_id)
SELECT 'Audio & Headphones', 'Headphones, earbuds, and audio accessories', c.category_id
FROM app.item_category c WHERE c.category_name = 'Electronics'
ON CONFLICT (category_name) DO NOTHING;

INSERT INTO app.item_category (category_name, description, parent_category_id)
SELECT 'Passports & Travel Docs', 'Passports, boarding passes, and visa papers', c.category_id
FROM app.item_category c WHERE c.category_name = 'Documents'
ON CONFLICT (category_name) DO NOTHING;

INSERT INTO app.item_category (category_name, description, parent_category_id)
SELECT 'Wallets & Identification', 'Wallets, currency notes, and identification cards', c.category_id
FROM app.item_category c WHERE c.category_name = 'Documents'
ON CONFLICT (category_name) DO NOTHING;

INSERT INTO app.item_category (category_name, description, parent_category_id)
SELECT 'Backpacks & Handbags', 'Daypacks, shoulder bags, and personal handbags', c.category_id
FROM app.item_category c WHERE c.category_name = 'Bags'
ON CONFLICT (category_name) DO NOTHING;

INSERT INTO app.item_category (category_name, description, parent_category_id)
SELECT 'Luggage & Suitcases', 'Rolling suitcases, checked luggage, and duffels', c.category_id
FROM app.item_category c WHERE c.category_name = 'Bags'
ON CONFLICT (category_name) DO NOTHING;

-- 5. FoundItem Stored At Timestamp
ALTER TABLE app.found_item
    ADD COLUMN IF NOT EXISTS stored_at TIMESTAMPTZ;

-- 6. Staff Phone & Username
ALTER TABLE app.staff
    ADD COLUMN IF NOT EXISTS username VARCHAR(60),
    ADD COLUMN IF NOT EXISTS phone VARCHAR(30);

UPDATE app.staff
SET
    username = SPLIT_PART(email, '@', 1) || CASE WHEN staff_id = 3 THEN '_sys' ELSE '' END,
    phone = '98000000' || LPAD(staff_id::text, 2, '0')
WHERE username IS NULL OR username = 'admin';

ALTER TABLE app.staff
    DROP CONSTRAINT IF EXISTS uq_staff_username;
ALTER TABLE app.staff
    ADD CONSTRAINT uq_staff_username UNIQUE (username);

-- 7. Staff ISA Specialization Subtype Tables
CREATE TABLE IF NOT EXISTS app.lost_found_officer (
    staff_id BIGINT PRIMARY KEY
        REFERENCES app.staff(staff_id)
        ON DELETE CASCADE,
    desk_location VARCHAR(100) NOT NULL DEFAULT 'Terminal 1 Custody Counter',
    certification_level VARCHAR(50) NOT NULL DEFAULT 'Level 2 Certified Intake',
    assigned_shift VARCHAR(30) NOT NULL DEFAULT 'Day'
);

CREATE TABLE IF NOT EXISTS app.security_officer (
    staff_id BIGINT PRIMARY KEY
        REFERENCES app.staff(staff_id)
        ON DELETE CASCADE,
    badge_number VARCHAR(50) NOT NULL DEFAULT 'SEC-001',
    clearance_level VARCHAR(50) NOT NULL DEFAULT 'Airside Terminal Clearance',
    duty_zone VARCHAR(50) NOT NULL DEFAULT 'Terminal 1 & 2'
);

CREATE TABLE IF NOT EXISTS app.administrator (
    staff_id BIGINT PRIMARY KEY
        REFERENCES app.staff(staff_id)
        ON DELETE CASCADE,
    admin_scope VARCHAR(100) NOT NULL DEFAULT 'Full System Administration',
    can_provision_roles BOOLEAN NOT NULL DEFAULT true,
    emergency_contact VARCHAR(100) DEFAULT 'admin-support@airport.test'
);

-- Seed ISA subtype rows matching staff roles
INSERT INTO app.lost_found_officer (staff_id, desk_location, certification_level)
SELECT staff_id, 'Terminal 1 Custody Desk', 'Level 2 Officer'
FROM app.staff
WHERE staff_role IN ('lost_found_agent', 'claims_verification_officer', 'inventory_storage_officer', 'return_dispatch_officer', 'supervisor', 'lost_found_officer')
ON CONFLICT (staff_id) DO NOTHING;

INSERT INTO app.security_officer (staff_id, badge_number, clearance_level)
SELECT staff_id, 'SEC-0' || staff_id, 'Airside Security Clearance'
FROM app.staff
WHERE staff_role IN ('ground_staff', 'security_officer')
ON CONFLICT (staff_id) DO NOTHING;

INSERT INTO app.administrator (staff_id, admin_scope, can_provision_roles)
SELECT staff_id, 'Airport Lost Property Register Root Administration', true
FROM app.staff
WHERE staff_role IN ('system_admin', 'administrator')
ON CONFLICT (staff_id) DO NOTHING;

-- 8. Storage Location Room Number & Cabinet Number
ALTER TABLE app.storage_location
    ADD COLUMN IF NOT EXISTS room_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS cabinet_number VARCHAR(50);

UPDATE app.storage_location
SET
    room_number = CASE
        WHEN storage_name ILIKE '%indore%' OR storage_name ILIKE '%idr%' THEN 'Room 104-A'
        ELSE 'Room 208-B'
    END,
    cabinet_number = CASE
        WHEN storage_name ILIKE '%secure%' THEN 'CAB-SEC-01'
        ELSE 'CAB-GEN-02'
    END
WHERE room_number IS NULL;

-- 9. Claim Approval & Verification Fields
ALTER TABLE app.claim
    ADD COLUMN IF NOT EXISTS verifying_staff_id BIGINT
        REFERENCES app.staff(staff_id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS approving_staff_id BIGINT
        REFERENCES app.staff(staff_id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS approved_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS verification_status VARCHAR(25) NOT NULL DEFAULT 'pending';

-- 10. ReturnDelivery Full Delivery Fields
ALTER TABLE app.return_delivery
    ADD COLUMN IF NOT EXISTS pickup_location VARCHAR(200),
    ADD COLUMN IF NOT EXISTS delivery_address TEXT,
    ADD COLUMN IF NOT EXISTS courier_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS tracking_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS shipped_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS delivered_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS delivery_status VARCHAR(30) NOT NULL DEFAULT 'initiated';

-- 11. ItemPhoto Enhancement (found_item_id & photo_type)
ALTER TABLE app.item_photo
    ADD COLUMN IF NOT EXISTS found_item_id BIGINT
        REFERENCES app.found_item(found_item_id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS photo_type VARCHAR(50) NOT NULL DEFAULT 'overview';

ALTER TABLE app.item_photo
    DROP CONSTRAINT IF EXISTS chk_photo_type;

ALTER TABLE app.item_photo
    ADD CONSTRAINT chk_photo_type
    CHECK (
        photo_type IN (
            'overview',
            'serial_number',
            'damage_detail',
            'receipt',
            'tag',
            'general'
        )
    );

-- Add index and composite constraint for found_item_id and photo_number
CREATE INDEX IF NOT EXISTS idx_item_photo_found_item
    ON app.item_photo (found_item_id, photo_number);

-- 12. Storage Slot Link on Found Item
ALTER TABLE app.found_item ADD COLUMN IF NOT EXISTS storage_id BIGINT;
ALTER TABLE app.found_item ADD COLUMN IF NOT EXISTS slot_number INT;

UPDATE app.found_item f
SET storage_id = a.storage_id,
    slot_number = a.slot_number
FROM app.storage_assignment a
WHERE f.found_item_id = a.found_item_id AND a.released_at IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_found_item_storage_slot'
    ) THEN
        ALTER TABLE app.found_item
        ADD CONSTRAINT fk_found_item_storage_slot
        FOREIGN KEY (storage_id, slot_number)
        REFERENCES app.storage_slot(storage_id, slot_number)
        ON DELETE SET NULL;
    END IF;
END $$;

-- 13. Domain Schema Attribute Synonyms & Helper Views
ALTER TABLE app.terminal ADD COLUMN IF NOT EXISTS name VARCHAR(100);
UPDATE app.terminal SET name = terminal_name WHERE name IS NULL;

ALTER TABLE app.airport_location ADD COLUMN IF NOT EXISTS name VARCHAR(150);
UPDATE app.airport_location SET name = location_name WHERE name IS NULL;

ALTER TABLE app.airport_location ADD COLUMN IF NOT EXISTS type VARCHAR(30);
UPDATE app.airport_location SET type = location_type WHERE type IS NULL;

ALTER TABLE app.flight ADD COLUMN IF NOT EXISTS airline_name VARCHAR(100);
UPDATE app.flight SET airline_name = airline WHERE airline_name IS NULL;

ALTER TABLE app.flight ADD COLUMN IF NOT EXISTS origin VARCHAR(100);
UPDATE app.flight SET origin = 'IDR' WHERE origin IS NULL;

ALTER TABLE app.flight ADD COLUMN IF NOT EXISTS destination VARCHAR(100);
UPDATE app.flight SET destination = 'BOM' WHERE destination IS NULL;

ALTER TABLE app.flight ADD COLUMN IF NOT EXISTS departure_timestamp TIMESTAMPTZ;
UPDATE app.flight SET departure_timestamp = scheduled_departure WHERE departure_timestamp IS NULL;

ALTER TABLE app.flight ADD COLUMN IF NOT EXISTS arrival_timestamp TIMESTAMPTZ;
UPDATE app.flight SET arrival_timestamp = scheduled_arrival WHERE arrival_timestamp IS NULL;

ALTER TABLE app.flight ADD COLUMN IF NOT EXISTS terminal_id BIGINT;
UPDATE app.flight SET terminal_id = departure_terminal_id WHERE terminal_id IS NULL;

ALTER TABLE app.lost_report ADD COLUMN IF NOT EXISTS status VARCHAR(25);
UPDATE app.lost_report SET status = report_status WHERE status IS NULL;

ALTER TABLE app.lost_report ADD COLUMN IF NOT EXISTS lost_timestamp TIMESTAMPTZ;
UPDATE app.lost_report SET lost_timestamp = lost_at WHERE lost_timestamp IS NULL;

ALTER TABLE app.lost_report ADD COLUMN IF NOT EXISTS report_submission_timestamp TIMESTAMPTZ;
UPDATE app.lost_report SET report_submission_timestamp = reported_at WHERE report_submission_timestamp IS NULL;

ALTER TABLE app.found_item ADD COLUMN IF NOT EXISTS status VARCHAR(25);
UPDATE app.found_item SET status = item_status WHERE status IS NULL;

ALTER TABLE app.found_item ADD COLUMN IF NOT EXISTS found_timestamp TIMESTAMPTZ;
UPDATE app.found_item SET found_timestamp = found_at WHERE found_timestamp IS NULL;

ALTER TABLE app.found_item ADD COLUMN IF NOT EXISTS condition VARCHAR(30);
UPDATE app.found_item f SET condition = COALESCE((SELECT condition FROM app.item i WHERE i.item_id = f.item_id), 'good') WHERE f.condition IS NULL;

ALTER TABLE app.item_match ADD COLUMN IF NOT EXISTS match_timestamp TIMESTAMPTZ;
UPDATE app.item_match SET match_timestamp = created_at WHERE match_timestamp IS NULL;

ALTER TABLE app.item_match ADD COLUMN IF NOT EXISTS verifying_staff_id BIGINT;
UPDATE app.item_match SET verifying_staff_id = reviewed_by_staff_id WHERE verifying_staff_id IS NULL;

ALTER TABLE app.item_match ADD COLUMN IF NOT EXISTS verification_timestamp TIMESTAMPTZ;
UPDATE app.item_match SET verification_timestamp = reviewed_at WHERE verification_timestamp IS NULL;

CREATE OR REPLACE VIEW app.lost_found_match AS SELECT * FROM app.item_match;

ALTER TABLE app.claim ADD COLUMN IF NOT EXISTS passenger_id BIGINT;
UPDATE app.claim c
SET passenger_id = (
    SELECT lr.passenger_id
    FROM app.item_match m
    JOIN app.lost_report lr ON m.lost_report_id = lr.lost_report_id
    WHERE m.match_id = c.match_id
)
WHERE c.passenger_id IS NULL;

ALTER TABLE app.claim ADD COLUMN IF NOT EXISTS status VARCHAR(25);
UPDATE app.claim SET status = claim_status WHERE status IS NULL;

ALTER TABLE app.claim ADD COLUMN IF NOT EXISTS remarks TEXT;
UPDATE app.claim SET remarks = decision_notes WHERE remarks IS NULL;

ALTER TABLE app.claim ADD COLUMN IF NOT EXISTS approval_timestamp TIMESTAMPTZ;
UPDATE app.claim SET approval_timestamp = approved_at WHERE approval_timestamp IS NULL;

ALTER TABLE app.return_delivery ADD COLUMN IF NOT EXISTS shipping_timestamp TIMESTAMPTZ;
UPDATE app.return_delivery SET shipping_timestamp = shipped_at WHERE shipping_timestamp IS NULL;

ALTER TABLE app.return_delivery ADD COLUMN IF NOT EXISTS delivered_timestamp TIMESTAMPTZ;
UPDATE app.return_delivery SET delivered_timestamp = delivered_at WHERE delivered_timestamp IS NULL;

-- Ensure permissions for airport_app on all new tables and views
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA app TO airport_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA app TO airport_app;
GRANT ALL PRIVILEGES ON app.lost_found_match TO airport_app;
