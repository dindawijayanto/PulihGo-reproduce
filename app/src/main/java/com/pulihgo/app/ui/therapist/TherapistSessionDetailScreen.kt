package com.pulihgo.app.ui.therapist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulihgo.app.R
import com.pulihgo.app.data.session.SessionPainLevel
import com.pulihgo.app.ui.summary.SessionSummaryLogic
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TherapistSessionDetailScreen(
    therapistName: String,
    detail: TherapistSessionDetail?,
    isDark: Boolean,
    onBack: () -> Unit,
    onNavigate: (TherapistTab) -> Unit,
    onLogout: () -> Unit,
) {
    val c = if (isDark) ThrColors.Dark else ThrColors.Light
    Column(Modifier.fillMaxSize().background(c.page).testTag("therapist_session_detail")) {
        ClinicalHeader(c, therapistName, TherapistTab.REPORTS, onNavigate, onLogout)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { DetailTitle(c, detail, onBack) }
            if (detail == null) {
                item { NotAvailableCard(c) }
            } else {
                item { OverviewCard(c, detail) }
                item { MetricGrid(c, detail) }
                item {
                    Text(
                        "Repetitions (${detail.reps.size})",
                        style = thrText(18.sp, FontWeight.Bold, c.text),
                    )
                }
                if (detail.reps.isEmpty()) {
                    item {
                        Text(
                            "No per-repetition data was recorded for this session.",
                            style = thrText(13.sp, FontWeight.Medium, c.muted),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface).padding(16.dp),
                        )
                    }
                } else {
                    items(detail.reps, key = { it.number }) { RepCard(c, it) }
                }
            }
            item { PortalFooter(c) }
        }
    }
}

@Composable
private fun DetailTitle(c: ThrColors, detail: TherapistSessionDetail?, onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(role = Role.Button, onClick = onBack)
                .padding(vertical = 4.dp)
                .testTag("therapist_session_back"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.pg_icon_arrow_left), null, tint = c.accentText, modifier = Modifier.size(16.dp))
            Text("Back to Reports", style = thrText(13.sp, FontWeight.SemiBold, c.accentText), modifier = Modifier.padding(start = 6.dp))
        }
        PortalBreadcrumb(c, "REPORTS", "SESSION DETAIL")
        Text("Session Detail", style = thrText(30.sp, FontWeight.Bold, c.text, line = 38.sp))
        if (detail != null) {
            Text(
                "${detail.patientName} • #${detail.patientId}",
                style = thrText(14.sp, FontWeight.SemiBold, c.muted),
            )
        }
    }
}

@Composable
private fun NotAvailableCard(c: ThrColors) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface).padding(20.dp).testTag("therapist_session_unavailable"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Session not available", style = thrText(18.sp, FontWeight.Bold, c.text))
        Text(
            "This session does not exist or belongs to a patient who is not assigned to you.",
            style = thrText(13.sp, FontWeight.Medium, c.muted, line = 19.sp),
        )
    }
}

@Composable
private fun OverviewCard(c: ThrColors, d: TherapistSessionDetail) {
    val start = Instant.ofEpochMilli(d.startedAtEpochMs).atZone(ZoneId.systemDefault())
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(d.exerciseName, style = thrText(17.sp, FontWeight.SemiBold, c.text))
                Text(
                    start.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy • HH:mm", Locale.ENGLISH)),
                    style = thrText(12.sp, FontWeight.Medium, c.muted),
                )
            }
            PainPill(c, d.painLevel)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (d.seeded) "Seeded history" else "Recorded on this device",
                style = thrText(11.sp, FontWeight.Bold, c.muted),
                modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(c.surfaceHigh).padding(horizontal = 6.dp, vertical = 2.dp),
            )
            Text(
                "ID: ${d.sessionId}",
                style = thrText(11.sp, FontWeight.SemiBold, c.faint),
                maxLines = 1,
                modifier = Modifier.testTag("therapist_session_id"),
            )
        }
    }
}

@Composable
private fun MetricGrid(c: ThrColors, d: TherapistSessionDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(
                c,
                "REPETITIONS",
                "${d.repetitions}",
                d.targetReps?.let { "Target $it per session" } ?: "No target set",
                Modifier.weight(1f),
            )
            MetricTile(
                c,
                "PEAK ROM",
                "${d.peakRomDeg.toInt()}°",
                romCaption(d),
                Modifier.weight(1f),
                outcome = d.romOutcome,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(c, "DURATION", SessionSummaryLogic.formatDuration(d.durationMs), "Start to finish", Modifier.weight(1f))
            MetricTile(
                c,
                "SMOOTHNESS",
                d.smoothnessPercent?.let { "$it%" } ?: "—",
                if (d.smoothnessPercent == null) "Not recorded" else "Average of all reps",
                Modifier.weight(1f),
            )
        }
    }
}

private fun romCaption(d: TherapistSessionDetail): String {
    val target = d.targetRomDeg?.let { "target ${it.toInt()}°" }
    val ceiling = d.romCeilingDeg?.let { "ceiling ${it.toInt()}°" }
    return listOfNotNull(target, ceiling).joinToString(" • ").ifEmpty { "No target set" }
}

@Composable
private fun MetricTile(
    c: ThrColors,
    label: String,
    value: String,
    caption: String,
    modifier: Modifier,
    outcome: RomOutcome? = null,
) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(c.surface).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = thrText(10.sp, FontWeight.Bold, c.muted, tracking = 0.5.sp))
        Text(value, style = thrText(24.sp, FontWeight.Bold, c.text, line = 30.sp))
        Text(caption, style = thrText(11.sp, FontWeight.Medium, c.muted, line = 15.sp))
        if (outcome != null && outcome != RomOutcome.UNKNOWN) OutcomePill(c, outcome)
    }
}

@Composable
private fun RepCard(c: ThrColors, rep: SessionRepRow) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(c.inset),
            contentAlignment = Alignment.Center,
        ) {
            Text("${rep.number}", style = thrText(13.sp, FontWeight.Bold, c.text))
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text("${rep.peakRomDeg.toInt()}° peak", style = thrText(14.sp, FontWeight.SemiBold, c.text))
            Text(
                listOfNotNull(
                    SessionSummaryLogic.formatDuration(rep.durationMs),
                    rep.smoothnessPercent?.let { "smoothness $it%" },
                ).joinToString(" • "),
                style = thrText(11.sp, FontWeight.Medium, c.muted),
            )
        }
        if (rep.outcome != RomOutcome.UNKNOWN) OutcomePill(c, rep.outcome)
    }
}

@Composable
private fun OutcomePill(c: ThrColors, outcome: RomOutcome) {
    when (outcome) {
        RomOutcome.TARGET_HIT -> TonePill(c.active, "Target hit")
        RomOutcome.BELOW_TARGET -> TonePill(c.idle, "Below target")
        RomOutcome.CEILING_EXCEEDED -> TonePill(StatusTone(c.dangerSoft, c.danger, c.dangerSoft, c.danger), "Over ceiling")
        RomOutcome.UNKNOWN -> Unit
    }
}

@Composable
private fun PainPill(c: ThrColors, pain: SessionPainLevel) {
    when (pain) {
        SessionPainLevel.NO_PAIN -> TonePill(c.active, "No pain")
        SessionPainLevel.MILD_PAIN -> TonePill(c.followUp, "Mild pain")
        SessionPainLevel.STOPPED_DUE_TO_PAIN -> TonePill(StatusTone(c.dangerSoft, c.danger, c.dangerSoft, c.danger), "Stopped: pain")
    }
}
