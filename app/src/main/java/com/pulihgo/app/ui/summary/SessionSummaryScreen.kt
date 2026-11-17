package com.pulihgo.app.ui.summary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pulihgo.app.R
import com.pulihgo.app.data.session.PatientSession
import com.pulihgo.app.data.session.SessionSource
import com.pulihgo.app.ui.theme.PlusJakartaSans

/**
 * PGF-PRO-02 — Patient Session Summary (Figma 08 Summary / Empty, 15 Summary / Session /
 * No Repetitions, 21 Summary / Completed / Repetition Chart) plus the "Understanding
 * Effort" explanation modal (Figma 18). Every number comes from a saved or seeded session.
 */
@Composable
fun PatientSessionSummaryScreen(
    session: PatientSession?,
    exerciseName: String?,
    romCeilingDeg: Double?,
    isDark: Boolean,
    onHome: () -> Unit,
    onExercise: () -> Unit,
    onSettings: () -> Unit,
    /** PGF-PRO-03: every recorded session (newest first) and the one being viewed. */
    history: List<PatientSession> = emptyList(),
    onSelectSession: (sessionId: String) -> Unit = {},
) {
    val c = summaryColors(isDark)
    var showEffort by rememberSaveable { mutableStateOf(false) }
    val state = SessionSummaryLogic.stateOf(session)
    val listState = rememberLazyListState()
    // Picking a session from the history list swaps the report above it: bring it into view.
    LaunchedEffect(session?.id) { if (listState.firstVisibleItemIndex > 0) listState.animateScrollToItem(0) }

    Column(Modifier.fillMaxSize().background(c.background).statusBarsPadding().testTag("patient_summary")) {
        if (state == SummaryState.EMPTY) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
                Spacer(Modifier.weight(1f))
                SettingsButton(c, onSettings)
            }
            EmptySummary(c, Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val s = session!!
                item { SummaryHeader(c, s, exerciseName, onSettings) }
                item { RepetitionAnalysis(c, s, romCeilingDeg) }
                item {
                    Text("Session Stats", style = sumText(16.sp, FontWeight.Black, c.text))
                }
                item { SessionStats(c, s, onExplainEffort = { showEffort = true }) }
                if (history.isNotEmpty()) {
                    item { ProgressCard(c, SessionHistoryLogic.progress(history), SessionHistoryLogic.romTrend(history)) }
                    item {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("Session History", style = sumText(16.sp, FontWeight.Black, c.text), modifier = Modifier.weight(1f))
                            Text("${history.size} ${if (history.size == 1) "session" else "sessions"}", style = sumText(12.sp, FontWeight.SemiBold, c.muted))
                        }
                    }
                    items(history, key = { it.id }) { h ->
                        HistoryRow(c, h, selected = h.id == s.id, onClick = { onSelectSession(h.id) })
                    }
                }
            }
        }
        SummaryBottomTabs(c, onHome = onHome, onExercise = onExercise)
    }

    if (showEffort) UnderstandingEffortDialog(isDark = isDark, onDismiss = { showEffort = false })
}

// ---------------------------------------------------------------- empty (Figma 08)

@Composable
private fun EmptySummary(c: SummaryColors, modifier: Modifier) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 30.dp).testTag("summary_empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(painterResource(R.drawable.pg_home_chart), null, tint = c.muted, modifier = Modifier.size(56.dp))
        Text(
            "No sessions yet",
            style = sumText(18.sp, FontWeight.Bold, c.text),
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            "Finish an exercise to see your session analytics and coach insight here.",
            style = sumText(14.sp, FontWeight.Normal, c.muted, line = 20.sp),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

// ---------------------------------------------------------------- report (Figma 15 / 21)

@Composable
private fun SummaryHeader(c: SummaryColors, s: PatientSession, exerciseName: String?, onSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text("POST SESSION", style = sumText(13.sp, FontWeight.Black, Teal, tracking = 0.6.sp))
            Text("Summary Report", style = sumText(22.sp, FontWeight.Black, c.text, line = 28.sp))
            Text(
                SessionSummaryLogic.formatSessionDate(s.record.endedAtEpochMs),
                style = sumText(13.sp, FontWeight.Normal, c.muted),
            )
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (exerciseName != null) {
                    Text(exerciseName.replace(" and ", " / "), style = sumText(12.sp, FontWeight.SemiBold, c.muted))
                }
                if (s.source == SessionSource.SEEDED) {
                    Text(
                        "Demo history",
                        style = sumText(10.sp, FontWeight.Bold, c.muted),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(c.outline)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        }
        SettingsButton(c, onSettings)
    }
}

@Composable
private fun RepetitionAnalysis(c: SummaryColors, s: PatientSession, romCeilingDeg: Double?) {
    val points = SessionSummaryLogic.chartPoints(s, romCeilingDeg)
    val hasSmoothness = SessionSummaryLogic.hasSmoothness(points)
    Column(
        Modifier.fillMaxWidth().clip(CardShape).background(c.surface).border(1.dp, c.outline, CardShape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Text("Repetition Analysis", style = sumText(16.sp, FontWeight.Bold, c.text))
            Text("Shows ROM (teal) and smoothness (green) per rep.", style = sumText(13.sp, FontWeight.Normal, c.muted, line = 18.sp))
        }
        when {
            s.record.repetitions == 0 -> ChartPlaceholder(c, "No repetitions completed")
            points.isEmpty() -> ChartPlaceholder(c, "Per-repetition data was not recorded for this session")
            else -> RepetitionChart(c, points)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Legend(c, Teal, "Peak ROM")
            Legend(c, Green, "Smoothness")
        }
        if (points.isNotEmpty() && !hasSmoothness) {
            Text("Smoothness was not measured for this session.", style = sumText(11.sp, FontWeight.Normal, c.muted))
        }
    }
}

@Composable
private fun ChartPlaceholder(c: SummaryColors, text: String) {
    Box(
        Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(12.dp)).background(c.chartPlaceholder),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = sumText(13.sp, FontWeight.Normal, c.muted), textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun RepetitionChart(c: SummaryColors, points: List<RepetitionPoint>) {
    val description = points.joinToString { p ->
        "rep ${p.repetition}: ${p.peakRomDeg.toInt()} degrees" + (p.smoothness?.let { ", smoothness ${(it * 100).toInt()}%" } ?: "")
    }
    val measurer = rememberTextMeasurer()
    val labelStyle = sumText(10.sp, FontWeight.SemiBold, c.muted)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
            .semantics { contentDescription = "Repetition chart. $description" }
            .testTag("summary_chart"),
    ) {
        val pad = 8.dp.toPx()
        val labelBand = 20.dp.toPx()
        val w = size.width - pad * 2
        val h = size.height - pad * 2 - labelBand
        val dash = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
        listOf(0f, 0.5f, 1f).forEach { f ->
            val y = pad + h * (1 - f)
            drawLine(c.chartGuide, Offset(pad, y), Offset(pad + w, y), strokeWidth = 1.dp.toPx(), pathEffect = dash)
        }
        fun x(i: Int) = if (points.size == 1) pad + w / 2 else pad + w * i / (points.size - 1)
        fun series(color: Color, value: (RepetitionPoint) -> Float?) {
            val pts = points.mapIndexedNotNull { i, p -> value(p)?.let { Offset(x(i), pad + h * (1 - it)) } }
            pts.zipWithNext { a, b -> drawLine(color, a, b, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round) }
            pts.forEach { drawCircle(color, radius = 3.5.dp.toPx(), center = it) }
        }
        series(Teal) { it.romFraction }
        series(Green) { p -> p.smoothness?.toFloat()?.coerceIn(0f, 1f) }
        points.forEachIndexed { i, p ->
            val layout = measurer.measure("${p.peakRomDeg.toInt()}°", labelStyle)
            val left = (x(i) - layout.size.width / 2f).coerceIn(0f, size.width - layout.size.width)
            drawText(layout, topLeft = Offset(left, size.height - layout.size.height))
        }
    }
}

@Composable
private fun Legend(c: SummaryColors, color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = sumText(13.sp, FontWeight.Normal, c.text), modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun SessionStats(c: SummaryColors, s: PatientSession, onExplainEffort: () -> Unit) {
    val comfort = SessionSummaryLogic.comfort(s.record.painLevel)
    val comfortColor = when (comfort.tone) {
        ComfortTone.GOOD -> Green
        ComfortTone.CAUTION -> Amber
        ComfortTone.STOP -> Red
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(c, "REPETITIONS", "${s.record.repetitions}", c.text, Modifier.weight(1f))
            StatTile(c, "PEAK ROM", "${s.record.peakRomDeg.toInt()}°", c.text, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(c, "DURATION", SessionSummaryLogic.formatDuration(s.durationMs), c.text, Modifier.weight(1f))
            StatTile(
                c, "JOINT COMFORT", comfort.text, comfortColor, Modifier.weight(1f),
                onInfo = onExplainEffort,
            )
        }
    }
}

@Composable
private fun StatTile(
    c: SummaryColors,
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier,
    onInfo: (() -> Unit)? = null,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .border(1.dp, c.outline, RoundedCornerShape(16.dp))
            .then(if (onInfo != null) Modifier.clickable(role = Role.Button, onClickLabel = "Explain effort levels", onClick = onInfo) else Modifier)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = sumText(12.sp, FontWeight.Normal, c.muted, tracking = 0.3.sp), modifier = Modifier.weight(1f))
            if (onInfo != null) {
                Icon(painterResource(R.drawable.pg_home_info), "Understanding effort", tint = c.muted, modifier = Modifier.size(15.dp))
            }
        }
        Text(value, style = sumText(16.sp, FontWeight.ExtraBold, valueColor), modifier = Modifier.padding(top = 4.dp))
    }
}

// ---------------------------------------------------------------- progress & history (PGF-PRO-03)

@Composable
private fun ProgressCard(c: SummaryColors, p: ProgressSnapshot, trend: List<Double>) {
    Column(
        Modifier.fillMaxWidth().clip(CardShape).background(c.surface).border(1.dp, c.outline, CardShape).padding(16.dp)
            .testTag("summary_progress"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("My Progress", style = sumText(16.sp, FontWeight.Bold, c.text))
                Text("Peak ROM across your recorded sessions.", style = sumText(13.sp, FontWeight.Normal, c.muted, line = 18.sp))
            }
            val change = p.romChangeDeg
            if (change != null) {
                val up = change >= 0
                Text(
                    (if (up) "+" else "−") + "${kotlin.math.abs(change).toInt()}°",
                    style = sumText(14.sp, FontWeight.ExtraBold, if (up) Green else Amber),
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                        .background((if (up) Green else Amber).copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        if (trend.size >= 2) {
            Canvas(
                Modifier.fillMaxWidth().height(56.dp).semantics {
                    contentDescription = "Peak ROM trend: " + trend.joinToString { "${it.toInt()} degrees" }
                },
            ) {
                val pad = 5.dp.toPx()
                val lo = trend.min()
                val span = (trend.max() - lo).coerceAtLeast(1.0)
                val pts = trend.mapIndexed { i, v ->
                    Offset(pad + (size.width - 2 * pad) * i / (trend.size - 1), pad + (size.height - 2 * pad) * (1 - ((v - lo) / span).toFloat()))
                }
                pts.zipWithNext { a, b -> drawLine(Teal, a, b, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round) }
                pts.forEach { drawCircle(Teal, radius = 3.dp.toPx(), center = it) }
            }
        } else {
            Text("Complete another session to see your ROM trend.", style = sumText(12.sp, FontWeight.Normal, c.muted))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ProgressStat(c, "SESSIONS", "${p.totalSessions}", Modifier.weight(1f))
            ProgressStat(c, "TOTAL REPS", "${p.totalRepetitions}", Modifier.weight(1f))
            ProgressStat(c, "BEST ROM", p.bestPeakRomDeg?.let { "${it.toInt()}°" } ?: "—", Modifier.weight(1f))
        }
    }
}

@Composable
private fun ProgressStat(c: SummaryColors, label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(c.chartPlaceholder).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(value, style = sumText(16.sp, FontWeight.ExtraBold, c.text))
        Text(label, style = sumText(10.sp, FontWeight.Bold, c.muted, tracking = 0.5.sp))
    }
}

@Composable
private fun HistoryRow(c: SummaryColors, s: PatientSession, selected: Boolean, onClick: () -> Unit) {
    val comfort = SessionSummaryLogic.comfort(s.record.painLevel)
    val comfortColor = when (comfort.tone) {
        ComfortTone.GOOD -> Green
        ComfortTone.CAUTION -> Amber
        ComfortTone.STOP -> Red
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) c.selected else c.surface)
            .border(1.dp, if (selected) Teal else c.outline, RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClickLabel = "View this session", onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("history_${s.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(R.drawable.pg_home_pulse), null, tint = Teal, modifier = Modifier.size(18.dp))
        Column(Modifier.padding(start = 10.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(SessionSummaryLogic.formatSessionDate(s.record.endedAtEpochMs), style = sumText(13.sp, FontWeight.Bold, c.text))
                if (s.source == SessionSource.SEEDED) {
                    Text(
                        "Demo",
                        style = sumText(9.sp, FontWeight.Bold, c.muted),
                        modifier = Modifier.padding(start = 6.dp).clip(RoundedCornerShape(5.dp)).background(c.outline)
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }
            Text(
                "${s.record.repetitions} reps • ${s.record.peakRomDeg.toInt()}° peak • ${SessionSummaryLogic.formatDuration(s.durationMs)}",
                style = sumText(12.sp, FontWeight.Normal, c.muted),
            )
        }
        Text(comfort.text, style = sumText(12.sp, FontWeight.Bold, comfortColor), modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun SettingsButton(c: SummaryColors, onSettings: () -> Unit) {
    IconButton(
        onClick = onSettings,
        modifier = Modifier.size(44.dp).background(c.surface, CircleShape).border(1.dp, c.outline, CircleShape),
    ) {
        Icon(painterResource(R.drawable.pg_home_settings), contentDescription = "Settings", tint = c.text, modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun SummaryBottomTabs(c: SummaryColors, onHome: () -> Unit, onExercise: () -> Unit) {
    Column(Modifier.navigationBarsPadding()) {
        HorizontalDivider(color = c.outline)
        Row(
            Modifier.fillMaxWidth().height(66.dp).background(c.surface),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SummaryTab(c, "HOME", R.drawable.pg_home_home, false, onHome, Modifier.weight(1f))
            SummaryTab(c, "EXERCISE", R.drawable.pg_home_activity, false, onExercise, Modifier.weight(1f))
            SummaryTab(c, "SUMMARY", R.drawable.pg_home_chart, true, {}, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryTab(c: SummaryColors, name: String, icon: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Column(
        modifier.clickable(enabled = !selected, role = Role.Tab, onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(painterResource(icon), contentDescription = name, tint = if (selected) Teal else c.muted, modifier = Modifier.size(20.dp))
        Text(
            name,
            style = sumText(11.sp, if (selected) FontWeight.Bold else FontWeight.Medium, if (selected) Teal else c.muted),
        )
    }
}

// ---------------------------------------------------------------- Understanding Effort (Figma 18)

/** Shared by Home's "Today's effort" card and the Summary joint-comfort tile. */
@Composable
fun UnderstandingEffortDialog(isDark: Boolean, onDismiss: () -> Unit) {
    val c = summaryColors(isDark)
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(c.surface)
                .padding(25.dp)
                .testTag("understanding_effort"),
        ) {
            Icon(painterResource(R.drawable.pg_home_info), null, tint = Teal, modifier = Modifier.size(48.dp))
            Text(
                "Understanding Effort",
                style = sumText(20.sp, FontWeight.Black, c.text),
                modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
            )
            EffortRow(c, Green, "SAFE PACE", "Controlled movements within safe, painless joint angles.")
            EffortRow(c, Amber, "EASIER PACE", "Mild pain or fast movements. Consider slowing down.")
            EffortRow(c, Red, "TIME TO REST", "Discomfort was flagged. Take a break to recover.")
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Color.White),
            ) { Text("Got it", style = sumText(16.sp, FontWeight.Black, Color.White)) }
        }
    }
}

@Composable
private fun EffortRow(c: SummaryColors, dot: Color, title: String, body: String) {
    Row(Modifier.padding(vertical = 10.dp)) {
        Box(Modifier.padding(top = 4.dp, end = 12.dp).size(10.dp).clip(CircleShape).background(dot))
        Column {
            Text(title, style = sumText(13.sp, FontWeight.Black, c.text, tracking = 0.4.sp))
            Text(body, style = sumText(13.sp, FontWeight.Normal, c.muted, line = 18.sp), modifier = Modifier.padding(top = 2.dp))
        }
    }
}

// ---------------------------------------------------------------- tokens

private val Teal = Color(0xFF0E7C7B)
private val Green = Color(0xFF1E9E5A)
private val Amber = Color(0xFFC77800)
private val Red = Color(0xFFD64545)
private val CardShape = RoundedCornerShape(20.dp)

internal data class SummaryColors(
    val background: Color,
    val surface: Color,
    val outline: Color,
    val text: Color,
    val muted: Color,
    val chartGuide: Color,
    val chartPlaceholder: Color,
    val selected: Color,
)

internal fun summaryColors(dark: Boolean) = if (dark) {
    SummaryColors(
        background = Color(0xFF111717),
        surface = Color(0xFF202727),
        outline = Color(0xFF3A4949),
        text = Color(0xFFF2F7F6),
        muted = Color(0xFFB9C6C6),
        chartGuide = Color(0xFF3A4949),
        chartPlaceholder = Color(0xFF172020),
        selected = Color(0xFF173B39),
    )
} else {
    SummaryColors(
        background = Color(0xFFFAFAF7),
        surface = Color(0xFFFFFFFF),
        outline = Color(0xFFE4EAEB),
        text = Color(0xFF1A1D1A),
        muted = Color(0xFF5B5F58),
        chartGuide = Color(0xFFE4EAEB),
        chartPlaceholder = Color(0xFFF4F6F5),
        selected = Color(0xFFE9F7F5),
    )
}

internal fun sumText(
    size: TextUnit,
    weight: FontWeight,
    color: Color,
    line: TextUnit = TextUnit.Unspecified,
    tracking: TextUnit = 0.sp,
) = TextStyle(fontFamily = PlusJakartaSans, fontSize = size, fontWeight = weight, color = color, lineHeight = line, letterSpacing = tracking)
