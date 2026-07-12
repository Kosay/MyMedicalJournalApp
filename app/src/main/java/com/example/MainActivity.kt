package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.ui.HealthViewModel
import com.example.ui.HealthViewModelFactory
import com.example.ui.screens.MainScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val factory = HealthViewModelFactory(application)
        val viewModel = ViewModelProvider(this, factory)[HealthViewModel::class.java]

        viewModel.scheduleDailyPatternCheck()
        viewModel.scheduleMedicationReminder()

        setContent {
            val themeMode by viewModel.theme.collectAsState()

            val isDark = when(themeMode.uppercase()) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            // Request POST_NOTIFICATIONS permission on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* granted silently; notifications work if user accepts */ }
                LaunchedEffect(Unit) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            MyApplicationTheme(darkTheme = isDark) {
                val lang by viewModel.language.collectAsState()
                var showSplash by remember { mutableStateOf(true) }
                var showOnboarding by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    delay(2800)
                    showOnboarding = !viewModel.onboardingCompleted
                    showSplash = false
                }

                if (showSplash) {
                    SplashScreen(onTimeout = { showSplash = false })
                } else if (showOnboarding) {
                    OnboardingScreen(
                        viewModel = viewModel,
                        lang = lang,
                        onFinished = { showOnboarding = false }
                    )
                } else {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}

