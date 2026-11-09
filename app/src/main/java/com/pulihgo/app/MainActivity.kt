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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pulihgo.app.ui.onboarding.OnboardingScreen
import com.pulihgo.app.ui.theme.PulihGoTheme

private const val ONBOARDING_SEEN_KEY = "pulihgo_onboarding_seen"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferences = getSharedPreferences("pulihgo_local_prefs", MODE_PRIVATE)

        setContent {
            var hasSeenOnboarding by rememberSaveable {
                mutableStateOf(preferences.getBoolean(ONBOARDING_SEEN_KEY, false))
            }

            PulihGoTheme(dynamicColor = false) {
                if (!hasSeenOnboarding) {
                    OnboardingScreen(onDone = {
                        preferences.edit().putBoolean(ONBOARDING_SEEN_KEY, true).apply()
                        hasSeenOnboarding = true
                    })
                } else {
                    PatientHomePlaceholder(onResetOnboarding = {
                        preferences.edit().putBoolean(ONBOARDING_SEEN_KEY, false).apply()
                        hasSeenOnboarding = false
                    })
                }
            }
        }
    }
}

/** Temporary destination only. Replace with Orie's PGF-PAT-03 Patient Home flow. */
@Composable
private fun PatientHomePlaceholder(onResetOnboarding: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Patient Home", style = MaterialTheme.typography.headlineMedium)
        Text("Placeholder — PGF-PAT-03 will replace this screen.")
        Button(onClick = onResetOnboarding, modifier = Modifier.padding(top = 24.dp)) {
            Text("Test Onboarding Again")
        }
    }
}
