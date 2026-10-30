# 📡 Spesifikasi Kontrak API & Integrasi Frontend
## PT Adijayantara Logistics Indonesia — Spring Boot Showcase Engine

Dokumen ini menjelaskan standar kontrak API, format pertukaran data JSON, mekanisme autentikasi berbasis Redis Session, proteksi Idempotency, serta panduan konsumsi API oleh frontend Next.js.

---

### 1. Standar Format Respons JSON (Uniform Response)

Semua endpoint RESTful di backend Spring Boot mengembalikan payload JSON seragam menggunakan wrapper `ApiResponse<T>`.

#### A. Format Sukses (Success Response)
```json
{
  "status": true,
  "message": "Data transaksi berhasil dimuat",
  "data": { ... },
  "meta": {
    "page": 1,
    "limit": 20,
    "total": 150,
    "total_pages": 8
  }
}
```
*Catatan:* Field `meta` bersifat opsional dan disertakan otomatis pada endpoint yang mendukung paginasi.

#### B. Format Gagal / Error (Error Response)
```json
{
  "status": false,
  "message": "Validasi input gagal",
  "error": "Field 'nominal' wajib diisi dan harus lebih besar dari 0"
}
```

HTTP Status Code dipetakan secara semantik:
- `200 OK`: Pengambilan data / pembaruan data sukses.
- `201 Created`: Pembuatan entitas baru berhasil.
- `400 Bad Request`: Validasi request body gagal atau payload salah format.
- `401 Unauthorized`: Token sesi kosong, kadaluarsa, atau tidak valid.
- `403 Forbidden`: Pengguna tidak memiliki izin akses (PBAC / permission check).
- `404 Not Found`: Sumber daya / entitas tidak ditemukan.
- `409 Conflict`: Bentrok data unik (misal duplikasi nomor rekening atau status transaksi tidak valid).
- `500 Internal Server Error`: Kegagalan server yang belum ditangani (dilindungi oleh `GlobalExceptionHandler`).

---

### 2. Autentikasi Sesi & Redis (`sess_<token>`)

Sistem mengadopsi mekanisme autentikasi *stateful token* berbasis Redis yang 100% kompatibel antar-engine (Go dan Spring Boot).

1. **Alur Login**:
   - Pengguna mengirim request `POST /api/v1/auth/login` dengan username/email dan password (BCrypt).
   - Backend memverifikasi kredensial dan menghasilkan token acak 64-karakter hex.
   - Sesi disimpan ke Redis dengan key: `sess_<token>` dan TTL selama **24 jam** (86.400 detik).
   - Response mengembalikan token dan metadata profil user.
2. **Pengiriman Header Autentikasi**:
   - Frontend Next.js mengirimkan header HTTP pada setiap request yang dilindungi:
     ```http
     Authorization: Bearer sess_a1b2c3d4e5f6...
     ```
3. **Filter Keamanan (`SessionAuthenticationFilter`)**:
   - Filter memeriksa awalan `Bearer sess_`.
   - Melakukan query Redis GET `sess_<token>`.
   - Mengambil data `user_id`, `role_id`, status aktif, dan permissions.
   - Menyuntikkan `UserPrincipal` ke dalam `SecurityContextHolder`.

---

### 3. Proteksi Idempotency (`X-Idempotency-Key`)

Untuk operasi finansial kritis (misalnya pencairan kas bon, pembayaran invoice, dan persetujuan transaksi), backend mendukung proteksi idempotensi:

- Header HTTP: `X-Idempotency-Key: <UUID>`
- Backend mencatat kunci idempotensi di Redis dengan TTL pendek (10 menit).
- Request ganda dengan kunci yang sama dalam rentang waktu tersebut akan dicegah dari eksekusi ganda dan mengembalikan respons yang sama tanpa melakukan mutasi ganda pada saldo kas/rekening.

---

### 4. Katalog Modul API Endpoint

Backend Spring Boot mengimplementasikan 10 modul utama operasional logistik:

| Modul | Base Path | Deskripsi & Fitur Utama |
|---|---|---|
| **1. Autentikasi & Akun** | `/api/v1/auth` | Login, Logout, Profile, Change Password, Token Verification |
| **2. Shipment & Order** | `/api/v1/shipments` | Manajemen Surat Jalan, Status Manifest, Rute, Pelanggan, Vendor |
| **3. Kasbon & Petty Cash** | `/api/v1/cash-advances` | Pengajuan Kasbon Supir, Persetujuan Finansial, Realisasi, Settlement |
| **4. Invoice & Billing** | `/api/v1/invoices` | Penerbitan Invoice Client, Termin Pembayaran, Pelunasan, Rekonsiliasi |
| **5. Rekening & Jurnal** | `/api/v1/accounts`, `/api/v1/journal` | Chart of Accounts (COA), Buku Kas, Jurnal Transaksi Berimbang |
| **6. Mitra & Kendaraan** | `/api/v1/partners`, `/api/v1/vehicles` | Data Armada Trucking, Uji KIR, Pajak STNK, Supir, Vendor Transport |
| **7. Analytics & Metrik** | `/api/v1/analytics` | Profit/Loss per Rute, Executive Cash Flow Summary, Tren Pengeluaran |
| **8. Audit & Forensic** | `/api/v1/audit` | Rekam Jejak Aktivitas User, Log Forensik Keuangan, Deteksi Anomali |
| **9. Notifikasi & Alert** | `/api/v1/notifications` | Pengingat Jatuh Tempo Invoice, Notifikasi Kasbon, Broadcast Pengumuman |
| **10. Utilisasi & PBAC** | `/api/v1/users`, `/api/v1/roles` | Manajemen Role (Admin, Operasional, Finance, Direksi), Hak Izin Menu |

---

### 5. Integrasi Frontend Next.js (`frontend/`)

Frontend Next.js showcase diatur pada port `:3001` dengan proxy bawaan Next.js (`next.config.ts`):

```typescript
// frontend/next.config.ts
import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactStrictMode: true,
  async rewrites() {
    const backendUrl = process.env.BACKEND_API_URL || "http://localhost:8081";
    return [
      {
        source: "/api/:path*",
        destination: `${backendUrl}/api/:path*`,
      },
    ];
  },
};

export default nextConfig;
```

Dengan konfigurasi ini:
- Frontend memanggil `/api/v1/...` secara lokal dari browser.
- Next.js server bertindak sebagai reverse proxy ke `http://localhost:8081` tanpa mengalami kendala CORS di browser.
