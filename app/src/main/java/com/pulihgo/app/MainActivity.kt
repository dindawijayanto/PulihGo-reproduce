package com.pulihgo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowInsetsControllerCompat
import java.time.LocalTime
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pulihgo.app.ui.account.DemoAuthRepository
import com.pulihgo.app.ui.account.ForgotPasswordScreen
import com.pulihgo.app.ui.account.LoginScreen
import com.pulihgo.app.ui.account.RegisterScreen
import com.pulihgo.app.ui.account.ResetLinkSentScreen
import com.pulihgo.app.ui.account.SettingsScreen
import com.pulihgo.app.ui.home.HomeExerciseSession
import com.pulihgo.app.ui.home.PatientHomeScreen
import com.pulihgo.app.ui.onboarding.OnboardingScreen
import com.pulihgo.app.ui.theme.PulihGoTheme
import java.time.LocalDate

private const val PREF_FILE = "pulihgo_local_prefs"
private const val ONBOARDING_SEEN_KEY = "pulihgo_onboarding_seen"
private const val APPEARANCE_KEY = "pulihgo_appearance_mode"

private enum class PatientRoute {
    ONBOARDING, LOGIN, REGISTER, FORGOT, LINK_SENT, HOME, SETTINGS,
    EXERCISE_HANDOFF, SUMMARY_HANDOFF
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferences = getSharedPreferences(PREF_FILE, MODE_PRIVATE)

        setContent {
            val auth = remember { DemoAuthRepository() }
            var route by rememberSaveable {
                mutableStateOf(
                    if (preferences.getBoolean(ONBOARDING_SEEN_KEY, false))
                        PatientRoute.LOGIN else PatientRoute.ONBOARDING
                )
            }
            var accountName by rememberSaveable { mutableStateOf("Budi Santoso") }
            var accountEmail by rememberSaveable { mutableStateOf(DemoAuthRepository.DEMO_EMAIL) }
            var draftEmail by rememberSaveable { mutableStateOf("") }
            var appearance by rememberSaveable {
                mutableStateOf(preferences.getString(APPEARANCE_KEY, "Light") ?: "Light")
            }
            // Only a local demo of the Home Session state. Never presented as sensor data.
            // Erza's session repository will replace this in the integration stage.
            var demoSessionCompleted by rememberSaveable { mutableStateOf(false) }
            val systemDark = isSystemInDarkTheme()
            val dark = when (appearance) {
                "Dark" -> true
                "System" -> systemDark
                else -> false
            }

            // Edge-to-edge status/navigation icons must contrast with the selected app mode.
            SideEffect {
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }

            PulihGoTheme(darkTheme = dark, dynamicColor = false) {
                when (route) {
                    PatientRoute.ONBOARDING -> OnboardingScreen(onDone = {
                        preferences.edit().putBoolean(ONBOARDING_SEEN_KEY, true).apply()
                        route = PatientRoute.LOGIN
                    }, isDark = dark)
                    PatientRoute.LOGIN -> LoginScreen(
                        prefillEmail = draftEmail,
                        isDark = dark,
                        onLogin = { email, password ->
                            val patient = auth.login(email, password)
                            if (patient == null) {
                                "Invalid demo credentials. Check the demo account shown below."
                            } else {
                                accountName = patient.name
                                accountEmail = patient.email
                                // Demo state belongs to a login session, not a different user.
                                demoSessionCompleted = false
                                route = PatientRoute.HOME
                                null
                            }
                        },
                        onRegister = { route = PatientRoute.REGISTER },
                        onForgotPassword = { route = PatientRoute.FORGOT }
                    )
                    PatientRoute.REGISTER -> RegisterScreen(
                        isDark = dark,
                        onBackToLogin = { route = PatientRoute.LOGIN },
                        onRegister = { name, email, password ->
                            if (!auth.register(name, email, password)) {
                                "This demo email is already registered."
                            } else {
                                draftEmail = email.trim()
                                route = PatientRoute.LOGIN
                                null
                            }
                        }
                    )
                    PatientRoute.FORGOT -> ForgotPasswordScreen(
                        isDark = dark,
                        onBackToLogin = { route = PatientRoute.LOGIN },
                        onRequestReset = { email ->
                            draftEmail = email.trim()
                            route = PatientRoute.LINK_SENT
                        }
                    )
                    PatientRoute.LINK_SENT -> ResetLinkSentScreen(
                        email = draftEmail,
                        isDark = dark,
                        onBackToLogin = { route = PatientRoute.LOGIN }
                    )
                    PatientRoute.HOME -> PatientHomeScreen(
                        patientName = accountName,
                        sessions = if (demoSessionCompleted) listOf(
                            // Synthetic demo sessions: 12 repetitions, peak 75°, 86% mean.
                            HomeExerciseSession(LocalDate.now(), 4, 62, 84, time = LocalTime.of(7, 45)),
                            HomeExerciseSession(LocalDate.now(), 5, 75, 86, time = LocalTime.of(7, 51)),
                            HomeExerciseSession(LocalDate.now(), 3, 68, 88, time = LocalTime.of(7, 56))
                        ) else emptyList(),
                        onSettings = { route = PatientRoute.SETTINGS },
                        onStartExercise = { route = PatientRoute.EXERCISE_HANDOFF },
                        onOpenSummary = { route = PatientRoute.SUMMARY_HANDOFF },
                        isDark = dark
                    )
                    PatientRoute.SETTINGS -> SettingsScreen(
                        name = accountName,
                        email = accountEmail,
                        selectedAppearance = appearance,
                        isDark = dark,
                        onAppearanceChange = { selected ->
                            appearance = selected
                            preferences.edit().putString(APPEARANCE_KEY, selected).apply()
                        },
                        onBack = { route = PatientRoute.HOME },
                        onLogOut = {
                            demoSessionCompleted = false
                            route = PatientRoute.LOGIN
                        }
                    )
                    PatientRoute.EXERCISE_HANDOFF -> FeatureHandoff(
                        title = "Exercise Library",
                        message = "This screen is owned by the team's Exercise module. The buttons below only simulate Patient Home states; no exercise is measured or saved.",
                        onBack = { route = PatientRoute.HOME },
                        primaryLabel = "Simulate Completed Session",
                        onPrimary = {
                            demoSessionCompleted = true
                            route = PatientRoute.HOME
                        },
                        secondaryLabel = "Reset Demo Session",
                        onSecondary = {
                            demoSessionCompleted = false
                            route = PatientRoute.HOME
                        },
                        isDark = dark
                    )
                    PatientRoute.SUMMARY_HANDOFF -> FeatureHandoff(
                        title = "Session Summary",
                        message = "Full summary navigation will be integrated with the team modules. Patient Home currently uses demo data only.",
                        onBack = { route = PatientRoute.HOME },
                        isDark = dark
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureHandoff(
    title: String,
    message: String,
    onBack: () -> Unit,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    isDark: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxSize()
            .background(if (isDark) Color(0xFF111717) else Color(0xFFF9F9F7))
            .statusBarsPadding().navigationBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground)
        Text(message, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 14.dp, bottom = 22.dp))
        if (primaryLabel != null && onPrimary != null) {
            Button(onClick = onPrimary,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D7C7C), contentColor = Color.White)) {
                Text(primaryLabel, color = Color.White)
            }
        }
        if (secondaryLabel != null && onSecondary != null) {
            Button(onClick = onSecondary,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D7C7C), contentColor = Color.White)) {
                Text(secondaryLabel, color = Color.White)
            }
        }
        Button(onClick = onBack, modifier = Modifier.padding(top = 10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D7C7C), contentColor = Color.White)) {
            Text("Back to Patient Home", color = Color.White)
        }
    }
}
