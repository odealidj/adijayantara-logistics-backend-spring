# 📚 Dokumentasi Arsitektur & Operasional — PT Adijayantara Logistics Indonesia
## Enterprise Fullstack Showcase Edition (Java 21 LTS + Spring Boot 3 & Next.js)

Selamat datang di pusat dokumentasi teknis repositori **`adijayantara-logistics-backend-spring`**. Dokumen-dokumen di dalam direktori ini ditujukan untuk arsitek perangkat lunak, developer backend/frontend, dan DevOps engineer guna memahami arsitektur, integrasi, pengujian, serta pengoperasian sistem.

---

### 📑 Daftar Panduan Teknis

| Dokumen | Deskripsi |
|---|---|
| [**1. Arsitektur Sistem & Concurrency Model**](./arsitektur_sistem_spring_boot.md) | Penjelasan Hexagonal Architecture, Java 21 Virtual Threads (Loom), isolasi port `:8081`, dan aturan Zero DDL Hibernate. |
| [**2. Spesifikasi Kontrak API & Autentikasi**](./panduan_api_dan_kontrak_frontend.md) | Standarisasi JSON Response, kontrak integrasi Next.js, autentikasi sesi Redis 24 jam (`sess_<token>`), dan OpenAPI/Swagger UI. |
| [**3. Panduan Pengujian & Verifikasi Suite**](./panduan_pengujian_dan_benchmark.md) | Strategi pengujian integrasi Spring Boot, cakupan test suite MockMvc 100%, dan cara menjalankan frontend Next.js showcase (:3001). |
| [**4. Panduan Deployment, Docker & Monitoring**](./panduan_deployment_dan_docker.md) | Panduan kontainerisasi multi-stage Docker, tuning alokasi memori JVM enterprise, telemetri Prometheus, dan isolasi VPS. |
| [**5. Desain UI Mobile & Wireframe Referensi**](./contoh-ui-mobile/README.md) | Dokumentasi komprehensif sistem desain UI, peta navigasi layar mobile, komponen wireframe, serta mapping fitur ke backend. |
| [**6. Galeri Screenshot UI Asli**](../contoh-ui/) | Direktori tangkapan layar asli rancangan antarmuka pengguna (Beranda, Laporan, Tagihan, Pengaturan, Tambah Transaksi). |

---

### 🏛️ Ringkasan Prinsip Arsitektur (Golden Rules)

1. **Port Anti-Collision**:
   - Backend Spring Boot berjalan pada port internal `:8081`.
   - Frontend Next.js Showcase berjalan pada port `:3001` (subfolder `frontend/`).
   - Tidak ada bentrok dengan service Go produksi (`:8080`) maupun frontend produksi (`:3000`).
2. **Zero DDL Modification**:
   - `spring.jpa.hibernate.ddl-auto=validate`.
   - Struktur database dikelola 100% secara terpusat oleh skrip migrasi Go di repositori utama.
3. **API Contract Parity**:
   - 100% kompatibel dengan frontend Next.js tanpa memerlukan perubahan logika UI.

