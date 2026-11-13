package com.pulihgo.app.ui.sensor

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.pulihgo.app.detection.Repetition
import com.pulihgo.app.detection.RepetitionDetector
import com.pulihgo.app.quality.SessionSmoothnessTracker
import com.pulihgo.app.quality.SmoothnessScore
import com.pulihgo.app.sensor.AngleCalibrator
import com.pulihgo.app.quality.RomSafetyConfig
import com.pulihgo.app.quality.RomSafetyEvaluator
import com.pulihgo.app.quality.RomSafetyLevel
import com.pulihgo.app.quality.RomSafetyStatus
import com.pulihgo.app.sensor.Angles
import com.pulihgo.app.sensor.Axis
import com.pulihgo.app.sensor.CalibratedAngleController
import com.pulihgo.app.sensor.CalibrationState
import com.pulihgo.app.sensor.DeviceAngleSensor
import com.pulihgo.app.sensor.SensorAccuracy
import com.pulihgo.app.sensor.SensorHealth
import com.pulihgo.app.sensor.SensorSource
import java.util.Locale

private val EXERCISE_AXIS = Axis.ROLL

@Composable
fun SensorCheckScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Current demo flow has one exercise. Pass the selected exercise ID once
    // the Exercise Library is connected to the sensor screen.
    val romSafetyEvaluator = remember(context) {
        runCatching {
            val exercisesJson = context.assets.open("mock_data/exercises.json")
                .bufferedReader().use { it.readText() }
            val config = RomSafetyConfig.fromExercisesJson(exercisesJson, exerciseId = "EX001")
            RomSafetyEvaluator(config)
        }.getOrNull()
    }

    var savedOffset by rememberSaveable { mutableStateOf<DoubleArray?>(null) }

    val controller = remember {
        val sensor = DeviceAngleSensor(context.applicationContext)
        CalibratedAngleController(
            source = sensor.source,
            updates = { sensor.updates() },
            clockMs = SystemClock::elapsedRealtime,
            calibrator = AngleCalibrator(
                initialOffset = savedOffset?.let { Angles(it[0], it[1], it[2]) },
            ),
        )
    }

    val detector = remember {
        RepetitionDetector(targetRomDeg = 70.0)
    }

    var repCount by remember { mutableStateOf(0) }
    var lastRepetition by remember {
        mutableStateOf<Repetition?>(null)
    }
    val smoothnessTracker = remember { SessionSmoothnessTracker() }
    var lastSmoothness by remember { mutableStateOf<SmoothnessScore?>(null) }
    var averageSmoothness by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(controller, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { controller.run() }
    }

    val state by controller.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.calibration) {
        val offset = (state.calibration as? CalibrationState.Calibrated)?.offset
        if (offset != null) savedOffset = doubleArrayOf(offset.pitch, offset.roll, offset.yaw)
        if (state.calibration is CalibrationState.NotCalibrated) savedOffset = null
    }

    LaunchedEffect(controller) {
        controller.samples.collect{ sample ->
            val currentState = controller.state.value

            if (
                currentState.calibration is CalibrationState.Calibrated &&
                currentState.health is SensorHealth.Streaming
            ) {
                val angle = sample.angle(EXERCISE_AXIS)

                val repetition = detector.processAngle(
                    angleDeg = angle,
                    timestampMs = sample.elapsedMs,
                )

                repCount = detector.count

                if (repetition != null) {
                    lastRepetition = repetition
                    lastSmoothness = smoothnessTracker.record(repetition)
                    averageSmoothness = smoothnessTracker.snapshot().averageScore
                }
            }
        }
    }

    LaunchedEffect(state.calibration) {
        if (state.calibration !is CalibrationState.Calibrated){
            detector.reset()
            repCount = 0
            lastRepetition = null
            smoothnessTracker.reset()
            lastSmoothness = null
            averageSmoothness = null
        }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            TextButton(onClick = onBack) { Text("‹ Kembali") }
            Text("Cek Sensor & Kalibrasi", style = MaterialTheme.typography.headlineSmall)

            HealthCard(state.source, state.health, state.accuracy)

            val ready = state.health is SensorHealth.Streaming
            CalibrationCard(
                calibration = state.calibration,
                calibratedAxisValue = state.calibrated?.get(EXERCISE_AXIS),
                enabled = ready,
                onCalibrate = controller::calibrate,
                onReset = controller::resetCalibration,
            )
            RomSafetyCard(
                status = if (ready && state.calibration is CalibrationState.Calibrated) {
                    romSafetyEvaluator?.evaluate(state.calibrated?.get(EXERCISE_AXIS))
                } else null,
                configAvailable = romSafetyEvaluator != null,
            )
            RepetitionCard(
                count = repCount,
                lastRepetition = lastRepetition,
                lastSmoothness = lastSmoothness,
                averageSmoothness = averageSmoothness,
            )
            AnglesCard("Sudut terkalibrasi (relatif ke posisi awal)", state.calibrated, highlight = EXERCISE_AXIS)
            AnglesCard("Sudut mentah dari sensor", state.raw, highlight = null)
        }
    }
}

private data class RomSafetyCardPresentation(
    val title: String,
    val instruction: String,
    val background: Color,
    val foreground: Color,
)

@Composable
private fun RomSafetyCard(status: RomSafetyStatus?, configAvailable: Boolean) {
    val presentation = when {
        !configAvailable -> RomSafetyCardPresentation(
            "Batas ROM tidak tersedia",
            "Konfigurasi batas latihan tidak dapat dibaca. Peringatan ROM belum aktif.",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
        status == null -> RomSafetyCardPresentation(
            "Pemantauan batas ROM",
            "Kalibrasi perangkat dan pastikan sensor terhubung untuk mengaktifkan peringatan.",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
        status.level == RomSafetyLevel.LIMIT_EXCEEDED -> RomSafetyCardPresentation(
            "Batas ROM terlampaui",
            "Kembalikan gerakan perlahan ke posisi awal yang nyaman. Hentikan latihan jika nyeri.",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
        status.level == RomSafetyLevel.APPROACHING_LIMIT -> RomSafetyCardPresentation(
            "Mendekati batas ROM",
            "Perlambat gerakan dan jangan memaksakan rotasi lengan.",
            Color(0xFFFFF1CC),
            Color(0xFF663C00),
        )
        else -> RomSafetyCardPresentation(
            "ROM dalam batas",
            "Lanjutkan gerakan secara perlahan dan terkendali.",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = presentation.background)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(presentation.title, style = MaterialTheme.typography.titleMedium, color = presentation.foreground)
            if (status != null) {
                Text(
                    String.format(
                        Locale.US,
                        "ROM saat ini: %.1f° | Mulai peringatan: %.1f° | Batas: %.1f°",
                        status.currentRomDeg, status.warningStartDeg, status.ceilingDeg,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = presentation.foreground,
                )
            }
            Text(presentation.instruction, style = MaterialTheme.typography.bodyMedium, color = presentation.foreground)
        }
    }
}

@Composable
private fun HealthCard(source: SensorSource?, health: SensorHealth, accuracy: SensorAccuracy) {
    val (title, detail, good) = when (health) {
        SensorHealth.Unavailable -> Triple(
            "Sensor tidak tersedia",
            "Perangkat ini tidak memiliki sensor rotasi maupun accelerometer, sehingga latihan tidak dapat diukur.",
            false,
        )
        SensorHealth.RegistrationFailed -> Triple(
            "Sensor gagal diaktifkan",
            "Sistem menolak pembacaan sensor. Tutup aplikasi lain yang memakai sensor, lalu coba lagi.",
            false,
        )
        SensorHealth.WaitingFirstSample -> Triple("Menghubungkan sensor…", "Menunggu data pertama.", false)
        SensorHealth.NoData -> Triple(
            "Sensor tidak mengirim data",
            "Sensor terdeteksi tetapi tidak ada data masuk. Mulai ulang aplikasi atau perangkat.",
            false,
        )
        is SensorHealth.Stalled -> Triple(
            "Pembacaan sensor terhenti",
            "Tidak ada data selama ${health.millisSinceLastSample / 1000.0} detik. Menunggu data kembali…",
            false,
        )
        is SensorHealth.Streaming -> Triple(
            "Sensor terhubung",
            String.format(Locale.US, "Data mengalir %.0f Hz.", health.sampleRateHz),
            true,
        )
    }
    val container = if (good) Color(0xFFE3F4EC) else MaterialTheme.colorScheme.errorContainer
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = container)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Color(0xFF1B1B1B))
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1B1B1B))
            source?.let { Text("Sumber: ${it.label}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF1B1B1B)) }
            if (source == SensorSource.ACCELEROMETER) {
                Text(
                    "Mode terbatas: tanpa gyroscope, sudut kurang stabil saat bergerak cepat.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8A4B00),
                )
            }
            if (good && (accuracy == SensorAccuracy.UNRELIABLE || accuracy == SensorAccuracy.LOW)) {
                Text(
                    "Akurasi sensor rendah. Gerakkan ponsel membentuk angka 8 beberapa kali, lalu kalibrasi ulang.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8A4B00),
                )
            }
        }
    }
}

@Composable
private fun CalibrationCard(
    calibration: CalibrationState,
    calibratedAxisValue: Double?,
    enabled: Boolean,
    onCalibrate: () -> Unit,
    onReset: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Kalibrasi posisi awal", style = MaterialTheme.typography.titleMedium)
            Text(
                "Pasang ponsel di lengan bawah, siku menempel di badan dan ditekuk 90°, telapak tangan menghadap ke dalam. " +
                    "Tahan diam, lalu tekan Kalibrasi. Posisi ini akan menjadi 0°.",
                style = MaterialTheme.typography.bodySmall,
            )
            when (calibration) {
                CalibrationState.NotCalibrated -> Text("Belum dikalibrasi.", style = MaterialTheme.typography.bodyMedium)
                is CalibrationState.Collecting -> {
                    Text("Tahan diam…", style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(progress = { calibration.progress }, modifier = Modifier.fillMaxWidth())
                }
                is CalibrationState.Calibrated -> Text(
                    "Terkalibrasi ✓  Sudut ${EXERCISE_AXIS.name.lowercase()} saat ini: ${formatDeg(calibratedAxisValue)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                is CalibrationState.FailedUnstable -> Text(
                    String.format(
                        Locale.US,
                        "Kalibrasi gagal: ponsel bergerak %.1f°. Tahan lengan tetap diam lalu coba lagi.",
                        calibration.spreadDeg,
                    ),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onCalibrate,
                    enabled = enabled && calibration !is CalibrationState.Collecting,
                ) {
                    Text(if (calibration is CalibrationState.Calibrated) "Kalibrasi ulang" else "Kalibrasi")
                }
                OutlinedButton(onClick = onReset, enabled = calibration !is CalibrationState.NotCalibrated) {
                    Text("Reset")
                }
            }
        }
    }
}

@Composable
private fun AnglesCard(title: String, angles: Angles?, highlight: Axis?) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Axis.entries.forEach { axis ->
                val value = angles?.get(axis)
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            axis.name.lowercase().replaceFirstChar { it.uppercase() } +
                                if (axis == highlight) "  (sumbu latihan)" else "",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (axis == highlight) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text(formatDeg(value), style = MaterialTheme.typography.titleMedium)
                    }
                    LinearProgressIndicator(
                        progress = { ((value?.let { kotlin.math.abs(it) } ?: 0.0) / 180.0).toFloat().coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun RepetitionCard(
    count: Int,
    lastRepetition: Repetition?,
    lastSmoothness: SmoothnessScore?,
    averageSmoothness: Double?,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Deteksi Repetisi",
                style = MaterialTheme.typography.titleMedium,
            )

            Text(
                "Jumlah repetisi: $count",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )

            if (lastRepetition != null) {
                Text("Peak ROM: %.1f°".format(lastRepetition.peakRomDeg))
                Text("Durasi: ${lastRepetition.durationMs} ms")
                Text(
                    "Jumlah sampel: ${lastRepetition.samples.size}"
                )
                val perRep = lastSmoothness?.value?.let { String.format(Locale.US, "%.2f / 1.00", it) }
                    ?: "Belum dapat dihitung"
                val sessionMean = averageSmoothness?.let { String.format(Locale.US, "%.2f / 1.00", it) }
                    ?: "Belum tersedia"
                Text("Smoothness repetisi: $perRep")
                Text("Rata-rata smoothness sesi: $sessionMean")
            } else {
                Text(
                    "Lakukan gerakan melewati threshold, " +
                            "kemudian kembali ke posisi awal."
                )
            }
        }
    }
}

private fun formatDeg(value: Double?): String =
    value?.let { String.format(Locale.US, "%.1f°", it) } ?: "—"
