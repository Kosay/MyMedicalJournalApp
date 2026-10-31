package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.ui.HealthViewModel
import com.example.ui.HealthViewModelFactory
import com.example.ui.screens.AppLockScreen
import com.example.ui.screens.MainScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

// FragmentActivity (not plain ComponentActivity) is required by BiometricPrompt for the app lock.
class MainActivity : FragmentActivity() {
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
                val appLockEnabled by viewModel.appLockEnabled.collectAsState()
                var showSplash by remember { mutableStateOf(true) }
                var showOnboarding by remember { mutableStateOf(false) }
                var isLocked by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    delay(2800)
                    showOnboarding = !viewModel.onboardingCompleted
                    isLocked = !showOnboarding && appLockEnabled && viewModel.hasAppLockPin()
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
                } else if (isLocked) {
                    AppLockScreen(
                        viewModel = viewModel,
                        lang = lang,
                        onUnlocked = { isLocked = false }
                    )
                } else {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }
}

