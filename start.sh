#!/usr/bin/env bash
set -e

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export DB_USERNAME="${DB_USERNAME:-airport_app}"
export DB_PASSWORD="${DB_PASSWORD:-shivam123}"
export DB_URL="${DB_URL:-jdbc:postgresql://127.0.0.1:5432/airport_lost_found}"

# Default port to 8085 to avoid collision with SGSITS autotest on 8080
if [ -z "$PORT" ]; then
    export PORT=8085
fi
export BACKEND_PORT="$PORT"

# Clean up any stale processes on PORT or 4173
fuser -k "${PORT}/tcp" 2>/dev/null || true
fuser -k "4173/tcp" 2>/dev/null || true

echo "========================================================"
echo " Airport Lost & Found Management System"
echo " Backend Target Port: $PORT"
echo " Database URL: $DB_URL"
echo "========================================================"

echo "1. Checking PostgreSQL service..."
if pg_isready -h 127.0.0.1 -p 5432 >/dev/null 2>&1; then
    echo "   [OK] PostgreSQL is active."
else
    echo "   [INFO] Starting PostgreSQL service..."
    sudo systemctl start postgresql || true
fi

echo "2. Building Frontend (Vite + React)..."
cd "$ROOT_DIR/frontend"
npm install --silent
npm run build

echo "3. Bundling Frontend into Spring Boot..."
mkdir -p "$ROOT_DIR/backend/airport-lost-found-api/src/main/resources/static"
rm -rf "$ROOT_DIR/backend/airport-lost-found-api/src/main/resources/static"/*
cp -r "$ROOT_DIR/frontend/dist"/* "$ROOT_DIR/backend/airport-lost-found-api/src/main/resources/static"/

echo "4. Building & Packaging Unified Spring Boot JAR..."
cd "$ROOT_DIR/backend/airport-lost-found-api"
./mvnw clean package -DskipTests -B

echo "========================================================"
echo " Build successful! Launching unified production server..."
echo " - Unified Web App: http://localhost:$PORT"
echo " - Frontend Preview: http://localhost:4173 (optional proxy)"
echo " Press Ctrl+C to terminate all services."
echo "========================================================"

# Trap SIGINT and SIGTERM to clean up background processes
cleanup() {
    echo ""
    echo "Stopping Airport Lost & Found services..."
    kill $BACKEND_PID $FRONTEND_PID 2>/dev/null || true
    wait $BACKEND_PID $FRONTEND_PID 2>/dev/null || true
    echo "All services stopped."
    exit 0
}
trap cleanup SIGINT SIGTERM EXIT

# Start backend
cd "$ROOT_DIR/backend/airport-lost-found-api"
PORT="$PORT" java -jar target/airport-lost-found-api-0.0.1-SNAPSHOT.jar &
BACKEND_PID=$!

# Wait for backend to be ready
echo "Waiting for Backend to start on port $PORT..."
for i in {1..35}; do
    if curl -s "http://localhost:$PORT/api/reference-data" >/dev/null 2>&1; then
        echo "   [OK] Backend is ready on http://localhost:$PORT"
        break
    fi
    sleep 1
done

# Start frontend preview
cd "$ROOT_DIR/frontend"
BACKEND_PORT="$PORT" npx vite preview --port 4173 --host 0.0.0.0 &
FRONTEND_PID=$!

wait
