# PGF-THR-05 — Therapy Plan & Exercise Configuration

## Scope / Definition of Done (Task Tracker)
Therapist dapat **melihat, mengubah, dan menyimpan** rencana latihan khusus pasien, mencakup **target ROM**, **batas ROM aman**, dan parameter latihan yang tersedia.

**Owner:** Farrel · **Review:** Dinda dan Orie · **Dependency:** PGF-THR-04, PGF-ALL-03.

## Architecture
- Therapist UI: React/Vite di `therapist-web/`, route `/patients/:patientId/therapy-plan`, diakses dari Patient Detail (`/patients/:patientId`).
- Persistensi: Supabase tabel **`public.therapy_plans`**, primary key `(patient_id, exercise_id)`; bukan global `public.exercises` (catalog milik tim).
- Akses: `getAssignedPatient` di frontend dan RLS PostgreSQL. Therapist hanya mengubah pasien yang menjadi tanggung jawabnya; Patient dapat **membaca**, tidak dapat mengubah.
- Database migration: `supabase/migrations/20261124_pgf_thr05_therapy_plans.sql` (harus dijalankan terpisah oleh anggota yang berwenang). RLS **aktif**, GRANT **tersedia dan dibatasi**. Tidak menggunakan `service_role` pada frontend.
- Belum mengubah Android. **PGF-INT-02** akan mengonsumsi Therapy Plan aktif dari Supabase saat Patient akan memulai sesi berikutnya. Jangan menyatakan perubahan THR-05 langsung terpakai di aplikasi Android sebelum INT-02 selesai.

## Perubahan file
- **Ditambah:** `therapist-web/src/lib/therapyPlan.ts`, `therapyPlanApi.ts`, `therapyPlan.test.ts`.
- **Ditambah:** `therapist-web/src/pages/TherapyPlanPage.tsx`, `therapyPlan.css`.
- **Ditambah:** `supabase/migrations/20261124_pgf_thr05_therapy_plans.sql` dan dokumen ini.
- **Patch kecil** pada `therapist-web/src/App.tsx` (satu import + route), `therapist-web/src/pages/PatientSessionsPage.tsx` (tombol menuju Therapy Plan).
- Tidak mengubah Android, Supabase `schema.sql`, `seed.sql`, login, Patient List/Dashboard/Reports, atau file `.env.local`.

## Validasi
- Target ROM: **lebih dari 0° dan kurang dari 180°**.
- Ceiling ROM: **lebih tinggi dari target, maksimum 180°**.
- Target reps: integer 1–100, sets: integer 1–10, sessions/week: integer 1–7, notes maksimal 500 karakter.
- Inactive/paused plan tetap disimpan (tidak dihapus), dan perlu diabaikan oleh Patient App pada PGF-INT-02.
- Nilai catalog defaults diperlihatkan saat belum ada plan, **belum dianggap tersimpan**. Tombol Save membuat plan secara eksplisit. Tidak mengarang data latihan baru.
- Unique PK mencegah dua plan untuk kombinasi pasien/exercise yang sama.
- Status saved hanya muncul setelah Supabase mengonfirmasi hasil upsert; simpan gagal tidak dianggap berhasil.

## Deploy SQL
1. Koordinasikan penerapan perubahan schema dengan Dinda karena ini Supabase bersama tim.
2. Buka dashboard Supabase proyek yang digunakan tim → SQL Editor → paste **seluruh** isi file migrasi → Run. **Jangan jalankan `seed.sql` ulang hanya untuk THR-05.**
3. Pastikan `public.therapy_plans` terbuat dan RLS aktif. Refresh halaman web setelah migrasi.
4. Konfirmasi uji akses dengan dua akun berbeda (assigned therapist dan patient), termasuk bahwa user anonim/tidak bertugas tidak bisa mengubah plan. Untuk query pada SQL Editor, gunakan pemeriksaan metadata; SQL Editor adalah admin, jadi tidak menggantikan uji RLS dari login biasa.
5. Migration file perlu dibawa ke alur deployment Supabase tim supaya environment baru juga memiliki tabel ini.

## Verifikasi web
```powershell
cd therapist-web
npm ci
npm test -- --run src/lib/therapyPlan.test.ts
npm run build
npm run dev
```
Buka **Therapy → Patients → View Patient → Therapy Plan**.

Checklist:
1. Pasien P001: satu exercise catalog terlihat; saat belum ada plan, tampil **Unsaved defaults**, belum ada record.
2. Ubah target ROM 60, ceiling 75, target reps 8, sets 2, sessions/week 4, notes, lalu Save. Refresh: semua nilai tetap.
3. Edit target ROM menjadi 65, Save. Tidak ada row duplikat untuk pasangan `(patient_id, exercise_id)`.
4. Ubah status ke Paused lalu Save. Refresh: tetap Paused.
5. Masukkan target 80 dan ceiling 75; tidak boleh disimpan. Uji reps 0 dan 1.5, ceiling >180, notes >500 melalui unit test.
6. Buka pasien P002: plan P001 **tidak boleh muncul**. Pasien tidak ditugaskan harus menampilkan akses tidak tersedia.
7. Logout, login sebagai Patient: tidak boleh mengubah record melalui API karena RLS/GRANT.
8. Pastikan Patient Detail, Patient List, Session Detail, Reports, dan Android tidak berubah.

## Dependency / Handoff PGF-INT-02
Kontrak tabel yang sudah dapat digunakan: `patient_id` (misal P001), `exercise_id` (EX001), `target_rom_deg`, `rom_ceiling_deg`, `target_reps`, `sets_per_session`, `sessions_per_week`, `notes`, `is_active`, `updated_at`.

Sumber Patient ID Android harus memetakan `User.legacy_user_id` ke `public.patients.id` lewat Supabase atau mapping yang otoritatif. Patient mengambil **hanya plan aktif untuk dirinya**. Jika offline/no plan, aturan fallback perlu ditetapkan secara eksplisit saat INT-02, bukan dibuat diam-diam di THR-05.

## Known boundaries
- Tidak ada rekomendasi klinis otomatis: seluruh nilai diisi/dikonfirmasi Therapist.
- Belum ada kalender penjadwalan, notifikasi, atau audit history versi plan. `updated_at/updated_by` merekam revisi terakhir saja.
- SQL migration membutuhkan aksi terpisah di dashboard Supabase; `npm run build` saja tidak menciptakan tabel.
- Belum ada bukti uji browser langsung atau build npm full di lingkungan ini; jalankan di laptop sebelum merge.
