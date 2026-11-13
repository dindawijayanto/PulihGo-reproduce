package com.pulihgo.app.ui.account

import androidx.annotation.DrawableRes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulihgo.app.R
import com.pulihgo.app.ui.theme.PlusJakartaSans

/**
 * PGF-PAT-02 / Patient Hi-Fi Final in Figma (node 157:221, 157:299,
 * 157:721, 157:788, 157:417, 157:701).
 * Demo UI only: no production auth, emails, or encryption are implemented.
 * Account icons use bundled vector drawables; the PAT-01 brand logo is reused.
 * Settings icon/layout alignment references Patient Hi-Fi Final Figma 157:417.
 * Custom vectors are close representations, not exported Figma vector assets.
 */
private val Teal = Color(0xFF0D7C7C)
private val Danger = Color(0xFFD9383A)
private val WarmBg = Color(0xFFF9F9F7)
private val DarkBg = Color(0xFF111717)
private val DarkCard = Color(0xFF202727)
private val Ink = Color(0xFF1A1E21)
private val Muted = Color(0xFF6C757D)
private val Line = Color(0xFFE2E8F0)

private data class AccountPalette(
    val background: Color,
    val card: Color,
    val text: Color,
    val secondary: Color,
    val border: Color
)

private fun palette(isDark: Boolean) = if (isDark) AccountPalette(
    DarkBg, DarkCard, Color.White, Color(0xFFB9C6C6), Color(0xFF526161)
) else AccountPalette(WarmBg, Color.White, Ink, Muted, Line)

@Composable
private fun AccountPage(isDark: Boolean, content: @Composable () -> Unit) {
    val p = palette(isDark)
    Box(Modifier.fillMaxSize().background(p.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) { content() }
    }
}

/**
 * Dedicated viewport-scrolling container for the taller Patient Settings screen.
 * Unlike a single full-screen static form, Settings can exceed device height,
 * so the entire content must be reachable on shorter phones.
 *
 * AccountPage remains unchanged for login/register/forgot screens.
 */
@Composable
private fun SettingsScrollablePage(isDark: Boolean, content: @Composable () -> Unit) {
    val p = palette(isDark)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(p.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp, end = 20.dp, top = 16.dp, bottom = 32.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item(key = "settings_content") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content()
            }
        }
    }
}

@Composable
private fun BrandWordmark(p: AccountPalette, modifier: Modifier = Modifier) {
    Text(
        "P U L I H G O",
        fontSize = 15.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 2.sp,
        color = p.text,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun BrandBadge(p: AccountPalette, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
            .background(p.card).border(1.dp, p.border, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.onboarding_logo),
            contentDescription = "PulihGo",
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun CardSurface(isDark: Boolean, content: @Composable () -> Unit) {
    val p = palette(isDark)
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(p.card)
            .border(1.dp, p.border.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) { content() }
}

@Composable
private fun HeaderText(title: String, subtext: String, p: AccountPalette) {
    Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = p.text,
        textAlign = TextAlign.Center, lineHeight = 32.sp)
    Spacer(Modifier.height(6.dp))
    Text(subtext, fontSize = 14.sp, lineHeight = 22.sp, color = p.secondary,
        textAlign = TextAlign.Center)
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    placeholder: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    @DrawableRes leadingIconRes: Int? = null
) {
    val p = palette(isDark)
    val fieldIcon = leadingIconRes ?: when (label) {
        "Full Name" -> R.drawable.pg_icon_user
        "Confirm Password" -> R.drawable.pg_icon_shield
        "Password" -> R.drawable.pg_icon_lock
        else -> R.drawable.pg_icon_mail
    }
    var visible by rememberSaveable { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label.uppercase(), color = p.secondary, fontSize = 12.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValue,
            singleLine = true,
            placeholder = { Text(placeholder, fontSize = 14.sp, color = p.secondary.copy(alpha = .7f)) },
            leadingIcon = {
                Icon(painter = painterResource(fieldIcon), contentDescription = null,
                    tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
            },
            visualTransformation = if (password && !visible) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (password) KeyboardType.Password else keyboardType),
            trailingIcon = if (password) ({
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        painter = painterResource(if (visible) R.drawable.pg_icon_eye_off else R.drawable.pg_icon_eye),
                        contentDescription = if (visible) "Hide password" else "Show password",
                        tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp)
                    )
                }
            }) else null,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Teal,
                unfocusedBorderColor = p.border,
                focusedTextColor = p.text,
                unfocusedTextColor = p.text,
                focusedContainerColor = p.card,
                unfocusedContainerColor = p.card,
                cursorColor = Teal
            ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = PlusJakartaSans, fontSize = 14.sp
            )
        )
    }
}

@Composable
private fun PrimaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, danger: Boolean = false) {
    val hasArrow = text.contains("→")
    val buttonText = text.replace("→", "").trim()
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = if (danger) Danger else Teal),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(buttonText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            if (hasArrow) {
                Spacer(Modifier.width(8.dp))
                Icon(painterResource(R.drawable.pg_icon_arrow_right), contentDescription = null,
                    modifier = Modifier.size(17.dp), tint = Color.White)
            }
        }
    }
}

@Composable
private fun SmallLink(text: String, onClick: () -> Unit) {
    val hasBackArrow = text.startsWith("←") || text.startsWith("‹")
    val visibleText = text.removePrefix("←").removePrefix("‹").trim()
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 44.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (hasBackArrow) {
                Icon(painterResource(R.drawable.pg_icon_arrow_left), contentDescription = null,
                    tint = Teal, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(visibleText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Teal)
        }
    }
}

@Composable
private fun ErrorText(message: String?) {
    if (message != null) {
        Spacer(Modifier.height(8.dp))
        Text(message, color = Danger, fontSize = 12.sp, lineHeight = 18.sp)
    }
}

@Composable
fun LoginScreen(
    prefillEmail: String = "",
    isDark: Boolean = false,
    onLogin: (email: String, password: String) -> String?,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit
) {
    val p = palette(isDark)
    var email by rememberSaveable(prefillEmail) { mutableStateOf(prefillEmail) }
    var password by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    AccountPage(isDark) {
        Spacer(Modifier.height(24.dp))
        BrandWordmark(p)
        Spacer(Modifier.height(28.dp))
        BrandBadge(p)
        Spacer(Modifier.height(28.dp))
        CardSurface(isDark) {
            HeaderText("Welcome back", "Sign in to continue your recovery journey", p)
            Spacer(Modifier.height(28.dp))
            FormField("Email Address", email, { email = it; error = null }, "name@example.com",
                isDark, keyboardType = KeyboardType.Email, leadingIconRes = R.drawable.pg_icon_at)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("PASSWORD", color = p.secondary, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = .6.sp)
                SmallLink("Forgot password?", onForgotPassword)
            }
            // Password label appears above as part of the linked field row.
            PasswordInput(password, { password = it; error = null }, isDark)
            ErrorText(error)
            Spacer(Modifier.height(20.dp))
            PrimaryAction("Log In  →", onClick = {
                error = if (email.isBlank()) {
                    "Enter your email address or username."
                } else if (email.contains("@") && !AccountValidation.isValidEmail(email)) {
                    "Enter a valid email address."
                } else if (password.isBlank()) {
                    "Enter your password."
                } else onLogin(email, password)
            })
            Spacer(Modifier.height(22.dp))
            HorizontalDivider(color = p.border)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Icon(painterResource(R.drawable.pg_icon_shield_check), contentDescription = null,
                    tint = Teal, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Demo UI · No production authentication is enabled", fontSize = 11.sp,
                    color = p.secondary, textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Don't have an account?", color = p.secondary, fontSize = 13.sp)
            SmallLink("Register as Patient", onRegister)
        }
        Spacer(Modifier.height(12.dp))
        Text("Demo Patient: alya@pulihgo.demo / demo123 · Therapist: therapist@pulihgo.demo / demo123",
            color = p.secondary, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PasswordInput(value: String, onValue: (String) -> Unit, isDark: Boolean) {
    val p = palette(isDark)
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value, onValueChange = onValue,
        // A grey hint indicates an empty field; actual password dots use text color.
        placeholder = {
            Text("••••••••", fontSize = 14.sp, color = p.secondary.copy(alpha = 0.55f))
        },
        singleLine = true,
        leadingIcon = {
            Icon(painterResource(R.drawable.pg_icon_lock), contentDescription = null,
                tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
        },
        visualTransformation = if (visible) androidx.compose.ui.text.input.VisualTransformation.None
        else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(painterResource(if (visible) R.drawable.pg_icon_eye_off else R.drawable.pg_icon_eye),
                    contentDescription = if (visible) "Hide password" else "Show password",
                    tint = Color(0xFF94A3B8), modifier = Modifier.size(20.dp))
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Teal, unfocusedBorderColor = p.border,
            focusedTextColor = p.text, unfocusedTextColor = p.text,
            focusedContainerColor = p.card, unfocusedContainerColor = p.card,
            cursorColor = Teal
        ),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        textStyle = androidx.compose.ui.text.TextStyle(
            fontFamily = PlusJakartaSans, fontSize = 14.sp
        )
    )
}

@Composable
fun RegisterScreen(
    isDark: Boolean = false,
    onBackToLogin: () -> Unit,
    onRegister: (name: String, email: String, password: String) -> String?
) {
    val p = palette(isDark)
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    var accepted by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    AccountPage(isDark) {
        Spacer(Modifier.height(12.dp))
        BrandWordmark(p)
        Spacer(Modifier.height(24.dp))
        Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE6F3F3)),
            contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.pg_icon_user), contentDescription = null,
                tint = Teal, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.height(12.dp))
        HeaderText("Create Patient Account", "Start tracking your arm and forearm rehabilitation", p)
        Spacer(Modifier.height(24.dp))
        CardSurface(isDark) {
            FormField("Full Name", name, { name = it; error = null }, "e.g. Budi Santoso", isDark)
            Spacer(Modifier.height(15.dp))
            FormField("Email Address", email, { email = it; error = null }, "budi@example.com",
                isDark, keyboardType = KeyboardType.Email)
            Spacer(Modifier.height(15.dp))
            FormField("Password", password, { password = it; error = null }, "Create a strong password",
                isDark, password = true)
            Spacer(Modifier.height(15.dp))
            FormField("Confirm Password", confirmation, { confirmation = it; error = null },
                "Re-enter password", isDark, password = true)
            Spacer(Modifier.height(12.dp))
            // Two visual requirements follow the Patient Hi-Fi Final register frame.
            PasswordRequirementHints(password, p)
            Spacer(Modifier.height(10.dp))
            TermsAgreementRow(accepted, { accepted = it }, p)
            ErrorText(error)
            Spacer(Modifier.height(16.dp))
            PrimaryAction("Register  →", onClick = {
                error = AccountValidation.validateRegistration(
                    name, email, password, confirmation, accepted
                )
                if (error == null) error = onRegister(name, email, password)
            })
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Already have an account?", fontSize = 13.sp, color = p.secondary)
            SmallLink("Log In", onBackToLogin)
        }
        Spacer(Modifier.height(18.dp))
    }
}

/** Visual password checklist; validated using the same rules as form submission. */
@Composable
private fun PasswordRequirementHints(password: String, p: AccountPalette) {
    val longEnough = password.length >= 8
    val containsNumberOrSymbol = AccountValidation.containsNumberOrSymbol(password)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (p.card == Color.White) Color(0xFFF6F8F9) else p.border.copy(alpha = 0.15f))
            .border(1.dp, p.border.copy(alpha = 0.42f), RoundedCornerShape(12.dp))
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PasswordRequirementRow("At least 8 characters long", longEnough, p)
        PasswordRequirementRow("Contains at least one number or symbol", containsNumberOrSymbol, p)
    }
}

@Composable
private fun PasswordRequirementRow(label: String, satisfied: Boolean, p: AccountPalette) {
    val fg = if (satisfied) Teal else p.secondary
    val circleColor = if (satisfied) Color(0xFFE0F5F3) else Color(0xFFE8EDF2)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(18.dp).clip(CircleShape).background(circleColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.pg_icon_check),
                contentDescription = if (satisfied) "Requirement met" else "Requirement not met",
                tint = if (satisfied) Teal else Color(0xFF94A3B8),
                modifier = Modifier.size(12.dp)
            )
        }
        Spacer(Modifier.width(9.dp))
        Text(label, color = fg, fontSize = 12.sp, lineHeight = 17.sp)
    }
}

@Composable
private fun TermsAgreementRow(checked: Boolean, onChecked: (Boolean) -> Unit, p: AccountPalette) {
    val label = buildAnnotatedString {
        append("I agree to the ")
        withStyle(SpanStyle(color = Teal, textDecoration = TextDecoration.Underline)) {
            append("Terms of Service & Privacy Policy")
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = checked,
            onCheckedChange = onChecked,
            colors = CheckboxDefaults.colors(checkedColor = Teal)
        )
        Text(
            text = label,
            modifier = Modifier.clickable { onChecked(!checked) },
            color = p.secondary,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
fun ForgotPasswordScreen(
    isDark: Boolean = false,
    onBackToLogin: () -> Unit,
    onRequestReset: (email: String) -> Unit
) {
    val p = palette(isDark)
    var email by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    AccountPage(isDark) {
        Box(Modifier.fillMaxWidth().height(52.dp)) {
            Box(
                Modifier.align(Alignment.CenterStart).size(48.dp).clip(CircleShape)
                    .background(p.card).border(1.dp, p.border.copy(alpha = .7f), CircleShape)
                    .clickable(onClick = onBackToLogin),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.pg_icon_back), contentDescription = "Back to Login",
                    tint = p.text, modifier = Modifier.size(24.dp))
            }
            Text("P U L I H G O", color = p.text, fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp,
                modifier = Modifier.align(Alignment.Center))
        }
        Spacer(Modifier.height(112.dp))
        CardSurface(isDark) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFE6F3F3)),
                contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.pg_icon_lock), contentDescription = null,
                    tint = Teal, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(20.dp))
            HeaderText("Forgot Password?",
                "Enter your registered email address and we'll send you instructions to reset your password.", p)
            Spacer(Modifier.height(30.dp))
            FormField("Email Address", email, { email = it; error = null },
                "name@example.com", isDark, keyboardType = KeyboardType.Email)
            ErrorText(error)
            Spacer(Modifier.height(18.dp))
            PrimaryAction("Send Reset Link  →", onClick = {
                error = if (AccountValidation.isValidEmail(email)) null else "Enter a valid email address."
                if (error == null) onRequestReset(email)
            })
            Spacer(Modifier.height(12.dp))
            SmallLink("← Return to Login", onBackToLogin)
            Text("Demo preview: a real reset email will not be sent.", color = p.secondary,
                fontSize = 11.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ResetLinkSentScreen(email: String, isDark: Boolean = false, onBackToLogin: () -> Unit) {
    val p = palette(isDark)
    var resendCount by rememberSaveable { mutableStateOf(0) }
    AccountPage(isDark) {
        Spacer(Modifier.height(116.dp))
        CardSurface(isDark) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFE6F3F3)),
                contentAlignment = Alignment.Center) {
                Icon(painterResource(R.drawable.pg_icon_send), contentDescription = null,
                    tint = Teal, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(16.dp))
            HeaderText("Reset Link Sent",
                "If an account exists for this email, reset instructions would be sent to:", p)
            Spacer(Modifier.height(12.dp))
            Text(email, color = p.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF4F9F8)).border(1.dp, Color(0xFFD7E9E8), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(painterResource(R.drawable.pg_icon_info), contentDescription = null,
                    tint = Teal, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(10.dp))
                Text("Demo preview only — no email has been sent. Your account data remains unchanged.",
                    fontSize = 12.sp, lineHeight = 18.sp, color = p.secondary)
            }
            Spacer(Modifier.height(24.dp))
            PrimaryAction("Back to Login", onBackToLogin)
            Spacer(Modifier.height(8.dp))
            SmallLink("Resend email", onClick = { resendCount++ })
            if (resendCount > 0) Text("Demo resend simulated ($resendCount). No email sent.",
                fontSize = 11.sp, color = p.secondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun SettingsScreen(
    name: String,
    email: String,
    selectedAppearance: String,
    isDark: Boolean = false,
    onAppearanceChange: (String) -> Unit,
    onBack: () -> Unit,
    onLogOut: () -> Unit
) {
    val p = palette(isDark)
    var showLogOut by rememberSaveable { mutableStateOf(false) }
    var openInfo by rememberSaveable { mutableStateOf<String?>(null) }

    SettingsScrollablePage(isDark) {
        // Settings header: a circular back control and a truly centered title.
        Box(Modifier.fillMaxWidth().height(56.dp)) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(p.card)
                    .border(1.dp, p.border.copy(alpha = .65f), CircleShape)
                    .clickable(onClick = onBack)
                    .align(Alignment.CenterStart),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(R.drawable.pg_icon_back),
                    contentDescription = "Back",
                    tint = p.text,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                "Settings", color = p.text, fontSize = 24.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(Modifier.height(22.dp))

        // Profile card -- matches the two-row final Hi-Fi composition.
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(p.card)
                .border(1.dp, p.border.copy(alpha = .45f), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(64.dp)
                        .clip(CircleShape)
                        .border(2.dp, Teal, CircleShape)
                        .background(Color(0xFFE8F7F7)),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = name.trim().split(Regex("\\s+"))
                        .take(2).mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
                        .joinToString("").ifBlank { "P" }
                    Text(initials, color = Teal, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(name, color = p.text, fontWeight = FontWeight.Bold, fontSize = 20.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Patient • Arm Rehabilitation", color = p.secondary, fontSize = 13.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(email, color = p.secondary, fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = p.border.copy(alpha = .6f))
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Therapy Status", fontSize = 13.sp, color = p.secondary)
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFE9FBF3))
                        .border(1.dp, Color(0xFF87EBC3), CircleShape)
                        .padding(horizontal = 11.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF08B981)))
                    Spacer(Modifier.width(7.dp))
                    Text("Active Program", color = Color(0xFF007A60),
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Appearance is a white card, with three icon tiles and a selected badge.
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(p.card)
                .border(1.dp, p.border.copy(alpha = .45f), RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            SectionCaption("APPEARANCE", p)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("System", "Light", "Dark").forEach { mode ->
                    val selected = selectedAppearance == mode
                    val iconRes = when(mode) {
                        "System" -> R.drawable.pg_icon_phone
                        "Light" -> R.drawable.pg_icon_sun
                        else -> R.drawable.pg_icon_moon
                    }
                    Box(
                        modifier = Modifier.weight(1f).height(108.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) Teal.copy(alpha = 0.06f) else p.background)
                            .border(if (selected) 2.dp else 1.dp,
                                if (selected) Teal else p.border, RoundedCornerShape(14.dp))
                            .clickable { onAppearanceChange(mode) }
                    ) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(painterResource(iconRes), contentDescription = "$mode appearance",
                                tint = if (selected) Teal else p.text,
                                modifier = Modifier.size(23.dp))
                            Text(mode, color = if (selected) Teal else p.text,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp)
                            Text(when {
                                selected -> "Active"
                                mode == "System" -> "Auto"
                                else -> "Standard"
                            }, color = if (selected) Teal else p.secondary, fontSize = 10.sp)
                        }
                        if (selected) {
                            Box(
                                modifier = Modifier.align(Alignment.TopEnd).padding(5.dp)
                                    .size(19.dp).clip(CircleShape).background(Teal),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(painterResource(R.drawable.pg_icon_check),
                                    contentDescription = "Selected", tint = Color.White,
                                    modifier = Modifier.size(13.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Three separate semantic icons, each in the correct pastel circle.
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(p.card)
                .border(1.dp, p.border.copy(alpha = .45f), RoundedCornerShape(20.dp))
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            SectionCaption("HELP & SUPPORT", p)
            Spacer(Modifier.height(16.dp))
            SettingsActionRow("Help & FAQ", R.drawable.pg_icon_help,
                Color(0xFF008B8A), Color(0xFFECFAF9), p) {
                openInfo = "Help & FAQ"
            }
            HorizontalDivider(color = p.border.copy(alpha = .55f))
            SettingsActionRow("Exercise Safety Guidelines", R.drawable.pg_icon_shield,
                Color(0xFF0C9E6E), Color(0xFFEBFAF3), p) {
                openInfo = "Exercise Safety Guidelines"
            }
            HorizontalDivider(color = p.border.copy(alpha = .55f))
            SettingsActionRow("Contact Therapist", R.drawable.pg_icon_chat,
                Color(0xFF2976F3), Color(0xFFEDF4FF), p) {
                openInfo = "Contact Therapist"
            }
        }

        Spacer(Modifier.height(22.dp))

        OutlinedButton(
            onClick = { showLogOut = true },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            border = BorderStroke(1.5.dp, Color(0xFFF59A9D)),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)
        ) {
            Icon(painterResource(R.drawable.pg_icon_logout),
                contentDescription = null, tint = Danger, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(9.dp))
            Text("Log Out", color = Danger, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(18.dp))
        Text("PulihGo Patient App • v1.4.2", color = p.secondary.copy(alpha = .65f),
            fontSize = 12.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
    }

    if (showLogOut) {
        Dialog(onDismissRequest = { showLogOut = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 342.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(p.card).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(56.dp).clip(CircleShape)
                        .background(Color(0xFFFDE8E8))
                        .border(1.dp, Color(0xFFF8B4B4), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.pg_icon_logout),
                        contentDescription = null, tint = Danger,
                        modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text("Log out?", color = p.text, fontWeight = FontWeight.Bold,
                    fontSize = 22.sp)
                Spacer(Modifier.height(10.dp))
                Text("Are you sure you want to log out of your PulihGo account? Your exercise streak and progress data will remain securely saved.",
                    color = p.secondary, fontSize = 14.sp, lineHeight = 21.sp,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { showLogOut = false; onLogOut() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Danger),
                    shape = CircleShape
                ) { Text("Log Out", fontWeight = FontWeight.Bold, color = Color.White) }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { showLogOut = false },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F4F8)),
                    shape = CircleShape
                ) { Text("Cancel", fontWeight = FontWeight.Bold, color = Ink) }
            }
        }
    }

    openInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { openInfo = null },
            title = { Text(info) },
            text = { Text(when (info) {
                "Exercise Safety Guidelines" -> "Stop exercising if you feel pain and follow your therapist's instructions. PulihGo does not replace medical advice."
                "Contact Therapist" -> "Therapist communication will be connected during team integration. No message has been sent."
                else -> "Support content will be connected during team integration."
            }) },
            confirmButton = { TextButton(onClick = { openInfo = null }) { Text("OK") } },
            containerColor = p.card, titleContentColor = p.text, textContentColor = p.secondary
        )
    }
}

@Composable
private fun SectionCaption(text: String, p: AccountPalette) {
    Text(text, color = p.secondary, fontSize = 12.sp, letterSpacing = .8.sp,
        fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun SettingsActionRow(
    label: String,
    iconRes: Int,
    iconColor: Color,
    iconBackground: Color,
    p: AccountPalette,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .heightIn(min = 70.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(iconBackground),
            contentAlignment = Alignment.Center) {
            Icon(painterResource(iconRes), contentDescription = null,
                tint = iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, color = p.text, fontSize = 14.sp,
            modifier = Modifier.weight(1f), maxLines = 1,
            overflow = TextOverflow.Ellipsis)
        Icon(painterResource(R.drawable.pg_icon_chevron_right),
            contentDescription = null, tint = Color(0xFF9CA3AF),
            modifier = Modifier.size(18.dp))
    }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 924)
@Composable
private fun LoginPreview() {
    LoginScreen(onLogin = { _, _ -> null }, onRegister = {}, onForgotPassword = {})
}

@Preview(showBackground = true, widthDp = 412, heightDp = 924)
@Composable
private fun SettingsPreview() {
    SettingsScreen("Budi Santoso", "patient@pulihgo.demo", "Light",
        onAppearanceChange = {}, onBack = {}, onLogOut = {})
}
