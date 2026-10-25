# ==============================================================================
# Airport Lost & Found Management System - Production Container
# Multi-stage build: React + Vite Frontend -> Spring Boot Backend -> Minimal JRE
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build Frontend SPA
# ------------------------------------------------------------------------------
FROM node:22-alpine AS frontend-builder
WORKDIR /app/frontend

COPY frontend/package*.json ./
RUN npm install --silent

COPY frontend/ ./
RUN npm run build

# ------------------------------------------------------------------------------
# Stage 2: Build Spring Boot Backend with Embedded Frontend Assets
# ------------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-21 AS backend-builder
WORKDIR /app/backend

COPY backend/airport-lost-found-api/pom.xml .
COPY backend/airport-lost-found-api/.mvn .mvn
COPY backend/airport-lost-found-api/mvnw .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY backend/airport-lost-found-api/src src
COPY --from=frontend-builder /app/frontend/dist src/main/resources/static

RUN ./mvnw clean package -DskipTests -B

# ------------------------------------------------------------------------------
# Stage 3: Minimal Production Runtime
# ------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup && \
    mkdir -p uploads/photos && \
    chown -R appuser:appgroup /app

USER appuser

COPY --chown=appuser:appgroup --from=backend-builder /app/backend/target/*.jar app.jar

ENV PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
