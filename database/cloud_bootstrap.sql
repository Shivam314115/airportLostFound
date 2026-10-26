-- ==========================================================================
-- Airport Lost & Found Management System - Cloud Database Bootstrap Script
-- Compatible with: Render, Railway, Fly.io, AWS RDS, Supabase, Neon, Docker
-- ==========================================================================

CREATE SCHEMA IF NOT EXISTS app;

-- >>> Start of database/01_schema.sql >>>
BEGIN;
-- [Cloud safe] Skipped SET ROLE

CREATE TABLE app.airport (
    airport_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    airport_code VARCHAR(4) NOT NULL UNIQUE,
    airport_name VARCHAR(150) NOT NULL,
    city VARCHAR(100) NOT NULL,
    country VARCHAR(100) NOT NULL,

    CONSTRAINT chk_airport_code
        CHECK (airport_code = UPPER(airport_code)),

    CONSTRAINT chk_airport_name
        CHECK (LENGTH(TRIM(airport_name)) > 0)
);


CREATE TABLE app.terminal (
    terminal_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    airport_id BIGINT NOT NULL
        REFERENCES app.airport(airport_id) ON DELETE RESTRICT,
    terminal_code VARCHAR(20) NOT NULL,
    terminal_name VARCHAR(100) NOT NULL,

    CONSTRAINT uq_terminal_code
        UNIQUE (airport_id, terminal_code),

    CONSTRAINT uq_terminal_airport_pair
        UNIQUE (terminal_id, airport_id),

    CONSTRAINT chk_terminal_code
        CHECK (LENGTH(TRIM(terminal_code)) > 0)
);


CREATE TABLE app.airport_location (
    location_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    airport_id BIGINT NOT NULL,
    terminal_id BIGINT,
    location_name VARCHAR(150) NOT NULL,
    location_type VARCHAR(30) NOT NULL,
    description TEXT,

    CONSTRAINT fk_location_airport
        FOREIGN KEY (airport_id)
        REFERENCES app.airport(airport_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_location_terminal_airport
        FOREIGN KEY (terminal_id, airport_id)
        REFERENCES app.terminal(terminal_id, airport_id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_location_type
        CHECK (
            location_type IN (
                'check_in',
                'security',
                'gate',
                'baggage_claim',
                'lounge',
                'restroom',
                'parking',
                'other'
            )
        ),

    CONSTRAINT chk_location_name
        CHECK (LENGTH(TRIM(location_name)) > 0)
);


CREATE TABLE app.passenger (
    passenger_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    phone VARCHAR(20),
    password_hash TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_passenger_name
        CHECK (LENGTH(TRIM(full_name)) > 0),

    CONSTRAINT chk_passenger_email
        CHECK (
            email = LOWER(email)
            AND POSITION('@' IN email) > 1
        )
);


CREATE TABLE app.staff (
    staff_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    staff_role VARCHAR(30) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_staff_name
        CHECK (LENGTH(TRIM(full_name)) > 0),

    CONSTRAINT chk_staff_role
        CHECK (
            staff_role IN (
                'lost_found_agent',
                'claims_verification_officer',
                'inventory_storage_officer',
                'return_dispatch_officer',
                'supervisor',
                'ground_staff',
                'system_admin',
                'lost_found_officer',
                'security_officer',
                'administrator'
            )
        ),

    CONSTRAINT chk_staff_email
        CHECK (
            email = LOWER(email)
            AND POSITION('@' IN email) > 1
        )
);


CREATE TABLE app.flight (
    flight_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    flight_number VARCHAR(15) NOT NULL,
    airline VARCHAR(100) NOT NULL,
    origin_airport_id BIGINT NOT NULL,
    destination_airport_id BIGINT NOT NULL,
    departure_terminal_id BIGINT,
    arrival_terminal_id BIGINT,
    scheduled_departure TIMESTAMPTZ NOT NULL,
    scheduled_arrival TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_flight_origin
        FOREIGN KEY (origin_airport_id)
        REFERENCES app.airport(airport_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_flight_destination
        FOREIGN KEY (destination_airport_id)
        REFERENCES app.airport(airport_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_flight_departure_terminal
        FOREIGN KEY (departure_terminal_id, origin_airport_id)
        REFERENCES app.terminal(terminal_id, airport_id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_flight_arrival_terminal
        FOREIGN KEY (arrival_terminal_id, destination_airport_id)
        REFERENCES app.terminal(terminal_id, airport_id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_flight_route
        CHECK (origin_airport_id <> destination_airport_id),

    CONSTRAINT chk_flight_times
        CHECK (scheduled_arrival > scheduled_departure),

    CONSTRAINT chk_flight_number
        CHECK (LENGTH(TRIM(flight_number)) > 0)
);


CREATE TABLE app.item_category (
    category_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_name VARCHAR(80) NOT NULL UNIQUE,
    description TEXT,

    CONSTRAINT chk_category_name
        CHECK (LENGTH(TRIM(category_name)) > 0)
);


CREATE TABLE app.item (
    item_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_id BIGINT NOT NULL
        REFERENCES app.item_category(category_id)
        ON DELETE RESTRICT,
    item_name VARCHAR(150) NOT NULL,
    brand VARCHAR(100),
    colour VARCHAR(60),
    description TEXT,
    estimated_value NUMERIC(12, 2),
    item_condition VARCHAR(20) NOT NULL DEFAULT 'unknown',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_item_name
        CHECK (LENGTH(TRIM(item_name)) > 0),

    CONSTRAINT chk_item_value
        CHECK (estimated_value IS NULL OR estimated_value >= 0),

    CONSTRAINT chk_item_condition
        CHECK (
            item_condition IN (
                'new',
                'good',
                'fair',
                'poor',
                'damaged',
                'unknown'
            )
        )
);


CREATE TABLE app.item_photo (
    item_id BIGINT NOT NULL
        REFERENCES app.item(item_id)
        ON DELETE CASCADE,
    photo_number INTEGER NOT NULL,
    photo_url TEXT NOT NULL,
    caption VARCHAR(250),
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (item_id, photo_number),

    CONSTRAINT chk_photo_number
        CHECK (photo_number > 0),

    CONSTRAINT chk_photo_url
        CHECK (LENGTH(TRIM(photo_url)) > 0)
);


CREATE TABLE app.lost_report (
    lost_report_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    passenger_id BIGINT NOT NULL
        REFERENCES app.passenger(passenger_id)
        ON DELETE RESTRICT,
    item_id BIGINT NOT NULL UNIQUE
        REFERENCES app.item(item_id)
        ON DELETE RESTRICT,
    flight_id BIGINT
        REFERENCES app.flight(flight_id)
        ON DELETE RESTRICT,
    last_seen_location_id BIGINT
        REFERENCES app.airport_location(location_id)
        ON DELETE RESTRICT,
    lost_at TIMESTAMPTZ NOT NULL,
    additional_details TEXT,
    report_status VARCHAR(25) NOT NULL DEFAULT 'open',
    reported_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_lost_report_status
        CHECK (
            report_status IN (
                'open',
                'candidate_found',
                'resolved',
                'closed'
            )
        )
);


CREATE TABLE app.found_item (
    found_item_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    item_id BIGINT NOT NULL UNIQUE
        REFERENCES app.item(item_id)
        ON DELETE RESTRICT,
    found_by_staff_id BIGINT NOT NULL
        REFERENCES app.staff(staff_id)
        ON DELETE RESTRICT,
    flight_id BIGINT
        REFERENCES app.flight(flight_id)
        ON DELETE RESTRICT,
    found_location_id BIGINT
        REFERENCES app.airport_location(location_id)
        ON DELETE RESTRICT,
    found_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    item_status VARCHAR(25) NOT NULL DEFAULT 'awaiting_review',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_found_item_status
        CHECK (
            item_status IN (
                'awaiting_review',
                'stored',
                'claimed',
                'returned',
                'disposed'
            )
        ),

    CONSTRAINT chk_found_item_times
        CHECK (received_at >= found_at)
);


CREATE TABLE app.item_match (
    match_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    lost_report_id BIGINT NOT NULL
        REFERENCES app.lost_report(lost_report_id)
        ON DELETE RESTRICT,
    found_item_id BIGINT NOT NULL
        REFERENCES app.found_item(found_item_id)
        ON DELETE RESTRICT,
    match_score NUMERIC(5, 2),
    verification_status VARCHAR(25) NOT NULL DEFAULT 'candidate',
    reviewed_by_staff_id BIGINT
        REFERENCES app.staff(staff_id)
        ON DELETE RESTRICT,
    remarks TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMPTZ,

    CONSTRAINT uq_item_match_pair
        UNIQUE (lost_report_id, found_item_id),

    CONSTRAINT chk_match_score
        CHECK (
            match_score IS NULL
            OR match_score BETWEEN 0 AND 100
        ),

    CONSTRAINT chk_match_status
        CHECK (
            verification_status IN (
                'candidate',
                'under_review',
                'verified',
                'rejected'
            )
        ),

    CONSTRAINT chk_match_review_time
        CHECK (
            reviewed_at IS NULL
            OR reviewed_at >= created_at
        )
);


CREATE TABLE app.claim (
    claim_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    match_id BIGINT NOT NULL UNIQUE
        REFERENCES app.item_match(match_id)
        ON DELETE RESTRICT,
    claim_status VARCHAR(25) NOT NULL DEFAULT 'submitted',
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_by_staff_id BIGINT
        REFERENCES app.staff(staff_id)
        ON DELETE RESTRICT,
    reviewed_at TIMESTAMPTZ,
    decision_notes TEXT,

    CONSTRAINT chk_claim_status
        CHECK (
            claim_status IN (
                'submitted',
                'under_review',
                'approved',
                'rejected',
                'withdrawn'
            )
        ),

    CONSTRAINT chk_claim_review_time
        CHECK (
            reviewed_at IS NULL
            OR reviewed_at >= submitted_at
        )
);


CREATE TABLE app.identity_verification (
    verification_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    claim_id BIGINT NOT NULL
        REFERENCES app.claim(claim_id)
        ON DELETE RESTRICT,
    verification_method VARCHAR(40) NOT NULL,
    verification_result VARCHAR(20) NOT NULL DEFAULT 'pending',
    verified_by_staff_id BIGINT
        REFERENCES app.staff(staff_id)
        ON DELETE RESTRICT,
    verification_notes TEXT,
    verified_at TIMESTAMPTZ,

    CONSTRAINT chk_verification_method
        CHECK (LENGTH(TRIM(verification_method)) > 0),

    CONSTRAINT chk_verification_result
        CHECK (
            verification_result IN (
                'pending',
                'verified',
                'failed'
            )
        ),

    CONSTRAINT chk_verification_details
        CHECK (
            (
                verification_result = 'pending'
                AND verified_by_staff_id IS NULL
                AND verified_at IS NULL
            )
            OR
            (
                verification_result IN ('verified', 'failed')
                AND verified_by_staff_id IS NOT NULL
                AND verified_at IS NOT NULL
            )
        )
);


CREATE TABLE app.storage_location (
    storage_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    location_id BIGINT NOT NULL
        REFERENCES app.airport_location(location_id)
        ON DELETE RESTRICT,
    storage_name VARCHAR(100) NOT NULL,
    description TEXT,
    capacity INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_storage_name_at_location
        UNIQUE (location_id, storage_name),

    CONSTRAINT chk_storage_capacity
        CHECK (capacity > 0),

    CONSTRAINT chk_storage_name
        CHECK (LENGTH(TRIM(storage_name)) > 0)
);


CREATE TABLE app.storage_slot (
    storage_id BIGINT NOT NULL
        REFERENCES app.storage_location(storage_id)
        ON DELETE RESTRICT,
    slot_number INTEGER NOT NULL,
    slot_status VARCHAR(20) NOT NULL DEFAULT 'available',

    PRIMARY KEY (storage_id, slot_number),

    CONSTRAINT chk_slot_number
        CHECK (slot_number > 0),

    CONSTRAINT chk_slot_status
        CHECK (slot_status IN ('available', 'maintenance'))
);


CREATE TABLE app.storage_assignment (
    assignment_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    found_item_id BIGINT NOT NULL
        REFERENCES app.found_item(found_item_id)
        ON DELETE RESTRICT,
    storage_id BIGINT NOT NULL,
    slot_number INTEGER NOT NULL,
    assigned_by_staff_id BIGINT NOT NULL
        REFERENCES app.staff(staff_id)
        ON DELETE RESTRICT,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    released_at TIMESTAMPTZ,

    CONSTRAINT fk_assignment_slot
        FOREIGN KEY (storage_id, slot_number)
        REFERENCES app.storage_slot(storage_id, slot_number)
        ON DELETE RESTRICT,

    CONSTRAINT chk_assignment_release_time
        CHECK (
            released_at IS NULL
            OR released_at >= assigned_at
        )
);


CREATE TABLE app.return_delivery (
    return_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    claim_id BIGINT NOT NULL UNIQUE
        REFERENCES app.claim(claim_id)
        ON DELETE RESTRICT,
    return_method VARCHAR(20) NOT NULL,
    return_status VARCHAR(20) NOT NULL DEFAULT 'initiated',
    handled_by_staff_id BIGINT NOT NULL
        REFERENCES app.staff(staff_id)
        ON DELETE RESTRICT,
    delivery_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,

    CONSTRAINT chk_return_method
        CHECK (
            return_method IN ('counter_handover', 'delivery')
        ),

    CONSTRAINT chk_return_status
        CHECK (
            return_status IN (
                'initiated',
                'in_transit',
                'completed',
                'failed',
                'cancelled'
            )
        ),

    CONSTRAINT chk_return_completed_at
        CHECK (
            (
                return_status = 'completed'
                AND completed_at IS NOT NULL
                AND completed_at >= created_at
            )
            OR
            (
                return_status <> 'completed'
                AND completed_at IS NULL
            )
        )
);


CREATE TABLE app.notification (
    notification_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    passenger_id BIGINT NOT NULL
        REFERENCES app.passenger(passenger_id)
        ON DELETE RESTRICT,
    notification_type VARCHAR(30) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMPTZ,

    CONSTRAINT chk_notification_type
        CHECK (
            notification_type IN (
                'match',
                'claim',
                'return',
                'system'
            )
        ),

    CONSTRAINT chk_notification_read_time
        CHECK (
            read_at IS NULL
            OR read_at >= created_at
        )
);


CREATE TABLE app.audit_log (
    audit_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_staff_id BIGINT
        REFERENCES app.staff(staff_id)
        ON DELETE SET NULL,
    action_type VARCHAR(30) NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    entity_id BIGINT,
    details JSONB,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_audit_action
        CHECK (
            action_type IN (
                'INSERT',
                'UPDATE',
                'DELETE',
                'LOGIN',
                'CLAIM_DECISION',
                'ITEM_RETURNED',
                'OTHER'
            )
        ),

    CONSTRAINT chk_audit_entity
        CHECK (LENGTH(TRIM(entity_name)) > 0)
);


CREATE INDEX idx_terminal_airport
    ON app.terminal(airport_id);

CREATE INDEX idx_location_airport
    ON app.airport_location(airport_id);

CREATE INDEX idx_flight_departure
    ON app.flight(scheduled_departure);

CREATE INDEX idx_item_category
    ON app.item(category_id);

CREATE INDEX idx_lost_report_passenger
    ON app.lost_report(passenger_id);

CREATE INDEX idx_lost_report_status_date
    ON app.lost_report(report_status, reported_at);

CREATE INDEX idx_found_item_status_date
    ON app.found_item(item_status, found_at);

CREATE INDEX idx_item_match_status_score
    ON app.item_match(verification_status, match_score DESC);

CREATE INDEX idx_claim_status_date
    ON app.claim(claim_status, submitted_at);

CREATE INDEX idx_notification_unread
    ON app.notification(passenger_id, created_at DESC)
    WHERE read_at IS NULL;

CREATE UNIQUE INDEX uq_active_item_assignment
    ON app.storage_assignment(found_item_id)
    WHERE released_at IS NULL;

CREATE UNIQUE INDEX uq_active_slot_assignment
    ON app.storage_assignment(storage_id, slot_number)
    WHERE released_at IS NULL;

CREATE INDEX idx_audit_entity_time
    ON app.audit_log(entity_name, entity_id, occurred_at DESC);

COMMIT;
-- [Cloud safe] Skipped RESET ROLE

-- <<< End of database/01_schema.sql <<<

-- >>> Start of database/02_seed.sql >>>

BEGIN;

INSERT INTO app.airport
    (airport_code, airport_name, city, country)
VALUES
    ('IDR', 'Devi Ahilya Bai Holkar Airport', 'Indore', 'India'),
    ('BOM', 'Chhatrapati Shivaji Maharaj International Airport', 'Mumbai', 'India');


INSERT INTO app.terminal
    (airport_id, terminal_code, terminal_name)
SELECT
    a.airport_id,
    v.terminal_code,
    v.terminal_name
FROM (
    VALUES
        ('IDR', 'T1', 'Terminal 1'),
        ('BOM', 'T2', 'Terminal 2')
) AS v(airport_code, terminal_code, terminal_name)
JOIN app.airport a
    ON a.airport_code = v.airport_code;


INSERT INTO app.airport_location
    (airport_id, terminal_id, location_name, location_type, description)
SELECT
    a.airport_id,
    t.terminal_id,
    v.location_name,
    v.location_type,
    v.description
FROM (
    VALUES
        ('IDR', 'T1', 'Gate 4', 'gate',
         'Departure gate used for demonstration records'),
        ('IDR', 'T1', 'Baggage Claim 1', 'baggage_claim',
         'Baggage collection area'),
        ('IDR', 'T1', 'Security Checkpoint A', 'security',
         'Passenger security screening area'),
        ('BOM', 'T2', 'Gate 12', 'gate',
         'Departure gate used for demonstration records')
) AS v(airport_code, terminal_code, location_name, location_type, description)
JOIN app.airport a
    ON a.airport_code = v.airport_code
JOIN app.terminal t
    ON t.airport_id = a.airport_id
   AND t.terminal_code = v.terminal_code;


INSERT INTO app.item_category
    (category_name, description)
VALUES
    ('Electronics', 'Electronic devices and accessories'),
    ('Documents', 'Travel documents and document folders'),
    ('Bags', 'Backpacks, suitcases and other bags');


INSERT INTO app.passenger
    (full_name, email, phone, password_hash)
VALUES
    ('Airport Passenger', 'riya.patel@example.test',
     '9000000001', 'DEMO_ONLY_NOT_A_VALID_HASH'),
    ('Airport Passenger Two', 'kabir.singh@example.test',
     '9000000002', 'DEMO_ONLY_NOT_A_VALID_HASH');


INSERT INTO app.staff
    (full_name, email, password_hash, staff_role)
VALUES
    ('Airport Staff Officer', 'aarav.officer@example.test',
     '{bcrypt}$2a$10$fObA0dsbEDCY/MtS7fOh8eJrUX/MQGhdLBHKNSH3H.uu..0nwn2U2', 'lost_found_officer'),
    ('Airport Security Officer', 'meera.security@example.test',
     '{bcrypt}$2a$10$fObA0dsbEDCY/MtS7fOh8eJrUX/MQGhdLBHKNSH3H.uu..0nwn2U2', 'security_officer'),
    ('Airport System Administrator', 'admin@example.test',
     '{bcrypt}$2a$10$fObA0dsbEDCY/MtS7fOh8eJrUX/MQGhdLBHKNSH3H.uu..0nwn2U2', 'administrator'),
    ('Airport Lost & Found Agent', 'aditi.agent@airport.test',
     '{bcrypt}$2a$10$fObA0dsbEDCY/MtS7fOh8eJrUX/MQGhdLBHKNSH3H.uu..0nwn2U2', 'lost_found_agent'),
    ('Claims Verification Officer', 'vikram.claims@airport.test',
     '{bcrypt}$2a$10$fObA0dsbEDCY/MtS7fOh8eJrUX/MQGhdLBHKNSH3H.uu..0nwn2U2', 'claims_verification_officer'),
    ('Inventory & Storage Officer', 'pooja.inventory@airport.test',
     '{bcrypt}$2a$10$fObA0dsbEDCY/MtS7fOh8eJrUX/MQGhdLBHKNSH3H.uu..0nwn2U2', 'inventory_storage_officer'),
    ('Return & Dispatch Officer', 'rahul.dispatch@airport.test',
     '{bcrypt}$2a$10$fObA0dsbEDCY/MtS7fOh8eJrUX/MQGhdLBHKNSH3H.uu..0nwn2U2', 'return_dispatch_officer'),
    ('Lost Property Supervisor', 'sunita.supervisor@airport.test',
     '{bcrypt}$2a$10$fObA0dsbEDCY/MtS7fOh8eJrUX/MQGhdLBHKNSH3H.uu..0nwn2U2', 'supervisor'),
    ('Airport Security Officer', 'karan.security@airport.test',
     '{bcrypt}$2a$10$fObA0dsbEDCY/MtS7fOh8eJrUX/MQGhdLBHKNSH3H.uu..0nwn2U2', 'ground_staff'),
    ('Airport System Administrator', 'admin@airport.test',
     '{bcrypt}$2a$10$fObA0dsbEDCY/MtS7fOh8eJrUX/MQGhdLBHKNSH3H.uu..0nwn2U2', 'system_admin');


INSERT INTO app.flight (
    flight_number,
    airline,
    origin_airport_id,
    destination_airport_id,
    departure_terminal_id,
    arrival_terminal_id,
    scheduled_departure,
    scheduled_arrival
)
SELECT
    '6E 237',
    'IndiGo',
    origin.airport_id,
    destination.airport_id,
    departure_terminal.terminal_id,
    arrival_terminal.terminal_id,
    CURRENT_TIMESTAMP - INTERVAL '2 days',
    CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '2 hours 15 minutes'
FROM app.airport origin
JOIN app.airport destination
    ON destination.airport_code = 'BOM'
JOIN app.terminal departure_terminal
    ON departure_terminal.airport_id = origin.airport_id
   AND departure_terminal.terminal_code = 'T1'
JOIN app.terminal arrival_terminal
    ON arrival_terminal.airport_id = destination.airport_id
   AND arrival_terminal.terminal_code = 'T2'
WHERE origin.airport_code = 'IDR';


INSERT INTO app.item (
    category_id,
    item_name,
    brand,
    colour,
    description,
    estimated_value,
    item_condition
)
SELECT
    c.category_id,
    v.item_name,
    v.brand,
    v.colour,
    v.description,
    v.estimated_value,
    v.item_condition
FROM (
    VALUES
        ('Electronics', 'Black wireless earbuds',
         'SoundBeat', 'Black',
         'Passenger description: black charging case with a small scratch.',
         2500.00::NUMERIC, 'good'),

        ('Electronics', 'Black wireless earbuds',
         'SoundBeat', 'Black',
         'Found item description: black charging case with a small scratch; recovered near Gate 4.',
         2500.00::NUMERIC, 'good'),

        ('Documents', 'Blue document folder',
         NULL, 'Blue',
         'Passenger description: blue folder containing ordinary travel papers.',
         300.00::NUMERIC, 'fair'),

        ('Documents', 'Blue document folder',
         NULL, 'Blue',
         'Found item description: blue plastic document folder recovered near security.',
         300.00::NUMERIC, 'fair')
) AS v(
    category_name,
    item_name,
    brand,
    colour,
    description,
    estimated_value,
    item_condition
)
JOIN app.item_category c
    ON c.category_name = v.category_name;


INSERT INTO app.lost_report (
    passenger_id,
    item_id,
    flight_id,
    last_seen_location_id,
    lost_at,
    additional_details
)
SELECT
    p.passenger_id,
    i.item_id,
    f.flight_id,
    l.location_id,
    CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '1 hour',
    'Passenger reports that the earbuds were left near the departure gate.'
FROM app.passenger p
JOIN app.item i
    ON i.item_name = 'Black wireless earbuds'
   AND i.description =
       'Passenger description: black charging case with a small scratch.'
JOIN app.flight f
    ON f.flight_number = '6E 237'
JOIN app.airport_location l
    ON l.location_name = 'Gate 4'
JOIN app.airport a
    ON a.airport_id = l.airport_id
   AND a.airport_code = 'IDR'
WHERE p.email = 'riya.patel@example.test';


INSERT INTO app.lost_report (
    passenger_id,
    item_id,
    flight_id,
    last_seen_location_id,
    lost_at,
    additional_details
)
SELECT
    p.passenger_id,
    i.item_id,
    f.flight_id,
    l.location_id,
    CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '30 minutes',
    'Passenger reports a missing blue document folder.'
FROM app.passenger p
JOIN app.item i
    ON i.item_name = 'Blue document folder'
   AND i.description =
       'Passenger description: blue folder containing ordinary travel papers.'
JOIN app.flight f
    ON f.flight_number = '6E 237'
JOIN app.airport_location l
    ON l.location_name = 'Security Checkpoint A'
JOIN app.airport a
    ON a.airport_id = l.airport_id
   AND a.airport_code = 'IDR'
WHERE p.email = 'kabir.singh@example.test';


INSERT INTO app.found_item (
    item_id,
    found_by_staff_id,
    flight_id,
    found_location_id,
    found_at,
    item_status
)
SELECT
    i.item_id,
    s.staff_id,
    f.flight_id,
    l.location_id,
    CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '1 hour 30 minutes',
    'stored'
FROM app.item i
JOIN app.staff s
    ON s.email = 'aarav.officer@example.test'
JOIN app.flight f
    ON f.flight_number = '6E 237'
JOIN app.airport_location l
    ON l.location_name = 'Gate 4'
JOIN app.airport a
    ON a.airport_id = l.airport_id
   AND a.airport_code = 'IDR'
WHERE i.item_name = 'Black wireless earbuds'
  AND i.description =
      'Found item description: black charging case with a small scratch; recovered near Gate 4.';


INSERT INTO app.found_item (
    item_id,
    found_by_staff_id,
    flight_id,
    found_location_id,
    found_at,
    item_status
)
SELECT
    i.item_id,
    s.staff_id,
    f.flight_id,
    l.location_id,
    CURRENT_TIMESTAMP - INTERVAL '2 days' + INTERVAL '45 minutes',
    'awaiting_review'
FROM app.item i
JOIN app.staff s
    ON s.email = 'meera.security@example.test'
JOIN app.flight f
    ON f.flight_number = '6E 237'
JOIN app.airport_location l
    ON l.location_name = 'Security Checkpoint A'
JOIN app.airport a
    ON a.airport_id = l.airport_id
   AND a.airport_code = 'IDR'
WHERE i.item_name = 'Blue document folder'
  AND i.description =
      'Found item description: blue plastic document folder recovered near security.';


INSERT INTO app.item_match (
    lost_report_id,
    found_item_id,
    match_score,
    verification_status,
    remarks
)
SELECT
    lr.lost_report_id,
    fi.found_item_id,
    CASE li.item_name
        WHEN 'Black wireless earbuds' THEN 96.50
        WHEN 'Blue document folder' THEN 72.00
    END,
    'candidate',
    CASE li.item_name
        WHEN 'Black wireless earbuds'
            THEN 'Strong candidate based on item description and location.'
        WHEN 'Blue document folder'
            THEN 'Possible match; additional verification is required.'
    END
FROM app.lost_report lr
JOIN app.item li
    ON li.item_id = lr.item_id
JOIN app.found_item fi
    ON TRUE
JOIN app.item found_description
    ON found_description.item_id = fi.item_id
   AND found_description.item_name = li.item_name
WHERE li.description LIKE 'Passenger description:%'
  AND found_description.description LIKE 'Found item description:%';

COMMIT;

-- <<< End of database/02_seed.sql <<<

-- >>> Start of database/05_views.sql >>>
BEGIN;
-- [Cloud safe] Skipped SET ROLE


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

DO $$ BEGIN GRANT SELECT ON
    app.v_match_overview,
    app.v_claim_workflow,
    app.v_storage_assignment_history
TO airport_app; EXCEPTION WHEN undefined_object THEN NULL; END $$;

COMMIT;
-- [Cloud safe] Skipped RESET ROLE

-- <<< End of database/05_views.sql <<<

-- >>> Start of database/06_triggers.sql >>>
BEGIN;
-- [Cloud safe] Skipped SET ROLE


-- 1. Require a verified match and verified identity before claim approval.

CREATE OR REPLACE FUNCTION app.fn_validate_claim_approval()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_match_status VARCHAR(25);
BEGIN
    IF NEW.claim_status = 'approved'
       AND OLD.claim_status IS DISTINCT FROM NEW.claim_status
    THEN
        SELECT m.verification_status
        INTO v_match_status
        FROM app.item_match AS m
        WHERE m.match_id = NEW.match_id;

        IF v_match_status IS DISTINCT FROM 'verified' THEN
            RAISE EXCEPTION
                'A claim cannot be approved until its match is verified.'
                USING ERRCODE = '23514';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM app.identity_verification AS iv
            WHERE iv.claim_id = NEW.claim_id
              AND iv.verification_result = 'verified'
              AND iv.verified_by_staff_id IS NOT NULL
              AND iv.verified_at IS NOT NULL
        ) THEN
            RAISE EXCEPTION
                'A claim cannot be approved until identity is verified.'
                USING ERRCODE = '23514';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;


-- 2. Require an approved claim and verified identity before completing a return.

CREATE OR REPLACE FUNCTION app.fn_validate_completed_return()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_claim_status VARCHAR(25);
    v_match_status VARCHAR(25);
BEGIN
    IF NEW.return_status <> 'completed' THEN
        RETURN NEW;
    END IF;

    SELECT
        c.claim_status,
        m.verification_status
    INTO
        v_claim_status,
        v_match_status
    FROM app.claim AS c
    JOIN app.item_match AS m
        ON m.match_id = c.match_id
    WHERE c.claim_id = NEW.claim_id;

    IF v_claim_status IS DISTINCT FROM 'approved' THEN
        RAISE EXCEPTION
            'A return cannot be completed before the claim is approved.'
            USING ERRCODE = '23514';
    END IF;

    IF v_match_status IS DISTINCT FROM 'verified' THEN
        RAISE EXCEPTION
            'A return requires a verified item match.'
            USING ERRCODE = '23514';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM app.identity_verification AS iv
        WHERE iv.claim_id = NEW.claim_id
          AND iv.verification_result = 'verified'
          AND iv.verified_by_staff_id IS NOT NULL
          AND iv.verified_at IS NOT NULL
    ) THEN
        RAISE EXCEPTION
            'A return requires verified passenger identity.'
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$;


-- 3. Require a completed return and released storage before marking an item returned.

CREATE OR REPLACE FUNCTION app.fn_validate_found_item_returned()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.item_status = 'returned'
       AND OLD.item_status IS DISTINCT FROM NEW.item_status
    THEN
        IF EXISTS (
            SELECT 1
            FROM app.storage_assignment AS sa
            WHERE sa.found_item_id = NEW.found_item_id
              AND sa.released_at IS NULL
        ) THEN
            RAISE EXCEPTION
                'Release the active storage assignment before returning the item.'
                USING ERRCODE = '23514';
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM app.item_match AS m
            JOIN app.claim AS c
                ON c.match_id = m.match_id
            JOIN app.return_delivery AS r
                ON r.claim_id = c.claim_id
            JOIN app.identity_verification AS iv
                ON iv.claim_id = c.claim_id
            WHERE m.found_item_id = NEW.found_item_id
              AND m.verification_status = 'verified'
              AND c.claim_status = 'approved'
              AND r.return_status = 'completed'
              AND iv.verification_result = 'verified'
              AND iv.verified_by_staff_id IS NOT NULL
              AND iv.verified_at IS NOT NULL
        ) THEN
            RAISE EXCEPTION
                'An item can only be returned after a valid completed return.'
                USING ERRCODE = '23514';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;


-- 4. Validate active storage assignments.

CREATE OR REPLACE FUNCTION app.fn_validate_storage_assignment()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_item_status VARCHAR(25);
    v_slot_status VARCHAR(20);
BEGIN
    -- Released assignments are historical records, not active assignments.
    IF NEW.released_at IS NOT NULL THEN
        RETURN NEW;
    END IF;

    SELECT f.item_status
    INTO v_item_status
    FROM app.found_item AS f
    WHERE f.found_item_id = NEW.found_item_id;

    IF v_item_status IS DISTINCT FROM 'stored' THEN
        RAISE EXCEPTION
            'Only a stored found item can receive an active storage assignment.'
            USING ERRCODE = '23514';
    END IF;

    SELECT ss.slot_status
    INTO v_slot_status
    FROM app.storage_slot AS ss
    WHERE ss.storage_id = NEW.storage_id
      AND ss.slot_number = NEW.slot_number;

    IF v_slot_status IS DISTINCT FROM 'available' THEN
        RAISE EXCEPTION
            'The selected storage slot is unavailable or does not exist.'
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$;


-- 5. Prevent identity verification from being downgraded after approval or return.

CREATE OR REPLACE FUNCTION app.fn_prevent_identity_downgrade()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.verification_result IS DISTINCT FROM 'verified'
       AND EXISTS (
           SELECT 1
           FROM app.claim AS c
           LEFT JOIN app.return_delivery AS r
               ON r.claim_id = c.claim_id
           WHERE c.claim_id = NEW.claim_id
             AND (
                 c.claim_status = 'approved'
                 OR r.return_status = 'completed'
             )
       )
    THEN
        RAISE EXCEPTION
            'Identity verification cannot be downgraded after claim approval or return completion.'
            USING ERRCODE = '23514';
    END IF;

    RETURN NEW;
END;
$$;


-- Install the triggers.
-- DROP IF EXISTS makes trigger replacement safe when rerunning this file.

DROP TRIGGER IF EXISTS trg_claim_approval_requires_verification
    ON app.claim;

CREATE TRIGGER trg_claim_approval_requires_verification
BEFORE UPDATE OF claim_status
ON app.claim
FOR EACH ROW
EXECUTE FUNCTION app.fn_validate_claim_approval();


DROP TRIGGER IF EXISTS trg_completed_return_requires_approval
    ON app.return_delivery;

CREATE TRIGGER trg_completed_return_requires_approval
BEFORE INSERT OR UPDATE
ON app.return_delivery
FOR EACH ROW
EXECUTE FUNCTION app.fn_validate_completed_return();


DROP TRIGGER IF EXISTS trg_found_item_return_requires_completed_return
    ON app.found_item;

CREATE TRIGGER trg_found_item_return_requires_completed_return
BEFORE UPDATE OF item_status
ON app.found_item
FOR EACH ROW
EXECUTE FUNCTION app.fn_validate_found_item_returned();


DROP TRIGGER IF EXISTS trg_storage_assignment_requires_available_slot
    ON app.storage_assignment;

CREATE TRIGGER trg_storage_assignment_requires_available_slot
BEFORE INSERT OR UPDATE
ON app.storage_assignment
FOR EACH ROW
EXECUTE FUNCTION app.fn_validate_storage_assignment();


DROP TRIGGER IF EXISTS trg_identity_verification_cannot_be_downgraded
    ON app.identity_verification;

CREATE TRIGGER trg_identity_verification_cannot_be_downgraded
BEFORE UPDATE OF verification_result
ON app.identity_verification
FOR EACH ROW
EXECUTE FUNCTION app.fn_prevent_identity_downgrade();


-- Trigger functions should not be directly executable by arbitrary users.
REVOKE ALL ON FUNCTION app.fn_validate_claim_approval() FROM PUBLIC;
REVOKE ALL ON FUNCTION app.fn_validate_completed_return() FROM PUBLIC;
REVOKE ALL ON FUNCTION app.fn_validate_found_item_returned() FROM PUBLIC;
REVOKE ALL ON FUNCTION app.fn_validate_storage_assignment() FROM PUBLIC;
REVOKE ALL ON FUNCTION app.fn_prevent_identity_downgrade() FROM PUBLIC;

DO $$ BEGIN GRANT EXECUTE ON FUNCTION app.fn_validate_claim_approval()
    TO airport_app; EXCEPTION WHEN undefined_object THEN NULL; END $$;
DO $$ BEGIN GRANT EXECUTE ON FUNCTION app.fn_validate_completed_return()
    TO airport_app; EXCEPTION WHEN undefined_object THEN NULL; END $$;
DO $$ BEGIN GRANT EXECUTE ON FUNCTION app.fn_validate_found_item_returned()
    TO airport_app; EXCEPTION WHEN undefined_object THEN NULL; END $$;
DO $$ BEGIN GRANT EXECUTE ON FUNCTION app.fn_validate_storage_assignment()
    TO airport_app; EXCEPTION WHEN undefined_object THEN NULL; END $$;
DO $$ BEGIN GRANT EXECUTE ON FUNCTION app.fn_prevent_identity_downgrade()
    TO airport_app; EXCEPTION WHEN undefined_object THEN NULL; END $$;

COMMIT;
-- [Cloud safe] Skipped RESET ROLE

-- <<< End of database/06_triggers.sql <<<

-- >>> Start of database/11_domain_alignment.sql >>>
-- ============================================================
-- Airport Lost & Found Management System
-- Phase 11: Comprehensive Domain Alignment Migration
-- ============================================================
-- [Cloud safe] Skipped SET ROLE

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
DO $$ BEGIN GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA app TO airport_app; EXCEPTION WHEN undefined_object THEN NULL; END $$;
DO $$ BEGIN GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA app TO airport_app; EXCEPTION WHEN undefined_object THEN NULL; END $$;
DO $$ BEGIN GRANT ALL PRIVILEGES ON app.lost_found_match TO airport_app; EXCEPTION WHEN undefined_object THEN NULL; END $$;

-- 12. Storage Locations, Slots & Workflow Seeds
INSERT INTO app.airport_location (airport_id, terminal_id, location_name, location_type, description)
SELECT a.airport_id, t.terminal_id, 'Lost & Found Office', 'other', 'Designated office for recovered items and claim processing.'
FROM app.airport a JOIN app.terminal t ON t.airport_id = a.airport_id
WHERE a.airport_code = 'IDR' AND t.terminal_code = 'T1'
  AND NOT EXISTS (SELECT 1 FROM app.airport_location l WHERE l.airport_id = a.airport_id AND l.location_name = 'Lost & Found Office');

INSERT INTO app.airport_location (airport_id, terminal_id, location_name, location_type, description)
SELECT a.airport_id, t.terminal_id, 'Terminal 1 Secure Vault Area', 'security', 'Restricted high-security vault storage room in Terminal 1.'
FROM app.airport a JOIN app.terminal t ON t.airport_id = a.airport_id
WHERE a.airport_code = 'IDR' AND t.terminal_code = 'T1'
  AND NOT EXISTS (SELECT 1 FROM app.airport_location l WHERE l.airport_id = a.airport_id AND l.location_name = 'Terminal 1 Secure Vault Area');

INSERT INTO app.storage_location (location_id, storage_name, description, capacity, room_number, cabinet_number, is_active)
SELECT l.location_id, 'IDR Lost & Found Cabinet A', 'Terminal 1 primary custodial cabinet for recovered electronics and valuables.', 10, 'Room 104-A', 'CAB-SEC-01', true
FROM app.airport_location l WHERE l.location_name = 'Lost & Found Office'
  AND NOT EXISTS (SELECT 1 FROM app.storage_location s WHERE s.storage_name = 'IDR Lost & Found Cabinet A');

INSERT INTO app.storage_location (location_id, storage_name, description, capacity, room_number, cabinet_number, is_active)
SELECT l.location_id, 'IDR Secure High-Value Vault', 'Climate-controlled high-security locker bank for jewelry, passports, and smartphones.', 12, 'Room 208-B', 'CAB-SEC-02', true
FROM app.airport_location l WHERE l.location_name = 'Terminal 1 Secure Vault Area'
  AND NOT EXISTS (SELECT 1 FROM app.storage_location s WHERE s.storage_name = 'IDR Secure High-Value Vault');

INSERT INTO app.storage_slot (storage_id, slot_number, slot_status)
SELECT s.storage_id, n.slot_number, 'available'
FROM app.storage_location s CROSS JOIN generate_series(1, 10) AS n(slot_number)
WHERE s.storage_name = 'IDR Lost & Found Cabinet A'
ON CONFLICT (storage_id, slot_number) DO NOTHING;

INSERT INTO app.storage_slot (storage_id, slot_number, slot_status)
SELECT s.storage_id, n.slot_number, 'available'
FROM app.storage_location s CROSS JOIN generate_series(1, 12) AS n(slot_number)
WHERE s.storage_name = 'IDR Secure High-Value Vault'
ON CONFLICT (storage_id, slot_number) DO NOTHING;

INSERT INTO app.storage_assignment (found_item_id, storage_id, slot_number, assigned_by_staff_id, assigned_at)
SELECT 1, s.storage_id, 1, COALESCE((SELECT staff_id FROM app.staff WHERE email = 'pooja.inventory@airport.test' LIMIT 1), 1), CURRENT_TIMESTAMP
FROM app.storage_location s
WHERE s.storage_name = 'IDR Lost & Found Cabinet A'
  AND EXISTS (SELECT 1 FROM app.found_item WHERE found_item_id = 1)
  AND NOT EXISTS (SELECT 1 FROM app.storage_assignment WHERE found_item_id = 1 AND released_at IS NULL)
  AND NOT EXISTS (SELECT 1 FROM app.storage_assignment WHERE storage_id = s.storage_id AND slot_number = 1 AND released_at IS NULL);

UPDATE app.found_item f
SET storage_id = s.storage_id, slot_number = 1, item_status = 'stored', stored_at = COALESCE(stored_at, CURRENT_TIMESTAMP)
FROM app.storage_location s
WHERE f.found_item_id = 1 AND s.storage_name = 'IDR Lost & Found Cabinet A';

INSERT INTO app.item_match (lost_report_id, found_item_id, match_score, verification_status, remarks)
SELECT 1, 1, 96.50, 'candidate', 'Strong correlation on brand, colour, and discovery checkpoint.'
WHERE EXISTS (SELECT 1 FROM app.lost_report WHERE lost_report_id = 1)
  AND EXISTS (SELECT 1 FROM app.found_item WHERE found_item_id = 1)
  AND NOT EXISTS (SELECT 1 FROM app.item_match WHERE lost_report_id = 1 AND found_item_id = 1);

INSERT INTO app.claim (match_id, claim_status, created_at)
SELECT m.match_id, 'submitted', CURRENT_TIMESTAMP - INTERVAL '2 hours'
FROM app.item_match m WHERE m.lost_report_id = 1 AND m.found_item_id = 1
  AND NOT EXISTS (SELECT 1 FROM app.claim c WHERE c.match_id = m.match_id);

INSERT INTO app.identity_verification (claim_id, verification_method, verification_result, verification_notes)
SELECT c.claim_id, 'in_person_document_check', 'pending', 'Awaiting government-issued photo ID check at custody desk.'
FROM app.claim c JOIN app.item_match m ON m.match_id = c.match_id
WHERE m.lost_report_id = 1 AND m.found_item_id = 1
  AND NOT EXISTS (SELECT 1 FROM app.identity_verification iv WHERE iv.claim_id = c.claim_id);

-- Dynamic Auto-Correlation for all open lost reports and found items
INSERT INTO app.item_match (lost_report_id, found_item_id, match_score, verification_status, remarks)
SELECT
    lr.lost_report_id, fi.found_item_id,
    LEAST(99.00,
        (CASE WHEN li.category_id = found_i.category_id THEN 35.00 ELSE 15.00 END) +
        (CASE WHEN LOWER(TRIM(li.item_name)) = LOWER(TRIM(found_i.item_name)) THEN 35.00
              WHEN LOWER(found_i.item_name) LIKE '%' || LOWER(TRIM(li.item_name)) || '%'
                OR LOWER(li.item_name) LIKE '%' || LOWER(TRIM(found_i.item_name)) || '%' THEN 25.00
              ELSE 0.00 END) +
        (CASE WHEN li.brand IS NOT NULL AND found_i.brand IS NOT NULL
                   AND LOWER(TRIM(li.brand)) = LOWER(TRIM(found_i.brand)) THEN 10.00 ELSE 0.00 END) +
        (CASE WHEN lr.flight_id IS NOT NULL AND fi.flight_id IS NOT NULL
                   AND lr.flight_id = fi.flight_id THEN 15.00 ELSE 0.00 END) +
        (CASE WHEN lr.last_seen_location_id IS NOT NULL AND fi.found_location_id IS NOT NULL
                   AND lr.last_seen_location_id = fi.found_location_id THEN 10.00 ELSE 0.00 END)
    ) AS calc_score,
    'candidate',
    CONCAT_WS(' ',
        CASE WHEN li.category_id = found_i.category_id THEN 'Matching category.' END,
        CASE WHEN LOWER(TRIM(li.item_name)) = LOWER(TRIM(found_i.item_name)) THEN 'Exact name match.' END,
        CASE WHEN lr.flight_id IS NOT NULL AND lr.flight_id = fi.flight_id THEN 'Matching flight.' END,
        CASE WHEN lr.last_seen_location_id IS NOT NULL AND lr.last_seen_location_id = fi.found_location_id THEN 'Matching checkpoint.' END
    ) AS remarks
FROM app.lost_report lr
JOIN app.item li ON li.item_id = lr.item_id
CROSS JOIN app.found_item fi
JOIN app.item found_i ON found_i.item_id = fi.item_id
WHERE lr.report_status IN ('open', 'candidate_found')
  AND fi.item_status IN ('awaiting_review', 'stored')
  AND li.category_id = found_i.category_id
  AND (
      LOWER(TRIM(li.item_name)) = LOWER(TRIM(found_i.item_name))
      OR LOWER(found_i.item_name) LIKE '%' || LOWER(TRIM(li.item_name)) || '%'
      OR LOWER(li.item_name) LIKE '%' || LOWER(TRIM(found_i.item_name)) || '%'
      OR (lr.flight_id IS NOT NULL AND lr.flight_id = fi.flight_id)
      OR (li.brand IS NOT NULL AND found_i.brand IS NOT NULL AND LOWER(TRIM(li.brand)) = LOWER(TRIM(found_i.brand)))
  )
  AND (
      35.00 +
      (CASE WHEN LOWER(TRIM(li.item_name)) = LOWER(TRIM(found_i.item_name)) THEN 35.00
            WHEN LOWER(found_i.item_name) LIKE '%' || LOWER(TRIM(li.item_name)) || '%'
              OR LOWER(li.item_name) LIKE '%' || LOWER(TRIM(found_i.item_name)) || '%' THEN 25.00
            ELSE 0.00 END) +
      (CASE WHEN li.brand IS NOT NULL AND found_i.brand IS NOT NULL
                 AND LOWER(TRIM(li.brand)) = LOWER(TRIM(found_i.brand)) THEN 10.00 ELSE 0.00 END) +
      (CASE WHEN lr.flight_id IS NOT NULL AND fi.flight_id IS NOT NULL
                 AND lr.flight_id = fi.flight_id THEN 15.00 ELSE 0.00 END) +
      (CASE WHEN lr.last_seen_location_id IS NOT NULL AND fi.found_location_id IS NOT NULL
                 AND lr.last_seen_location_id = fi.found_location_id THEN 10.00 ELSE 0.00 END)
  ) >= 50.00
  AND NOT EXISTS (SELECT 1 FROM app.item_match m WHERE m.lost_report_id = lr.lost_report_id AND m.found_item_id = fi.found_item_id)
ON CONFLICT (lost_report_id, found_item_id) DO NOTHING;

INSERT INTO app.claim (match_id, claim_status)
SELECT m.match_id, 'submitted'
FROM app.item_match m
WHERE NOT EXISTS (SELECT 1 FROM app.claim c WHERE c.match_id = m.match_id)
ON CONFLICT (match_id) DO NOTHING;

INSERT INTO app.identity_verification (claim_id, verification_method, verification_result, verification_notes)
SELECT c.claim_id, 'in_person_document_check', 'pending', 'Automated match candidate awaiting claims officer document verification.'
FROM app.claim c
WHERE NOT EXISTS (SELECT 1 FROM app.identity_verification iv WHERE iv.claim_id = c.claim_id)
ON CONFLICT DO NOTHING;

UPDATE app.lost_report lr
SET report_status = 'candidate_found'
WHERE lr.report_status = 'open'
  AND EXISTS (SELECT 1 FROM app.item_match m WHERE m.lost_report_id = lr.lost_report_id);

-- <<< End of database/11_domain_alignment.sql <<<
