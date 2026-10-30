# 🏛️ Arsitektur Sistem & Model Konkurensi — Spring Boot Edition
## PT Adijayantara Logistics Indonesia

Dokumen ini menjelaskan arsitektur perangkat lunak, pola desain, model konkurensi, dan kebijakan integritas basis data yang diimplementasikan pada repositori `adijayantara-logistics-backend-spring`.

---

### 1. Pola Desain: Clean / Hexagonal Architecture

Sistem mengadopsi pemisahan lapisan logika (*Separation of Concerns*) yang ketat:

```text
┌─────────────────────────────────────────────────────────────┐
│                    Web & API Controllers                    │
│    (AuthController, CashflowController, InvoiceController,  │
│     AnalyticsController, AuditController, UserController)   │
└──────────────────────────────┬──────────────────────────────┘
                               │ (DTO Request / Response)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                    Application Services                     │
│    (CashflowService, InvoiceService, ExcelExportService,    │
│     AuditService, AnalyticsService, NotificationService)    │
└──────────────┬──────────────────────────────┬───────────────┘
               │                              │
               ▼                              ▼
┌─────────────────────────────┐┌──────────────────────────────┐
│       Domain Entities       ││    Persistence Repositories  │
│  (CashflowEntry, Invoice,   ││  (Spring Data JPA &          │
│   User, Role, Vendor, etc.) ││   PostgreSQL JdbcClient)     │
└─────────────────────────────┘└──────────────┬───────────────┘
                                              │
                                              ▼
                               [PostgreSQL 15 & Redis 7]
```

1. **Domain Layer (`domain/entity`, `domain/enums`)**:
   - Berisi 13 entitas JPA dan tipe enumerasi bisnis (e.g. `EntryType`, `InvoiceStatus`, `UserRole`, `NotificationSeverity`).
   - Bebas dari dependensi logika web atau HTTP.
2. **Persistence Layer (`repository/`)**:
   - Menggunakan kombinasi **Spring Data JPA** untuk operasi CRUD tipe aman dan **`JdbcClient`** untuk query agregasi analitik, matriks BCG, dan window functions kompleks yang memerlukan efisiensi tinggi.
3. **Application Service Layer (`service/`)**:
   - Mengenkapsulasi seluruh aturan bisnis: kalkulasi rolling balance kas berurutan dengan *pessimistic locking* (`SELECT FOR UPDATE`), audit trail logging otomatis, dan pemindaian jatuh tempo faktur piutang.
4. **Transport Layer (`controller/`, `dto/`)**:
   - Menerima HTTP request, melakukan validasi payload, dan membungkus response dalam struktur standar `ApiResponse`.

---

### 2. Model Konkurensi: Java 21 Virtual Threads (Project Loom)

Secara tradisional, Spring Boot menggunakan *platform thread pool* (Tomcat worker threads 200) yang memakan memori ~1MB per thread OS. Pada sistem ini, **Virtual Threads** diaktifkan penuh pada `application.yml`:

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

#### Keunggulan Teknis:
- **Konkurensi Ultra-Tinggi**: Mampu menangani ribuan request I/O blocking (seperti query PostgreSQL, Redis lookup, atau rendering file Excel Apache POI) tanpa menghabiskan thread pool OS.
- **Konsumsi Memori Rendah**: Virtual Thread dialokasikan secara dinamis di Java heap dan di-unmount saat thread berada dalam kondisi menunggu I/O (*carrier thread unparking*).

---

### 3. Kebijakan Integritas Database: Zero DDL Modification

Sesuai aturan arsitektur permanen (`AGENTS.md`):
- Skema PostgreSQL dikelola **eksklusif** oleh migrasi SQL di monorepo Golang (`services/core-go/migrations/`).
- Konfigurasi Hibernate diset:
  ```yaml
  spring:
    jpa:
      hibernate:
        ddl-auto: validate
  ```
- **Larangan**: Spring Boot tidak diperkenankan memasang Flyway/Liquibase atau mengeksekusi perintah DDL (`ALTER`, `CREATE`, `DROP`). Seluruh entitas JPA beradaptasi terhadap skema yang telah ada.

---

### 4. Rolling Balance Saldo Kas & Row-Level Locking

Pada sistem pencatatan buku kas operasional, saldo berjalan (*running balance*) baris ke-$n$ bergantung pada saldo baris ke-$(n-1)$.

Untuk mencegah *race condition* atau saldo korup saat ada dua staf menginput pengeluaran armada secara bersamaan:
1. `CashflowService` mengambil transaksi kas terakhir dengan **Pessimistic Lock**:
   ```sql
   SELECT * FROM cashflow_entries ORDER BY id DESC LIMIT 1 FOR UPDATE
   ```
2. Saldo baru dihitung secara atomik:
   - Jika `TOP_UP`: $\text{Saldo Baru} = \text{Saldo Sebelumnya} + \text{Kredit}$
   - Jika `SHIPMENT`: $\text{Saldo Baru} = \text{Saldo Sebelumnya} - \text{Debit}$
3. Transaksi di-commit secara terisolasi dalam Spring `@Transactional`.
