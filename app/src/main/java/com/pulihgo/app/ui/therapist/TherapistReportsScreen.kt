package com.pulihgo.app.ui.therapist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulihgo.app.R
import com.pulihgo.app.ui.summary.SessionSummaryLogic
import java.time.LocalDate
import java.util.Locale

/**
 * PGF-THR-06 — Figma "Screen 06 — Therapist Reports / Empty" and "Screen 07 — With Data /
 * Selected Patient", adapted to a single Pixel 8 column. Charts and tables use only the
 * selected patient's seeded + device sessions. Export PDF, raw telemetry, sync/audit badges
 * and narrative clinical insights from the mock are omitted (no data or feature behind them).
 */
@Composable
fun TherapistReportsScreen(
    therapistName: String,
    patients: List<TherapistPatientSessions>,
    selectedPatientId: String?,
    exercises: Map<String, ExerciseTarget>,
    isDark: Boolean,
    today: LocalDate,
    onSelectPatient: (patientId: String?) -> Unit,
    onNavigate: (TherapistTab) -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit,
) {
    val c = if (isDark) ThrColors.Dark else ThrColors.Light
    var rangeName by rememberSaveable { mutableStateOf(ReportRange.LAST_7_DAYS.name) }
    val range = ReportRange.valueOf(rangeName)
    val selected = patients.firstOrNull { it.patient.id == selectedPatientId }
    val report = remember(selected, range, exercises, today) {
        selected?.let { TherapistReportsLogic.build(it, range, exercises, today) }
    }

    Column(Modifier.fillMaxSize().background(c.page).testTag("therapist_reports")) {
        ClinicalHeader(c, therapistName, TherapistTab.REPORTS, onNavigate, onLogout)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ReportsTitle(c, today, onRefresh) }
            item {
                SelectorCard(
                    c = c,
                    patients = patients,
                    selected = selected,
                    range = range,
                    onSelectPatient = onSelectPatient,
                    onRangeChange = { rangeName = it.name },
                )
            }
            if (report == null) {
                item {
                    SelectPatientPrompt(
                        c = c,
                        suggestion = patients.firstOrNull(),
                        onLoad = { onSelectPatient(it) },
                        onDirectory = { onNavigate(TherapistTab.THERAPY) },
                    )
                }
            } else {
                item { PatientBanner(c, report) }
                item { KpiCards(c, report) }
                item { RomTrendCard(c, report) }
                item { WeeklyRepsCard(c, report) }
                item { HistoryHeader(c, report) }
                if (report.rows.isEmpty()) {
                    item {
                        EmptyRange(c, report, onShowAll = { rangeName = ReportRange.ALL_TIME.name })
                    }
                } else {
                    items(report.rows, key = { it.id }) { row -> SessionRowCard(c, row) }
                }
            }
            item { PortalFooter(c) }
        }
    }
}

// ---------------------------------------------------------------- title & selector

@Composable
private fun ReportsTitle(c: ThrColors, today: LocalDate, onRefresh: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PortalBreadcrumb(c, "REPORTS", "WEEKLY REPORTS")
        Text("Weekly Reports", style = thrText(30.sp, FontWeight.Bold, c.text, line = 38.sp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.clip(RoundedCornerShape(9999.dp)).background(c.surface).padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.pg_thr_review), null, tint = c.muted, modifier = Modifier.size(12.dp))
                Text(
                    TherapistDashboardLogic.formatLongDate(today),
                    style = thrText(12.sp, FontWeight.SemiBold, c.muted),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            Row(
                Modifier
                    .clip(RoundedCornerShape(9999.dp))
                    .background(c.chip)
                    .clickable(role = Role.Button, onClick = onRefresh)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.pg_exercise_refresh), null, tint = c.accentText, modifier = Modifier.size(12.dp))
                Text("Refresh", style = thrText(12.sp, FontWeight.SemiBold, c.text), modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun SelectorCard(
    c: ThrColors,
    patients: List<TherapistPatientSessions>,
    selected: TherapistPatientSessions?,
    range: ReportRange,
    onSelectPatient: (String?) -> Unit,
    onRangeChange: (ReportRange) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("SELECT PATIENT TO VIEW REPORTS", style = thrText(10.sp, FontWeight.Bold, c.faint, tracking = 0.5.sp))
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(c.inset)
                    .border(BorderStroke(1.dp, if (selected != null) c.accentText else c.divider), RoundedCornerShape(8.dp))
                    .clickable(role = Role.DropdownList) { open = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("reports_patient_selector"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    selected?.let { "${it.name}  (#${it.patient.id})" } ?: "Select an assigned patient…",
                    style = thrText(14.sp, if (selected != null) FontWeight.SemiBold else FontWeight.Medium, if (selected != null) c.text else c.faint),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(painterResource(R.drawable.pg_home_chevron_down), null, tint = c.muted, modifier = Modifier.size(14.dp))
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                if (patients.isEmpty()) {
                    DropdownMenuItem(text = { Text("No patients assigned") }, onClick = { open = false }, enabled = false)
                }
                patients.forEach { p ->
                    DropdownMenuItem(
                        text = { Text("${p.name}  (#${p.patient.id})") },
                        onClick = {
                            open = false
                            onSelectPatient(p.patient.id)
                        },
                    )
                }
            }
        }
        Text("RANGE", style = thrText(10.sp, FontWeight.Bold, c.faint, tracking = 0.5.sp), modifier = Modifier.padding(top = 4.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(9999.dp)).background(c.inset).padding(4.dp),
        ) {
            ReportRange.entries.forEach { r ->
                val isOn = r == range
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9999.dp))
                        .background(if (isOn) c.navSelected else Color.Transparent)
                        .clickable(enabled = !isOn, role = Role.Tab) { onRangeChange(r) }
                        .semantics { this.selected = isOn }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(r.label, style = thrText(12.sp, FontWeight.SemiBold, if (isOn) c.onNavSelected else c.muted))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- empty (Screen 06)

@Composable
private fun SelectPatientPrompt(
    c: ThrColors,
    suggestion: TherapistPatientSessions?,
    onLoad: (String) -> Unit,
    onDirectory: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface).padding(horizontal = 24.dp, vertical = 32.dp)
            .testTag("reports_empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(c.surfaceHigh), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.pg_home_chart), null, tint = c.accentText, modifier = Modifier.size(26.dp))
        }
        Text(
            "Select a patient to view reports",
            style = thrText(20.sp, FontWeight.SemiBold, c.text, line = 26.sp).copy(textAlign = TextAlign.Center),
            modifier = Modifier.padding(top = 18.dp),
        )
        Text(
            "ROM progression graphs, weekly repetition distributions and session logs are patient-specific. Choose an assigned patient above to load their rehabilitation report.",
            style = thrText(14.sp, FontWeight.Normal, c.muted, line = 21.sp).copy(textAlign = TextAlign.Center),
            modifier = Modifier.padding(top = 10.dp),
        )
        if (suggestion != null) {
            Row(
                Modifier
                    .padding(top = 22.dp)
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(c.accentStrong)
                    .clickable(role = Role.Button) { onLoad(suggestion.patient.id) }
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Load ${suggestion.name} (#${suggestion.patient.id})",
                    style = thrText(14.sp, FontWeight.SemiBold, c.onAccentStrong),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            "Go to Patient Directory",
            style = thrText(13.sp, FontWeight.SemiBold, c.accentText),
            modifier = Modifier
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onDirectory)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

// ---------------------------------------------------------------- report (Screen 07)

@Composable
private fun PatientBanner(c: ThrColors, r: PatientReport) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surfaceHigh).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(c.active.soft), contentAlignment = Alignment.Center) {
            Text(TherapistPatientListLogic.initials(r.name), style = thrText(15.sp, FontWeight.Bold, c.text))
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text("SELECTED PATIENT", style = thrText(10.sp, FontWeight.Bold, c.faint, tracking = 0.5.sp))
            Text(r.name, style = thrText(17.sp, FontWeight.SemiBold, c.text))
            Text("#${r.patientId} • ${r.goal}", style = thrText(12.sp, FontWeight.Medium, c.muted), maxLines = 2)
            Text(r.rangeLabel, style = thrText(11.sp, FontWeight.SemiBold, c.accentText), modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun KpiCards(c: ThrColors, r: PatientReport) {
    val k = r.kpis
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallKpi(c, "SESSIONS", "${k.sessions}", if (k.painReportedSessions > 0) "${k.painReportedSessions} with pain reported" else "No pain reported", Modifier.weight(1f), alert = k.painReportedSessions > 0)
            SmallKpi(c, "TOTAL REPS", String.format(Locale.US, "%,d", k.totalRepetitions), r.targetRepsPerSession?.let { "Target $it per session" } ?: "Completed repetitions", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallKpi(
                c, "BEST PEAK ROM", k.bestPeakRomDeg?.let { "${it.toInt()}°" } ?: "—",
                k.averagePeakRomDeg?.let { "Average ${it.toInt()}°" } ?: "No repetitions in range", Modifier.weight(1f),
            )
            SmallKpi(
                c, "AVG SMOOTHNESS", k.averageSmoothness?.let { "${(it * 100).toInt()}%" } ?: "—",
                if (k.averageSmoothness != null) "Sessions that measured it" else "Not measured in range", Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SmallKpi(c: ThrColors, label: String, value: String, caption: String, modifier: Modifier, alert: Boolean = false) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(c.surfaceHigh).padding(14.dp).heightIn(min = 104.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = thrText(10.sp, FontWeight.SemiBold, c.muted, tracking = 0.5.sp))
        Text(value, style = thrText(24.sp, FontWeight.Bold, if (value == "—") c.faint else c.text, line = 30.sp))
        Text(caption, style = thrText(11.sp, FontWeight.Medium, if (alert) c.warnText else c.muted, line = 15.sp))
    }
}

@Composable
private fun RomTrendCard(c: ThrColors, r: PatientReport) {
    val measurer = rememberTextMeasurer()
    val labelStyle = thrText(10.sp, FontWeight.SemiBold, c.muted)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("ROM RECOVERY TREND", style = thrText(11.sp, FontWeight.Bold, c.muted, tracking = 0.5.sp))
        Text("Peak ROM across sessions in the selected range", style = thrText(13.sp, FontWeight.Medium, c.text))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendDot(c, c.accentText, "Peak ROM", dashed = false)
            r.targetRomDeg?.let { LegendDot(c, c.active.pillText, "Target ${it.toInt()}°", dashed = true) }
            r.romCeilingDeg?.let { LegendDot(c, c.danger, "Ceiling ${it.toInt()}°", dashed = true) }
        }
        if (r.romTrend.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)).background(c.inset), contentAlignment = Alignment.Center) {
                Text("No sessions with repetitions in this range", style = thrText(12.sp, FontWeight.Medium, c.muted))
            }
            return@Column
        }
        val points = r.romTrend
        val top = maxOf(r.romCeilingDeg ?: 0.0, r.targetRomDeg ?: 0.0, points.maxOf { it.peakRomDeg }) + 10
        val bottom = (points.minOf { it.peakRomDeg } - 15).coerceAtLeast(0.0)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(170.dp)
                .semantics { contentDescription = "Peak ROM trend: " + points.joinToString { "${it.label} ${it.peakRomDeg.toInt()} degrees" } }
                .testTag("reports_rom_chart"),
        ) {
            val left = 30.dp.toPx()
            val pad = 8.dp.toPx()
            val labelBand = 18.dp.toPx()
            val w = size.width - left - pad
            val h = size.height - pad * 2 - labelBand
            fun y(v: Double) = pad + h * (1 - ((v - bottom) / (top - bottom)).toFloat())
            fun x(i: Int) = if (points.size == 1) left + w / 2 else left + w * i / (points.size - 1)
            val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
            // Axis ticks every ~20°.
            var tick = (bottom / 10).toInt() * 10.0
            while (tick <= top) {
                if (tick >= bottom) {
                    drawLine(c.rowDivider, Offset(left, y(tick)), Offset(left + w, y(tick)), strokeWidth = 1f)
                    val t = measurer.measure("${tick.toInt()}°", labelStyle)
                    drawText(t, topLeft = Offset(0f, y(tick) - t.size.height / 2))
                }
                tick += 20.0
            }
            r.targetRomDeg?.let { drawLine(c.active.pillText, Offset(left, y(it)), Offset(left + w, y(it)), strokeWidth = 1.5.dp.toPx(), pathEffect = dash) }
            r.romCeilingDeg?.let { drawLine(c.danger, Offset(left, y(it)), Offset(left + w, y(it)), strokeWidth = 1.5.dp.toPx(), pathEffect = dash) }
            val pts = points.mapIndexed { i, p -> Offset(x(i), y(p.peakRomDeg)) }
            pts.zipWithNext { a, b -> drawLine(c.accentText, a, b, strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round) }
            pts.forEach { drawCircle(c.accentText, radius = 4.dp.toPx(), center = it) }
            // Date labels: first, last and evenly spaced ones in between to avoid overlap.
            val step = ((points.size + 4) / 5).coerceAtLeast(1)
            points.forEachIndexed { i, p ->
                if (i % step == 0 || i == points.lastIndex) {
                    val t = measurer.measure(p.label, labelStyle)
                    val lx = (x(i) - t.size.width / 2f).coerceIn(left - 4f, size.width - t.size.width)
                    drawText(t, topLeft = Offset(lx, size.height - t.size.height))
                }
            }
        }
        val latest = points.last().peakRomDeg
        r.targetRomDeg?.let { target ->
            Text(
                if (latest >= target) "Latest session reached the ${target.toInt()}° target (${latest.toInt()}°)."
                else "Latest session peak ${latest.toInt()}° — ${(target - latest).toInt()}° below the ${target.toInt()}° target.",
                style = thrText(12.sp, FontWeight.Medium, c.muted, line = 17.sp),
            )
        }
    }
}

@Composable
private fun LegendDot(c: ThrColors, color: Color, label: String, dashed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (dashed) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(3) { Box(Modifier.size(width = 4.dp, height = 2.dp).background(color)) }
            }
        } else {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        }
        Text(label, style = thrText(11.sp, FontWeight.SemiBold, c.muted), modifier = Modifier.padding(start = 5.dp))
    }
}

@Composable
private fun WeeklyRepsCard(c: ThrColors, r: PatientReport) {
    val total = r.week.sumOf { it.repetitions }
    val max = (r.week.maxOfOrNull { it.repetitions } ?: 0).coerceAtLeast(1)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("WEEKLY REP DISTRIBUTION", style = thrText(11.sp, FontWeight.Bold, c.muted, tracking = 0.5.sp), modifier = Modifier.weight(1f))
            Text(
                "7-Day Dose",
                style = thrText(10.sp, FontWeight.SemiBold, c.muted),
                modifier = Modifier.clip(RoundedCornerShape(9999.dp)).background(c.chartTrack).padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$total", style = thrText(28.sp, FontWeight.Bold, c.text, line = 34.sp))
            Text(
                "reps in the last 7 days",
                style = thrText(12.sp, FontWeight.SemiBold, c.muted),
                modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
            )
        }
        Row(
            Modifier.fillMaxWidth().height(110.dp).semantics {
                contentDescription = "Repetitions per day: " + r.week.joinToString { "${it.dayLetter} ${it.repetitions}" }
            },
            verticalAlignment = Alignment.Bottom,
        ) {
            r.week.forEach { d ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${d.repetitions}", style = thrText(10.sp, FontWeight.SemiBold, if (d.repetitions > 0) c.text else c.faint))
                    val barH = if (d.repetitions == 0) 4.dp else (10 + 56 * d.repetitions / max).dp
                    Box(
                        Modifier
                            .padding(top = 2.dp)
                            .width(24.dp)
                            .height(barH)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(if (d.repetitions > 0) c.accentStrong else c.chartTrack),
                    )
                    Text(
                        d.dayLetter,
                        style = thrText(10.sp, if (d.isToday) FontWeight.Bold else FontWeight.SemiBold, if (d.isToday) c.accentText else c.muted),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
        Text(
            "Adherence score needs a prescribed schedule and is not calculated yet.",
            style = thrText(11.sp, FontWeight.Medium, c.faint, line = 15.sp),
        )
    }
}

@Composable
private fun HistoryHeader(c: ThrColors, r: PatientReport) {
    Column(Modifier.padding(top = 4.dp)) {
        Text("SESSION HISTORY", style = thrText(11.sp, FontWeight.Bold, c.muted, tracking = 0.5.sp))
        Text("Repetitions, peak angle and duration of each session", style = thrText(13.sp, FontWeight.Medium, c.text))
        Text(
            "Showing ${r.rows.size} logged ${if (r.rows.size == 1) "session" else "sessions"}" +
                if (r.sessionsOutsideRange > 0) " • ${r.sessionsOutsideRange} more outside this range" else "",
            style = thrText(11.sp, FontWeight.SemiBold, c.faint),
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun EmptyRange(c: ThrColors, r: PatientReport, onShowAll: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No sessions in ${r.range.label.lowercase(Locale.ENGLISH)}", style = thrText(15.sp, FontWeight.SemiBold, c.text))
        if (r.sessionsOutsideRange > 0) {
            Text(
                "Show All Time (${r.sessionsOutsideRange})",
                style = thrText(13.sp, FontWeight.SemiBold, c.accentText),
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, c.divider), RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onShowAll)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        } else {
            Text("This patient has not recorded any sessions yet.", style = thrText(12.sp, FontWeight.Medium, c.muted), modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun SessionRowCard(c: ThrColors, row: ReportSessionRow) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(row.dateLabel, style = thrText(14.sp, FontWeight.SemiBold, c.text))
                    Text(" • ${row.timeLabel}", style = thrText(12.sp, FontWeight.Medium, c.muted))
                    if (row.seeded) {
                        Text(
                            "Seeded",
                            style = thrText(9.sp, FontWeight.Bold, c.muted),
                            modifier = Modifier.padding(start = 6.dp).clip(RoundedCornerShape(4.dp)).background(c.surfaceHigh)
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                }
                Text(row.exerciseName.replace(" and ", " / "), style = thrText(11.sp, FontWeight.Medium, c.muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            ReportStatusPill(c, row.status)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RowStat(c, "REPS", "${row.repetitions}", Modifier.weight(1f))
            RowStat(c, "PEAK ROM", "${row.peakRomDeg.toInt()}°", Modifier.weight(1f))
            RowStat(c, "DURATION", SessionSummaryLogic.formatDuration(row.durationMs), Modifier.weight(1f))
        }
    }
}

@Composable
private fun RowStat(c: ThrColors, label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(8.dp)).background(c.inset).padding(horizontal = 10.dp, vertical = 7.dp)) {
        Text(label, style = thrText(9.sp, FontWeight.Bold, c.faint, tracking = 0.4.sp))
        Text(value, style = thrText(13.sp, FontWeight.SemiBold, c.text))
    }
}

@Composable
private fun ReportStatusPill(c: ThrColors, status: ReportSessionStatus) {
    when (status) {
        ReportSessionStatus.COMPLETED -> TonePill(c.active, "Completed")
        ReportSessionStatus.MILD_PAIN -> TonePill(c.followUp, "Mild Pain")
        ReportSessionStatus.STOPPED_FOR_PAIN -> TonePill(StatusTone(c.dangerSoft, c.danger, c.dangerSoft, c.danger), "Stopped: Pain")
        ReportSessionStatus.NO_REPETITIONS -> TonePill(c.idle, "No Reps")
    }
}
