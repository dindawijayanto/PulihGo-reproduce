package com.pulihgo.app.ui.therapist

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulihgo.app.R
import com.pulihgo.app.data.session.SessionPainLevel
import com.pulihgo.app.ui.theme.PlusJakartaSans
import java.util.Locale

/**
 * PGF-THR-03 — Figma "Screen 03 — Therapist / Patient List", adapted from the
 * 1371px desktop table to a single-column Pixel 8 layout: each table row becomes
 * a card that keeps the same column order (Patient, Clinical ID, Active Program,
 * Last Session Telemetry, Adherence, Status, Action).
 *
 * Figma content without a data source (telemetry stream, clock, adherence %, risk
 * level, scheduled reviews, Export CSV, Assign New Patient, HIPAA badge) is either
 * omitted or shown as an explicit "not available yet" state — never as numbers.
 */
@Composable
fun TherapistPatientListScreen(
    therapistName: String,
    items: List<PatientListItem>,
    isDark: Boolean,
    onOpenDashboard: () -> Unit,
    onViewPatient: (patientId: String) -> Unit,
    nowEpochMs: Long = System.currentTimeMillis(),
) {
    val c = if (isDark) ThrColors.Dark else ThrColors.Light
    var query by rememberSaveable { mutableStateOf("") }
    var filterName by rememberSaveable { mutableStateOf(PatientListFilter.ALL.name) }
    var sortName by rememberSaveable { mutableStateOf(PatientListSort.MOST_RECENT_SESSION.name) }
    var requestedPage by rememberSaveable { mutableIntStateOf(0) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val filter = PatientListFilter.valueOf(filterName)
    val sort = PatientListSort.valueOf(sortName)

    val visible = remember(items, query, filter, sort) {
        TherapistPatientListLogic.apply(items, query, filter, sort)
    }
    val page = TherapistPatientListLogic.page(visible, requestedPage)
    val overview = remember(items) { TherapistPatientListLogic.overview(items) }
    val filterCounts = remember(items) {
        PatientListFilter.entries.associateWith { f -> items.count { TherapistPatientListLogic.matchesFilter(it, f) } }
    }
    val resetPaging = { requestedPage = 0 }

    // Header stays pinned (as in Figma) so scrolled content never slides under the status bar.
    Column(Modifier.fillMaxSize().background(c.page).testTag("therapist_patient_list")) {
        ClinicalHeader(c, therapistName, onOpenDashboard)
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .imePadding(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                TitleBlock(c, overview)
            }
            item {
                ControlPanel(
                    c = c,
                    query = query,
                    onQueryChange = { query = it; resetPaging() },
                    filter = filter,
                    counts = filterCounts,
                    onFilterChange = { filterName = it.name; resetPaging() },
                    sort = sort,
                    onSortChange = { sortName = it.name; resetPaging() },
                )
            }
            item {
                DirectoryHeader(c)
            }
            when {
                items.isEmpty() -> item {
                    EmptyDirectory(
                        c = c,
                        title = "No patients assigned yet",
                        body = "Patients assigned to you will appear here with their latest session results.",
                    )
                }
                visible.isEmpty() -> item {
                    EmptyDirectory(
                        c = c,
                        title = if (query.isBlank()) "No patients in this view" else "No patients match \"${query.trim()}\"",
                        body = "Try another name or ID, or clear the search and filters.",
                        actionLabel = "Clear search & filters",
                        onAction = {
                            query = ""
                            filterName = PatientListFilter.ALL.name
                            resetPaging()
                        },
                    )
                }
                else -> items(page.items, key = { it.patientId }) { item ->
                    PatientRow(
                        c = c,
                        item = item,
                        selected = item.patientId == selectedId,
                        nowEpochMs = nowEpochMs,
                        onSelect = { selectedId = item.patientId },
                        onView = {
                            selectedId = item.patientId
                            onViewPatient(item.patientId)
                        },
                    )
                }
            }
            item {
                DirectoryFooter(
                    c = c,
                    therapistName = therapistName,
                    page = page,
                    onPage = { requestedPage = it },
                )
            }
            item { InsightCards(c, overview) }
            item {
                Text(
                    "PulihGo Digital Rehabilitation Platform • Therapist Portal",
                    style = thrText(11.sp, FontWeight.SemiBold, c.muted),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).navigationBarsPadding(),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- header

@Composable
private fun ClinicalHeader(c: ThrColors, therapistName: String, onOpenDashboard: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.header)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(c.logoTile),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.onboarding_logo),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
            Column(Modifier.padding(start = 8.dp).weight(1f)) {
                Text("PulihGo", style = thrText(18.sp, FontWeight.SemiBold, c.text, line = 20.sp))
                Text("CLINICAL HUB", style = thrText(11.sp, FontWeight.SemiBold, c.muted, tracking = 0.6.sp))
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    therapistName.ifBlank { "Therapist" },
                    style = thrText(12.sp, FontWeight.SemiBold, c.text),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("PHYSIOTHERAPIST", style = thrText(11.sp, FontWeight.SemiBold, c.muted, tracking = 0.4.sp))
            }
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(c.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.pg_thr_user), contentDescription = null, tint = c.onAccent, modifier = Modifier.size(12.dp))
            }
        }
        // Figma nav pill. Reports/Settings belong to later tasks, so they are shown but inactive.
        Row(
            Modifier
                .clip(RoundedCornerShape(9999.dp))
                .background(c.surface)
                .padding(4.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavPill(c, "Dashboard", selected = false, enabled = true, onClick = onOpenDashboard)
            NavPill(c, "Therapy", selected = true, enabled = true, onClick = {})
            NavPill(c, "Reports", selected = false, enabled = false, onClick = {})
            NavPill(c, "Settings", selected = false, enabled = false, onClick = {})
        }
    }
}

@Composable
private fun NavPill(c: ThrColors, label: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(9999.dp))
            .background(if (selected) c.navSelected else Color.Transparent)
            .clickable(enabled = enabled && !selected, role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected }
            .alpha(if (enabled) 1f else 0.45f)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            style = if (selected) thrText(14.sp, FontWeight.Medium, c.onNavSelected)
            else thrText(12.sp, FontWeight.SemiBold, c.muted),
        )
    }
}

// ---------------------------------------------------------------- title

@Composable
private fun TitleBlock(c: ThrColors, overview: CohortOverview) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 16.dp)) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = c.muted, fontWeight = FontWeight.SemiBold)) { append("THERAPY") }
                withStyle(SpanStyle(color = c.divider)) { append("  /  ") }
                withStyle(SpanStyle(color = c.accentText, fontWeight = FontWeight.Bold)) { append("PATIENTS") }
            },
            style = thrText(11.sp, FontWeight.SemiBold, c.muted, tracking = 0.6.sp),
        )
        Row(
            Modifier.padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("My Patients", style = thrText(30.sp, FontWeight.Bold, c.text, line = 38.sp))
            Row(
                Modifier.clip(RoundedCornerShape(9999.dp)).background(c.chip).padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(c.accent))
                Text(
                    "${overview.totalPatients} TOTAL ${if (overview.totalPatients == 1) "PATIENT" else "PATIENTS"}",
                    style = thrText(11.sp, FontWeight.SemiBold, c.muted, tracking = 0.3.sp),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        Text(
            "Manage and monitor rehabilitation sessions recorded by your assigned patients.",
            style = thrText(13.sp, FontWeight.Normal, c.muted, line = 18.sp),
            modifier = Modifier.padding(top = 4.dp),
        )
        // Figma "Quick Cohort Summary Pill": On Track / At Risk → On Track / Needs Follow-up.
        Row(
            Modifier
                .padding(top = 14.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(c.surface)
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val onTrack = overview.activePatients - overview.needsFollowUp
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).background(c.surfaceHigh).padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.pg_thr_on_track), null, tint = c.accentText, modifier = Modifier.size(15.dp))
                Text("$onTrack On Track", style = thrText(11.sp, FontWeight.SemiBold, c.text), modifier = Modifier.padding(start = 6.dp))
            }
            Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.pg_thr_alert), null, tint = c.danger, modifier = Modifier.size(14.dp))
                Text(
                    "${overview.needsFollowUp} Needs Follow-up",
                    style = thrText(11.sp, FontWeight.SemiBold, c.muted),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- search / filter / sort

@Composable
private fun ControlPanel(
    c: ThrColors,
    query: String,
    onQueryChange: (String) -> Unit,
    filter: PatientListFilter,
    counts: Map<PatientListFilter, Int>,
    onFilterChange: (PatientListFilter) -> Unit,
    sort: PatientListSort,
    onSortChange: (PatientListSort) -> Unit,
) {
    val focus = LocalFocusManager.current
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(c.inset)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.pg_thr_search), null, tint = c.muted, modifier = Modifier.size(14.dp))
            Box(Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 12.dp)) {
                if (query.isEmpty()) {
                    Text(
                        "Search patients by name or ID (e.g. Alya, P001)…",
                        style = thrText(13.sp, FontWeight.Normal, c.faint),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = thrText(13.sp, FontWeight.Normal, c.text),
                    cursorBrush = SolidColor(c.accentText),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
                    modifier = Modifier.fillMaxWidth().testTag("patient_search").semantics {
                        contentDescription = "Search patients by name or ID"
                    },
                )
            }
            if (query.isNotEmpty()) {
                Text(
                    "Clear",
                    style = thrText(12.sp, FontWeight.SemiBold, c.accentText),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(role = Role.Button) { onQueryChange("") }
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                )
            }
        }
        Row(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(c.inset)
                .padding(4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            PatientListFilter.entries.forEach { f ->
                val selected = f == filter
                Text(
                    "${f.label} (${counts[f] ?: 0})",
                    style = thrText(
                        12.sp,
                        if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        if (selected) c.onAccent else c.muted,
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selected) c.accent else Color.Transparent)
                        .clickable(role = Role.Tab) { onFilterChange(f) }
                        .semantics { this.selected = selected }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
        SortDropdown(c, sort, onSortChange)
    }
}

@Composable
private fun SortDropdown(c: ThrColors, sort: PatientListSort, onSortChange: (PatientListSort) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(c.inset)
                .clickable(role = Role.DropdownList) { open = true }
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.pg_thr_sort), null, tint = c.accentText, modifier = Modifier.size(width = 12.dp, height = 8.dp))
            Text(
                "Sort by: ${sort.label}",
                style = thrText(12.sp, FontWeight.SemiBold, c.text),
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Icon(painterResource(R.drawable.pg_home_chevron_down), null, tint = c.muted, modifier = Modifier.size(14.dp))
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = c.surfaceHigh,
        ) {
            PatientListSort.entries.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option.label,
                            style = thrText(
                                13.sp,
                                if (option == sort) FontWeight.Bold else FontWeight.Medium,
                                if (option == sort) c.accentText else c.text,
                            ),
                        )
                    },
                    onClick = {
                        onSortChange(option)
                        open = false
                    },
                )
            }
        }
    }
}

// ---------------------------------------------------------------- directory

@Composable
private fun DirectoryHeader(c: ThrColors) {
    Row(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .background(c.surface)
            .background(c.tableHeader)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text("PATIENT", style = thrText(11.sp, FontWeight.Bold, c.muted, tracking = 0.6.sp), modifier = Modifier.weight(1f))
        Text("STATUS", style = thrText(11.sp, FontWeight.Bold, c.muted, tracking = 0.6.sp))
    }
}

@Composable
private fun PatientRow(
    c: ThrColors,
    item: PatientListItem,
    selected: Boolean,
    nowEpochMs: Long,
    onSelect: () -> Unit,
    onView: () -> Unit,
) {
    val tone = c.toneFor(item.status)
    val rowBg = when {
        selected -> c.selectedRow
        item.status == PatientStatus.NEEDS_FOLLOW_UP -> c.followUpRow
        else -> Color.Transparent
    }
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(c.surface)
            .background(rowBg)
            .clickable(onClick = onSelect)
            .semantics { this.selected = selected }
            .testTag("patient_row_${item.patientId}"),
    ) {
        HorizontalDivider(color = c.rowDivider, thickness = 1.dp)
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp)) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(tone.soft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(TherapistPatientListLogic.initials(item.name), style = thrText(12.sp, FontWeight.Bold, tone.strong))
                    }
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 1.dp, y = 1.dp)
                            .size(11.dp)
                            .clip(CircleShape)
                            .background(c.surface)
                            .padding(1.5.dp)
                            .clip(CircleShape)
                            .background(tone.strong),
                    )
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(
                        item.name,
                        style = thrText(18.sp, FontWeight.SemiBold, c.text, line = 23.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${item.age} yrs • ${armLabel(item.affectedArm)}",
                        style = thrText(11.sp, FontWeight.SemiBold, c.muted, line = 14.sp),
                    )
                }
                StatusPill(c, item.status)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.width(76.dp)) {
                    FieldLabel(c, "CLINICAL ID")
                    Text(
                        "#${item.patientId}",
                        style = thrText(12.sp, FontWeight.Bold, c.muted),
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(c.surfaceHigh)
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }
                Column(Modifier.weight(1f)) {
                    FieldLabel(c, "ACTIVE PROGRAM")
                    Text(
                        item.rehabilitationGoal.ifBlank { "Not set" },
                        style = thrText(13.sp, FontWeight.Medium, c.text, line = 18.sp),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1.25f)) {
                    FieldLabel(c, "LAST SESSION TELEMETRY")
                    LastSession(c, item, nowEpochMs)
                }
                Column(Modifier.weight(1f)) {
                    FieldLabel(c, "ADHERENCE (7-DAY)")
                    Text(
                        "Awaiting schedule data",
                        style = thrText(11.sp, FontWeight.SemiBold, c.faint, line = 14.sp),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Box(
                        Modifier.padding(top = 6.dp).fillMaxWidth().height(6.dp)
                            .clip(RoundedCornerShape(9999.dp)).background(c.surfaceHigh),
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                ViewPatientButton(c, highlighted = selected, onClick = onView)
            }
        }
    }
}

@Composable
private fun LastSession(c: ThrColors, item: PatientListItem, nowEpochMs: Long) {
    val last = item.lastSession
    if (last == null) {
        Text(
            "No sessions recorded yet",
            style = thrText(13.sp, FontWeight.Medium, c.faint, line = 18.sp),
            modifier = Modifier.padding(top = 4.dp),
        )
        return
    }
    Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        when (last.painLevel) {
            SessionPainLevel.STOPPED_DUE_TO_PAIN -> WarningLine(c, "Stopped early: Pain reported")
            SessionPainLevel.MILD_PAIN -> WarningLine(c, "Mild pain reported")
            SessionPainLevel.NO_PAIN -> Unit
        }
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = c.accentText, fontWeight = FontWeight.SemiBold)) {
                    append("${last.peakRomDeg.toInt()}°")
                }
                withStyle(SpanStyle(color = c.muted)) { append(" Peak ROM") }
                withStyle(SpanStyle(color = c.divider)) { append(" • ") }
                withStyle(SpanStyle(color = c.text)) {
                    append("${last.repetitions} ${if (last.repetitions == 1) "rep" else "reps"}")
                }
            },
            style = thrText(13.sp, FontWeight.Normal, c.text, line = 18.sp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.pg_thr_clock), null, tint = c.accentText, modifier = Modifier.size(11.dp))
            Text(
                TherapistPatientListLogic.formatSessionTime(last.endedAtEpochMs, nowEpochMs),
                style = thrText(11.sp, FontWeight.SemiBold, c.muted),
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun WarningLine(c: ThrColors, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(R.drawable.pg_thr_warning), null, tint = c.warnText, modifier = Modifier.size(width = 13.dp, height = 12.dp))
        Text(text, style = thrText(13.sp, FontWeight.Medium, c.warnText, line = 18.sp), modifier = Modifier.padding(start = 5.dp))
    }
}

@Composable
private fun StatusPill(c: ThrColors, status: PatientStatus) {
    val tone = c.toneFor(status)
    val label = when (status) {
        PatientStatus.ON_TRACK -> "Active"
        PatientStatus.NEEDS_FOLLOW_UP -> "Needs Follow-up"
        PatientStatus.NO_SESSIONS -> "No Sessions"
    }
    Row(
        Modifier.clip(RoundedCornerShape(9999.dp)).background(tone.pill).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(tone.strong))
        Text(label, style = thrText(11.sp, FontWeight.SemiBold, tone.pillText), modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun ViewPatientButton(c: ThrColors, highlighted: Boolean, onClick: () -> Unit) {
    val bg = if (highlighted) c.accentStrong else c.surfaceHigh
    val fg = if (highlighted) c.onAccentStrong else c.text
    Row(
        Modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("View Patient", style = thrText(13.sp, FontWeight.Medium, fg))
        Icon(
            painterResource(R.drawable.pg_home_chevron_right),
            contentDescription = null,
            tint = fg,
            modifier = Modifier.padding(start = 6.dp).size(14.dp),
        )
    }
}

@Composable
private fun EmptyDirectory(
    c: ThrColors,
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(c.surface)
            .padding(horizontal = 24.dp, vertical = 36.dp)
            .testTag("patient_list_empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(c.surfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.pg_thr_search), null, tint = c.accentText, modifier = Modifier.size(18.dp))
        }
        Text(
            title,
            style = thrText(16.sp, FontWeight.SemiBold, c.text, line = 22.sp),
            modifier = Modifier.padding(top = 14.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            body,
            style = thrText(12.sp, FontWeight.Normal, c.muted, line = 17.sp),
            modifier = Modifier.padding(top = 6.dp),
        )
        if (actionLabel != null) {
            Text(
                actionLabel,
                style = thrText(12.sp, FontWeight.SemiBold, c.accentText),
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, c.divider), RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onAction)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun DirectoryFooter(c: ThrColors, therapistName: String, page: PatientPage, onPage: (Int) -> Unit) {
    val from = if (page.totalMatches == 0) 0 else page.pageIndex * TherapistPatientListLogic.PAGE_SIZE + 1
    val to = page.pageIndex * TherapistPatientListLogic.PAGE_SIZE + page.items.size
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            .background(c.surface)
            .background(c.tableFooter)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (page.totalMatches == 0) {
                buildAnnotatedString { append("No patients to show under care of ${therapistName.ifBlank { "you" }}") }
            } else buildAnnotatedString {
                append("Showing ")
                withStyle(SpanStyle(color = c.text, fontWeight = FontWeight.Bold)) { append("$from–$to") }
                append(" of ")
                withStyle(SpanStyle(color = c.text, fontWeight = FontWeight.Bold)) { append("${page.totalMatches}") }
                append(" ${if (page.totalMatches == 1) "patient" else "patients"} under care of ${therapistName.ifBlank { "you" }}")
            },
            style = thrText(11.sp, FontWeight.Normal, c.muted, line = 15.sp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            PageButton(c, enabled = page.pageIndex > 0, onClick = { onPage(page.pageIndex - 1) }) {
                Icon(painterResource(R.drawable.pg_home_chevron_left), "Previous page", tint = c.muted, modifier = Modifier.size(14.dp))
            }
            repeat(page.pageCount) { index ->
                val current = index == page.pageIndex
                PageButton(c, enabled = !current, onClick = { onPage(index) }, current = current) {
                    Text(
                        "${index + 1}",
                        style = thrText(11.sp, if (current) FontWeight.Bold else FontWeight.SemiBold, if (current) c.onAccent else c.text),
                    )
                }
            }
            PageButton(c, enabled = page.pageIndex < page.pageCount - 1, onClick = { onPage(page.pageIndex + 1) }) {
                Icon(painterResource(R.drawable.pg_home_chevron_right), "Next page", tint = c.text, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun PageButton(
    c: ThrColors,
    enabled: Boolean,
    onClick: () -> Unit,
    current: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (current) c.accent else c.surfaceHigh)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled || current) 1f else 0.4f),
        contentAlignment = Alignment.Center,
    ) { content() }
}

// ---------------------------------------------------------------- insight cards

@Composable
private fun InsightCards(c: ThrColors, overview: CohortOverview) {
    Column(
        Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InsightCard(
            c = c,
            label = "AVERAGE COHORT COMPLIANCE",
            icon = R.drawable.pg_thr_compliance,
            value = { Text("—", style = thrText(28.sp, FontWeight.Bold, c.faint, line = 34.sp)) },
            caption = "Available once a prescribed exercise schedule exists",
        )
        InsightCard(
            c = c,
            label = "TOTAL REPETITIONS LOGGED",
            icon = R.drawable.pg_thr_reps,
            value = {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        String.format(Locale.US, "%,d", overview.totalRepetitions),
                        style = thrText(28.sp, FontWeight.Bold, c.accentText, line = 34.sp),
                    )
                    Text("reps", style = thrText(11.sp, FontWeight.SemiBold, c.muted), modifier = Modifier.padding(start = 6.dp, bottom = 6.dp))
                }
            },
            caption = "From sessions saved on this device",
        )
        InsightCard(
            c = c,
            label = "NEXT SCHEDULED REVIEW",
            icon = R.drawable.pg_thr_review,
            value = { Text("Not scheduled", style = thrText(22.sp, FontWeight.SemiBold, c.faint, line = 28.sp)) },
            caption = "Review scheduling is not available yet",
        )
    }
}

@Composable
private fun InsightCard(
    c: ThrColors,
    label: String,
    @DrawableRes icon: Int,
    value: @Composable () -> Unit,
    caption: String,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = thrText(11.sp, FontWeight.SemiBold, c.muted, tracking = 0.5.sp))
            value()
            Text(caption, style = thrText(11.sp, FontWeight.Normal, c.muted, line = 15.sp))
        }
        Box(
            Modifier.padding(start = 12.dp).size(width = 37.dp, height = 43.dp).clip(RoundedCornerShape(8.dp)).background(c.surfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), null, tint = c.accentText, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
private fun FieldLabel(c: ThrColors, text: String) {
    Text(text, style = thrText(10.sp, FontWeight.Bold, c.faint, tracking = 0.5.sp))
}

// ---------------------------------------------------------------- tokens

private fun armLabel(raw: String): String = when (raw.trim().uppercase()) {
    "RIGHT" -> "Right arm"
    "LEFT" -> "Left arm"
    "" -> "Arm not set"
    else -> raw.trim().lowercase().replaceFirstChar { it.titlecase(Locale.ENGLISH) } + " arm"
}

private fun thrText(
    size: TextUnit,
    weight: FontWeight,
    color: Color,
    line: TextUnit = TextUnit.Unspecified,
    tracking: TextUnit = 0.sp,
) = TextStyle(fontFamily = PlusJakartaSans, fontSize = size, fontWeight = weight, color = color, lineHeight = line, letterSpacing = tracking)

internal data class StatusTone(val soft: Color, val strong: Color, val pill: Color, val pillText: Color)

/** Dark values are taken from the Figma frame; light values reuse the shared PulihGo palette. */
internal data class ThrColors(
    val page: Color,
    val header: Color,
    val logoTile: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val inset: Color,
    val chip: Color,
    val tableHeader: Color,
    val tableFooter: Color,
    val rowDivider: Color,
    val text: Color,
    val muted: Color,
    val faint: Color,
    val divider: Color,
    val accent: Color,
    val accentText: Color,
    val onAccent: Color,
    val accentStrong: Color,
    val onAccentStrong: Color,
    val navSelected: Color,
    val onNavSelected: Color,
    val danger: Color,
    val warnText: Color,
    val selectedRow: Color,
    val followUpRow: Color,
    val active: StatusTone,
    val followUp: StatusTone,
    val idle: StatusTone,
) {
    fun toneFor(status: PatientStatus) = when (status) {
        PatientStatus.ON_TRACK -> active
        PatientStatus.NEEDS_FOLLOW_UP -> followUp
        PatientStatus.NO_SESSIONS -> idle
    }

    companion object {
        val Dark = ThrColors(
            page = Color(0xFF111317),
            header = Color(0xFF0C0E12),
            logoTile = Color(0xFF282A2E),
            surface = Color(0xFF1A1C20),
            surfaceHigh = Color(0xFF1E2024),
            inset = Color(0xFF0C0E12),
            chip = Color(0xFF282A2E),
            tableHeader = Color(0xB30C0E12),
            tableFooter = Color(0xCC0C0E12),
            rowDivider = Color(0xFF1E2024),
            text = Color(0xFFE2E2E8),
            muted = Color(0xFFB9CAC5),
            faint = Color(0xFF84948F),
            divider = Color(0xFF3B4A46),
            accent = Color(0xFF70FFE5),
            accentText = Color(0xFF70FFE5),
            onAccent = Color(0xFF00372F),
            accentStrong = Color(0xFF00E5C9),
            onAccentStrong = Color(0xFF006155),
            navSelected = Color(0xFFE2E2E8),
            onNavSelected = Color(0xFF2F3035),
            danger = Color(0xFFFFB4AB),
            warnText = Color(0xFFFCD34D),
            selectedRow = Color(0x0F70FFE5),
            followUpRow = Color(0x0D451A03),
            active = StatusTone(Color(0x3300E5C9), Color(0xFF70FFE5), Color(0x66022C22), Color(0xFF6EE7B7)),
            followUp = StatusTone(Color(0x33F59E0B), Color(0xFFFBBF24), Color(0x66451A03), Color(0xFFFCD34D)),
            idle = StatusTone(Color(0xFF282A2E), Color(0xFF84948F), Color(0xFF282A2E), Color(0xFFB9CAC5)),
        )
        val Light = ThrColors(
            page = Color(0xFFF9F9F7),
            header = Color(0xFFFFFFFF),
            logoTile = Color(0xFFEAF8F7),
            surface = Color(0xFFFFFFFF),
            surfaceHigh = Color(0xFFEFF4F4),
            inset = Color(0xFFF3F6F6),
            chip = Color(0xFFEAF8F7),
            tableHeader = Color(0xFFF3F6F6),
            tableFooter = Color(0xFFF7F9F9),
            rowDivider = Color(0xFFE4EAEB),
            text = Color(0xFF1A1E21),
            muted = Color(0xFF5D6B72),
            faint = Color(0xFF7B8A90),
            divider = Color(0xFFC9D3D5),
            accent = Color(0xFF0D7C7C),
            accentText = Color(0xFF0D7C7C),
            onAccent = Color(0xFFFFFFFF),
            accentStrong = Color(0xFF0D7C7C),
            onAccentStrong = Color(0xFFFFFFFF),
            navSelected = Color(0xFF1A1E21),
            onNavSelected = Color(0xFFFFFFFF),
            danger = Color(0xFFB3261E),
            warnText = Color(0xFFB45309),
            selectedRow = Color(0x140D7C7C),
            followUpRow = Color(0x14F59E0B),
            active = StatusTone(Color(0x1F0D7C7C), Color(0xFF0D7C7C), Color(0xFFD1FAE5), Color(0xFF047857)),
            followUp = StatusTone(Color(0x29F59E0B), Color(0xFFD97706), Color(0xFFFEF3C7), Color(0xFF92400E)),
            idle = StatusTone(Color(0xFFEFF4F4), Color(0xFF7B8A90), Color(0xFFEFF4F4), Color(0xFF5D6B72)),
        )
    }
}
