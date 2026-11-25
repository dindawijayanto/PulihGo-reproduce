# PGF-INT-05 — Final Integration & Safety QA

**Project:** PulihGo — Android Patient & Therapist Web Portal  
**Task:** PGF-INT-05 — Final Integration & Safety QA  
**Target branch:** `feat/farrel-pgf-int-05-final-integration-qa`  
**Base commit:** `9ffda1d3d19827a7efe9b1dbb95268d3066e82b9`  
**Related PR:** #23 (QS-03), #24 (THR-04), #25 (THR-05), #26 (INT-02)  
**Testing date:** 25 November 2026 (WIB)  
**Device:** Realme RMX3085 — Android 13  
**Test environment:** Windows 11, Android device, Therapist Web (Vite), Supabase PostgreSQL

## A. Scope and Objective

PGF-INT-05 bertujuan memverifikasi integrasi akhir antara aplikasi Android Patient, Therapist Web Portal, dan Supabase. Pengujian difokuskan pada konsistensi Therapy Plan, keselamatan latihan, pencatatan hasil sensor, Pain Check, penyimpanan lokal, serta sinkronisasi sesi ke cloud.

Cakupan pengujian menghubungkan fitur dari PGF-THR-05, PGF-INT-02, PGF-QS-01/02/03, sensor dan kalibrasi, local persistence, serta Supabase session synchronization.

Implementasi PR ini hanya menambahkan automated integration tests, read-only SQL, script verifikasi, dan dokumentasi QA. Tidak terdapat perubahan pada production logic, algoritma sensor, database schema, RLS policy, maupun kredensial demo.

## B. Automated Verification

Pengujian otomatis dijalankan melalui PowerShell dari root repository menggunakan perintah:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\qa\run-pgf-int-05.ps1
```

### B.1. Results

| Component | Verification | Result |
|---|---|---|
| Installer preflight | Verifikasi branch dan file | PASS |
| Android | JVM unit tests | PASS |
| Android | Debug APK build | PASS |
| Therapist Web | Therapy Plan validation tests | PASS — 9 tests |
| Therapist Web | Therapy Plan API tests | PASS — 6 tests |
| Therapist Web | Production build | PASS |
| Git | `git diff --check` | PASS |

Output akhir dari script:

```text
[PGF-INT-05] ALL SELECTED AUTOMATED CHECKS PASSED.
```

Android Gradle menyelesaikan pengujian dan build dengan `BUILD SUCCESSFUL`. Pengujian Therapist Web menyelesaikan 15 dari 15 scoped tests, sedangkan Vite production build juga berhasil.

### B.2. Integration Regression Coverage

Test class `PGFINT05FlowTest` menambahkan 11 skenario pengujian lintas modul yang mencakup:

1. Integrasi Therapy Plan dari data berbentuk respons Supabase ke konfigurasi latihan Android.
2. Penggunaan target ROM dan ceiling dalam Live Exercise Tracker.
3. Peringatan keselamatan berdasarkan batas ROM 65°/75°.
4. Penyimpanan status Paused dalam offline cache.
5. Validasi dan penolakan data Therapy Plan yang tidak valid atau berasal dari pasien lain.
6. Penanganan Therapy Plan kosong tanpa mencampurkan resep pasien lain.
7. Penghentian latihan akibat nyeri melalui Pain Check.
8. Pencegahan perubahan status emergency stop menjadi normal finish.
9. Konsistensi identitas sesi ketika penyimpanan dicoba ulang.
10. Pemisahan target repetitions dan actual repetitions.
11. Kompatibilitas data sesi lama yang tidak memiliki snapshot target.

Pengujian JVM memverifikasi perilaku logika aplikasi, tetapi tidak menggantikan pengujian sensor fisik, UI perangkat, autentikasi aktual, RLS, maupun keberhasilan upload ke Supabase. Karena itu, pengujian langsung menggunakan Android dan Therapist Web dilakukan sebagai verifikasi tambahan.

**Testing limitation:** Script hanya menjalankan scoped Vitest terkait Therapy Plan. Full `npm test` tidak termasuk dalam hasil ini karena sebelumnya terdapat kegagalan pada pengujian floating-point `reports.test.ts` (`0.86` dibandingkan dengan `0.8600000000000001`). Hal tersebut tidak dianggap sebagai bukti bahwa seluruh test lain telah lulus.

## C. Manual QA Matrix

**Tester:** Farrel Brilliant  
**Date:** 25 November 2026 (WIB)  
**Device:** Realme RMX3085 — Android 13  
**Base commit:** `9ffda1d`  
**Exercise:** Forearm Supination and Pronation (`EX001`)  
**Test patient:** P001 — Alya Putri  
**Isolation patient:** P002 — Bima Pratama

Status pengujian dibagi menjadi:

- **PASS:** Perilaku utama berhasil diverifikasi melalui perangkat, Supabase, atau bukti pengujian langsung.
- **PARTIAL:** Sebagian perilaku sudah dibuktikan, tetapi terdapat kondisi tambahan yang belum diuji.
- **NOT TESTED:** Belum tersedia bukti pengujian manual untuk skenario tersebut.

| ID | Area | Status | Test Result / Evidence |
|---|---|---|---|
| E01 | Therapist plan creation/update | PASS | Therapy Plan P001/EX001 berhasil dibuat, diubah, dan disimpan melalui Therapist Web dengan target 65°, ceiling 75°, 8 reps, 2 sets, dan 4 sessions/week. |
| E02 | Cross-app synchronization | PASS | Android menampilkan konfigurasi yang sama setelah Refresh Therapy Plan dan status `Therapist plan checked with Supabase`. |
| E03 | Paused state | PASS | Ketika Therapist mengubah status menjadi Paused, Android menampilkan pemberitahuan Paused dan menonaktifkan Calibrate & Start. |
| E04 | Active state | PASS | Setelah status dikembalikan menjadi Active dan plan di-refresh, tombol Calibrate & Start kembali aktif. |
| E05 | Offline Therapy Plan cache | PASS | Android menampilkan `Offline: using last saved therapist plan` dan mempertahankan target 65°/75°/8 reps ketika offline. |
| E06 | Offline Paused persistence | NOT TESTED | Belum dilakukan verifikasi perangkat terhadap plan Paused yang disimpan dalam cache lalu dibuka kembali saat offline. |
| E07 | First-use offline | NOT TESTED | Belum diuji pada instalasi baru tanpa verified plan cache. |
| E08 | Patient isolation | PASS | Akun Bima/P002 menampilkan default katalog 70°/80°/10 reps, tidak menggunakan Therapy Plan Alya/P001. |
| E09 | Updated plan for next session | PARTIAL | Perubahan target Therapist menjadi 65° diterapkan pada sesi berikutnya. Verifikasi khusus terhadap perubahan target saat sesi sedang berlangsung belum dilakukan. |
| S01 | Sensor calibration | PARTIAL | Sesi Live Exercise berhasil dijalankan dan menghasilkan pengukuran ROM. Akurasi titik netral 0° belum diukur secara terpisah. |
| S02 | Repetition recording | PASS | Android menghasilkan jumlah repetisi aktual yang berbeda dari target, termasuk 7 actual reps terhadap 8 prescribed reps. |
| S03 | ROM Safety | PASS | Android menampilkan peringatan ketika ROM mencapai 80° dengan ceiling 75°. Peak Angle tercatat 83°. Peringatan limit terverifikasi, tetapi pengujian ketepatan setiap ambang secara fisik tidak dilakukan. |
| S04 | Smoothness validation | NOT TESTED | Belum tersedia pemeriksaan manual khusus atas nilai smoothness pada hasil sesi terbaru. |
| S05 | Normal Finish & Pain Check | PASS | Sesi NO_PAIN dan MILD_PAIN berhasil disimpan serta terverifikasi melalui kolom `pain_level` di Supabase. |
| S06 | Emergency Stop | PASS | Tombol `Stop, it hurts` berhasil mengakhiri sesi, menampilkan konfirmasi penyimpanan, dan menghasilkan `STOPPED_DUE_TO_PAIN` pada database. Pencegahan double-submit didukung automated tests, belum diuji manual. |
| D01 | Offline local persistence | PASS | Sesi yang diselesaikan saat offline tetap tersedia untuk diunggah ketika koneksi kembali dan sync dipicu. |
| D02 | Cloud retry after reconnection | PARTIAL | Sesi offline berhasil terunggah setelah sesi baru diselesaikan saat online. Auto-retry saat konektivitas pulih belum terbukti; pemeriksaan deduplikasi seluruh ID belum dilakukan. |
| D03 | Session target snapshot | PASS | Supabase mencatat `target_rom_deg=65`, `target_reps=8`, sedangkan actual repetitions mengikuti hasil sensor. |
| D04 | Legacy record compatibility | PASS | Sesi lama tanggal 13 November 2026 tetap tersedia dengan `target_rom_deg` dan `target_reps` bernilai NULL. |
| D05 | Therapist Web visibility | NOT TESTED | Data sesi sudah ada di Supabase, tetapi pemeriksaan langsung tampilan sesi terbaru pada Therapist Web belum didokumentasikan. |
| A01 | RLS access control | NOT TESTED | RLS pada Therapy Plan sudah diaktifkan dan policy tersedia, tetapi pengujian penolakan write dengan token Patient atau Therapist yang tidak ditugaskan belum dilakukan. |

### C.1. Therapy Plan Synchronization

Pengujian dilakukan dengan mengubah konfigurasi Therapy Plan milik Alya melalui Therapist Web. Parameter yang digunakan pada pengujian akhir adalah:

| Parameter | Value |
|---|---|
| Patient ID | P001 |
| Exercise ID | EX001 |
| Target ROM | 65° |
| Safe ROM Ceiling | 75° |
| Target Repetitions | 8 |
| Sets per Session | 2 |
| Sessions per Week | 4 |
| Prescription Status | Active |

Setelah perubahan disimpan melalui Therapist Web, Android berhasil mengambil konfigurasi terbaru dari Supabase melalui Refresh Therapy Plan.

Pengujian perubahan status juga berhasil. Ketika plan berstatus Paused, Android menonaktifkan tombol Calibrate & Start. Setelah status diubah kembali menjadi Active dan plan di-refresh, tombol tersebut kembali dapat digunakan.

Hal ini menunjukkan bahwa konfigurasi latihan yang dibuat Therapist sudah diterapkan pada alur persiapan latihan Android.

### C.2. Offline Cache and Patient Isolation

Setelah sinkronisasi berhasil dilakukan saat online, koneksi internet Android dimatikan.

Aplikasi tetap menampilkan parameter 65° target ROM, 75° ceiling, 8 repetitions, 2 sets, dan 4 sessions/week. Status yang muncul adalah:

```text
Offline: using last saved therapist plan
```

Pengujian tersebut membuktikan bahwa aplikasi dapat menggunakan Therapy Plan terakhir yang telah diverifikasi ketika tidak tersedia koneksi internet.

Pengujian isolasi pasien dilakukan dengan logout dari akun Alya dan login menggunakan akun Bima.

Akun Bima menampilkan konfigurasi katalog 70°/80°/10 repetitions, bukan konfigurasi khusus Alya. Hal ini menunjukkan bahwa data plan Alya tidak terbawa ke akun Bima dalam skenario pengujian.

Perlu dicatat bahwa default katalog pada Bima merupakan perilaku aplikasi ketika Supabase sudah diperiksa dan tidak ditemukan Therapy Plan khusus untuk latihan tersebut. Perilaku tersebut tidak berarti resep Alya digunakan oleh Bima.

### C.3. ROM Safety Verification

ROM Safety diuji menggunakan konfigurasi target 65° dan ceiling 75°.

Pada layar Live Exercise, aplikasi menampilkan pengukuran ROM sebesar 80° dengan Peak Angle sebesar 83°. Ketika pengukuran melewati ceiling 75°, aplikasi menampilkan pesan:

```text
WARNING: SLOW DOWN & RETURN
Exceeded ROM limit of 75°. Return slowly.
```

Peringatan tersebut membuktikan bahwa batas ROM dari Therapy Plan sudah digunakan dalam Live Exercise, bukan batas default katalog 80°.

Pengujian ini membuktikan munculnya visual warning pada perangkat ketika nilai ROM melampaui ceiling. Namun, hasil tersebut bukan bukti validasi akurasi sensor secara klinis maupun kemampuan aplikasi membatasi gerakan fisik pengguna.

Pengujian tambahan untuk ketepatan ambang batas dilakukan melalui automated tests. Tidak diperlukan gerakan fisik berlebihan hanya untuk memicu peringatan.

### C.4. Pain Check and Emergency Stop

Pengujian Pain Check mencakup tiga kemungkinan hasil:

- `NO_PAIN`
- `MILD_PAIN`
- `STOPPED_DUE_TO_PAIN`

Ketiga jenis hasil tersebut telah terverifikasi pada tabel `exercise_sessions` di Supabase.

Pada pengujian Emergency Stop, tombol `Stop, it hurts` digunakan sebagai simulasi penghentian darurat tanpa sengaja menimbulkan nyeri. Setelah sesi berakhir, Android kembali ke halaman Preparation dan menampilkan:

```text
Session saved and flagged for joint discomfort.
```

Database Supabase mencatat sesi terbaru dengan hasil berikut:

| Parameter | Result |
|---|---|
| Patient ID | P001 |
| Exercise ID | EX001 |
| Pain Level | STOPPED_DUE_TO_PAIN |
| Actual Repetitions | 1 |
| Target ROM | 65° |
| Target Repetitions | 8 |
| Source | DEVICE |
| Started At | 25 November 2026, 19:43:29 WIB |

Data ini menunjukkan bahwa status penghentian akibat nyeri berhasil dibedakan dari sesi normal dan dikirimkan ke Supabase tanpa menggantinya menjadi NO_PAIN atau MILD_PAIN.

### C.5. Local Persistence and Cloud Synchronization

Pengujian dilakukan dengan menyelesaikan beberapa sesi saat Android berada dalam kondisi offline, termasuk sesi dengan respons No Pain dan Mild Pain.

Setelah internet kembali aktif, sesi-sesi tersebut belum langsung muncul pada Supabase. Namun, ketika satu sesi baru diselesaikan saat online, proses sinkronisasi berjalan dan sesi-sesi sebelumnya ikut terunggah.

Hasil ini menunjukkan bahwa mekanisme local-first persistence dan pending session upload berhasil bekerja.

**Temuan:** Saat ini tidak ada bukti bahwa aplikasi secara otomatis melakukan retry tepat setelah koneksi internet kembali aktif tanpa pemicu tambahan. Sinkronisasi yang terverifikasi terjadi ketika aplikasi menjalankan proses upload kembali setelah menyelesaikan sesi.

Dengan demikian, skenario pengiriman sesi tertunda berhasil, tetapi pengujian auto-retry berbasis perubahan konektivitas belum lulus.

### C.6. Session Data Verification in Supabase

Query audit pada tabel `public.exercise_sessions` menampilkan data terbaru berikut:

| Started At (WIB) | Pain Level | Target Reps | Actual Reps | Target ROM |
|---|---|---:|---:|---:|
| 19:43:29 | STOPPED_DUE_TO_PAIN | 8 | 1 | 65° |
| 19:41:30 | NO_PAIN | 8 | 8 | 65° |
| 19:39:16 | MILD_PAIN | 8 | 12 | 65° |
| 19:38:18 | MILD_PAIN | 8 | 1 | 65° |
| 19:37:47 | NO_PAIN | 8 | 3 | 65° |

Seluruh baris tersebut berasal dari perangkat Android (`source = DEVICE`) dengan patient ID P001 dan exercise ID EX001.

Dari data ini terlihat bahwa target repetitions dan actual repetitions tetap dipisahkan. Contohnya, sesi dengan target 8 repetitions dapat menghasilkan 1, 3, 8, atau 12 actual repetitions berdasarkan hasil pengukuran yang dicatat aplikasi.

Data historis dari 13 November 2026 masih memiliki target ROM dan target repetitions bernilai NULL karena dibuat sebelum implementasi snapshot target dari PGF-INT-02.

Tidak dilakukan perubahan atau backfill terhadap data historis tersebut.

## D. Evidence and Documentation

Bukti yang tersedia dari proses pengujian tanggal 25 November 2026 meliputi:

| Evidence ID | Description | Status |
|---|---|---|
| EV-01 | Therapist Web menampilkan Therapy Plan P001 yang tersimpan | Available |
| EV-02 | Android menampilkan target terbaru dari Supabase | Available |
| EV-03 | Android menonaktifkan Calibrate & Start pada status Paused | Available |
| EV-04 | Android mengaktifkan Calibrate & Start pada status Active | Available |
| EV-05 | Android menggunakan cached Therapy Plan saat offline | Available |
| EV-06 | Akun Bima menampilkan konfigurasi berbeda dari Alya | Available |
| EV-07 | SQL membuktikan target ROM 65°, target 8 reps, dan actual 7 reps | Available |
| EV-08 | SQL menunjukkan sesi offline yang akhirnya tersinkron | Available |
| EV-09 | Android menampilkan ROM Safety warning untuk ceiling 75° | Available |
| EV-10 | Android menampilkan konfirmasi sesi yang dihentikan karena nyeri | Available |
| EV-11 | SQL menunjukkan NO_PAIN, MILD_PAIN, dan STOPPED_DUE_TO_PAIN | Available |
| EV-12 | Terminal menunjukkan Android build, tests, dan Therapist Web checks berhasil | Available |

Status **Available** berarti bukti telah ditunjukkan dalam proses pengujian dan percakapan pengerjaan. Bukti tersebut belum otomatis berarti seluruh file screenshot sudah dilampirkan ke GitHub PR.

Screenshot pendukung dapat dilampirkan pada PR atau laporan terpisah dengan menghapus informasi pribadi, token, dan data pasien yang tidak relevan.

File sensitif seperti `local.properties`, `.env.local`, access token, refresh token, service-role key, dan Authorization header tidak boleh dimasukkan ke repository.

## E. Read-Only Supabase Audit

Pengujian database dilakukan melalui SQL Editor Supabase menggunakan query SELECT tanpa mengubah data.

File audit tersedia di:

```text
scripts/qa/pgf-int-05-read-only.sql
```

Contoh query untuk memeriksa hasil sesi:

```sql
select
    patient_id,
    exercise_id,
    pain_level,
    repetitions,
    target_rom_deg,
    target_reps,
    source,
    started_at at time zone 'Asia/Jakarta' as started_wib
from public.exercise_sessions
where patient_id = 'P001'
order by started_at desc
limit 10;
```

Query digunakan untuk memverifikasi status Pain Check, jumlah repetisi aktual, target yang digunakan saat sesi, dan keberhasilan sinkronisasi cloud.

Pengujian ini tidak melakukan INSERT, UPDATE, DELETE, perubahan schema, atau perubahan RLS policy.

## F. Risk Register and Known Limitations

| ID | Risk / Limitation | Finding | Recommendation |
|---|---|---|---|
| R1 | Stale Active plan while offline | Cached Active plan masih dapat digunakan meskipun Therapist mungkin telah mengubah plan di server saat perangkat offline. | Tentukan batas usia cache dan kebijakan online re-verification sebelum penggunaan klinis. |
| R2 | Historical ROM ceiling not stored | Session record menyimpan target ROM dan target reps, tetapi belum menyimpan snapshot ceiling historis. | Evaluasi penambahan kolom immutable ROM ceiling pada `exercise_sessions`. |
| R3 | Local login and cloud authentication | Login lokal yang berhasil belum tentu menghasilkan sesi Supabase valid. | Pertahankan verifikasi akses cloud dan perbaiki pemulihan autentikasi. |
| R4 | Unencrypted SharedPreferences | Cache Therapy Plan disimpan secara lokal tanpa enkripsi; clinical notes tidak ikut disimpan. | Evaluasi encrypted storage dan cache expiration untuk implementasi produksi. |
| R5 | Sets/week not enforced | Sets dan sessions/week hanya ditampilkan sebagai panduan, bukan penghitung atau scheduler. | Tambahkan adherence tracking pada pengembangan berikutnya jika dibutuhkan. |
| R6 | ROM warning limitation | Peringatan ROM berbasis pembacaan sensor, bukan sistem pembatas gerakan fisik. | Lakukan validasi sensor dan evaluasi klinis sebelum penggunaan nyata. |
| R7 | No automatic reconnect-triggered sync | Pending sessions baru terbukti terunggah ketika sync dipanggil kembali, misalnya setelah menyelesaikan sesi online. | Pertimbangkan WorkManager dengan network constraints atau mekanisme retry saat koneksi pulih. |
| R8 | Incomplete access-control QA | RLS policy tersedia, tetapi penolakan akses lintas peran belum diuji langsung dengan token pengguna yang berbeda. | Lakukan pengujian autentikasi dan authorization khusus bersama backend owner. |

**Safety note:** PulihGo pada tahap ini merupakan prototipe akademik. Keberhasilan pengujian fungsional tidak dapat dianggap sebagai validasi keselamatan klinis atau persetujuan untuk digunakan sebagai perangkat rehabilitasi medis.

## G. Final QA Summary

Berdasarkan automated tests dan pengujian langsung menggunakan perangkat Android, integrasi utama PulihGo telah berhasil diverifikasi.

Hasil pengujian menunjukkan bahwa:

1. Therapy Plan dari Therapist Web dapat disimpan ke Supabase dan digunakan oleh Android Patient.
2. Perubahan Active dan Paused diterapkan pada akses memulai latihan setelah refresh.
3. Cached Therapy Plan dapat digunakan ketika perangkat offline.
4. Data Therapy Plan pasien yang berbeda tidak tercampur pada skenario Alya dan Bima.
5. ROM Safety memberikan peringatan berdasarkan ceiling yang dikonfigurasi Therapist.
6. Pain Check menyimpan hasil NO_PAIN, MILD_PAIN, dan STOPPED_DUE_TO_PAIN secara terpisah.
7. Hasil sesi menyimpan target latihan dan jumlah repetisi aktual sebagai nilai yang berbeda.
8. Sesi yang disimpan saat offline berhasil tersinkron ke Supabase setelah proses sync dipicu kembali.
9. Data sesi historis tetap tersedia tanpa memaksakan nilai target baru.
10. Automated integration tests, Android build, scoped Therapist Web tests, dan web production build selesai tanpa kegagalan.

**Overall result: PASS for verified core integration flows, with documented limitations and outstanding QA items.**

Pengujian yang masih berstatus PARTIAL atau NOT TESTED tidak diklaim telah selesai. Terutama, perlu perhatian lanjutan terhadap offline Paused cache, first-use offline, auto-retry sinkronisasi, akurasi kalibrasi, smoothness verification, Therapist Web session visibility, dan RLS access-control testing.

Untuk kebutuhan demo mata kuliah, hasil ini memberikan bukti fungsional bahwa alur utama Patient Android, Therapist Web, dan Supabase telah terintegrasi. Namun, kesiapan produksi dan keselamatan klinis tetap berada di luar ruang lingkup pengujian ini.

## H. PR Review and Handoff

Review yang disarankan sebelum merge:

- **Therapist / Backend Owner:** Verifikasi akses RLS, tampilan sesi Therapist, dan risiko stale plan.
- **Android / Cloud Owner:** Tinjau perilaku pending sync, authentication recovery, dan mekanisme retry.
- **Sensor Owner:** Tinjau kalibrasi, akurasi sensor, repetition detection, dan ROM Safety warning.
- **Demo Reviewer:** Periksa kelengkapan evidence, konsistensi Therapy Plan, Pain Check, dan offline-to-online flow.

PR PGF-INT-05 berfokus pada verifikasi dan dokumentasi kualitas integrasi, bukan perubahan production code. Keterbatasan yang ditemukan perlu dipertahankan sebagai known issues atau tindak lanjut agar hasil QA tetap dapat dipertanggungjawabkan.
