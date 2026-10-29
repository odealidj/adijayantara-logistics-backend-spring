# ==============================================================================
# Multi-stage Dockerfile: Spring Boot 3 + Java 21 LTS
# PT Adijayantara Logistics Indonesia
# ==============================================================================

# Stage 1: Build & Package
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Salin konfigurasi gradle wrapper & dependencies cache
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

# Download dependensi agar ter-cache di layer Docker
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# Salin source code & kompilasi binary jar
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# Stage 2: Minimal Production Runtime
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Buat non-root user untuk alasan keamanan enterprise
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser:appgroup

# Salin hasil build artifact dari builder
COPY --from=builder /app/build/libs/*.jar app.jar

# Konfigurasi Port & Alokasi Memori JVM (Ramah Resource VPS)
EXPOSE 8081
ENV JAVA_OPTS="-Xms128m -Xmx384m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

