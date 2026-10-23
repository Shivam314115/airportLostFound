# AirportLostandFound — Setup & Development Guide

This guide details the complete local setup instructions for the **AirportLostandFound** system, including the PostgreSQL database, Spring Boot REST API, and the React + Vite frontend with the interactive 3D passenger aircraft.

---

## 1. System Architecture & Prerequisites

### Architecture Overview
- **Frontend:** React 18, TypeScript, Vite, Three.js (`@react-three/fiber`, `@react-three/drei`), Lucide Icons, and Apple San Francisco design system.
- **Backend:** Java 21+ / 25, Spring Boot 3.4+, Spring Security (session-based authentication with CSRF and BCrypt), Spring Data JDBC.
- **Database:** PostgreSQL 16+ with dedicated roles (`airport_owner`, `airport_app`), schema isolation (`app`), automated audit triggers, and scoring views.

### Required Software
Ensure the following tools are installed on your machine:
| Tool | Minimum Version | Check Command |
| :--- | :--- | :--- |
| **Java Development Kit (JDK)** | 21+ (25 recommended) | `java -version` |
| **Node.js & npm** | Node 20+, npm 10+ | `node -v && npm -v` |
| **PostgreSQL** | 16+ | `psql --version` |
| **Git** | 2.40+ | `git --version` |
| **Docker & Docker Compose** *(Optional)* | 24+ | `docker compose version` |

---

## 2. Option A: Quick Start via Docker Compose

The fastest way to spin up the entire system (PostgreSQL database, Spring Boot API, and Nginx frontend) is using Docker Compose:

```bash
# 1. Clone or navigate to the repository root
cd /home/shivam/airportLostFound

# 2. Start all services in detached mode
docker compose up --build -d

# 3. Verify container status
docker compose ps
```

### Exposed Endpoints in Docker
- **Frontend Web UI:** `http://localhost:3000`
- **Backend REST API:** `http://localhost:8080/api/reference-data`
- **PostgreSQL Database:** `localhost:5432` (`airport_admin` / `admin_password_123`)

To stop the containers:
```bash
docker compose down
```

---

## 3. Option B: Local Step-by-Step Setup

If you prefer to run services natively on your local machine for active development, follow these steps.

### Step 1: Database Setup (PostgreSQL)

1. **Start PostgreSQL service:**
   ```bash
   # On Linux (systemd)
   sudo systemctl start postgresql
   sudo systemctl status postgresql

   # On macOS (Homebrew)
   brew services start postgresql@16
   ```

2. **Initialize Database, Roles, and Schemas:**
   Run the SQL scripts in order using the PostgreSQL superuser:
   ```bash
   # Connect as postgres superuser and run the setup scripts
   psql -U postgres -d postgres -f database/setup_local.psql
   ```

   Alternatively, if configuring manually:
   ```bash
   psql -U postgres -c "CREATE ROLE airport_owner NOLOGIN;"
   psql -U postgres -c "CREATE ROLE airport_app WITH LOGIN PASSWORD 'shivam123';"
   psql -U postgres -c "CREATE DATABASE airport_lost_found OWNER airport_owner;"
   psql -U postgres -c "GRANT CONNECT ON DATABASE airport_lost_found TO airport_app;"

   # Run schemas, seeds, views, triggers, and domain alignment
   psql -U postgres -d airport_lost_found -f database/01_schema.sql
   psql -U postgres -d airport_lost_found -f database/02_seed.sql
   psql -U postgres -d airport_lost_found -f database/05_views.sql
   psql -U postgres -d airport_lost_found -f database/06_triggers.sql
   psql -U postgres -d airport_lost_found -f database/11_domain_alignment.sql
   ```

3. **Verify Database Connectivity:**
   ```bash
   PGPASSWORD=shivam123 psql -h 127.0.0.1 -p 5432 -U airport_app -d airport_lost_found -c "SELECT COUNT(*) FROM app.categories;"
   # Expected output: 3
   ```

---

### Step 2: Backend Setup (Spring Boot)

1. **Navigate to the Backend Directory:**
   ```bash
   cd backend/airport-lost-found-api
   ```

2. **Configure Environment Variables:**
   The backend connects to PostgreSQL using the following environment variables (using port 8085 to avoid collision with SGSITS testing on 8080):
   ```bash
   export DB_URL="jdbc:postgresql://localhost:5432/airport_lost_found"
   export DB_USERNAME="airport_app"
   export DB_PASSWORD="shivam123"
   export PORT=8085
   ```

3. **Compile and Package Application:**
   ```bash
   ./mvnw clean package -DskipTests
   ```

4. **Launch the Spring Boot API:**
   ```bash
   PORT=8085 ./mvnw spring-boot:run
   ```

5. **Verify Backend Health & Photo Uploads:**
   In another terminal, test the public reference data endpoint:
   ```bash
   curl -s http://localhost:8085/api/reference-data | jq .
   ```
   You should receive airport categories with subcategory hierarchies, locations, and flights.
   Test public photo service:
   ```bash
   curl -s http://localhost:8085/api/photos/sample.png -I
   ```

---

### Step 3: Frontend Setup (React + Vite)

1. **Navigate to the Frontend Directory:**
   ```bash
   cd frontend
   ```

2. **Install Node Dependencies:**
   ```bash
   npm install
   ```

3. **Verify 3D Airplane Asset:**
   Ensure `plane.glb` is present in `public/`:
   ```bash
   ls -lh public/plane.glb
   # Should report approximately ~1.7 MB
   ```

4. **Start Vite Development Server (with hot reloading):**
   ```bash
   npm run dev
   ```
   The development server will start on `http://localhost:5173`.
   API requests to `/api/*` are automatically proxied to `http://localhost:8080` via `vite.config.ts`.

5. **Or Start Production Preview:**
   ```bash
   npm run build
   npm run preview -- --port 4173 --host 0.0.0.0
   ```
   Accessible at `http://localhost:4173`.

---

## 4. Option C: One-Click Startup Script

A convenient startup shell script is provided in the root directory:

```bash
chmod +x start.sh
./start.sh
```

This script:
1. Validates that PostgreSQL is running on port 5432.
2. Compiles and packages the Spring Boot backend with Maven.
3. Installs frontend dependencies and runs a production build.
4. Outputs the running addresses for backend and frontend.

---

## 5. Demonstration Credentials & RBAC Subrole Matrix

The platform implements strict Role-Based Access Control (RBAC) across passenger self-service and **7 specialized airport staff subroles**, plus legacy staff backward compatibility:

| Subrole Title | Database Role (`staff_role`) | Default Login Email | Password | Granted Scope & Authorities |
| :--- | :--- | :--- | :--- | :--- |
| **Airport Lost & Found Agent** | `lost_found_agent` | `agent@airport.test` | `shivam123` | Intake newly discovered property, search catalog (`ROLE_STAFF_INTAKE`, `ROLE_STAFF_SEARCH`) |
| **Claims Verification Officer** | `claims_verification_officer` | `claims@airport.test` | `shivam123` | Review passenger claims, evaluate match score, check ID, approve/reject claims (`ROLE_CLAIMS_OFFICER`) |
| **Inventory & Storage Officer** | `inventory_storage_officer` | `inventory@airport.test` | `shivam123` | Track physical locker facilities, view slot occupancy, assign items to slots (`ROLE_INVENTORY_OFFICER`) |
| **Return & Dispatch Officer** | `return_dispatch_officer` | `dispatch@airport.test` | `shivam123` | Terminal desk counter handover, courier dispatch coordination (`ROLE_DISPATCH_OFFICER`) |
| **Lost Property Supervisor** | `supervisor` | `supervisor@airport.test` | `shivam123` | Executive oversight, real-time KPI telemetry, dispute escalation, view audit stream (`ROLE_SUPERVISOR`) |
| **Airport Security Officer** | `ground_staff` | `security@airport.test` | `shivam123` | Restricted field intake: report items discovered in terminals/gates (`ROLE_GROUND_STAFF`) |
| **Airport System Administrator** | `system_admin` | `admin@airport.test` | `shivam123` | Provision/deactivate staff accounts, alter subroles, system audit stream (`ROLE_SYSTEM_ADMIN`) |
| **Airport Passenger** | `passenger` | `passenger@airport.test` | `Password123!` | Report lost items, view personal reports, review candidate matches (`ROLE_PASSENGER`) |

---

## 6. Project Directory Layout

```
airportLostFound/
├── backend/
│   └── airport-lost-found-api/   # Spring Boot application
│       ├── Dockerfile            # Multi-stage JDK 25 builder & JRE runner
│       ├── pom.xml               # Maven dependencies and plugins
│       └── src/                  # Controllers, Services, Repositories, Security
├── database/                     # PostgreSQL relational scripts
│   ├── 01_schema.sql             # Tables, foreign keys, constraints
│   ├── 02_seed.sql               # Seed data for categories, locations, flights
│   ├── 05_views.sql              # Automated matching & telemetry views
│   ├── 06_triggers.sql           # Audit logging triggers
│   └── setup_local.psql          # Local role & database creator
├── frontend/                     # React 18 + Vite frontend
│   ├── public/plane.glb          # 3D Commercial Passenger Airplane model
│   ├── src/
│   │   ├── api/client.ts         # REST API adapter, CSRF handler, session storage
│   │   ├── components/           # AirplaneCanvas (Three.js), Dialog
│   │   ├── App.tsx               # Primary application view routing & state
│   │   ├── styles.css            # San Francisco design tokens & responsive CSS
│   │   └── main.tsx              # Application root
│   └── vite.config.ts            # Development proxy configuration
├── docker-compose.yml            # Multi-container orchestration
├── start.sh                      # Local build & start runner
├── setup.md                      # Setup documentation (this file)
├── deploy.md                     # Deployment guide
└── tests.md                      # Comprehensive testing instructions
```
