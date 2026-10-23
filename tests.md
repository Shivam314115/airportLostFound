# AirportLostandFound — Comprehensive Testing Guide

This guide provides complete instructions for testing each and every function of the **AirportLostandFound** system, including automated backend unit/integration tests, end-to-end API verification via cURL, database trigger audits, and frontend interactive verification.

---

## 1. Automated Backend Test Suite (Maven)

The Spring Boot backend contains a comprehensive automated test suite covering authentication, session handling, database transactions, ACID rollback verification, API error handling, and automated matching algorithms.

### Running All Tests
From the `backend/airport-lost-found-api` directory:
```bash
cd backend/airport-lost-found-api
./mvnw test
```

### Breakdown of Test Classes & Tested Functions

| Test Class | Focus & Functions Tested |
| :--- | :--- |
| **`AirportLostFoundApiApplicationTests`** | Spring Boot application context initialization and bean wiring. |
| **`AuthControllerTest`** | MockMvc tests for `POST /api/auth/login`, `POST /api/auth/register`, and session validation. |
| **`AuthSessionIntegrationTest`** | End-to-end HTTP session persistence, cookie generation, and CSRF token verification. |
| **`AuthServiceTest`** | BCrypt password hashing, credential verification, and user authentication state. |
| **`PasswordConfigTest`** | Password encoder strength and salt generation. |
| **`LostReportControllerTest`** | MockMvc tests for creating and querying passenger lost reports. |
| **`LostReportServiceTest`** | Business logic for report lodging, category mapping, and flight associations. |
| **`LostReportServiceIntegrationTest`** | Real database insertion into PostgreSQL `app.lost_reports` with reference data. |
| **`LostReportTransactionRollbackIntegrationTest`** | Verifies ACID transaction rollback when an error occurs during report creation. |
| **`LostReportApiErrorIntegrationTest`** | Validates HTTP 400 Bad Request responses when required fields are missing or invalid. |
| **`MatchControllerTest`** | API endpoint tests for candidate and verified match queries. |
| **`MatchServiceTest`** | Automated match score calculation between lost reports and custody items. |
| **`MatchRepositoryIntegrationTest`** | Database view queries against `app.v_candidate_matches`. |
| **`PassengerServiceIntegrationTest`** | Passenger profile creation, unique email constraint enforcement, and retrieval. |
| **`StaffRbacIntegrationTest`** | End-to-end Role-Based Access Control (RBAC) across 7 staff roles, authority verification, item intake, claim approvals, inventory assignments, dispatches, supervisor telemetry, and admin staff provisioning. |

### Running a Specific Test Class
```bash
./mvnw test -Dtest=StaffRbacIntegrationTest
./mvnw test -Dtest=LostReportServiceIntegrationTest
./mvnw test -Dtest=AuthSessionIntegrationTest
./mvnw test -Dtest=MatchRepositoryIntegrationTest
```

---

## 2. End-to-End API Functional Testing via cURL

You can test every API endpoint manually using `curl` against a running backend (`http://localhost:8080`).

### Test 1: Fetch Reference Data (Public Endpoint)
Tests that airport categories, recovery locations, and scheduled flights are successfully loaded from PostgreSQL:
```bash
curl -i -X GET http://localhost:8080/api/reference-data
```
**Expected Response:** `HTTP/1.1 200 OK`
```json
{
  "categories": [
    { "categoryId": 1, "categoryName": "Electronics", "description": "..." },
    { "categoryId": 2, "categoryName": "Documents", "description": "..." },
    { "categoryId": 3, "categoryName": "Bags", "description": "..." }
  ],
  "locations": [
    { "locationId": 1, "locationName": "Security Checkpoint A", "locationType": "security" }
  ],
  "flights": [
    { "flightId": 1, "flightNumber": "6E 237", "airline": "IndiGo" }
  ]
}
```

---

### Test 2: Register a New Passenger Account
Registers a new passenger with encrypted password storage:
```bash
curl -i -c cookies.txt -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Aryan Sharma",
    "email": "aryan.sharma@example.com",
    "phone": "+919876543210",
    "password": "SecurePassword123!",
    "idDocumentType": "passport",
    "idDocumentNumber": "Z9876543"
  }'
```
**Expected Response:** `HTTP/1.1 201 Created`
```json
{
  "passengerId": 2,
  "fullName": "Aryan Sharma",
  "email": "aryan.sharma@example.com",
  "phone": "+919876543210"
}
```

---

### Test 3: Authenticate / Login (Session Creation)
Authenticates credentials and establishes a session cookie:
```bash
curl -i -b cookies.txt -c cookies.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "aryan.sharma@example.com",
    "password": "SecurePassword123!"
  }'
```
**Expected Response:** `HTTP/1.1 200 OK`
Header `Set-Cookie: JSESSIONID=...; Path=/; HttpOnly`

---

### Test 4: Retrieve Authenticated User Profile (`/api/auth/me`)
Tests session identity resolution:
```bash
curl -i -b cookies.txt -X GET http://localhost:8080/api/auth/me
```
**Expected Response:** `HTTP/1.1 200 OK` returning passenger profile for `aryan.sharma@example.com`.

---

### Test 5: Lodge a Lost Property Report
Submits a lost item report linked to the authenticated session:
```bash
# First get the CSRF token from the session cookies if enabled
CSRF_TOKEN=$(grep XSRF-TOKEN cookies.txt | awk '{print $7}')

curl -i -b cookies.txt -c cookies.txt -X POST http://localhost:8080/api/lost-reports \
  -H "Content-Type: application/json" \
  -H "X-XSRF-TOKEN: $CSRF_TOKEN" \
  -d '{
    "itemName": "Bose Noise Cancelling Headphones 700",
    "categoryId": 1,
    "description": "Black over-ear headphones inside a zippered hard leather case.",
    "lostAt": "2026-10-21T18:30:00",
    "locationId": 1,
    "flightId": 1,
    "estimatedValue": 25000.00,
    "additionalDetails": "Left in plastic security bin 4B at Checkpoint A"
  }'
```
**Expected Response:** `HTTP/1.1 201 Created`
```json
{
  "lostReportId": 3,
  "passengerId": 2,
  "itemName": "Bose Noise Cancelling Headphones 700",
  "reportStatus": "OPEN",
  "categoryName": "Electronics"
}
```

---

### Test 6: Query Passenger's Lost Reports
Verifies that newly lodged reports appear under the authenticated passenger:
```bash
curl -i -b cookies.txt -X GET http://localhost:8080/api/lost-reports
```
**Expected Response:** `HTTP/1.1 200 OK` returning the list of active reports for the session.

---

### Test 7: Query Candidate Matches (`/api/matches`)
Tests retrieval of correlated custody matches computed by database views:
```bash
curl -i -b cookies.txt -X GET http://localhost:8080/api/matches
```
**Expected Response:** `HTTP/1.1 200 OK` returning match records with `matchScore`, `matchStatus`, and officer notes.

---

### Test 8: Logout (Session Invalidation)
Tests session termination:
```bash
curl -i -b cookies.txt -X POST http://localhost:8080/api/auth/logout
```
**Expected Response:** `HTTP/1.1 200 OK` with session invalidation.
A subsequent call to `GET /api/lost-reports` will return `HTTP/1.1 403 Forbidden`.

---

### Test 9: Staff Subrole Authentication & RBAC Verification

#### 9A: Lost & Found Agent (`agent@airport.test`) — Item Intake
```bash
# 1. Fetch CSRF token
CSRF_JSON=$(curl -s -c staff_cookie.txt http://localhost:8080/api/auth/csrf)
CSRF_TOKEN=$(echo $CSRF_JSON | jq -r .token)
CSRF_HEADER=$(echo $CSRF_JSON | jq -r .headerName)

# 2. Login as Airport Lost & Found Agent
curl -i -b staff_cookie.txt -c staff_cookie.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "$CSRF_HEADER: $CSRF_TOKEN" \
  -d '{"email":"agent@airport.test","password":"shivam123"}'

# 3. Verify Staff Profile & Authorities
curl -s -b staff_cookie.txt http://localhost:8080/api/auth/me | jq .

# 4. Agent registers newly discovered found item
curl -i -b staff_cookie.txt -X POST http://localhost:8080/api/staff/items \
  -H "Content-Type: application/json" \
  -H "$CSRF_HEADER: $CSRF_TOKEN" \
  -d '{
    "categoryId": 1,
    "itemName": "Black Wireless Headphones",
    "brand": "Sony",
    "colour": "Black",
    "description": "Left on Seat 14B",
    "itemCondition": "good",
    "foundAt": "2026-10-22T05:00:00Z"
  }'
```
**Expected:** `HTTP/1.1 201 Created` with `"foundItemId": <id>`.

#### 9B: Claims Officer (`claims@airport.test`) — Review & Approval
```bash
# Login as Claims Verification Officer
curl -i -b claims_cookie.txt -c claims_cookie.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "$CSRF_HEADER: $CSRF_TOKEN" \
  -d '{"email":"claims@airport.test","password":"shivam123"}'

# List claims awaiting verification
curl -s -b claims_cookie.txt http://localhost:8080/api/staff/claims | jq .
```
**Expected:** `HTTP/1.1 200 OK` array of candidate matches & passenger claims.

#### 9C: RBAC Enforcement — Airport Security Officer Forbidden from Admin/Claims Endpoints
```bash
# Login as Airport Security Officer
curl -i -b ground_cookie.txt -c ground_cookie.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "$CSRF_HEADER: $CSRF_TOKEN" \
  -d '{"email":"security@airport.test","password":"shivam123"}'

# Attempt to access System Admin staff directory
curl -i -b ground_cookie.txt http://localhost:8080/api/staff/admin/users
```
**Expected:** `HTTP/1.1 403 Forbidden` (`Access Denied` enforced by Spring Security method security).

#### 9D: System Administrator (`admin@airport.test`) — Staff Account Management
```bash
# Login as System Administrator
curl -i -b admin_cookie.txt -c admin_cookie.txt -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "$CSRF_HEADER: $CSRF_TOKEN" \
  -d '{"email":"admin@airport.test","password":"shivam123"}'

# List all airport staff
curl -s -b admin_cookie.txt http://localhost:8080/api/staff/admin/users | jq .
```
**Expected:** `HTTP/1.1 200 OK` listing all 10 airport staff accounts with their assigned subroles.

---

## 3. Database Audit & Trigger Testing

PostgreSQL triggers automatically log all mutations to the immutable audit log table `app.audit_logs`.

To verify that the audit trigger fired for the newly lodged report:
```bash
PGPASSWORD=shivam123 psql -h 127.0.0.1 -U airport_app -d airport_lost_found -c "
SELECT audit_id, table_name, operation, record_id, changed_by, changed_at
FROM app.audit_logs
ORDER BY audit_id DESC
LIMIT 5;
"
```
**Expected Result:** A row with `table_name = 'lost_reports'` and `operation = 'INSERT'`.

---

## 4. Frontend Functional & Interactive QA Checklist

### 1. 3D Airplane Model & Mouse Interaction
- [ ] **Mouse Drag to Rotate:** Clicking and dragging on the aircraft smoothly orbits the camera around the airplane.
- [ ] **Cursor State:** Cursor displays `grab` on hover and `grabbing` when actively dragging.
- [ ] **Clamping Safeguards:** Rotating vertically stops before flipping upside down (clamped at `minPolarAngle` ~27° and `maxPolarAngle` ~122°).
- [ ] **Zooming:** Scrolling mouse wheel or pinch-to-zoom scales the camera smoothly between zoom bounds.
- [ ] **Model Proportions:** The aircraft appears without clouds, scaled cleanly (`0.48`) within the hero container.
- [ ] **Overlay Graphics:** The dashed flight orbit with 3 teal waypoint pins and airplane silhouette is rendered crisply.

### 2. Live Search & Category Filtering
- [ ] **Full-Text Search:** Typing into the search bar (`"MacBook"`, `"Passport"`, `"Baggage"`) instantly filters the visible register cards in real time.
- [ ] **Clear Search Button:** Clicking the `✕` button clears the query and restores the full list.
- [ ] **Category Chips:** Clicking `All categories`, `Electronics`, `Documents`, or `Bags` dynamically filters the cards and updates the active chip indicator.
- [ ] **Database Counter Badge:** Displays `X records in database` with a glowing green status indicator linked to live PostgreSQL records.

### 3. Modals & Dialog Workflows
- [ ] **Report Dialog:**
  - Clicking `+ Report an item` opens the modal.
  - Submitting without required fields highlights field constraints.
  - Submitting with valid data sends a real POST request to `/api/lost-reports` and immediately reflects in the database.
- [ ] **Sign In / Registration Modal:**
  - Tab switching between *Sign in* and *Create account* works smoothly.
  - Validates email format and minimum 8-character password.
  - Upon successful login, the navigation bar updates to display the passenger session chip with initials and sign-out action.
- [ ] **Help Modal:** Clicking *Help* in the navigation bar opens the connected architecture service guide.

### 4. Responsive Viewport Verification
- [ ] **Desktop (1360px+):** Left text block (`480–520px`) and right airplane container (`680–760px`) align horizontally and center vertically.
- [ ] **Tablet (1024px):** Layout stacks into a single column with full-width text and responsive 3D stage (`height: 400px`).
- [ ] **Mobile (375px):** Navigation collapses into the mobile hamburger menu, search bar spans full width, and touch gestures allow one-finger orbit rotation.

---

## 5. Image & Domain Entities Functional Verification

### Test 14: Multipart Photo Upload via cURL
Uploads an image file to the custody repository:
```bash
curl -i -X POST http://localhost:8085/api/photos/upload \
  -F "file=@test_image.png;type=image/png" \
  -F "caption=Front view of laptop casing" \
  -F "photoType=overview"
```
**Expected Response:** `HTTP/1.1 201 Created`
```json
{
  "photoUrl": "/api/photos/a1b2c3d4-....png",
  "caption": "Front view of laptop casing",
  "photoType": "overview",
  "filename": "a1b2c3d4-....png"
}
```

### Test 15: Retrieve Uploaded Photo via Public HTTP Endpoint
```bash
curl -i -X GET http://localhost:8085/api/photos/a1b2c3d4-....png
```
**Expected Response:** `HTTP/1.1 200 OK` with `Content-Type: image/png` and binary image content.

### Test 16: Verify Category Hierarchy and Subcategories
```bash
curl -s http://localhost:8085/api/reference-data | jq '.categories[] | select(.parentCategoryId != null)'
```
**Expected Result:** Subcategories (`Smartphones & Tablets`, `Laptops & Computers`, `Passports & Travel Docs`, `Luggage & Suitcases`) referencing their parent categories.

### Test 17: Verify Staff ISA Specialization Tables
Connect to PostgreSQL and verify subtype records:
```bash
PGPASSWORD=shivam123 psql -h 127.0.0.1 -p 5432 -U airport_app -d airport_lost_found -c "
SELECT s.staff_id, s.name, s.role,
       CASE WHEN lfo.staff_id IS NOT NULL THEN 'Lost & Found Officer'
            WHEN sec.staff_id IS NOT NULL THEN 'Security Officer'
            WHEN adm.staff_id IS NOT NULL THEN 'Administrator'
            ELSE 'Base Staff' END AS subtype
FROM app.staff s
LEFT JOIN app.lost_found_officer lfo ON s.staff_id = lfo.staff_id
LEFT JOIN app.security_officer sec ON s.staff_id = sec.staff_id
LEFT JOIN app.administrator adm ON s.staff_id = adm.staff_id;
"
```

### Test 18: Verify Dependent Entities & Derived Metrics
1. **PassengerPhone Dependent Entity:**
   ```bash
   PGPASSWORD=shivam123 psql -h 127.0.0.1 -p 5432 -U airport_app -d airport_lost_found -c "SELECT * FROM app.passenger_phone;"
   ```
2. **ItemPhoto Weak Entity:**
   ```bash
   PGPASSWORD=shivam123 psql -h 127.0.0.1 -p 5432 -U airport_app -d airport_lost_found -c "SELECT found_item_id, photo_number, photo_type, photo_url FROM app.item_photo;"
   ```
3. **StorageSlot Weak Entity & Computed Occupancy:**
   ```bash
   PGPASSWORD=shivam123 psql -h 127.0.0.1 -p 5432 -U airport_app -d airport_lost_found -c "
   SELECT loc.name AS room, loc.room_number, loc.cabinet_number,
          COUNT(f.found_item_id) AS occupied_slots,
          loc.capacity
   FROM app.storage_location loc
   LEFT JOIN app.found_item f ON loc.storage_id = f.storage_id
   GROUP BY loc.storage_id, loc.name, loc.room_number, loc.cabinet_number, loc.capacity;
   "
   ```
4. **Days in Storage Derived Calculation:**
   ```bash
   PGPASSWORD=shivam123 psql -h 127.0.0.1 -p 5432 -U airport_app -d airport_lost_found -c "
   SELECT f.found_item_id, f.stored_at,
          EXTRACT(DAY FROM CURRENT_TIMESTAMP - f.stored_at) AS days_in_storage
   FROM app.found_item f
   WHERE f.stored_at IS NOT NULL;
   "
   ```

