# AirportLostandFound — Deployment Guide

This guide details instructions for deploying the **AirportLostandFound** system into production environments, ranging from single-server VPS setups with Docker Compose to managed cloud providers (AWS, GCP, Render, Railway, DigitalOcean).

---

## 1. Production Architecture Overview

In production, the system consists of three distinct tiers:

```
[ Client Browser ]
       │  (HTTPS :443)
       ▼
[ Nginx Reverse Proxy / Load Balancer ]
       ├── Static SPA Routes (/*)  ──► [ Frontend Static Assets / Nginx ]
       └── API Requests (/api/*)   ──► [ Spring Boot Backend (:8080) ]
                                              │  (JDBC)
                                              ▼
                                    [ PostgreSQL 16+ Database (:5432) ]
```

---

## 2. Environment Variables Specification

The system is configured strictly via standard environment variables:

### Backend Service (`airport-lost-found-api`)
| Variable | Description | Example Value |
| :--- | :--- | :--- |
| `DB_URL` | JDBC Connection String | `jdbc:postgresql://postgres.internal:5432/airport_lost_found` |
| `DB_USERNAME` | Application Database User | `airport_app` |
| `DB_PASSWORD` | Database User Password | `StrongSecretPassword!123` |
| `PORT` | HTTP Port for Spring Boot | `8080` |
| `SPRING_PROFILES_ACTIVE` | Active Spring Profile | `prod` |

### Database (`PostgreSQL`)
| Variable | Description | Example Value |
| :--- | :--- | :--- |
| `POSTGRES_DB` | Initial Database Name | `airport_lost_found` |
| `POSTGRES_USER` | Admin User | `airport_admin` |
| `POSTGRES_PASSWORD` | Admin Password | `AdminStrongPassword!456` |

---

## 3. Deployment Strategy 1: Docker Compose (VPS / Cloud VM)

Recommended for AWS EC2, DigitalOcean Droplets, Hetzner, or Linode servers.

### Step 1: Clone Repository onto the Server
```bash
git clone <repository_url> /opt/airportLostFound
cd /opt/airportLostFound
```

### Step 2: Create a Production Environment File (`.env`)
Create a `.env` file in the project root:
```ini
POSTGRES_DB=airport_lost_found
POSTGRES_USER=airport_admin
POSTGRES_PASSWORD=ReplaceWithStrongDbAdminPassword!
DB_USERNAME=airport_app
DB_PASSWORD=ReplaceWithStrongAppPassword!
PORT=8080
```

### Step 3: Launch Containers
```bash
docker compose -f docker-compose.yml up -d --build
```

### Step 4: Verify Container Health
```bash
docker compose ps
docker compose logs -f backend
```

---

## 4. Deployment Strategy 2: PaaS Platforms (Render / Railway / Fly.io)

### 1. PostgreSQL Database Service
1. Create a Managed PostgreSQL instance on your provider.
2. Connect to the database using `psql` or the web console and execute the SQL scripts in order:
   - `database/01_schema.sql`
   - `database/02_seed.sql`
   - `database/05_views.sql`
   - `database/06_triggers.sql`
   - `database/11_domain_alignment.sql`

### 2. Backend Web Service
1. Create a new **Web Service** pointing to the repository.
2. Set Root Directory to: `backend/airport-lost-found-api`.
3. Choose **Dockerfile** as the build runtime (multi-stage JDK 25 builder provided in `Dockerfile`).
4. Set Environment Variables:
   - `DB_URL`: Provided JDBC URL from your managed database.
   - `DB_USERNAME`: Database user.
   - `DB_PASSWORD`: Database password.
   - `PORT`: `8080`.

### 3. Frontend Static Web Service
1. Create a **Static Site** (or Docker Web Service) pointing to the repository.
2. Set Root Directory: `frontend`.
3. Build Command: `npm install && npm run build`.
4. Publish Directory: `dist`.
5. Rewrite Rules: Route all requests (`/*`) to `/index.html` for client-side routing.
6. Configure API Proxy: In production, configure your domain's CDN or reverse proxy to route `/api/*` requests to the Backend Web Service.

---

## 5. Nginx Reverse Proxy & SSL Configuration

When deploying behind an independent Nginx server or host, use this battle-tested configuration:

```nginx
# /etc/nginx/sites-available/airportlostfound.conf

# 1. HTTP -> HTTPS Redirect
server {
    listen 80;
    listen [::]:80;
    server_name lostfound.your-airport.com;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}

# 2. Production HTTPS Server
server {
    listen 443 ssl http2;
    listen [::]:443 ssl http2;
    server_name lostfound.your-airport.com;

    # SSL Certificates (managed via Certbot / Let's Encrypt)
    ssl_certificate /etc/letsencrypt/live/lostfound.your-airport.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/lostfound.your-airport.com/privkey.pem;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;

    # Gzip Compression
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml image/svg+xml;
    gzip_min_length 1024;

    # Frontend Single Page Application
    root /opt/airportLostFound/frontend/dist;
    index index.html;

    # Static 3D Assets Caching (.glb, .woff2, images)
    location ~* \.(glb|woff2|woff|png|jpg|jpeg|svg|ico)$ {
        expires 30d;
        add_header Cache-Control "public, no-transform";
    }

    # SPA Fallback
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Backend REST API Reverse Proxy
    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_http_version 1.1;

        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Session Cookie handling
        proxy_cookie_path / "/; Secure; HttpOnly; SameSite=Lax";

        # Timeouts
        proxy_connect_timeout 60s;
        proxy_read_timeout 60s;
    }
}
```

### Install SSL Certificates via Certbot
```bash
sudo certbot --nginx -d lostfound.your-airport.com
```

---

## 6. Security Hardening Checklist

Before exposing the service to public traffic:

1. **Principle of Least Privilege in Database:**
   - The application connects as `airport_app`, which only has `SELECT, INSERT, UPDATE, DELETE` permissions inside the `app` schema.
   - It cannot drop schemas, alter tables, or access database administration functions.

2. **Session Cookie Security:**
   - Spring Security cookies (`JSESSIONID`) are managed with `HttpOnly` and `SameSite=Lax`.
   - Ensure the reverse proxy sets `Secure` over HTTPS connections.

3. **CSRF Protection:**
   - Spring Security requires a CSRF token for all state-modifying requests (`POST`, `PUT`, `DELETE`).
   - The frontend automatically extracts the `XSRF-TOKEN` cookie and attaches it via the `X-XSRF-TOKEN` header.

4. **Container Security:**
   - The backend `Dockerfile` runs as a dedicated non-root user (`appuser`).
   - The frontend container uses a lightweight Alpine image.

---

## 7. Database Backup & Maintenance Procedures

### Daily Automated Backup
Add a cron job to take daily compressed dumps:
```bash
# Add to crontab: crontab -e
0 2 * * * PGPASSWORD="shivam123" pg_dump -h 127.0.0.1 -U airport_app -d airport_lost_found -Fc > /var/backups/airport_lost_found_$(date +\%Y\%m\%d).dump
```

### Database Restoration
```bash
PGPASSWORD="admin_password" pg_restore -h 127.0.0.1 -U airport_admin -d airport_lost_found -c /var/backups/airport_lost_found_20261021.dump
```
