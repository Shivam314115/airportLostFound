BEGIN;

SET ROLE airport_owner;

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

RESET ROLE;
