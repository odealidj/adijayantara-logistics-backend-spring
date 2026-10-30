# 🚀 Panduan Deployment, Docker & Monitoring
## PT Adijayantara Logistics Indonesia — Spring Boot Showcase Engine

Dokumen ini menjelaskan prosedur kontainerisasi Docker multi-stage, konfigurasi alokasi memori JVM enterprise, panduan deployment di VPS (`103.94.238.109`), serta integrasi monitoring Prometheus.

---

### 1. Multi-Stage Dockerfile untuk Backend Spring Boot

Backend Java 21 dikontainerisasi menggunakan multi-stage build untuk menghasilkan image Docker yang ramping, aman, dan efisien (~180 MB) berbasis Eclipse Temurin 21 Alpine JRE.

```dockerfile
# Stage 1: Build JAR dengan Gradle
FROM gradle:8.8-jdk21-alpine AS builder
WORKDIR /app
COPY build.gradle settings.gradle /app/
COPY src /app/src
RUN gradle bootJar --no-daemon -x test

# Stage 2: Runtime Minimalis JRE 21
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Tambahkan user non-root untuk keamanan kontainer
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser:appgroup

COPY --from=builder /app/build/libs/*.jar app.jar

# JVM Flag Tuning untuk efisiensi RAM di VPS
ENV JAVA_OPTS="-Xms128m -Xmx384m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

---

### 2. Multi-Stage Dockerfile untuk Frontend Next.js (`frontend/Dockerfile`)

Frontend showcase menggunakan Node.js Alpine dengan standalone output mode:

```dockerfile
# Stage 1: Dependencies
FROM node:20-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# Stage 2: Builder
FROM node:20-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm run build

# Stage 3: Runner
FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV PORT=3001

RUN addgroup --system --gid 1001 nodejs
RUN adduser --system --uid 1001 nextjs
USER nextjs

COPY --from=builder /app/public ./public
COPY --from=builder --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=builder --chown=nextjs:nodejs /app/.next/static ./.next/static

EXPOSE 3001

CMD ["node", "server.js"]
```

---

### 3. Docker Compose Showcase (`docker-compose.showcase.yml`)

Untuk menjalankan seluruh stack Spring Boot + Frontend Showcase secara lokal maupun di server:

```yaml
version: '3.8'

services:
  spring-backend:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: cashflow_spring_backend
    restart: unless-stopped
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=cashflow_db
      - DB_PORT=5432
      - DB_NAME=cashflow_db
      - DB_USER=postgres
      - DB_PASSWORD=postgres
      - REDIS_HOST=cashflow_redis
      - REDIS_PORT=6379
    networks:
      - cashflow_network

  spring-frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: cashflow_spring_frontend
    restart: unless-stopped
    ports:
      - "3001:3001"
    environment:
      - BACKEND_API_URL=http://spring-backend:8081
    depends_on:
      - spring-backend
    networks:
      - cashflow_network

networks:
  cashflow_network:
    external: true
```

---

### 4. Tuning Alokasi Memori JVM (Resource Constrained Environment)

Agar service Spring Boot tidak memakan alokasi RAM yang berlebih dan tidak mengganggu performa Core Go API di VPS Produksi:

- **`-Xms128m`**: Heap memory awal dialokasikan sebesar 128 MB.
- **`-Xmx384m`**: Batas maksimum heap memory dibatasi pada 384 MB (sangat aman untuk VPS dengan sisa RAM terbatas).
- **`-XX:+UseG1GC`**: Garbage Collector modern yang memprioritaskan latensi rendah tanpa stop-the-world yang lama.
- **`-XX:MaxGCPauseMillis=100`**: Menjaga pause time GC di bawah 100 milidetik.
- **`-XX:+ExitOnOutOfMemoryError`**: Memaksa kontainer melakukan restart instan jika terjadi OOM, sehingga supervisor/Docker dapat memulihkannya kembali secara bersih.

---

### 5. Isolasi Deployment di VPS Produksi (`103.94.238.109`)

Sesuai aturan arsitektur permanen (**RULE 1** & **RULE 2**):
1. Service Go produksi (`cashflow-backend.service`) tetap berjalan tak terganggu di port `:8080`.
2. Frontend Next.js produksi tetap melayani pengguna di port `:3000`.
3. Stack Spring Boot Showcase beroperasi di port terisolasi:
   - Backend Spring: `:8081`
   - Frontend Showcase: `:3001`
4. Akses Nginx opsional untuk showcase:
   - Subdomain khusus dapat diarahkan langsung ke port `:3001` (misal `spring.adijayantara.com`).
   - Tidak ada modifikasi pada konfigurasi upstream Nginx yang melayani domain produksi utama.

---

### 6. Health Check & Observability (Actuator & Prometheus)

Backend Spring Boot telah dilengkapi dengan **Spring Boot Actuator** dan **Micrometer Prometheus**:

- **Health Check Endpoint**:
  ```http
  GET http://localhost:8081/actuator/health
  ```
  *Response:* `{"status": "UP"}`
- **Prometheus Metrics Scrape Endpoint**:
  ```http
  GET http://localhost:8081/actuator/prometheus
  ```
  Endpoint ini mengekspos metrik standar JVM:
  - `jvm_memory_used_bytes`
  - `jvm_threads_live_threads`
  - `http_server_requests_seconds_count`
  - `hikaricp_connections_active`

Prometheus yang berjalan di server (`cashflow_prometheus` pada port `:9090`) dapat menambahkan target scrape:
```yaml
scrape_configs:
  - job_name: 'spring-showcase-engine'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8081']
```
