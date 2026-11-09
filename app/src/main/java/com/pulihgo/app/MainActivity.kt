package com.pulihgo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pulihgo.app.ui.account.DemoAuthRepository
import com.pulihgo.app.ui.account.ForgotPasswordScreen
import com.pulihgo.app.ui.account.LoginScreen
import com.pulihgo.app.ui.account.RegisterScreen
import com.pulihgo.app.ui.account.ResetLinkSentScreen
import com.pulihgo.app.ui.account.SettingsScreen
import com.pulihgo.app.ui.onboarding.OnboardingScreen
import com.pulihgo.app.ui.theme.PulihGoTheme

private const val PREF_FILE = "pulihgo_local_prefs"
private const val ONBOARDING_SEEN_KEY = "pulihgo_onboarding_seen"
private const val APPEARANCE_KEY = "pulihgo_appearance_mode"

private enum class PatientRoute { ONBOARDING, LOGIN, REGISTER, FORGOT, LINK_SENT, HOME, SETTINGS }

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
            val systemDark = isSystemInDarkTheme()
            val dark = when (appearance) {
                "Dark" -> true
                "System" -> systemDark
                else -> false
            }

            PulihGoTheme(darkTheme = dark, dynamicColor = false) {
                when (route) {
                    PatientRoute.ONBOARDING -> OnboardingScreen(onDone = {
                        preferences.edit().putBoolean(ONBOARDING_SEEN_KEY, true).apply()
                        route = PatientRoute.LOGIN
                    })
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
                    PatientRoute.HOME -> PatientHomePlaceholder(
                        onSettings = { route = PatientRoute.SETTINGS },
                        onResetOnboarding = {
                            preferences.edit().putBoolean(ONBOARDING_SEEN_KEY, false).apply()
                            route = PatientRoute.ONBOARDING
                        }
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
                        onLogOut = { route = PatientRoute.LOGIN }
                    )
                }
            }
        }
    }
}

/** Temporary PGF-PAT-03 destination: deliberately not a real home screen yet. */
@Composable
private fun PatientHomePlaceholder(onSettings: () -> Unit, onResetOnboarding: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Patient Home", style = MaterialTheme.typography.headlineMedium)
        Text("Placeholder — PGF-PAT-03 will replace this screen.")
        Button(onClick = onSettings, modifier = Modifier.padding(top = 16.dp)) { Text("Account Settings") }
        Button(onClick = onResetOnboarding, modifier = Modifier.padding(top = 8.dp)) { Text("Test Onboarding Again") }
    }
}
