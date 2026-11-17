package com.pulihgo.app.ui.therapist

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulihgo.app.R
import com.pulihgo.app.ui.theme.PlusJakartaSans
import java.util.Locale

/**
 * Shared "Clinical Hub" chrome for the Therapist portal screens (Figma Therapist Final):
 * header with nav pill, colour tokens, typography helper and small shared widgets.
 */
enum class TherapistTab(val label: String) {
    DASHBOARD("Dashboard"),
    THERAPY("Therapy"),
    REPORTS("Reports"),
    SETTINGS("Settings"),
}

/** Settings belongs to PGF-THR-01 (Dinda) and is shown but not navigable yet. */
internal val EnabledTherapistTabs = setOf(TherapistTab.DASHBOARD, TherapistTab.THERAPY, TherapistTab.REPORTS)

@Composable
internal fun ClinicalHeader(
    c: ThrColors,
    therapistName: String,
    selected: TherapistTab,
    onNavigate: (TherapistTab) -> Unit,
    onLogout: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
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
            // Avatar doubles as the account menu so the therapist can always log out
            // (Settings / profile is PGF-THR-01).
            Box {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(c.accent)
                        .clickable(role = Role.Button) { menuOpen = true }
                        .semantics { contentDescription = "Account menu" }
                        .testTag("therapist_account_menu"),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.pg_thr_user), contentDescription = null, tint = c.onAccent, modifier = Modifier.size(13.dp))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Log out", style = thrText(14.sp, FontWeight.Medium, Color.Unspecified)) },
                        leadingIcon = { Icon(painterResource(R.drawable.pg_icon_logout), null, modifier = Modifier.size(18.dp)) },
                        onClick = {
                            menuOpen = false
                            onLogout()
                        },
                    )
                }
            }
        }
        Row(
            Modifier
                .clip(RoundedCornerShape(9999.dp))
                .background(c.surface)
                .padding(4.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TherapistTab.entries.forEach { tab ->
                NavPill(
                    c = c,
                    label = tab.label,
                    selected = tab == selected,
                    enabled = tab in EnabledTherapistTabs,
                    onClick = { onNavigate(tab) },
                )
            }
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
            .alpha(if (enabled || selected) 1f else 0.45f)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            label,
            style = if (selected) thrText(14.sp, FontWeight.Medium, c.onNavSelected)
            else thrText(12.sp, FontWeight.SemiBold, c.muted),
        )
    }
}

/** Breadcrumb + page title used at the top of each portal screen. */
@Composable
internal fun PortalBreadcrumb(c: ThrColors, section: String, page: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(section, style = thrText(11.sp, FontWeight.SemiBold, c.muted, tracking = 0.6.sp))
        Text(" / ", style = thrText(11.sp, FontWeight.SemiBold, c.faint))
        Text(page, style = thrText(11.sp, FontWeight.Bold, c.accentText, tracking = 0.6.sp))
    }
}

@Composable
internal fun PortalFooter(c: ThrColors) {
    Text(
        "PulihGo Digital Rehabilitation Platform • Therapist Portal",
        style = thrText(11.sp, FontWeight.SemiBold, c.muted),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).navigationBarsPadding(),
    )
}

@Composable
internal fun StatusPill(c: ThrColors, status: PatientStatus) {
    val tone = c.toneFor(status)
    val label = when (status) {
        PatientStatus.ON_TRACK -> "Active"
        PatientStatus.NEEDS_FOLLOW_UP -> "Needs Follow-up"
        PatientStatus.NO_SESSIONS -> "No Sessions"
    }
    TonePill(tone, label)
}

@Composable
internal fun TonePill(tone: StatusTone, label: String) {
    Row(
        Modifier.clip(RoundedCornerShape(9999.dp)).background(tone.pill).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(tone.strong))
        Text(label, style = thrText(11.sp, FontWeight.SemiBold, tone.pillText), modifier = Modifier.padding(start = 6.dp))
    }
}

/** Card with an uppercase label, a value slot, a caption and an icon tile (Figma KPI / insight cards). */
@Composable
internal fun InsightCard(
    c: ThrColors,
    label: String,
    @DrawableRes icon: Int,
    value: @Composable () -> Unit,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
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

/** Large KPI number with a smaller unit, e.g. "62°  peak". */
@Composable
internal fun KpiValue(c: ThrColors, value: String, unit: String?, muted: Boolean = false) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, style = thrText(28.sp, FontWeight.Bold, if (muted) c.faint else c.text, line = 34.sp))
        if (unit != null) {
            Text(unit, style = thrText(12.sp, FontWeight.SemiBold, c.muted), modifier = Modifier.padding(start = 6.dp, bottom = 6.dp))
        }
    }
}

// ---------------------------------------------------------------- tokens

internal fun armLabel(raw: String): String = when (raw.trim().uppercase()) {
    "RIGHT" -> "Right arm"
    "LEFT" -> "Left arm"
    "" -> "Arm not set"
    else -> raw.trim().lowercase().replaceFirstChar { it.titlecase(Locale.ENGLISH) } + " arm"
}

internal fun thrText(
    size: TextUnit,
    weight: FontWeight,
    color: Color,
    line: TextUnit = TextUnit.Unspecified,
    tracking: TextUnit = 0.sp,
) = TextStyle(fontFamily = PlusJakartaSans, fontSize = size, fontWeight = weight, color = color, lineHeight = line, letterSpacing = tracking)

internal data class StatusTone(val soft: Color, val strong: Color, val pill: Color, val pillText: Color)

/** Dark values are taken from the Figma frames; light values reuse the shared PulihGo palette. */
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
    val dangerSoft: Color,
    val warnText: Color,
    val selectedRow: Color,
    val followUpRow: Color,
    val chartTrack: Color,
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
            dangerSoft = Color(0x3393000A),
            warnText = Color(0xFFFCD34D),
            selectedRow = Color(0x0F70FFE5),
            followUpRow = Color(0x0D451A03),
            chartTrack = Color(0xFF333539),
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
            dangerSoft = Color(0x1AB3261E),
            warnText = Color(0xFFB45309),
            selectedRow = Color(0x140D7C7C),
            followUpRow = Color(0x14F59E0B),
            chartTrack = Color(0xFFE2E9EA),
            active = StatusTone(Color(0x1F0D7C7C), Color(0xFF0D7C7C), Color(0xFFD1FAE5), Color(0xFF047857)),
            followUp = StatusTone(Color(0x29F59E0B), Color(0xFFD97706), Color(0xFFFEF3C7), Color(0xFF92400E)),
            idle = StatusTone(Color(0xFFEFF4F4), Color(0xFF7B8A90), Color(0xFFEFF4F4), Color(0xFF5D6B72)),
        )
    }
}
