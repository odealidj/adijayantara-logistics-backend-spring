# 🚚 PT Adijayantara Logistics Indonesia — Enterprise Fullstack (Spring Boot & Next.js Showcase Edition)

[![Java Version](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Next.js](https://img.shields.io/badge/Next.js-16.3-black?style=for-the-badge&logo=next.js&logoColor=white)](https://nextjs.org/)
[![Virtual Threads](https://img.shields.io/badge/Virtual%20Threads-Project%20Loom-437291?style=for-the-badge)](https://openjdk.org/projects/loom/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)

Repositori Fullstack Showcase mandiri (*standalone*) untuk **PT. Adijayantara Logistics Indonesia**, yang menggabungkan:
1. **Backend Engine**: **Java 21 LTS + Spring Boot 3.3.5** (Port `:8081`)
2. **Frontend UI Engine**: **Next.js 16 + React 19 + Tailwind CSS** (Port `:3001` di folder `frontend/`)

Proyek ini dirancang dengan **100% API Contract Parity** terhadap antarmuka frontend serta berbagi skema basis data PostgreSQL yang sama dengan implementasi produksi Golang (`cashflow-shipment-adi`), terisolasi penuh secara port dan runtime sehingga bebas bentrok (*anti-collision*).

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
- 📊 **Executive Analytics & Forensic Audit**: BCG 4-Quadrant Route Matrix, Cashflow Runway 60-hari, Customer Discipline & Pareto 80/20, Staff SLA scorecard, and Input Lag monitoring.
- 📑 **Excel Export/Import Engine**: Standar kop surat resmi PT Adijayantara, auto-styling border & currency number format `#,##0`, multi-format batch import with balance restoration.
- 👥 **PBAC Role & User Management**: RBAC/PBAC matrix granularity, Last Admin Standing protection, Anti Self-Deletion, C-Level Immunity.
- 🔔 **Intelligent Financial Notifications**: Scan otomatis invoice H-3, Hari-H, Overdue piutang, dan peringatan kritis saldo kas operasional menipis.
- 🔒 **Spring Security 6 Stateless Session**: Kompatibel penuh dengan token sesi Redis (`sess_<token>`) yang diterbitkan oleh sistem autentikasi frontend.
- 📊 **Prometheus & Actuator**: Metrik telemetri real-time yang siap dikonsumsi oleh Prometheus & Grafana.
- 📖 **OpenAPI 3 / Swagger UI**: Dokumentasi API interaktif pada `/swagger-ui.html`.

---

## 📋 Katalog Endpoint API Lengkap (100% Contract Parity)

| Modul | Endpoint | Method | Deskripsi |
|---|---|---|---|
| **Auth** | `/api/v1/auth/login` | POST | Login kredensial & terbitkan sesi Redis `sess_<hex>` |
| | `/api/v1/auth/me` | GET | Profil user & hak akses sesi aktif saat ini |
| | `/api/v1/auth/logout` | POST | Revoke token sesi dari Redis |
| **Cashflow** | `/api/v1/cashflow` | GET | Daftar buku kas dengan paginasi, search, dan filter periode |
| | `/api/v1/cashflow/summary` | GET | Ringkasan saldo berjalan, total debit, kredit, omset |
| | `/api/v1/cashflow/periods` | GET | Daftar tahun & bulan transaksi yang tersedia |
| | `/api/v1/cashflow/shipment` | POST | Catat pengiriman armada (pessimistic lock rolling balance) |
| | `/api/v1/cashflow/topup` | POST | Suntikan modal awal / penambahan dana kas operasional |
| | `/api/v1/cashflow/{id}` | PUT, DELETE | Update dan soft-delete transaksi kas |
| | `/api/v1/cashflow/{id}/status` | PATCH | Pembaruan status operasional transaksi |
| | `/api/v1/cashflow/export` | GET | Ekspor Excel resmi buku kas PT Adijayantara |
| | `/api/v1/cashflow/import` | POST | Impor batch transaksi kas via file spreadsheet Excel |
| **Invoices** | `/api/v1/invoices` | GET, POST | Manajemen piutang, auto kalkulasi TOP & jatuh tempo |
| | `/api/v1/invoices/summary` | GET | Kartu KPI tagihan: total unpaid, overdue, paid |
| | `/api/v1/invoices/periods` | GET | Periode penagihan aktif |
| | `/api/v1/invoices/export` | GET | Rekapitulasi faktur resmi & 3 kolom tanda tangan otorisasi |
| | `/api/v1/invoices/{id}` | GET, PUT, DELETE | Detail, edit, dan soft-delete faktur penagihan |
| | `/api/v1/invoices/{id}/history` | GET | Riwayat pelunasan dan rescheduling jatuh tempo |
| | `/api/v1/invoices/{id}/pay` | PATCH | Pelunasan tagihan & auto-topup buku kas `INVOICE_PAYMENT` |
| | `/api/v1/invoices/{id}/reschedule` | POST | Perpanjangan jatuh tempo & audit logging |
| **Master Data** | `/api/v1/vendors` | GET, POST, PUT, DELETE | Master transporter armada rekanan |
| | `/api/v1/customers` | GET, POST, PUT, DELETE | Master klien / pemilik muatan |
| | `/api/v1/activity-presets` | GET, POST, PUT, DELETE | Master keterangan kegiatan & rute armada supir |
| **Analytics** | `/api/v1/analytics/cashflow-runway` | GET | Proyeksi likuiditas 60 hari & CCC Gap analisis |
| | `/api/v1/analytics/route-matrix` | GET | Matriks 4-kuadran BCG (Stars, Cash Cows, Dogs, Question Marks) |
| | `/api/v1/analytics/customer-discipline-pareto` | GET | DSO penagihan klien, 80/20 omset, dan deteksi churn alert |
| | `/api/v1/analytics/vendor-efficiency` | GET | Konsentrasi ketergantungan mitra rekanan transporter |
| **Audit** | `/api/v1/audit/summary` | GET | KPI forensik operasional & kepatuhan SLA staf |
| | `/api/v1/audit/staff-scorecard` | GET | Scorecard performa input staf admin & finance |
| | `/api/v1/audit/anomalies` | GET | Deteksi fraud transaksi akhir pekan & input tertunda |
| | `/api/v1/audit/input-lag` | GET | Catatan lag penginputan buku kas (hari) |
| | `/api/v1/audit/logs` | GET, GET /{id} | Audit trail aktivitas user berpaginasi |
| **Notifications** | `/api/v1/notifications` | GET | Feed notifikasi penerima role/user dengan filter urgensi |
| | `/api/v1/notifications/unread-count` | GET | Badge jumlah notifikasi belum dibaca |
| | `/api/v1/notifications/{id}/read` | PATCH | Tandai sebuah notifikasi telah dibaca |
| | `/api/v1/notifications/read-all` | POST | Tandai seluruh notifikasi telah dibaca |
| | `/api/v1/notifications/{id}` | DELETE | Hapus notifikasi |
| | `/api/v1/notifications/check-invoices` | POST | Pemindaian jatuh tempo & penerbitan alert otomatis |
| **Roles & PBAC** | `/api/v1/roles` | GET, POST | Daftar peran sistem & custom role beserta user count |
| | `/api/v1/roles/permissions` | GET | Kamus seluruh kode izin dikelompokkan per modul |
| | `/api/v1/roles/{id}` | GET, PUT, DELETE | Detail dan pembaruan nama/deskripsi peran |
| | `/api/v1/roles/{id}/permissions` | PUT | Pembaruan matriks checklist hak akses peran |
| **Users** | `/api/v1/users` | GET, POST | Manajemen data staf pengguna |
| | `/api/v1/users/{id}` | GET, PUT, DELETE | Detail, update profil/role/status, dan soft-delete |
| | `/api/v1/users/{id}/password` | PATCH | Reset kata sandi pengguna & force logout sesi |
| **System** | `/api/v1/system/metrics` | GET | Telemetri runtime JVM, DB pool, dan KPI bisnis |
| | `/api/v1/system/simulate-slow-query` | POST | Simulasi query lambat PostgreSQL |
| | `/api/v1/system/benchmark/*` | GET, POST | Status & orkestrasi k6 performance testing |
| **Utility** | `/api/v1/utility/idempotency-key` | GET | RFC 4122 UUID v4 generator untuk header mutasi |
| | `/health`, `/api/v1/health` | GET | Healthcheck endpoint publik |

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

### 2. Jalankan Backend Spring Boot (Port :8081)
```bash
./gradlew bootRun
```
Backend aktif di **http://localhost:8081**.
- **Swagger Documentation**: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)
- **Actuator Health**: [http://localhost:8081/actuator/health](http://localhost:8081/actuator/health)
- **Prometheus Metrics**: [http://localhost:8081/actuator/prometheus](http://localhost:8081/actuator/prometheus)

### 3. Jalankan Frontend Next.js Showcase (Port :3001)
Buka terminal baru di subdirektori `frontend/`:
```bash
cd frontend
npm install
npm run dev
```
Frontend web aktif di **http://localhost:3001**, otomatis terhubung ke Backend Spring Boot di port `:8081`.

---

## ⚖️ Aturan Sistem & Integritas Data

Sesuai pedoman arsitektur [AGENTS.md](./AGENTS.md):
1. **Zero DDL Modification**: Spring Boot dilarang mengeksekusi DDL migrasi skema. Struktur tabel dikelola secara terpusat oleh skrip migrasi Go.
2. **Spring Mengikuti Frontend**: Seluruh endpoint, DTO request/response, dan format waktu beradaptasi 100% terhadap kontrak frontend Next.js.
3. **Port Isolation**: Service ini berjalan di port `:8081` agar bebas bentrok dengan service Go (`:8080`) dan Next.js (`:3000`).

---

## 📜 Lisensi
Hak Cipta © 2026 PT. Adijayantara Logistics Indonesia. Seluruh hak cipta dilindungi undang-undang.

