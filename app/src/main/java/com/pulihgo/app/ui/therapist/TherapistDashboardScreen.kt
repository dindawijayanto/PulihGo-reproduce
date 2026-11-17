package com.pulihgo.app.ui.therapist

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulihgo.app.R
import java.time.LocalDate
import java.util.Locale

/**
 * PGF-THR-02 — Figma "Screen 01 — Therapist Dashboard / Empty" and
 * "Screen 02 — Therapist Dashboard / With Data", adapted to a single Pixel 8 column.
 *
 * Values come from seeded history (sessions.json) and sessions saved on this device.
 * Figma items with no data source (adherence %, telemetry stream, clock, session quota,
 * batch prescribe, HIPAA/encryption claims) are omitted or shown as "not available".
 */
@Composable
fun TherapistDashboardScreen(
    therapistName: String,
    summary: DashboardSummary,
    isDark: Boolean,
    today: LocalDate,
    onNavigate: (TherapistTab) -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit,
    onViewPatient: (patientId: String) -> Unit,
    nowEpochMs: Long = System.currentTimeMillis(),
) {
    val c = if (isDark) ThrColors.Dark else ThrColors.Light
    Column(Modifier.fillMaxSize().background(c.page).testTag("therapist_dashboard")) {
        ClinicalHeader(c, therapistName, TherapistTab.DASHBOARD, onNavigate, onLogout)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { DashboardTitle(c, summary, today, onRefresh) }
            if (summary.isEmpty) {
                item { EmptyDashboardCard(c, hasPatients = summary.assignedPatients > 0, onViewPatients = { onNavigate(TherapistTab.THERAPY) }) }
                item { StandbyTiles(c, summary) }
            } else {
                item { KpiGrid(c, summary) }
                item { RepetitionTimeline(c, summary.week) }
                item { RecentActivityHeader(c, summary) }
                items(summary.recentActivity, key = { it.patientId }) { row ->
                    RecentActivityCard(c, row, nowEpochMs, onView = { onViewPatient(row.patientId) })
                }
                item {
                    Text(
                        "Showing ${summary.recentActivity.size} most recent ${if (summary.recentActivity.size == 1) "record" else "records"}" +
                            if (summary.recentActivity.any { it.seeded }) " • Seeded = demo history from mock data" else "",
                        style = thrText(11.sp, FontWeight.SemiBold, c.muted, line = 15.sp),
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
                item { MonitoringPanel(c, summary.flags, nowEpochMs, onViewPatient) }
            }
            item { PortalFooter(c) }
        }
    }
}

// ---------------------------------------------------------------- title

@Composable
private fun DashboardTitle(c: ThrColors, summary: DashboardSummary, today: LocalDate, onRefresh: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PortalBreadcrumb(c, "DASHBOARD", "OVERVIEW")
        Text("Overall Statistics", style = thrText(30.sp, FontWeight.Bold, c.text, line = 38.sp))
        if (summary.assignedPatients > 0) {
            Row(
                Modifier.clip(RoundedCornerShape(9999.dp)).background(c.surfaceHigh).padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(c.accent))
                Text(
                    "Cohort: ${summary.assignedPatients} assigned ${if (summary.assignedPatients == 1) "patient" else "patients"}",
                    style = thrText(11.sp, FontWeight.Medium, c.muted),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
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
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("dashboard_refresh"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.pg_exercise_refresh), null, tint = c.accentText, modifier = Modifier.size(12.dp))
                Text("Refresh", style = thrText(12.sp, FontWeight.SemiBold, c.text), modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

// ---------------------------------------------------------------- empty (Screen 01)

@Composable
private fun EmptyDashboardCard(c: ThrColors, hasPatients: Boolean, onViewPatients: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.surface)
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .testTag("dashboard_empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(c.surfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.pg_home_pulse), null, tint = c.accentText, modifier = Modifier.size(26.dp))
        }
        Text(
            "No patient data yet",
            style = thrText(22.sp, FontWeight.SemiBold, c.text, line = 28.sp),
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            if (hasPatients) "Patient statistics and range of motion results will appear after your patients record their first exercise sessions."
            else "Patient statistics and range of motion results will appear after patients are assigned to your care and initial exercise sessions are recorded.",
            style = thrText(15.sp, FontWeight.Normal, c.muted, line = 22.sp),
            modifier = Modifier.padding(top = 10.dp),
        )
        Row(
            Modifier
                .padding(top = 22.dp)
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(c.accentStrong)
                .clickable(role = Role.Button, onClick = onViewPatients)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("View Patients", style = thrText(16.sp, FontWeight.SemiBold, c.onAccentStrong))
            Icon(painterResource(R.drawable.pg_home_chevron_right), null, tint = c.onAccentStrong, modifier = Modifier.padding(start = 8.dp).size(14.dp))
        }
        Row(Modifier.padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.pg_home_info), null, tint = c.accentText, modifier = Modifier.size(15.dp))
            Text(
                "Patients record sessions with the motion sensor in the PulihGo mobile app.",
                style = thrText(12.sp, FontWeight.SemiBold, c.muted, line = 16.sp),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun StandbyTiles(c: ThrColors, summary: DashboardSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InsightCard(
            c, "WEEKLY ADHERENCE", R.drawable.pg_thr_compliance,
            value = { KpiValue(c, "—", "/ ${summary.assignedPatients} assigned", muted = true) },
            caption = "Needs a prescribed exercise schedule",
        )
        InsightCard(
            c, "AVERAGE PEAK ROM", R.drawable.pg_home_pulse,
            value = { KpiValue(c, "—", "deg", muted = true) },
            caption = "No calibrated sessions recorded yet",
        )
        InsightCard(
            c, "TOTAL EXERCISE DOSE", R.drawable.pg_thr_reps,
            value = { KpiValue(c, "0", "reps recorded") },
            caption = "0 logged sessions",
        )
    }
}

// ---------------------------------------------------------------- populated (Screen 02)

@Composable
private fun KpiGrid(c: ThrColors, s: DashboardSummary) {
    val awaiting = s.assignedPatients - s.patientsWithSessions
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiTile(
                c, "ACTIVE PATIENTS", R.drawable.pg_thr_user, Modifier.weight(1f),
                value = "${s.patientsWithSessions}", unit = if (s.patientsWithSessions == 1) "patient" else "patients",
                caption = if (awaiting > 0) "$awaiting awaiting first session" else "All ${s.assignedPatients} assigned have sessions",
            )
            KpiTile(
                c, "AVERAGE ADHERENCE", R.drawable.pg_thr_compliance, Modifier.weight(1f),
                value = "—", unit = null, muted = true,
                caption = "Needs a prescribed schedule",
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KpiTile(
                c, "TOTAL EXERCISE DOSE", R.drawable.pg_thr_reps, Modifier.weight(1f),
                value = String.format(Locale.US, "%,d", s.weeklyRepetitions), unit = "reps",
                caption = "Past 7 days • ${s.weeklySessions} ${if (s.weeklySessions == 1) "session" else "sessions"}",
            )
            KpiTile(
                c, "AVERAGE PEAK ROM", R.drawable.pg_home_pulse, Modifier.weight(1f),
                value = s.averageLatestPeakRomDeg?.let { "${it.toInt()}°" } ?: "—",
                unit = s.targetRomDeg?.let { "/ ${it.toInt()}° target" },
                caption = "Latest session per patient",
            )
        }
    }
}

@Composable
private fun KpiTile(
    c: ThrColors,
    label: String,
    icon: Int,
    modifier: Modifier,
    value: String,
    unit: String?,
    caption: String,
    muted: Boolean = false,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfaceHigh)
            .padding(14.dp)
            .heightIn(min = 128.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(label, style = thrText(10.sp, FontWeight.SemiBold, c.muted, line = 13.sp, tracking = 0.5.sp), modifier = Modifier.weight(1f))
            Box(
                Modifier.padding(start = 6.dp).size(28.dp).clip(RoundedCornerShape(8.dp)).background(c.chip),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(icon), null, tint = c.accentText, modifier = Modifier.size(14.dp))
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = thrText(26.sp, FontWeight.Bold, if (muted) c.faint else c.text, line = 32.sp))
            if (unit != null) {
                Text(unit, style = thrText(12.sp, FontWeight.SemiBold, c.muted), modifier = Modifier.padding(start = 4.dp, bottom = 5.dp))
            }
        }
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.padding(top = 5.dp).size(6.dp).clip(CircleShape).background(if (muted) c.faint else c.accent))
            Text(caption, style = thrText(11.sp, FontWeight.Medium, c.muted, line = 15.sp), modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun RepetitionTimeline(c: ThrColors, week: List<DailyReps>) {
    val max = (week.maxOfOrNull { it.repetitions } ?: 0).coerceAtLeast(1)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(c.chip),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.pg_home_chart), null, tint = c.accentText, modifier = Modifier.size(16.dp))
            }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text("Cohort Repetition Timeline", style = thrText(13.sp, FontWeight.SemiBold, c.text))
                Text("Daily repetitions from saved sessions.", style = thrText(12.sp, FontWeight.Normal, c.muted, line = 16.sp))
            }
            Text(
                "Rolling 7-Day",
                style = thrText(10.sp, FontWeight.SemiBold, c.muted),
                modifier = Modifier.clip(RoundedCornerShape(9999.dp)).background(c.chartTrack).padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Row(
            Modifier.fillMaxWidth().semantics {
                contentDescription = "Daily repetitions: " + week.joinToString { "${it.date.dayOfWeek.name.lowercase()} ${it.repetitions}" }
            },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            week.forEach { day ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        if (day.repetitions > 0) "${day.repetitions}" else "",
                        style = thrText(10.sp, FontWeight.SemiBold, c.muted),
                    )
                    val h = if (day.repetitions == 0) 6.dp else (8 + 48 * day.repetitions / max).dp
                    Box(
                        Modifier
                            .padding(top = 2.dp)
                            .width(22.dp)
                            .height(h)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(if (day.isToday) c.accent else c.chartTrack),
                    )
                    Text(
                        day.dayLetter,
                        style = thrText(11.sp, if (day.isToday) FontWeight.Bold else FontWeight.SemiBold, if (day.isToday) c.accentText else c.muted),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentActivityHeader(c: ThrColors, s: DashboardSummary) {
    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.pg_thr_clock), null, tint = c.accentText, modifier = Modifier.size(15.dp))
        Text(
            "Recent Patient Activity",
            style = thrText(17.sp, FontWeight.SemiBold, c.text),
            modifier = Modifier.padding(start = 8.dp).weight(1f),
        )
        Text(
            "${s.patientsWithSessions} of ${s.assignedPatients} updated",
            style = thrText(11.sp, FontWeight.SemiBold, c.muted),
            modifier = Modifier.clip(RoundedCornerShape(9999.dp)).background(c.surface).padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun RecentActivityCard(c: ThrColors, row: RecentActivityRow, nowEpochMs: Long, onView: () -> Unit) {
    val alert = row.outcome != ActivityOutcome.COMPLETED
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.surface)
            .background(if (row.outcome == ActivityOutcome.STOPPED_FOR_PAIN) c.dangerSoft else androidx.compose.ui.graphics.Color.Transparent)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(if (alert) c.toneFor(PatientStatus.NEEDS_FOLLOW_UP).soft else c.active.soft),
                contentAlignment = Alignment.Center,
            ) {
                Text(TherapistPatientListLogic.initials(row.name), style = thrText(14.sp, FontWeight.Bold, c.text))
            }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(row.name, style = thrText(15.sp, FontWeight.SemiBold, c.text), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("#${row.patientId} • ${row.exerciseName}", style = thrText(11.sp, FontWeight.Medium, c.muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutcomePill(c, row.outcome)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniStat(c, "LATEST SESSION", TherapistPatientListLogic.formatSessionTime(row.endedAtEpochMs, nowEpochMs), Modifier.weight(1.4f))
            MiniStat(c, "REPS", "${row.repetitions}", Modifier.weight(0.8f))
            MiniStat(c, "PEAK ROM", "${row.peakRomDeg.toInt()}°", Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (row.seeded) {
                Text(
                    "Seeded",
                    style = thrText(10.sp, FontWeight.SemiBold, c.muted),
                    modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(c.surfaceHigh).padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(c.surfaceHigh)
                    .clickable(role = Role.Button, onClick = onView)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("View Details", style = thrText(13.sp, FontWeight.Medium, c.text))
                Icon(painterResource(R.drawable.pg_home_chevron_right), null, tint = c.text, modifier = Modifier.padding(start = 6.dp).size(13.dp))
            }
        }
    }
}

@Composable
private fun MiniStat(c: ThrColors, label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(8.dp)).background(c.inset).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(label, style = thrText(9.sp, FontWeight.Bold, c.faint, tracking = 0.4.sp), maxLines = 1)
        Text(value, style = thrText(13.sp, FontWeight.SemiBold, c.text), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun OutcomePill(c: ThrColors, outcome: ActivityOutcome) {
    when (outcome) {
        ActivityOutcome.COMPLETED -> TonePill(c.active, "Completed")
        ActivityOutcome.MILD_PAIN -> TonePill(c.followUp, "Mild Pain")
        ActivityOutcome.STOPPED_FOR_PAIN -> TonePill(StatusTone(c.dangerSoft, c.danger, c.dangerSoft, c.danger), "Stopped: Pain")
    }
}

// ---------------------------------------------------------------- monitoring flags

@Composable
private fun MonitoringPanel(c: ThrColors, flags: List<MonitoringFlag>, nowEpochMs: Long, onViewPatient: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surfaceHigh)) {
        Row(
            Modifier.fillMaxWidth().background(c.chip).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.pg_thr_alert), null, tint = c.danger, modifier = Modifier.size(15.dp))
            Text("Monitoring Flags", style = thrText(16.sp, FontWeight.SemiBold, c.text), modifier = Modifier.padding(start = 8.dp).weight(1f))
            Box(
                Modifier.size(24.dp).clip(CircleShape).background(if (flags.isEmpty()) c.surface else c.dangerSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text("${flags.size}", style = thrText(11.sp, FontWeight.Bold, if (flags.isEmpty()) c.muted else c.danger))
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(if (flags.isEmpty()) c.active.soft else c.dangerSoft)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painterResource(if (flags.isEmpty()) R.drawable.pg_thr_on_track else R.drawable.pg_thr_warning),
                    null,
                    tint = if (flags.isEmpty()) c.accentText else c.danger,
                    modifier = Modifier.size(15.dp),
                )
                Text(
                    if (flags.isEmpty()) "No flags in your patients' latest sessions"
                    else "${flags.size} monitoring ${if (flags.size == 1) "flag" else "flags"} detected",
                    style = thrText(12.sp, FontWeight.SemiBold, if (flags.isEmpty()) c.accentText else c.danger),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            flags.forEach { flag -> FlagItem(c, flag, nowEpochMs, onView = { onViewPatient(flag.patientId) }) }
        }
        HorizontalDivider(color = c.rowDivider)
        Row(Modifier.background(c.inset).padding(16.dp)) {
            Icon(painterResource(R.drawable.pg_home_info), null, tint = c.faint, modifier = Modifier.padding(top = 1.dp).size(13.dp))
            Text(
                "Flags are derived from each patient's latest saved session and their pain feedback. They are not a diagnosis — verify clinically before changing a plan.",
                style = thrText(10.sp, FontWeight.Normal, c.muted, line = 15.sp),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun FlagItem(c: ThrColors, flag: MonitoringFlag, nowEpochMs: Long, onView: () -> Unit) {
    val (label, tone) = when (flag.kind) {
        FlagKind.PAIN_STOPPED -> "Pain Alert" to StatusTone(c.dangerSoft, c.danger, c.dangerSoft, c.danger)
        FlagKind.MILD_PAIN -> "Mild Pain" to c.followUp
        FlagKind.CEILING_REACHED -> "Ceiling Reached" to c.active
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(c.surface).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(flag.name, style = thrText(14.sp, FontWeight.SemiBold, c.text))
                Text("#${flag.patientId}", style = thrText(11.sp, FontWeight.Medium, c.muted))
            }
            TonePill(tone, label)
        }
        Text(flag.detail, style = thrText(12.sp, FontWeight.Normal, c.muted, line = 17.sp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = c.faint)) { append(TherapistDashboardLogic.formatAgo(flag.endedAtEpochMs, nowEpochMs)) }
                },
                style = thrText(11.sp, FontWeight.SemiBold, c.faint),
                modifier = Modifier.weight(1f),
            )
            Text(
                "Review Patient",
                style = thrText(12.sp, FontWeight.SemiBold, c.accentText),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(role = Role.Button, onClick = onView)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            )
        }
    }
}
