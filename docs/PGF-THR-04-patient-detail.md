# PGF-THR-04 — Patient Detail & Comfort Feedback

## Scope / Definition of Done (Task Tracker)
Therapist dapat melihat profil, ringkasan latihan, riwayat aktivitas, dan feedback kenyamanan pasien.

## Implementasi
- Platform: `therapist-web/` (React + Vite), mengikuti keputusan tim memindahkan Therapist Portal ke web.
- URL tetap: `/patients/:patientId` → `PatientSessionsPage`. Tidak mengubah `App.tsx` atau route.
- Ownership: `getAssignedPatient(therapist.id, patientId)` harus mengembalikan assigned patient sebelum memanggil `listSessions`.
- Profile: nama, ID, usia, lengan, tujuan rehabilitasi dan catatan (bila ada).
- Summary: jumlah sesi, total repetisi, ROM tertinggi yang benar-benar tercatat, dan average smoothness hanya dari sesi dengan skor tersedia; `null` ditampilkan `—`, bukan 0%.
- Comfort: outcome terbaru yang memang disimpan pasien (`NO_PAIN`, `MILD_PAIN`, `STOPPED_DUE_TO_PAIN`) + ringkasan count. Bukan diagnosis medis.
- Activity: semua sesi terurut terbaru dahulu, waktu, exercise, repetisi, peak ROM, durasi, smoothness, feedback, indikator seeded demo; link ke Session Detail yang sudah ada.
- Loading, unknown/unassigned patient, error+retry, auto sync polling, dark/light support.
- Unit test pure summaries: empty, ownership-scope, sort, pain statuses, valid smoothness, no fabricated peak, seeded records.

## Pembatasan scope / No Collision
- Mengganti hanya halaman web `therapist-web/src/pages/PatientSessionsPage.tsx` yang sebelumnya masih tabel sesi dengan profil singkat.
- Menambah `lib/patientDetail.ts`, `lib/patientDetail.test.ts` dan `pages/patientDetail.css`.
- Tidak mengubah Supabase schema, `api.ts`, hooks `useCohort`, Patient List, Dashboard, Reports, Android navigation, sensor, ataupun perhitungan KPI milik Erza.
- PGF-THR-05 (edit/save Therapy Plan) dikerjakan pada paket berikutnya; bukan bagian patch ini.
- Data aktual dari Supabase, bukan mock hardcode; bila demo seeded, diberi label.

## QA checklist (manual)
1. `cd therapist-web && npm ci && npm test && npm run build`.
2. Configure `.env.local` dari `.env.example` sesuai project milik tim (jangan commit secret).
3. Login Therapist; buka Therapy → Patients → View Patient.
4. Verifikasi profil, 4 KPI, last pain feedback, semua sesi, dan link Session Detail.
5. Login patient lain / buka pasien tidak assigned dengan URL langsung → `Patient not available`; tidak ada data pasien lain.
6. Pasien dengan 0 sesi menampilkan empty, rata-rata smoothness `—`.
7. Skenario NO_PAIN, MILD_PAIN, STOPPED_DUE_TO_PAIN terlihat sesuai record.
8. Seeded ditandai "Demo history", bukan seolah hasil sensor HP nyata.
9. Refresh gagal menampilkan warning dan data terakhir tidak hilang; jika initial request gagal ada Retry.
10. Test desktop/mobile, light/dark, auto-sync on/off, back ke list, dan deep link Session Detail.

**Catatan:** `npm test`/build dan uji Supabase live perlu dilakukan di perangkat developer. Patch installer hanya memverifikasi base file repo & working tree.
