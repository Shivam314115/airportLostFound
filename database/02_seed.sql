
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
