package com.pulihgo.app.ui.patient

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulihgo.app.R
import com.pulihgo.app.data.exercise.Exercise
import com.pulihgo.app.data.session.SessionPainLevel
import java.util.Locale

/*
 * PGF-PAT-04 Figma alignment (read-only Figma references):
 * P07 Exercise Library       76:1802  412 x 924
 * P09 Exercise / Preparation 76:1876  412 x 924
 *
 * UI-only change. No change to catalog, calibration, repetition detector,
 * smoothness calculation, or ROM safety warnings.
 */
private val BrandTeal = Color(0xFF0D7C7C)
private val BrandGreen = Color(0xFF159F73)

private data class ExerciseColors(
    val background: Color,
    val surface: Color,
    val accentSoft: Color,
    val warningSurface: Color,
    val text: Color,
    val secondary: Color,
    val outline: Color,
)

private fun exerciseColors(isDark: Boolean): ExerciseColors = if (isDark) {
    ExerciseColors(
        background = Color(0xFF111717),
        surface = Color(0xFF202727),
        accentSoft = Color(0xFF173B39),
        warningSurface = Color(0xFF20352F),
        text = Color(0xFFF2F7F6),
        secondary = Color(0xFFB9C6C6),
        outline = Color(0xFF3A4949),
    )
} else {
    ExerciseColors(
        background = Color(0xFFF9F9F7),
        surface = Color.White,
        accentSoft = Color(0xFFE9F7F5),
        warningSurface = Color(0xFFE9F7F5),
        text = Color(0xFF1A1E21),
        secondary = Color(0xFF66757D),
        outline = Color(0xFFE4EAEB),
    )
}

private val ExerciseCardShape = RoundedCornerShape(18.dp)

/** P07: exercise selection card + safety hint + fixed three-tab bottom navigation. */
@Composable
fun PatientExerciseLibraryScreen(
    exercises: List<Exercise>,
    onSelectExercise: (Exercise) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit = onBack,
    onSummary: () -> Unit = {},
    onSettings: () -> Unit = {},
    isDark: Boolean = false,
) {
    val c = exerciseColors(isDark)
    Column(
        Modifier.fillMaxSize().background(c.background).statusBarsPadding().navigationBarsPadding()
            .testTag("exercise_library"),
    ) {
        LibraryTopBar(c, onSettings)
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "CHOOSE EXERCISE",
                color = BrandTeal,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )
            if (exercises.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = c.surface),
                    shape = ExerciseCardShape,
                    border = BorderStroke(1.dp, c.outline),
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Latihan belum tersedia", style = MaterialTheme.typography.titleMedium, color = c.text)
                        Text("Data latihan belum dapat dimuat. Coba buka halaman ini kembali.",
                            style = MaterialTheme.typography.bodySmall, color = c.secondary)
                    }
                }
            } else {
                exercises.forEach { exercise ->
                    LibraryExerciseCard(exercise = exercise, c = c, onClick = { onSelectExercise(exercise) })
                }
            }
            LibrarySafetyNote(c)
        }
        ExerciseBottomNav(c, onHome = onHome, onSummary = onSummary)
    }
}

@Composable
private fun LibraryTopBar(c: ExerciseColors, onSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("PULIHGO", color = c.text, fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp, letterSpacing = 0.8.sp)
        Spacer(Modifier.weight(1f))
        ExerciseSettingsButton(c, onSettings)
    }
}

@Composable
private fun ExerciseSettingsButton(c: ExerciseColors, onSettings: () -> Unit) {
    IconButton(
        onClick = onSettings,
        modifier = Modifier.size(44.dp).background(c.surface, CircleShape)
            .border(1.dp, c.outline, CircleShape).testTag("exercise_settings"),
    ) {
        Icon(painterResource(R.drawable.pg_home_settings), contentDescription = "Settings",
            modifier = Modifier.size(22.dp), tint = c.text)
    }
}

@Composable
private fun LibraryExerciseCard(exercise: Exercise, c: ExerciseColors, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("exercise_card_${exercise.id}")
            .semantics { contentDescription = "Pilih latihan ${exercise.name}" }
            .clickable(onClick = onClick),
        shape = ExerciseCardShape,
        colors = CardDefaults.cardColors(containerColor = c.surface),
        border = BorderStroke(1.dp, c.outline),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(c.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.pg_exercise_refresh), contentDescription = null,
                    modifier = Modifier.size(36.dp), tint = BrandTeal)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    exercise.name.replace(" and ", " / "),
                    color = c.text,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    // Show the entire exercise explanation, including on smaller devices.
                    if (exercise.id == "EX001")
                        "Twist your forearm palm-up and palm-down to measure your range of motion."
                    else exercise.description,
                    color = c.secondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    softWrap = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(painterResource(R.drawable.pg_icon_check), contentDescription = null,
                        modifier = Modifier.size(16.dp), tint = BrandGreen)
                    Text("Target ROM: ${romText(exercise.targetRomDeg)}",
                        color = BrandGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Icon(painterResource(R.drawable.pg_home_chevron_right), contentDescription = null,
                modifier = Modifier.size(18.dp), tint = c.secondary)
        }
    }
}

@Composable
private fun LibrarySafetyNote(c: ExerciseColors) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(ExerciseCardShape).background(c.warningSurface)
            .padding(17.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.pg_exercise_shield), contentDescription = null,
            modifier = Modifier.size(18.dp), tint = BrandTeal)
        Text(
            "All exercises are calibrated to your neutral resting position. Move slowly and stop if you feel pain.",
            color = c.secondary,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ExerciseBottomNav(c: ExerciseColors, onHome: () -> Unit, onSummary: () -> Unit) {
    Column {
        HorizontalDivider(color = c.outline)
        Row(
            Modifier.fillMaxWidth().height(72.dp).background(c.surface),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExerciseTab(Modifier.weight(1f), "Home", R.drawable.pg_home_home, false, c, onHome)
            ExerciseTab(Modifier.weight(1f), "Exercise", R.drawable.pg_home_activity, true, c, {})
            ExerciseTab(Modifier.weight(1f), "Summary", R.drawable.pg_home_chart, false, c, onSummary)
        }
    }
}

@Composable
private fun ExerciseTab(
    modifier: Modifier, label: String, icon: Int, selected: Boolean,
    c: ExerciseColors, onClick: () -> Unit,
) {
    val tone = if (selected) BrandTeal else c.secondary
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(painterResource(icon), contentDescription = label,
            modifier = Modifier.size(27.dp), tint = tone)
        Text(label, color = tone, fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

/** P09: one preparation card, three steps, and the calibration handoff. */
@Composable
fun PatientExercisePreparationScreen(
    exercise: Exercise,
    onBack: () -> Unit,
    onContinueToCalibration: () -> Unit,
    onSettings: () -> Unit = {},
    isDark: Boolean = false,
    savedPainLevel: SessionPainLevel? = null,
) {
    val c = exerciseColors(isDark)
    Surface(Modifier.fillMaxSize().testTag("exercise_preparation"), color = c.background) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            PreparationTopBar(c, onBack, onSettings)
            Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = c.surface),
                    border = BorderStroke(1.dp, c.outline),
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            painterResource(R.drawable.pg_icon_shield_check), contentDescription = null,
                            tint = BrandGreen, modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Ready to Practice?", color = c.text,
                            fontSize = 20.sp, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Please follow the guidelines to ensure safety and accurate movement tracking.",
                            color = c.secondary, fontSize = 13.sp,
                            lineHeight = 18.sp, textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(20.dp))
                        PreparationInstruction(1,
                            "Strap the phone screen-up along your affected forearm.", c)
                        PreparationInstruction(2,
                            "Sit upright with your elbow bent at a comfortable 90° angle.", c)
                        PreparationInstruction(3,
                            "Hold your forearm flat in a neutral starting position.", c)
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = onContinueToCalibration,
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("start_calibration"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                        ) {
                            Icon(painterResource(R.drawable.pg_exercise_options),
                                contentDescription = null, tint = Color.White,
                                modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Calibrate & Start", color = Color.White,
                                fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Tapping starts session calibration. This sets your current position as 0 (neutral).",
                            color = c.secondary, fontSize = 12.sp, lineHeight = 18.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                if (savedPainLevel != null) {
                    Spacer(Modifier.height(20.dp))
                    SessionSavedBanner(savedPainLevel, c)
                }
            }
        }
    }
}

@Composable
private fun SessionSavedBanner(pain: SessionPainLevel, c: ExerciseColors) {
    val isStopped = pain == SessionPainLevel.STOPPED_DUE_TO_PAIN
    val shade = if (isStopped) Color(0xFFFFECEB) else Color(0xFFE3F6EC)
    val tone = if (isStopped) Color(0xFFDD4145) else Color(0xFF159F73)
    val label = when (pain) {
        SessionPainLevel.STOPPED_DUE_TO_PAIN -> "Session saved and flagged for joint discomfort."
        SessionPainLevel.NO_PAIN -> "Session saved successfully (no pain)."
        SessionPainLevel.MILD_PAIN -> "Session saved successfully (mild pain)."
    }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(if (c.background == Color(0xFF111717)) c.surface else shade)
        .border(1.dp, tone, RoundedCornerShape(14.dp)).padding(16.dp)
        .testTag("session_saved_banner"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Vector glyphs are geometrically centered; text baselines were shifting
        // the check/exclamation below the circular badge on Android devices.
        Box(
            modifier = Modifier.size(20.dp).clip(CircleShape).background(tone),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(
                    if (isStopped) R.drawable.pg_exercise_alert_mark else R.drawable.pg_icon_check
                ),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(label, color = tone, fontSize = 12.sp,
            fontWeight = FontWeight.Bold, lineHeight = 17.sp)
    }
}

@Composable
private fun PreparationTopBar(c: ExerciseColors, onBack: () -> Unit, onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(44.dp).background(c.surface, CircleShape)
                .border(1.dp, c.outline, CircleShape).testTag("preparation_back"),
        ) {
            Icon(painterResource(R.drawable.pg_icon_arrow_left),
                contentDescription = "Back to Exercise Library",
                modifier = Modifier.size(24.dp), tint = c.text)
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text("P U L I H G O", color = c.text, fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold, maxLines = 1)
        }
        ExerciseSettingsButton(c, onSettings)
    }
}

@Composable
private fun PreparationInstruction(number: Int, text: String, c: ExerciseColors) {
    Row(
        Modifier.fillMaxWidth().height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(c.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", color = BrandTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Text(text, modifier = Modifier.weight(1f), color = c.text,
            fontSize = 13.sp, lineHeight = 20.sp, maxLines = 3)
    }
}

private fun romText(degrees: Double): String = String.format(Locale.US, "%.0f°", degrees)
