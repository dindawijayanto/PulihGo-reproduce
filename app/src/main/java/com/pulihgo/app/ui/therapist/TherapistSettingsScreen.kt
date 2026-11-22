package com.pulihgo.app.ui.therapist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulihgo.app.R
import com.pulihgo.app.data.model.User
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TherapistSettingsScreen(
    user: User,
    profile: TherapistProfile,
    preferences: TherapistPreferences,
    assignedPatientCount: Int,
    isDark: Boolean,
    today: LocalDate,
    onSaveProfile: (TherapistProfile) -> String?,
    onPreferencesChange: (TherapistPreferences) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onChangePassword: (current: String, new: String, confirmation: String) -> String?,
    onViewCaseload: () -> Unit,
    onNavigate: (TherapistTab) -> Unit,
    onLogout: () -> Unit,
) {
    val c = if (isDark) ThrColors.Dark else ThrColors.Light
    var fullName by rememberSaveable(profile) { mutableStateOf(profile.fullName) }
    var email by rememberSaveable(profile) { mutableStateOf(profile.email) }
    var specialization by rememberSaveable(profile) { mutableStateOf(profile.specialization) }
    var affiliation by rememberSaveable(profile) { mutableStateOf(profile.affiliation) }
    var profileError by rememberSaveable { mutableStateOf<String?>(null) }
    var profileSaved by rememberSaveable { mutableStateOf(false) }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var passwordUpdated by rememberSaveable { mutableStateOf(false) }
    var showLogout by rememberSaveable { mutableStateOf(false) }

    val edited = TherapistProfile(fullName, email, specialization, affiliation)
    val dirty = TherapistSettingsLogic.hasChanges(profile, edited)

    fun save() {
        profileError = onSaveProfile(edited)
        profileSaved = profileError == null
    }
    fun edit(update: () -> Unit) {
        update()
        profileError = null
        profileSaved = false
    }

    Column(Modifier.fillMaxSize().background(c.page).testTag("therapist_settings")) {
        ClinicalHeader(c, profile.fullName, TherapistTab.SETTINGS, onNavigate, onLogout)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { SettingsTitle(c, today, dirty, onSave = ::save) }
            item {
                SettingsCard(c, R.drawable.pg_icon_user, "THERAPIST PROFILE", trailing = { TonePill(c.active, "Demo Account") }) {
                    IdentityBox(c, user, profile)
                    ThrField(
                        c = c,
                        label = "FULL LEGAL NAME",
                        value = fullName,
                        onValueChange = { edit { fullName = it } },
                        placeholder = "Full name",
                        icon = R.drawable.pg_thr_user,
                        modifier = Modifier.testTag("therapist_settings_name"),
                    )
                    ThrField(
                        c = c,
                        label = "PROFESSIONAL EMAIL",
                        value = email,
                        onValueChange = { edit { email = it } },
                        placeholder = "name@clinic.org",
                        icon = R.drawable.pg_icon_at,
                        keyboardType = KeyboardType.Email,
                        modifier = Modifier.testTag("therapist_settings_email"),
                    )
                    ThrField(
                        c = c,
                        label = "CLINICAL SPECIALIZATION",
                        value = specialization,
                        onValueChange = { edit { specialization = it } },
                        placeholder = "Specialization",
                        icon = R.drawable.pg_thr_compliance,
                    )
                    ReadOnlyField(
                        c = c,
                        label = "ACTIVE PATIENT LOAD",
                        value = TherapistSettingsLogic.patientLoadLabel(assignedPatientCount),
                    )
                    ThrField(
                        c = c,
                        label = "PRIMARY CLINICAL AFFILIATION",
                        value = affiliation,
                        onValueChange = { edit { affiliation = it } },
                        placeholder = "Clinic or hospital",
                        icon = R.drawable.pg_home_home,
                    )
                    profileError?.let {
                        Text(it, style = thrText(12.sp, FontWeight.SemiBold, c.danger), modifier = Modifier.testTag("therapist_settings_error"))
                    }
                    if (profileSaved) {
                        Text(
                            "Profile saved on this device.",
                            style = thrText(12.sp, FontWeight.SemiBold, c.accentText),
                            modifier = Modifier.testTag("therapist_settings_saved"),
                        )
                    }
                    Box(Modifier.alpha(if (dirty) 1f else 0.5f)) {
                        AccentButton(
                            c = c,
                            label = "Update Profile Information",
                            onClick = { if (dirty) save() },
                            modifier = Modifier.testTag("therapist_settings_update_profile"),
                        )
                    }
                    HorizontalDivider(color = c.divider.copy(alpha = 0.4f))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Signed in as @${user.username}",
                            style = thrText(12.sp, FontWeight.SemiBold, c.muted),
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "View Caseload",
                            style = thrText(12.sp, FontWeight.SemiBold, c.accentText),
                            modifier = Modifier.clickable(role = Role.Button, onClick = onViewCaseload).padding(4.dp),
                        )
                    }
                }
            }
            item {
                SettingsCard(c, R.drawable.pg_home_settings, "SYSTEM PREFERENCES") {
                    PreferenceRow(
                        c = c,
                        title = "Dark Mode",
                        caption = "Dark clinical interface for low-light reading",
                        tag = "therapist_pref_dark",
                        checked = isDark,
                        onCheckedChange = onDarkModeChange,
                    )
                    HorizontalDivider(color = c.divider.copy(alpha = 0.3f))
                    PreferenceRow(
                        c = c,
                        title = "Telemetry Audio Alerts",
                        caption = "Audible warning when a patient exceeds the range-of-motion ceiling",
                        tag = "therapist_pref_audio",
                        checked = preferences.audioAlerts,
                        onCheckedChange = { onPreferencesChange(preferences.copy(audioAlerts = it)) },
                    )
                    HorizontalDivider(color = c.divider.copy(alpha = 0.3f))
                    LanguageRow(c, preferences.language) { onPreferencesChange(preferences.copy(language = it)) }
                    HorizontalDivider(color = c.divider.copy(alpha = 0.3f))
                    PreferenceRow(
                        c = c,
                        title = "Auto-Sync Patient Sessions",
                        caption = "Refresh patient sessions saved on this device automatically",
                        tag = "therapist_pref_sync",
                        checked = preferences.autoSync,
                        onCheckedChange = { onPreferencesChange(preferences.copy(autoSync = it)) },
                    )
                }
            }
            item {
                SettingsCard(c, R.drawable.pg_icon_shield, "SECURITY & ACTIVE SESSION") {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(c.inset).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(c.chip),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(painterResource(R.drawable.pg_icon_phone), null, tint = c.accentText, modifier = Modifier.size(18.dp))
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text("This device", style = thrText(16.sp, FontWeight.SemiBold, c.text))
                            Text("Local demo session • @${user.username}", style = thrText(12.sp, FontWeight.Medium, c.muted))
                        }
                        TonePill(c.active, "Active")
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("Account Password", style = thrText(16.sp, FontWeight.SemiBold, c.text))
                            Text(
                                if (passwordUpdated) "Updated just now • resets when the app restarts"
                                else "Demo password • changes last until the app restarts",
                                style = thrText(12.sp, FontWeight.Medium, c.muted, line = 16.sp),
                            )
                        }
                        OutlineChipButton(c, "Update Password", Modifier.testTag("therapist_update_password")) { showPassword = true }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(c.dangerSoft)
                            .border(1.dp, c.danger.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(painterResource(R.drawable.pg_thr_warning), null, tint = c.danger, modifier = Modifier.size(16.dp))
                                Text(
                                    "Session Sign Out",
                                    style = thrText(16.sp, FontWeight.SemiBold, c.danger),
                                    modifier = Modifier.padding(start = 6.dp),
                                )
                            }
                            Text(
                                "End this session and return to the therapist sign in",
                                style = thrText(12.sp, FontWeight.Medium, c.muted, line = 16.sp),
                            )
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(c.danger)
                                .clickable(role = Role.Button) { showLogout = true }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("therapist_settings_logout"),
                        ) {
                            Text("Log Out", style = thrText(13.sp, FontWeight.SemiBold, c.page))
                        }
                    }
                }
            }
            item { PortalFooter(c) }
        }
    }

    if (showPassword) {
        ChangePasswordDialog(
            c = c,
            onDismiss = { showPassword = false },
            onSubmit = { current, new, confirmation ->
                val error = onChangePassword(current, new, confirmation)
                if (error == null) {
                    showPassword = false
                    passwordUpdated = true
                }
                error
            },
        )
    }

    if (showLogout) {
        AlertDialog(
            onDismissRequest = { showLogout = false },
            containerColor = c.surface,
            title = { Text("Log out of the portal?", style = thrText(18.sp, FontWeight.Bold, c.text)) },
            text = {
                Text(
                    "You will return to the therapist sign in. Patient sessions on this device are kept.",
                    style = thrText(14.sp, FontWeight.Normal, c.muted, line = 20.sp),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogout = false
                        onLogout()
                    },
                    modifier = Modifier.testTag("therapist_logout_confirm"),
                ) { Text("Log Out", style = thrText(14.sp, FontWeight.SemiBold, c.danger)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogout = false }) {
                    Text("Cancel", style = thrText(14.sp, FontWeight.SemiBold, c.muted))
                }
            },
        )
    }
}

@Composable
private fun SettingsTitle(c: ThrColors, today: LocalDate, dirty: Boolean, onSave: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PortalBreadcrumb(c, "SETTINGS", "GENERAL")
        Text("Settings & Account", style = thrText(30.sp, FontWeight.Bold, c.text, line = 38.sp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.ENGLISH)),
                style = thrText(12.sp, FontWeight.SemiBold, c.muted),
                modifier = Modifier.clip(RoundedCornerShape(9999.dp)).background(c.chip).padding(horizontal = 12.dp, vertical = 6.dp),
            )
            Box(Modifier.alpha(if (dirty) 1f else 0.5f)) {
                OutlineChipButton(c, "Save Changes", Modifier.testTag("therapist_settings_save")) { if (dirty) onSave() }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    c: ThrColors,
    icon: Int,
    title: String,
    trailing: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.surface).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), null, tint = c.accentText, modifier = Modifier.size(16.dp))
            Text(
                title,
                style = thrText(12.sp, FontWeight.SemiBold, c.muted, tracking = 0.6.sp),
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
            trailing()
        }
        content()
    }
}

@Composable
private fun IdentityBox(c: ThrColors, user: User, profile: TherapistProfile) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.inset).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)).background(c.accentStrong),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                TherapistSettingsLogic.initials(profile.fullName),
                style = thrText(24.sp, FontWeight.Bold, c.onAccentStrong),
            )
        }
        Column(Modifier.weight(1f).padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                profile.fullName,
                style = thrText(20.sp, FontWeight.SemiBold, c.text, line = 26.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("therapist_settings_display_name"),
            )
            Text("Physiotherapist • ${profile.specialization}", style = thrText(12.sp, FontWeight.Normal, c.muted, line = 16.sp))
            Text(
                "ID: ${user.id}",
                style = thrText(11.sp, FontWeight.SemiBold, c.muted, tracking = 0.4.sp),
                modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(4.dp)).background(c.chip).padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun ReadOnlyField(c: ThrColors, label: String, value: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = thrText(13.sp, FontWeight.SemiBold, c.text), modifier = Modifier.padding(bottom = 8.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(c.inset).padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.pg_thr_reps), null, tint = c.muted, modifier = Modifier.size(18.dp))
            Text(
                value,
                style = thrText(17.sp, FontWeight.SemiBold, c.accentText),
                modifier = Modifier.padding(start = 12.dp).testTag("therapist_settings_patient_load"),
            )
        }
    }
}

@Composable
private fun PreferenceRow(
    c: ThrColors,
    title: String,
    caption: String,
    tag: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = thrText(17.sp, FontWeight.SemiBold, c.text, line = 22.sp))
            Text(caption, style = thrText(12.sp, FontWeight.Normal, c.muted, line = 17.sp))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = c.accentStrong,
                checkedThumbColor = Color.White,
                uncheckedTrackColor = c.chip,
                uncheckedThumbColor = c.muted,
                uncheckedBorderColor = c.divider,
            ),
            modifier = Modifier.testTag(tag),
        )
    }
}

@Composable
private fun LanguageRow(c: ThrColors, selected: TherapistLanguage, onSelect: (TherapistLanguage) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text("Clinical Terminology & Language", style = thrText(17.sp, FontWeight.SemiBold, c.text, line = 22.sp))
            Text("Preferred language for clinical notes", style = thrText(12.sp, FontWeight.Normal, c.muted, line = 17.sp))
        }
        Box {
            OutlineChipButton(c, selected.label, Modifier.testTag("therapist_pref_language")) { open = true }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                TherapistLanguage.entries.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(language.label, style = thrText(14.sp, FontWeight.Medium, Color.Unspecified)) },
                        onClick = {
                            open = false
                            onSelect(language)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun OutlineChipButton(c: ThrColors, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(c.chip)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, style = thrText(13.sp, FontWeight.SemiBold, c.text))
    }
}

@Composable
private fun ChangePasswordDialog(
    c: ThrColors,
    onDismiss: () -> Unit,
    onSubmit: (current: String, new: String, confirmation: String) -> String?,
) {
    var current by rememberSaveable { mutableStateOf("") }
    var new by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text("Update Password", style = thrText(18.sp, FontWeight.Bold, c.text)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ThrPasswordField(c, "Current password", current, { current = it; error = null }, "Current password",
                    Modifier.testTag("therapist_password_current"))
                ThrPasswordField(c, "New password", new, { new = it; error = null }, "At least 8 characters",
                    Modifier.testTag("therapist_password_new"))
                ThrPasswordField(c, "Confirm new password", confirmation, { confirmation = it; error = null }, "Repeat new password",
                    Modifier.testTag("therapist_password_confirm"))
                Text(
                    error ?: "Demo only: the new password lasts until the app restarts.",
                    style = thrText(12.sp, FontWeight.SemiBold, if (error != null) c.danger else c.faint, line = 16.sp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { error = onSubmit(current, new, confirmation) },
                modifier = Modifier.testTag("therapist_password_submit"),
            ) { Text("Update", style = thrText(14.sp, FontWeight.SemiBold, c.accentText)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", style = thrText(14.sp, FontWeight.SemiBold, c.muted)) }
        },
    )
}
