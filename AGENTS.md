# ATURAN SISTEM ARSITEKTUR & PENGEMBANGAN: GO & JAVA SPRING BOOT
## PT Adijayantara Logistics Indonesia — Multi-Engine Architecture Rules

Dokumen ini adalah aturan permanen (*system architecture rules*) yang WAJIB dipatuhi oleh AI Assistant (Antigravity) dalam seluruh tugas pengembangan, pengujian, pemeliharaan, dan deployment sistem Adijayantara Logistics.

---

### RULE 1: LINGKUNGAN PRODUKSI (PRODUCTION ENVIRONMENT)
1. **Service Go adalah Tulang Punggung Utama Produksi**:
   - Di VPS Produksi (`103.94.238.109`), backend operasional utama SELALU menggunakan Go: `cashflow-backend.service` (Systemd) pada port internal `:8080`.
   - Nginx reverse proxy di port `:80` dan `:443` secara default mengarahkan `/api/` ke service Go (`http://127.0.0.1:8080`).
2. **Larangan Menghentikan Service Go**:
   - DILARANG KERAS mematikan, me-replace, atau mengalihkan traffic utama dari `cashflow-backend.service` tanpa instruksi tertulis dan eksplisit dari User.
3. **Peran Spring Boot di Server/VPS**:
   - Jika service Java Spring Boot di-deploy ke VPS, ia HANYA berjalan sebagai **Showcase / Staging / Benchmark Engine** pada port terisolasi (`:8081`).
   - Spring Boot TIDAK BOLEH mengganggu ketersediaan (*high availability*) maupun alokasi resource dari service Go di produksi.

---

### RULE 2: ISOLASI PORT & PROSES RUNTIME (ANTI-COLLISION / BEBAS BENTROK)
Untuk mencegah *port conflict* dan bentrok *process runtime*:
- **Port 8080**: Khusus untuk **Core Go API** (`services/core-go`).
- **Port 8081**: Khusus untuk **Java Spring Boot API** (`adijayantara-logistics-backend-spring`).
- **Port 3000**: Khusus untuk **Frontend Next.js Produksi** (`apps/web-next`).
- **Port 3001**: Khusus untuk **Frontend Next.js Showcase Spring** (`adijayantara-logistics-web-spring`).
- **Port 5432**: Database PostgreSQL (shared container `cashflow_db`).
- **Port 6379**: Cache & Session Redis (shared container `cashflow_redis`).
- **Port 9090**: Prometheus Metrics Monitoring (`cashflow_prometheus`).

Kedua backend (Go & Spring) dan kedua frontend dapat berjalan bersamaan secara simultan tanpa saling memblokir (*non-interfering concurrent execution*).

---

### RULE 3: INTEGRITAS DATABASE POSTGRESQL (SINGLE SOURCE OF TRUTH & ZERO SPRING DDL)
1. **Skema Database Dikelola Eksklusif oleh Migrasi Go**:
   - Seluruh perubahan DDL (tabel, kolom, relasi, tipe enum, indeks, sequence, trigger) HANYA didefinisikan dan dieksekusi melalui migration scripts di `services/core-go/migrations/`.
2. **Spring Boot Dilarang Menjalankan Migration (Zero Migration Tool)**:
   - Spring Boot DILARANG memasang atau menjalankan tools migrasi database otomatis (seperti Flyway, Liquibase, atau sejenisnya).
   - Inisialisasi dan migrasi struktur database dikelola 100% di luar Spring Boot.
3. **Logika Spring Boot Dilarang Mengubah Struktur Database (Zero DDL Modification)**:
   - Konfigurasi Hibernate WAJIB diset: `spring.jpa.hibernate.ddl-auto=validate` (atau `none`).
   - DILARANG KERAS menggunakan mode `create`, `create-drop`, atau `update`.
   - Logika di Spring Boot (Entity, Repository, Service, Custom Query) DILARANG mengeksekusi perintah DDL (`CREATE`, `ALTER`, `DROP`) terhadap table, column, index, constraint, trigger, sequence, maupun enum type.
   - Spring Boot HANYA memiliki hak akses manipulasi data (DML: `SELECT`, `INSERT`, `UPDATE`, `DELETE` data transaksi), BUKAN struktur skema.
4. **Data Loss Prevention**:
   - Dilarang keras mengeksekusi perintah SQL destruktif (`DROP`, `TRUNCATE`, `DELETE` tanpa `WHERE 1=1`) baik di lokal maupun di VPS tanpa konfirmasi eksplisit dari User.

---

### RULE 4: KESETARAAN KONTRAK API (SPRING MENGIKUTI FRONTEND, TIDAK SEBALIKNYA)
1. **Prinsip Baku: Spring Mengikuti Frontend Next.js (Bukan Sebaliknya)**:
   - Kode aplikasi frontend Next.js (`apps/web-next`) TIDAK BOLEH diubah hanya demi menyesuaikan Spring Boot.
   - Frontend Next.js adalah referensi baku kontrak API. Backend Spring Boot yang WAJIB menyesuaikan diri 100% terhadap:
     - Seluruh endpoint URI, HTTP method, query parameter, dan path variable yang dipanggil oleh Next.js.
     - Struktur JSON request body yang dikirim oleh Next.js.
     - Format JSON response yang diharapkan oleh Next.js.
     - Header autentikasi (`Authorization: Bearer sess_<token>`).
     - Format tanggal/waktu (`YYYY-MM-DD` atau ISO 8601 string) yang dikonsumsi oleh Next.js.
2. **Format Response JSON Wajib Seragam**:
   - Seluruh endpoint Spring Boot WAJIB membungkus response dalam struktur standar:
     ```json
     {
       "status": true,
       "message": "Pesan sukses atau info",
       "data": { ... },
       "meta": { "page": 1, "limit": 20, ... }
     }
     ```
   - Penanganan error harus menghasilkan format error standar:
     ```json
     {
       "status": false,
       "message": "Pesan ramah pengguna",
       "error": "Detail teknis (opsional)"
     }
     ```
3. **Kompatibilitas Sesi Autentikasi**:
   - Token sesi disimpan di Redis dengan format kunci yang kompatibel (`sess_<token>`), sehingga sesi user yang login tidak tertolak saat berganti engine backend.

---

### RULE 5: PEMISAHAN REPOSITORI, TARGET REMOTE GIT & COMMIT BOUNDARIES
Sistem ini menggunakan 2 repositori mandiri yang terisolasi ketat:
1. **Repositori Fullstack Showcase Spring Boot (`adijayantara-logistics-backend-spring`)**:
   - **Target Remote Git**: `git@github.com:odealidj/adijayantara-logistics-backend-spring.git`
   - **Direktori Lokal**: `/home/aliube/Workspace/Adi/adijayantara-logistics-backend-spring`
   - **Cakupan Kode**: Seluruh perubahan ekosistem Spring Boot:
     - Backend Java 21, Gradle, Dockerfile Spring (`src/`, `build.gradle`, dll pada port `:8081`).
     - Frontend Next.js khusus Spring Showcase (`frontend/` pada port `:3001`).
2. **Repositori Monorepo Utama Produksi (`cashflow-shipment-adi`)**:
   - **Target Remote Git**: `git@github.com:odealidj/cashflow-shipment-adi.git`
   - **Direktori Lokal**: `/home/aliube/Workspace/Adi/cashflow-shipment-app`
   - **Cakupan Kode**: Seluruh kode ekosistem produksi Go:
     - Frontend Next.js Produksi (`apps/web-next` pada port `:3000`).
     - Core Go API (`services/core-go` pada port `:8080`).
     - Gateway (`services/gateway-go`).
     - Migrasi Database PostgreSQL & Skrip (`services/core-go/migrations/`, `scripts/`).
     - Dokumen & Infra produksi.
3. **Larangan Keras Percampuran Kode (*No Cross-Commit Pollution*)**:
   - DILARANG KERAS men-commit berkas dari salah satu repositori ke repositori lainnya.
   - Perubahan pada ekosistem Spring (baik Java `src/` maupun UI `frontend/`) HANYA di-commit ke `adijayantara-logistics-backend-spring.git`.
   - Sebelum menjalankan `git commit` dan `git push`, asisten WAJIB memverifikasi *working directory* dan URL *git remote* agar tidak terjadi salah kamar commit.
