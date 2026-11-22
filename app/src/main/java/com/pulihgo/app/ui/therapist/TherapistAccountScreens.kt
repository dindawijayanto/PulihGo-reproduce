package com.pulihgo.app.ui.therapist

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulihgo.app.R
import com.pulihgo.app.ui.account.AccountValidation

@Composable
fun TherapistLoginScreen(
    isDark: Boolean,
    onLogin: (identifier: String, password: String) -> String?,
    onForgotPassword: () -> Unit,
    onPatientLogin: () -> Unit,
) {
    val c = if (isDark) ThrColors.Dark else ThrColors.Light
    var identifier by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var failed by rememberSaveable { mutableStateOf(false) }

    fun submit() {
        error = when {
            identifier.isBlank() -> "Enter your email address or username."
            identifier.contains("@") && !AccountValidation.isValidEmail(identifier) -> "Enter a valid email address."
            password.isBlank() -> "Enter your password."
            else -> onLogin(identifier, password)
        }
        failed = error != null
    }

    PortalGatewayPage(c, Modifier.testTag("therapist_login")) {
        GatewayCard(c) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LogoTile(c, 40)
                Text(
                    "PulihGo",
                    style = thrText(22.sp, FontWeight.SemiBold, c.text),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            PortalChip(c)
            Spacer(Modifier.height(12.dp))
            Text("Welcome back", style = thrText(28.sp, FontWeight.Bold, c.text, line = 34.sp))
            Text(
                "Sign in to access your patient monitoring dashboard.",
                style = thrText(14.sp, FontWeight.Normal, c.muted, line = 20.sp),
                modifier = Modifier.padding(top = 4.dp),
            )
            if (failed) {
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(c.dangerSoft)
                        .padding(14.dp)
                        .testTag("therapist_login_error_banner"),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(painterResource(R.drawable.pg_thr_alert), null, tint = c.danger, modifier = Modifier.size(18.dp))
                    Text(
                        "Unable to sign in. Please check your email and password.",
                        style = thrText(13.sp, FontWeight.SemiBold, c.danger, line = 19.sp),
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            ThrField(
                c = c,
                label = "Email Address",
                value = identifier,
                onValueChange = { identifier = it; error = null },
                placeholder = "therapist@example.com",
                icon = R.drawable.pg_icon_mail,
                keyboardType = KeyboardType.Email,
                isError = failed,
                modifier = Modifier.testTag("therapist_login_identifier"),
            )
            Spacer(Modifier.height(16.dp))
            ThrPasswordField(
                c = c,
                label = "Password",
                value = password,
                onValueChange = { password = it; error = null },
                placeholder = "Enter your password",
                isError = failed,
                modifier = Modifier.testTag("therapist_login_password"),
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    error.orEmpty(),
                    style = thrText(12.sp, FontWeight.SemiBold, c.danger, line = 16.sp),
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
                Text(
                    "Forgot password?",
                    style = thrText(13.sp, FontWeight.SemiBold, c.accentText),
                    modifier = Modifier
                        .clickable(role = Role.Button, onClick = onForgotPassword)
                        .padding(vertical = 4.dp)
                        .testTag("therapist_forgot_link"),
                )
            }
            Spacer(Modifier.height(20.dp))
            AccentButton(
                c = c,
                label = if (failed) "Try Again  →" else "Sign In  →",
                onClick = ::submit,
                modifier = Modifier.testTag("therapist_sign_in"),
            )
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = c.divider.copy(alpha = 0.5f))
            Spacer(Modifier.height(16.dp))
            Text(
                "AUTHORIZED THERAPIST ACCESS ONLY.",
                style = thrText(11.sp, FontWeight.SemiBold, c.muted, tracking = 0.5.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Demo: therapist@pulihgo.demo / demo123",
                style = thrText(11.sp, FontWeight.Normal, c.faint),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Not a therapist?", style = thrText(13.sp, FontWeight.Normal, c.muted))
            Text(
                "Patient sign in",
                style = thrText(13.sp, FontWeight.SemiBold, c.accentText),
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = onPatientLogin)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("therapist_to_patient_login"),
            )
        }
    }
}

@Composable
fun TherapistForgotPasswordScreen(
    isDark: Boolean,
    onBackToLogin: () -> Unit,
) {
    val c = if (isDark) ThrColors.Dark else ThrColors.Light
    var email by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var sentTo by rememberSaveable { mutableStateOf<String?>(null) }

    PortalGatewayPage(c, Modifier.testTag("therapist_forgot")) {
        GatewayCard(c) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(c.chip)
                    .clickable(role = Role.Button, onClick = onBackToLogin),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.pg_icon_arrow_left), "Back to sign in", tint = c.text, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.align(Alignment.CenterHorizontally).size(48.dp).clip(CircleShape).background(c.chip),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(if (sentTo == null) R.drawable.pg_icon_lock else R.drawable.pg_icon_send),
                    null,
                    tint = c.accentText,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            val sent = sentTo
            if (sent == null) {
                Text(
                    "Forgot your password?",
                    style = thrText(22.sp, FontWeight.Bold, c.text, line = 28.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Enter your registered email address to request password reset instructions.",
                    style = thrText(14.sp, FontWeight.Normal, c.muted, line = 20.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                )
                Spacer(Modifier.height(24.dp))
                ThrField(
                    c = c,
                    label = "Email Address",
                    value = email,
                    onValueChange = { email = it; error = null },
                    placeholder = "therapist@example.com",
                    icon = R.drawable.pg_icon_mail,
                    keyboardType = KeyboardType.Email,
                    isError = error != null,
                    modifier = Modifier.testTag("therapist_forgot_email"),
                )
                if (error != null) {
                    Text(
                        error.orEmpty(),
                        style = thrText(12.sp, FontWeight.SemiBold, c.danger),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Spacer(Modifier.height(20.dp))
                AccentButton(
                    c = c,
                    label = "Send Reset Link  ➤",
                    onClick = {
                        if (AccountValidation.isValidEmail(email)) sentTo = email.trim()
                        else error = "Enter a valid email address."
                    },
                    modifier = Modifier.testTag("therapist_send_reset"),
                )
            } else {
                Text(
                    "Check your inbox",
                    style = thrText(22.sp, FontWeight.Bold, c.text, line = 28.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "If a therapist account exists for $sent, reset instructions would be sent there.",
                    style = thrText(14.sp, FontWeight.Normal, c.muted, line = 20.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp).testTag("therapist_reset_sent"),
                )
                Text(
                    "Demo preview: no email has been sent.",
                    style = thrText(12.sp, FontWeight.SemiBold, c.faint),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                Spacer(Modifier.height(20.dp))
                AccentButton(c = c, label = "Back to Sign In", onClick = onBackToLogin)
            }
            Text(
                if (sent == null) "Back to Sign In" else "Use a different email",
                style = thrText(14.sp, FontWeight.Medium, c.muted),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clickable(role = Role.Button) {
                        if (sent == null) onBackToLogin() else sentTo = null
                    }
                    .padding(vertical = 6.dp),
            )
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = c.divider.copy(alpha = 0.5f))
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.pg_icon_shield_check), null, tint = c.muted, modifier = Modifier.size(14.dp))
                Text(
                    "Authorized therapist access only.",
                    style = thrText(12.sp, FontWeight.SemiBold, c.muted),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun PortalGatewayPage(c: ThrColors, modifier: Modifier, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(c.page).then(modifier)) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(c.header)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LogoTile(c, 32)
            Text(
                "PulihGo",
                style = thrText(18.sp, FontWeight.SemiBold, c.text),
                modifier = Modifier.padding(start = 8.dp),
            )
            Text(
                "THERAPIST PORTAL",
                style = thrText(10.sp, FontWeight.SemiBold, c.muted, tracking = 0.6.sp),
                modifier = Modifier
                    .padding(start = 10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(c.chip)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("PulihGo Biomechanical Telemetry", style = thrText(11.sp, FontWeight.SemiBold, c.muted))
            Text("Demo Session", style = thrText(11.sp, FontWeight.SemiBold, c.muted))
        }
    }
}

@Composable
private fun GatewayCard(c: ThrColors, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        Modifier
            .widthIn(max = 440.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.surface)
            .border(1.dp, c.divider.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(24.dp),
        content = content,
    )
}

@Composable
private fun LogoTile(c: ThrColors, sizeDp: Int) {
    Box(
        Modifier.size(sizeDp.dp).clip(RoundedCornerShape(8.dp)).background(c.logoTile),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.onboarding_logo), null, modifier = Modifier.size((sizeDp * 0.75f).dp))
    }
}

@Composable
private fun PortalChip(c: ThrColors) {
    Row(
        Modifier.clip(RoundedCornerShape(9999.dp)).background(c.active.pill).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(c.accentText))
        Text(
            "THERAPIST PORTAL",
            style = thrText(11.sp, FontWeight.SemiBold, c.accentText, tracking = 0.4.sp),
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

@Composable
internal fun AccentButton(c: ThrColors, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(c.accentStrong)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = thrText(17.sp, FontWeight.SemiBold, c.onAccentStrong))
    }
}

@Composable
internal fun ThrField(
    c: ThrColors,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    enabled: Boolean = true,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = thrText(13.sp, FontWeight.SemiBold, c.text), modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            isError = isError,
            placeholder = { Text(placeholder, style = thrText(14.sp, FontWeight.Normal, c.faint)) },
            leadingIcon = { Icon(painterResource(icon), null, tint = c.muted, modifier = Modifier.size(18.dp)) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = thrText(14.sp, FontWeight.Normal, c.text),
            shape = RoundedCornerShape(8.dp),
            colors = thrFieldColors(c),
            modifier = modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun ThrPasswordField(
    c: ThrColors,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = thrText(13.sp, FontWeight.SemiBold, c.text), modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            isError = isError,
            placeholder = { Text(placeholder, style = thrText(14.sp, FontWeight.Normal, c.faint)) },
            leadingIcon = {
                Icon(painterResource(R.drawable.pg_icon_lock), null, tint = if (isError) c.danger else c.muted, modifier = Modifier.size(18.dp))
            },
            trailingIcon = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        painterResource(if (visible) R.drawable.pg_icon_eye_off else R.drawable.pg_icon_eye),
                        if (visible) "Hide password" else "Show password",
                        tint = c.muted,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            textStyle = thrText(14.sp, FontWeight.Normal, c.text),
            shape = RoundedCornerShape(8.dp),
            colors = thrFieldColors(c),
            modifier = modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun thrFieldColors(c: ThrColors) = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = c.inset,
    unfocusedContainerColor = c.inset,
    disabledContainerColor = c.inset,
    errorContainerColor = c.inset,
    focusedBorderColor = c.accentText,
    unfocusedBorderColor = c.divider.copy(alpha = 0.4f),
    disabledBorderColor = c.divider.copy(alpha = 0.25f),
    errorBorderColor = c.danger,
    focusedTextColor = c.text,
    unfocusedTextColor = c.text,
    disabledTextColor = c.muted,
    errorTextColor = c.text,
    cursorColor = c.accentText,
)
