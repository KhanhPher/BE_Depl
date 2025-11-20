# Multi-stage build cho Spring Boot

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom.xml và download dependencies (layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build JAR file
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy JAR từ builder stage
COPY --from=builder /app/target/*.jar app.jar

# Expose port (Railway sẽ tự động map port)
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=120s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT:-8080}/actuator/health || exit 1

# Run application
# Railway sẽ tự động set PORT environment variable
ENTRYPOINT ["sh", "-c", "java -Xmx512m -Xms256m -jar -Dserver.port=${PORT:-8080} app.jar"]

