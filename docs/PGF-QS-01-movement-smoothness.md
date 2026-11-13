# PGF-QS-01 — Movement Quality & Safety: Smoothness Score (Sprint 1)

**Owner:** Farrel

**Reviewer:** Erza

Acceptance criterion: "Sistem menghasilkan skor smoothness 0–1 berdasarkan data pergerakan untuk setiap repetisi dan menyediakan hasilnya bagi ringkasan sesi."

## Kontrak data

Input: `Repetition` hasil `RepetitionDetector.processAngle`, berisi `repetitionNumber`, `durationMs`, dan `samples: List<AngleSample(timestampMs, angleDeg)>`. Sampel sudah dikalibrasi dari `CalibratedAngleController` dan memakai sumbu `ROLL`. Status kalibrasi ditangani oleh alur sensor yang ada.

Output: `SmoothnessScore` dengan nilai `value` dalam rentang **0..1**, `pathEfficiency`, `speedConsistency`, dan nomor repetisi. Nilai `null` berarti sampel tidak cukup/tidak valid; **jangan samakan dengan skor 0**. `SessionSmoothnessTracker.snapshot()` menghasilkan `SessionSmoothnessSummary(byRepetition, averageScore)` untuk konsumen Session Summary dan laporan.

## Rumus heuristik, bukan skor medis

Dengan `a[i] = abs(angleDeg[i])` (agar supinasi/pronasi sama-sama berlaku),

- `expectedTravel = (max(a) - a[first]) + (max(a) - a[last])`.
- `actualTravel = sum(abs(a[i] - a[i-1]))`.
- `pathEfficiency = clamp(expectedTravel / actualTravel, 0, 1)`.
- `velocity[i] = abs(a[i]-a[i-1]) / (timestamp[i]-timestamp[i-1])`.
- `meanSpeed = actualTravel / totalDuration`.
- `deviation = sum(abs(velocity[i] - meanSpeed) * dt[i])`.
- `speedConsistency = clamp(1 - deviation / (2 * actualTravel), 0, 1)`.
- **`smoothness = pathEfficiency * speedConsistency`**.

Pola terkontrol satu kali naik-turun dengan kecepatan stabil mendekati 1. Gerak bolak-balik/noisy atau kecepatan tidak konsisten menghasilkan skor lebih rendah. Ini indikator **heuristik pola gerakan** dan bukan diagnosis, rekomendasi medis, atau sertifikasi keselamatan.

Kebijakan invalid: jumlah sampel kurang dari 5, NaN/Infinity, timestamp tidak meningkat, atau gerakan sangat kecil (<1° expected travel) → `null`. Timestamp boleh tidak seragam, tetapi harus strictly increasing.

## Pemakaian

```kotlin
val tracker = SessionSmoothnessTracker()

// Di callback RepetitionDetector ketika satu repetisi selesai:
val rep: Repetition = /* event repetisi */
val result: SmoothnessScore? = tracker.record(rep)

// Ketika sesi berakhir, serahkan snapshot immutable ini ke Session Summary:
val summary: SessionSmoothnessSummary = tracker.snapshot()
val average: Double? = summary.averageScore
val perRep: List<SmoothnessScore> = summary.byRepetition

// Ketika kalibrasi di-reset / memulai sesi baru:
tracker.reset()
```

Untuk Sprint 1, integrasi ke layar sensor existing menampilkan skor terakhir dan rata-rata sesi pada kartu repetisi. Penyimpanan sesi dan navigasi halaman Session Summary belum tersedia di `main` pada baseline ini; modul berikutnya dapat menerima `tracker.snapshot()` tanpa merancang ulang rumus.

## Uji

`app/src/test/java/com/pulihgo/app/quality/MovementSmoothnessTest.kt` memeriksa controlled=1.0, noisy < controlled, gerak negatif, timestamp bervariasi, input invalid, rerun ID yang sama, serta reset/snapshot. Jalankan `gradlew.bat :app:testDebugUnitTest` di lingkungan Android SDK.
