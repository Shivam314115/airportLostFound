# Stage 1: Build the Spring Boot application using Maven and JDK 21
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace/app

# Cache dependencies
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Compile and package application
COPY src src
RUN ./mvnw package -DskipTests -B

# Stage 2: Minimal runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup && \
    mkdir -p uploads/photos && \
    chown -R appuser:appgroup /app

USER appuser

COPY --chown=appuser:appgroup --from=builder /workspace/app/target/*.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
