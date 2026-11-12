package com.pulihgo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.pulihgo.app.navigation.AppNavigation
import com.pulihgo.app.ui.theme.PulihGoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PulihGoTheme {
                AppNavigation()
            }
        }
    }
}
