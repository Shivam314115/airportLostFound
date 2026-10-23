# Return Gate — Airport Lost & Found Management System

A production-ready digital lost-and-found experience for airport operations, connecting an editorial, high-precision web frontend to a Spring Boot REST API and PostgreSQL relational database.

---

## Architecture Overview

```
┌─────────────────────────────────┐
│     Return Gate React SPA       │  (Vite + React 18, Radix UI, Montserrat + SF)
│     http://localhost:4173       │
└───────────────┬─────────────────┘
                │ /api/* (JSON + Session Cookie + CSRF)
┌───────────────▼─────────────────┐
│   Spring Boot 4.1.1 REST API    │  (Spring Security, JDBC Template, BCrypt)
│     http://localhost:8080       │
└───────────────┬─────────────────┘
                │ SQL (Schema: app)
┌───────────────▼─────────────────┐
│      PostgreSQL Database        │  (Roles: airport_owner, airport_app)
│     localhost:5432              │
└─────────────────────────────────┘
```

---

## Quick Start (Local)

### Prerequisites
- **Java 25** (or compatible OpenJDK)
- **Node.js 20+** & **npm**
- **PostgreSQL 16/17** running locally on port `5432` with database `airport_lost_found`

### 1. Launch with One Command
```bash
./start.sh
```

### 2. Manual Startup

#### Backend
```bash
cd backend/airport-lost-found-api
export DB_USERNAME=airport_app
export DB_PASSWORD=shivam123
export DB_URL=jdbc:postgresql://127.0.0.1:5432/airport_lost_found
./mvnw spring-boot:run
```

#### Frontend
```bash
cd frontend
npm install
npm run dev
# Or preview the production build:
npm run build && npm run preview
```

Frontend will be available at `http://localhost:5173` (or `http://localhost:4173` for preview) and automatically proxies `/api` calls to the Spring Boot backend on `http://localhost:8080`.

---

## Docker Deployment

To launch the full stack (PostgreSQL + Spring Boot API + Nginx Frontend) in containers:

```bash
docker-compose up --build
```

- **Frontend App**: `http://localhost:3000`
- **Backend API**: `http://localhost:8080`
- **PostgreSQL**: `localhost:5432`

---

## Database & API Integration

### Core Endpoints

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `GET` | `/api/reference-data` | Returns categories, locations, flights from DB | Public |
| `GET` | `/api/auth/csrf` | Fetches CSRF token and establishes session cookie | Public |
| `POST` | `/api/passengers` | Registers new passenger in `app.passenger` | Public |
| `POST` | `/api/auth/login` | Authenticates passenger and sets session | Public |
| `GET` | `/api/auth/me` | Returns current authenticated passenger profile | Authenticated |
| `POST` | `/api/auth/logout` | Clears authenticated session | Authenticated |
| `GET` | `/api/lost-reports` | Lists public recovery register reports | Public |
| `GET` | `/api/lost-reports?passengerId={id}` | Lists reports belonging to the current passenger | Authenticated |
| `POST` | `/api/lost-reports` | Lodges a new missing item report in PostgreSQL | Authenticated |
| `GET` | `/api/matches` | Returns matches from `app.v_match_overview` | Authenticated |

---

## Pre-Seeded Test Credentials

- **Email**: `passenger@airport.test`
- **Password**: `Password123!`
- *(Or register any new passenger account directly in the UI)*
