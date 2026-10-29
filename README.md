# 🚚 PT Adijayantara Logistics Indonesia — Enterprise Backend (Spring Boot Edition)

[![Java Version](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Virtual Threads](https://img.shields.io/badge/Virtual%20Threads-Project%20Loom-437291?style=for-the-badge)](https://openjdk.org/projects/loom/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)

Modul backend *enterprise-grade* untuk **Sistem Pengendalian Buku Kas Operasional, Manajemen Pengiriman (*Shipment Control*), dan Penagihan Faktur (*Invoicing & Accounts Receivable*)** pada **PT. Adijayantara Logistics Indonesia**.

Proyek ini dibangun sebagai implementasi berstandar perbankan & korporasi menggunakan **Java 21 LTS** dan **Spring Boot 3**, yang dirancang dengan **100% API Contract Parity** terhadap antarmuka frontend Next.js serta berbagi skema basis data PostgreSQL yang sama dengan implementasi Golang.

---

## 🏛️ Arsitektur Sistem & Dual-Engine Showcase

Sistem ERP Adijayantara Logistics mengadopsi arsitektur *Multi-Engine Dual Showcase*:
1. **Core Go API (`cashflow-backend.service`)**: Berjalan di port `:8080` sebagai tulang punggung produksi berkecepatan tinggi dan konkurensi ultra-ringan (~35 MB RAM).
2. **Java Spring Boot API (`adijayantara-logistics-backend-spring`)**: Berjalan di port `:8081` sebagai mesin komparatif standar korporasi dengan fitur *Virtual Threads*, *Spring Security*, dan *Spring Data JPA*.

### Ekosistem Terhubung
- **Frontend Monorepo (Next.js 16 + PWA)**: [odealidj/cashflow-shipment-adi](https://github.com/odealidj/cashflow-shipment-adi)
- **High-Throughput Go Engine**: [services/core-go](https://github.com/odealidj/cashflow-shipment-adi/tree/main/services/core-go)

---

## 🚀 Fitur Utama & Keunggulan Teknis

- ⚡ **Java 21 Virtual Threads (Project Loom)**: Menangani konkurensi I/O tinggi dengan konsumsi resource ringan tanpa batasan *thread pool* konvensional.
- 📐 **Hexagonal / Clean Architecture**: Pemisahan ketat antara domain bisnis, application service, controller web, dan repository persistence.
- 💰 **Rolling Saldo Engine**: Algoritma kalkulasi saldo kas berjalan otomatis baris demi baris secara presisi dan konsisten.
- 🧾 **Invoicing & Terms of Payment (TOP)**: Otomasi kalkulasi jatuh tempo, pelacakan umur piutang, dan pencatatan kas masuk saat pelunasan (*settlement*).
- 🔒 **Spring Security 6 Stateless Session**: Kompatibel penuh dengan token sesi Redis (`sess_<token>`) yang diterbitkan oleh sistem autentikasi frontend.
- 📊 **Prometheus & Actuator**: Metrik telemetri real-time yang siap dikonsumsi oleh Prometheus & Grafana.
- 📖 **OpenAPI 3 / Swagger UI**: Dokumentasi API interaktif pada `/swagger-ui.html`.

---

## 🛠️ Persyaratan Lingkungan (*Prerequisites*)

- **Java Development Kit (JDK)**: OpenJDK 21 LTS (Temurin/Corretto)
- **Build Tool**: Gradle 8.10+ (atau gunakan `./gradlew` bawaan)
- **PostgreSQL**: 15+ (Database: `cashflow_db`)
- **Redis**: 7+ (Port: `6379`)

---

## 🏃 Menjalankan Aplikasi Secara Lokal

### 1. Pastikan PostgreSQL & Redis Aktif
Jika menggunakan Docker:
```bash
docker start cashflow_db cashflow_redis
```

### 2. Kompilasi & Jalankan Service
```bash
./gradlew bootRun
```
Aplikasi akan aktif di **http://localhost:8081**.

### 3. Akses Swagger UI & Health Check
- **Swagger Documentation**: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- **Health Check**: [http://localhost:8081/actuator/health](http://localhost:8081/actuator/health)
- **Prometheus Metrics**: [http://localhost:8081/actuator/prometheus](http://localhost:8081/actuator/prometheus)

---

## ⚖️ Aturan Sistem & Integritas Data

Sesuai pedoman arsitektur [AGENTS.md](./AGENTS.md):
1. **Zero DDL Modification**: Spring Boot dilarang mengeksekusi DDL migrasi skema. Struktur tabel dikelola secara terpusat oleh skrip migrasi Go.
2. **Spring Mengikuti Frontend**: Seluruh endpoint, DTO request/response, dan format waktu beradaptasi 100% terhadap kontrak frontend Next.js.
3. **Port Isolation**: Service ini berjalan di port `:8081` agar bebas bentrok dengan service Go (`:8080`) dan Next.js (`:3000`).

---

## 📜 Lisensi
Hak Cipta © 2026 PT. Adijayantara Logistics Indonesia. Seluruh hak cipta dilindungi undang-undang.

