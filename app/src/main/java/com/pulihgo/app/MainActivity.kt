package com.pulihgo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowInsetsControllerCompat
import com.pulihgo.app.navigation.AppNavigation
import com.pulihgo.app.ui.theme.PulihGoTheme

private const val PREF_FILE = "pulihgo_local_prefs"
private const val APPEARANCE_KEY = "pulihgo_appearance_mode"

/** One root theme and one shared router for Patient and Therapist. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferences = getSharedPreferences(PREF_FILE, MODE_PRIVATE)

        setContent {
            var appearance by rememberSaveable {
                mutableStateOf(preferences.getString(APPEARANCE_KEY, "Light") ?: "Light")
            }
            val systemDark = isSystemInDarkTheme()
            val dark = when (appearance) {
                "Dark" -> true
                "System" -> systemDark
                else -> false
            }

            SideEffect {
                WindowInsetsControllerCompat(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }

            PulihGoTheme(darkTheme = dark, dynamicColor = false) {
                AppNavigation(
                    isDark = dark,
                    appearance = appearance,
                    onAppearanceChange = { selected ->
                        appearance = selected
                        preferences.edit().putString(APPEARANCE_KEY, selected).apply()
                    }
                )
            }
        }
    }
}
