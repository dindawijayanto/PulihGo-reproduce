package com.pulihgo.app.sensor

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Normalisasi sudut (PGF-SEN-02). Semua nilai dalam derajat. */
object AngleNormalizer {

    /**
     * Membungkus sudut ke rentang (-180, 180]. Ini modulo wrap, bukan clamp:
     * tidak ada data yang dibuang, nilai hanya dipetakan ke sudut ekuivalennya.
     * Contoh: 190 menjadi -170, -190 menjadi 170, 540 menjadi 180, -180 menjadi 180.
     */
    fun wrapDeg180(deg: Double): Double {
        val mod360 = ((deg % 360.0) + 360.0) % 360.0 // [0, 360)
        return if (mod360 > 180.0) mod360 - 360.0 else mod360
    }

    /** Jarak terpendek antara dua sudut, dalam [0, 180]. */
    fun angularDistance(a: Double, b: Double): Double = abs(wrapDeg180(a - b))

    /**
     * Rata-rata sirkular. Rata-rata aritmetika biasa salah di batas ±180°:
     * mean(179, -179) seharusnya 180, bukan 0.
     */
    fun circularMeanDeg(values: List<Double>): Double {
        require(values.isNotEmpty()) { "values must not be empty" }
        var sumSin = 0.0
        var sumCos = 0.0
        for (v in values) {
            val rad = Math.toRadians(v)
            sumSin += sin(rad)
            sumCos += cos(rad)
        }
        return wrapDeg180(Math.toDegrees(atan2(sumSin, sumCos)))
    }

    /** Sudut relatif terhadap [offset] per sumbu, sudah dinormalisasi ke (-180, 180]. */
    fun relative(raw: Angles, offset: Angles): Angles = Angles(
        pitch = wrapDeg180(raw.pitch - offset.pitch),
        roll = wrapDeg180(raw.roll - offset.roll),
        yaw = wrapDeg180(raw.yaw - offset.yaw),
    )
}

/** Status proses kalibrasi untuk ditampilkan di UI. */
sealed interface CalibrationState {
    data object NotCalibrated : CalibrationState
    data class Collecting(val collected: Int, val required: Int) : CalibrationState {
        val progress: Float get() = collected.toFloat() / required
    }
    data class Calibrated(val offset: Angles) : CalibrationState
    /** Ponsel bergerak terlalu banyak selama kalibrasi. [spreadDeg] = penyimpangan terbesar. */
    data class FailedUnstable(val spreadDeg: Double) : CalibrationState
}

/**
 * Kalibrasi "zero" / tare (PGF-SEN-02).
 *
 * 1. Pasien menahan posisi netral, lalu UI memanggil [start].
 * 2. [onSample] mengumpulkan [requiredSamples] sampel (25 sampel di 50 Hz ≈ 0,5 detik).
 * 3. Offset = rata-rata sirkular per sumbu. Jika ada sampel yang menyimpang lebih dari
 *    [maxSpreadDeg] dari rata-rata, kalibrasi ditolak dengan [CalibrationState.FailedUnstable].
 * 4. Setelah itu [relative] mengembalikan sudut relatif terhadap posisi awal,
 *    sehingga posisi awal = 0°, dan hasilnya selalu berada di (-180, 180].
 *
 * Bukan thread-safe; panggil dari satu thread (main thread di Compose).
 */
class AngleCalibrator(
    private val requiredSamples: Int = 25,
    private val maxSpreadDeg: Double = 4.0,
    initialOffset: Angles? = null,
) {
    init {
        require(requiredSamples > 0) { "requiredSamples must be > 0" }
    }

    private val buffer = mutableListOf<Angles>()

    /** Offset valid terakhir; tetap dipakai selama kalibrasi ulang berjalan atau gagal. */
    private var previousOffset: Angles? = initialOffset

    var state: CalibrationState =
        initialOffset?.let { CalibrationState.Calibrated(it) } ?: CalibrationState.NotCalibrated
        private set

    /** Offset aktif, atau null jika belum dikalibrasi. */
    val offset: Angles? get() = (state as? CalibrationState.Calibrated)?.offset

    /** Mulai (atau ulangi) kalibrasi. Offset lama tetap dipakai sampai kalibrasi baru berhasil. */
    fun start() {
        buffer.clear()
        state = CalibrationState.Collecting(0, requiredSamples)
    }

    /** Masukkan sampel mentah. Hanya berpengaruh saat state = Collecting. */
    fun onSample(raw: Angles): CalibrationState {
        if (state !is CalibrationState.Collecting) return state
        buffer += raw
        if (buffer.size < requiredSamples) {
            state = CalibrationState.Collecting(buffer.size, requiredSamples)
            return state
        }

        val mean = Angles(
            pitch = AngleNormalizer.circularMeanDeg(buffer.map { it.pitch }),
            roll = AngleNormalizer.circularMeanDeg(buffer.map { it.roll }),
            yaw = AngleNormalizer.circularMeanDeg(buffer.map { it.yaw }),
        )
        val spread = buffer.maxOf { s ->
            maxOf(
                AngleNormalizer.angularDistance(s.pitch, mean.pitch),
                AngleNormalizer.angularDistance(s.roll, mean.roll),
                AngleNormalizer.angularDistance(s.yaw, mean.yaw),
            )
        }
        buffer.clear()
        state = if (spread > maxSpreadDeg) {
            CalibrationState.FailedUnstable(spread)
        } else {
            previousOffset = mean
            CalibrationState.Calibrated(mean)
        }
        return state
    }

    /**
     * Sudut relatif terhadap posisi awal. Selama kalibrasi ulang berjalan atau gagal,
     * offset terakhir yang valid tetap dipakai. Jika belum pernah dikalibrasi,
     * offset dianggap 0 (nilai mentah yang dinormalisasi).
     */
    fun relative(raw: Angles): Angles = AngleNormalizer.relative(raw, offset ?: previousOffset ?: Angles.ZERO)

    /** Hapus kalibrasi sepenuhnya. */
    fun reset() {
        buffer.clear()
        previousOffset = null
        state = CalibrationState.NotCalibrated
    }
}
