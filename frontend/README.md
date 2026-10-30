# 🌐 PT Adijayantara Logistics Indonesia — Web Frontend (Spring Boot Showcase Edition)

[![Next.js](https://img.shields.io/badge/Next.js-16.3-black?style=for-the-badge&logo=next.js&logoColor=white)](https://nextjs.org/)
[![React](https://img.shields.io/badge/React-19.2-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-v4-38B2AC?style=for-the-badge&logo=tailwind-css&logoColor=white)](https://tailwindcss.com/)
[![Target Backend](https://img.shields.io/badge/Backend-Spring%20Boot%20(Port%208081)-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://github.com/odealidj/adijayantara-logistics-backend-spring)

Aplikasi Web Frontend mandiri (*standalone*) untuk **PT Adijayantara Logistics Indonesia**, yang secara khusus diintegrasikan dan dipasangkan dengan backend **Java 21 LTS + Spring Boot 3.3.5**.

Repositori ini sepenuhnya terisolasi dari repositori produksi utama Golang guna menjamin keamanan operasional dan independensi deployment (*zero blast radius*).

---

## 🏗️ Arsitektur & Konektivitas Sistem

```
[Browser / PWA User]
        │
        ▼
[Next.js Web Frontend] (Port :3001)
        │
        ▼ (Proxy Rewrite /api/v1 -> localhost:8081)
[Java Spring Boot Backend] (Port :8081)
        │
        ├─► [PostgreSQL Database] (Port :5432 - cashflow_db)
        └─► [Redis / Valkey Cache] (Port :6379 - session storage)
```

- **Backend Repository**: [odealidj/adijayantara-logistics-backend-spring](https://github.com/odealidj/adijayantara-logistics-backend-spring)
- **Port Isolasi Default**: `:3001` (Bebas tabrakan dengan FE produksi di port `:3000`).

---

## 🚀 Fitur Unggulan

- 📊 **Executive Analytics & Strategy Dashboard**:
  - Cashflow Runway 60 hari & CCC Gap.
  - Route Matrix BCG 4-Kuadran (Stars, Cash Cows, Dogs, Question Marks).
  - DSO & Pareto 80/20 Pelanggan dengan Churn Alert.
  - Konsentrasi Transporter Vendor Mitra.
- 📋 **Cashflow & Shipment Management**:
  - Pencatatan pengiriman muatan armada & saldo kas berjalan real-time.
  - Top-up modal kas operasional.
  - Ekspor resmi kop surat PT Adijayantara & Impor batch spreadsheet Excel.
- 🧾 **Invoicing & Accounts Receivable**:
  - Monitoring penagihan faktur, syarat pembayaran (TOP), dan pelunasan kas masuk.
  - Reschedule jadwal jatuh tempo & pelacakan histori.
- 🛡️ **Forensic Audit & SLA Monitoring**:
  - Staff scorecard performa input data.
  - Deteksi anomali transaksi akhir pekan & input tertunda (*lag*).
  - Audit trail komparasi perubahan per field (*diff viewer*).
- 👥 **Role & User Management (PBAC)**:
  - Matriks hak akses per modul dinamis.
  - Proteksi anti self-deletion dan imunitas C-Level.
- 🔔 **Pusat Notifikasi Cerdas**:
  - Alert H-3, Hari H, dan Overdue faktur.
  - Deteksi dini kas kritis dan shipment margin minus.
- 📱 **Mobile PWA Responsive**:
  - Tampilan navigasi mobile responsif dan Quick Action.

---

## 🛠️ Persyaratan Lingkungan (*Prerequisites*)

- **Node.js**: v20.x atau v22.x LTS
- **npm** / **yarn** / **pnpm**
- **Backend Service**: Java Spring Boot aktif di `http://localhost:8081`

---

## 🏃 Menjalankan Aplikasi Secara Lokal

### 1. Salin Konfigurasi Environment
```bash
cp .env.example .env.local
```

Isi berkas `.env.local`:
```env
BACKEND_API_URL=http://localhost:8081
PORT=3001
```

### 2. Install Dependensi
```bash
npm install
```

### 3. Jalankan Server Development
```bash
npm run dev
```
Aplikasi akan aktif di **http://localhost:3001**.

### 4. Build untuk Lingkungan Produksi / Showcase
```bash
npm run build
npm start
```

---

## 📜 Lisensi
Hak Cipta © 2026 PT. Adijayantara Logistics Indonesia. Seluruh hak cipta dilindungi undang-undang.
