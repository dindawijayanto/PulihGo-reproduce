package com.pulihgo.app.ui.sensor

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.pulihgo.app.R
import com.pulihgo.app.data.exercise.Exercise
import com.pulihgo.app.data.session.ExerciseSessionRecord
import com.pulihgo.app.data.session.SessionPainLevel
import com.pulihgo.app.detection.RepetitionDetector
import com.pulihgo.app.quality.RomSafetyConfig
import com.pulihgo.app.quality.RomSafetyEvaluator
import com.pulihgo.app.quality.RomSafetyLevel
import com.pulihgo.app.quality.SessionSmoothnessTracker
import com.pulihgo.app.sensor.AngleCalibrator
import com.pulihgo.app.sensor.Axis
import com.pulihgo.app.sensor.CalibratedAngleController
import com.pulihgo.app.sensor.CalibrationState
import com.pulihgo.app.sensor.DeviceAngleSensor
import com.pulihgo.app.sensor.SensorHealth
import java.util.Locale
import kotlin.math.abs

/** Live exercise Figma P10/P19/P20. Existing SensorCheckScreen remains untouched. */
private val LiveTeal = Color(0xFF0D7C7C)
private val LiveGreen = Color(0xFF1DA162)
private val LiveRed = Color(0xFFDD4145)
private val LiveCream = Color(0xFFF9F9F7)

private data class LivePalette(
    val background: Color,
    val surface: Color,
    val text: Color,
    val muted: Color,
    val border: Color,
    val softRed: Color,
)

private fun livePalette(isDark: Boolean) = if (isDark) {
    LivePalette(Color(0xFF111717), Color(0xFF202727), Color(0xFFF2F7F6),
        Color(0xFFB9C6C6), Color(0xFF3A4949), Color(0xFF50282A))
} else {
    LivePalette(LiveCream, Color.White, Color(0xFF1A1E21),
        Color(0xFF68716D), Color(0xFFE3E7E3), Color(0xFFFCE8E7))
}

@Composable
fun LiveExerciseScreen(
    exercise: Exercise,
    patientId: String,
    isDark: Boolean = false,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    onSessionCompleted: (ExerciseSessionRecord) -> Boolean,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val c = livePalette(isDark)
    val controller = remember(context) {
        val sensor = DeviceAngleSensor(context.applicationContext)
        CalibratedAngleController(
            source = sensor.source,
            updates = { sensor.updates() },
            clockMs = SystemClock::elapsedRealtime,
            calibrator = AngleCalibrator(),
        )
    }
    val detector = remember(exercise.id, exercise.targetRomDeg) {
        runCatching { RepetitionDetector(targetRomDeg = exercise.targetRomDeg) }.getOrNull()
    }
    val evaluator = remember(exercise.id, exercise.romCeilingDeg) {
        runCatching { RomSafetyEvaluator(RomSafetyConfig(exercise.romCeilingDeg)) }.getOrNull()
    }
    val smoothness = remember(exercise.id) { SessionSmoothnessTracker() }
    val state by controller.state.collectAsStateWithLifecycle()

    var calibrationRequested by remember { mutableStateOf(false) }
    var measuredRom by remember { mutableDoubleStateOf(0.0) }
    var peakRom by remember { mutableDoubleStateOf(0.0) }
    var repCount by remember { mutableIntStateOf(0) }
    var startEpochMs by remember { mutableLongStateOf(0L) }
    var showPainCheck by remember { mutableStateOf(false) }
    var showExitConfirmation by remember { mutableStateOf(false) }
    var exitToSettings by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(true) }

    val tones = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 35) }.getOrNull() }
    DisposableEffect(tones) { onDispose { tones?.release() } }

    LaunchedEffect(controller, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { controller.run() }
    }
    // Figma's "Calibrate & Start": begin the existing 25-sample neutral calibration
    // automatically, once the sensor is actually streaming. Never assume calibration.
    LaunchedEffect(state.health, state.calibration) {
        if (!calibrationRequested && state.health is SensorHealth.Streaming &&
            state.calibration is CalibrationState.NotCalibrated
        ) {
            calibrationRequested = true
            controller.calibrate()
        }
    }
    LaunchedEffect(controller, detector) {
        controller.samples.collect { sample ->
            val current = controller.state.value
            if (current.calibration is CalibrationState.Calibrated &&
                current.health is SensorHealth.Streaming
            ) {
                val angle = sample.angle(Axis.ROLL)
                if (angle.isFinite()) {
                    if (startEpochMs == 0L) startEpochMs = System.currentTimeMillis()
                    measuredRom = abs(angle)
                    peakRom = maxOf(peakRom, measuredRom)
                    detector?.let { d ->
                        val completed = d.processAngle(angle, sample.elapsedMs)
                        repCount = d.count
                        if (completed != null) smoothness.record(completed)
                    }
                }
            }
        }
    }

    val ready = state.health is SensorHealth.Streaming &&
        state.calibration is CalibrationState.Calibrated && detector != null && evaluator != null
    val romLevel = if (ready) evaluator?.evaluate(measuredRom)?.level else null
    val exceedsCeiling = romLevel == RomSafetyLevel.LIMIT_EXCEEDED
    val targetReached = ready && exercise.targetRomDeg > 0.0 && measuredRom >= exercise.targetRomDeg
    val ringColor = when {
        exceedsCeiling -> LiveRed
        targetReached -> LiveGreen
        else -> LiveTeal
    }

    LaunchedEffect(romLevel, targetReached, soundEnabled) {
        if (soundEnabled && ready && (exceedsCeiling || targetReached)) {
            tones?.startTone(ToneGenerator.TONE_PROP_BEEP, 140)
        }
    }

    fun saveSession(pain: SessionPainLevel) {
        val end = System.currentTimeMillis()
        val start = startEpochMs.takeIf { it > 0L } ?: end
        val record = runCatching {
            ExerciseSessionRecord(
                patientId = patientId,
                exerciseId = exercise.id,
                startedAtEpochMs = start,
                endedAtEpochMs = end,
                repetitions = repCount,
                peakRomDeg = peakRom,
                averageSmoothness = smoothness.snapshot().averageScore,
                painLevel = pain,
            )
        }.getOrNull()
        if (record == null || !onSessionCompleted(record)) saveFailed = true
    }

    BackHandler {
        exitToSettings = false
        showExitConfirmation = true
    }

    Surface(Modifier.fillMaxSize().testTag("exercise_live"), color = c.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(32.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LiveRoundButton(c, R.drawable.pg_icon_arrow_left, "Back") {
                    exitToSettings = false
                    showExitConfirmation = true
                }
                Text("SESSION IN PROGRESS", modifier = Modifier.weight(1f),
                    color = LiveTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                    letterSpacing = 2.0.sp, textAlign = TextAlign.Center, maxLines = 1)
                LiveRoundButton(
                    c,
                    if (soundEnabled) R.drawable.pg_exercise_sound_on else R.drawable.pg_exercise_sound_off,
                    if (soundEnabled) "Mute guidance sound" else "Enable guidance sound",
                ) { soundEnabled = !soundEnabled }
                Spacer(Modifier.width(8.dp))
                LiveRoundButton(c, R.drawable.pg_home_settings, "Settings") {
                    exitToSettings = true
                    showExitConfirmation = true
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(exercise.name.replace(" and ", " / "),
                modifier = Modifier.fillMaxWidth(), color = c.text,
                fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center, maxLines = 2,
                overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(38.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(color = ringColor, style = Stroke(width = 12.dp.toPx()))
                    }
                    Text(
                        if (ready) String.format(Locale.US, "%.0f", measuredRom) else "0",
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = 52.sp, fontWeight = FontWeight.ExtraBold,
                        color = if (exceedsCeiling) LiveRed else c.text,
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            val instructionTitle = when {
                evaluator == null || detector == null -> "Exercise parameters unavailable"
                state.health is SensorHealth.Unavailable -> "Sensor unavailable"
                state.calibration is CalibrationState.FailedUnstable -> "Calibration was unstable"
                !ready -> "Hold still while calibrating"
                exceedsCeiling -> "WARNING: SLOW DOWN & RETURN"
                targetReached -> "TARGET HIT!"
                else -> "Move slowly and with control."
            }
            val instructionBody = when {
                evaluator == null || detector == null -> "Check the exercise target and ROM ceiling."
                state.health is SensorHealth.Unavailable -> "This device has no compatible movement sensor."
                state.calibration is CalibrationState.FailedUnstable -> "Keep the phone still, then retry calibration."
                !ready -> "Keep your forearm neutral. Waiting for a valid sensor reading."
                exceedsCeiling -> "Exceeded configured ROM limit of ${exercise.romCeilingDeg.toInt()}°. Return slowly."
                targetReached -> "Now rotate slowly back to the starting point."
                else -> "Aim for your target ROM of ${exercise.targetRomDeg.toInt()}°"
            }
            val warn = exceedsCeiling
            Card(
                Modifier.fillMaxWidth().heightIn(min = 96.dp).testTag("live_status"),
                colors = CardDefaults.cardColors(containerColor = if (warn) c.softRed else c.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp,
                    if (warn) LiveRed else c.border),
                shape = RoundedCornerShape(16.dp),
            ) {
                // Allow the guidance to expand when it wraps. The previous fixed
                // 74dp card clipped the second line of the calibration message.
                Column(
                    Modifier.fillMaxWidth().heightIn(min = 96.dp)
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        instructionTitle,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = when { warn -> LiveRed; targetReached && ready -> LiveGreen; else -> c.text },
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        instructionBody,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = c.muted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            // Preserve PGF-QS-02's approaching-limit safety guidance while the
            // main target-hit display follows Figma's green P19 state.
            if (ready && romLevel == RomSafetyLevel.APPROACHING_LIMIT) {
                Text("Approaching ROM limit (${exercise.romCeilingDeg.toInt()}°). Move slowly.",
                    color = LiveTeal, fontSize = 11.sp, lineHeight = 15.sp,
                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                    textAlign = TextAlign.Center)
            }
            if (state.calibration is CalibrationState.FailedUnstable) {
                TextButton(onClick = { controller.calibrate() },
                    modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Retry calibration", color = LiveTeal)
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LiveStatCard("REPETITIONS", "$repCount", c, Modifier.weight(1f))
                LiveStatCard("PEAK ANGLE", String.format(Locale.US, "%.0f", peakRom),
                    c, Modifier.weight(1f))
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { showPainCheck = true },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("finish_practice"),
                shape = RoundedCornerShape(12.dp),
                enabled = startEpochMs > 0L,
                colors = ButtonDefaults.buttonColors(containerColor = LiveTeal),
            ) { Text("✓  Finish Practice", fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { saveSession(SessionPainLevel.STOPPED_DUE_TO_PAIN) },
                modifier = Modifier.fillMaxWidth().height(52.dp).testTag("stop_it_hurts"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = c.softRed),
                border = androidx.compose.foundation.BorderStroke(1.3.dp, LiveRed),
            ) { Text("●  Stop, it hurts", color = LiveRed, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPainCheck) {
        Dialog(onDismissRequest = { showPainCheck = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(c.background).padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("pain_check"),
                    colors = CardDefaults.cardColors(containerColor = c.surface),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, c.border),
                ) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(42.dp).border(1.dp, LiveTeal, CircleShape),
                            contentAlignment = Alignment.Center) {
                            Icon(painterResource(R.drawable.pg_exercise_heart), null,
                                tint = LiveTeal, modifier = Modifier.size(23.dp))
                        }
                        Spacer(Modifier.height(20.dp))
                        Text("How did that feel?", color = c.text, fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.height(7.dp))
                        Text("Please report any joint pain experienced during your exercise.",
                            color = c.muted, fontSize = 13.sp, lineHeight = 19.sp,
                            textAlign = TextAlign.Center)
                        Spacer(Modifier.height(22.dp))
                        PainChoice("No pain", R.drawable.pg_icon_shield_check, LiveGreen, c) {
                            saveSession(SessionPainLevel.NO_PAIN)
                        }
                        Spacer(Modifier.height(12.dp))
                        PainChoice("Mild pain", R.drawable.pg_exercise_pain_warning, Color(0xFFE99B28), c) {
                            saveSession(SessionPainLevel.MILD_PAIN)
                        }
                        Spacer(Modifier.height(24.dp))
                        TextButton(onClick = { showPainCheck = false }) {
                            Text("Return to Session", color = LiveTeal,
                                fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("Leave current session?") },
            text = { Text("Your current unsaved practice data will be lost. Finish Practice or Stop, it hurts to save it first.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirmation = false
                    if (exitToSettings) onSettings() else onBack()
                }) { Text("Leave", color = LiveRed) }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) { Text("Keep practicing") }
            },
        )
    }
    if (saveFailed) {
        AlertDialog(
            onDismissRequest = { saveFailed = false },
            title = { Text("Session not saved") },
            text = { Text("Unable to save this session on your device. Please try again.") },
            confirmButton = {
                TextButton(onClick = { saveFailed = false }) { Text("OK") }
            },
        )
    }
}

@Composable
private fun LiveRoundButton(c: LivePalette, iconId: Int, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(44.dp)
        .clip(CircleShape).background(c.surface).border(1.dp, c.border, CircleShape)) {
        Icon(painterResource(iconId), contentDescription = description,
            tint = c.text, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun LiveStatCard(label: String, value: String, c: LivePalette, modifier: Modifier) {
    Card(modifier.height(86.dp),
        colors = CardDefaults.cardColors(containerColor = c.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, c.border),
        shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                color = c.muted, letterSpacing = .6.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = c.text, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun PainChoice(label: String, iconRes: Int, iconColor: Color,
                       c: LivePalette, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(54.dp)
        .clip(RoundedCornerShape(16.dp))
        .border(1.dp, c.border, RoundedCornerShape(16.dp))
        .clickable(onClick = onClick).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = c.text, fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f), fontSize = 15.sp)
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp),
        )
    }
}
