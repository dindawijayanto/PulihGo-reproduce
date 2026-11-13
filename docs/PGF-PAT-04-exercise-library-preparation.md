# PGF-PAT-04 — Patient UI: Exercise Library & Preparation

**Sprint:** 1

**Owner:** Farrel

**Reviewer:** Dinda
**Ruang lingkup:** UI daftar latihan dan persiapan latihan. Tidak mengubah sensor, kalibrasi, repetisi, ataupun home final milik anggota lain.

## Acceptance criteria

> Pengguna dapat memilih Forearm Supination/Pronation dan melihat instruksi penggunaan perangkat, posisi latihan, serta langkah persiapan kalibrasi.

| Kriteria | Implementasi |
| --- | --- |
| Pilih Forearm Supination/Pronation | `PatientExerciseLibraryScreen`, memuat `mock_data/exercises.json` dan tombol `Lihat Persiapan Latihan` |
| Instruksi penggunaan perangkat | `PatientExercisePreparationScreen`, langkah memasang smartphone stabil pada lengan bawah |
| Posisi latihan | Petunjuk siku dekat badan, sekitar 90°, dan posisi lengan bawah netral |
| Persiapan kalibrasi | Langkah menahan ponsel diam dan CTA `Cek Sensor & Kalibrasi` |
| Integrasi | Home placeholder → Exercise Library → Preparation → `SENSOR_CHECK` yang sudah dimiliki tim sensor |
| Batasan akses | Route `PATIENT_EXERCISES` dan `PATIENT_EXERCISE_PREPARATION` hanya untuk role PATIENT |
| Data / error | Data latihan berasal dari JSON existing; saat gagal dimuat ditampilkan empty state |

## Catatan teknis

- Konfigurasi latihan **tidak di-hardcode** di UI. JSON repo yang ada sekarang berisi target 70°, 10 repetisi, dan ceiling 80°. Tidak mengubah data demo tersebut.
- `ExerciseCatalog` hanya membaca file existing `app/src/main/assets/mock_data/exercises.json`; `ExerciseParser` adalah parser murni untuk unit test.
- Persiapan tidak langsung memulai pembacaan sensor. Tombol membuka `SensorCheckScreen`, dan pengguna tetap harus menekan **Kalibrasi** setelah data sensor tersedia. Hal ini sesuai kontrak PGF-SEN-01/02.
- Gerakan yang tersedia saat ini adalah latihan rotasi lengan bawah. Tidak membuat layar latihan lain atau memalsukan dukungan sensor.
- Perubahan kecil pada `PatientHomePlaceholder` diperlukan hanya untuk akses ke daftar latihan, tanpa mengganti desain Home final milik Orie.
- Untuk keamanan, informasi petunjuk bukan penentuan batas medis universal; target/ceiling diambil dari konfigurasi demo.

## Uji otomatis

Unit test parser berada di `app/src/test/java/com/pulihgo/app/data/exercise/ExerciseCatalogTest.kt`.

Pada Windows PowerShell di root proyek:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

## Uji manual di Android

1. Login `patient01` / `demo123` (akun demo yang tersedia pada `mock_data/users.json`).
2. Di Home, pilih `Pilih Latihan`. Pastikan **Forearm Supination / Pronation** muncul bersama target ROM dan repetisi dari JSON.
3. Ketuk `Lihat Persiapan Latihan`. Pastikan tiga langkah persiapan, petunjuk gerakan, dan safety notes terlihat. Pastikan layar tetap bisa di-scroll pada perangkat kecil.
4. Ketuk `Cek Sensor & Kalibrasi`. Pastikan membuka layar sensor **existing** dan tidak melakukan kalibrasi otomatis.
5. Gunakan tombol kembali: Sensor Check → Preparation → Exercise Library → Home.
6. Coba login role Therapist. Route Patient tidak boleh dapat diakses lewat `RoleRouter`.
7. (Opsional) Coba data `exercises` kosong pada fixture development untuk melihat empty state.

## Batas scope pekerjaan berikutnya

PGF-QS-01 (Movement Quality & Safety / smoothness) **belum** dikerjakan pada paket ini; harus berada pada branch/PR terpisah setelah PGF-PAT-04 direview.
