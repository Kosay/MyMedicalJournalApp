package com.example.ui.screens

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.fragment.app.FragmentActivity
import com.example.ui.HealthViewModel
import com.example.ui.LocalStrings

private val SlateDarkBg = Color(0xFF1A2332)
private val HighlightTeal = Color(0xFF00BFA5)
private val MutedText = Color(0xFF8899AA)
private val DividerColor = Color(0xFF2E4057)

/**
 * Shown after onboarding (when app lock is enabled) and before MainScreen.
 * Tries biometric auth first if available; falls back to the device PIN the
 * user set up in Settings. Neither the PIN nor biometric data leaves the device.
 */
@Composable
fun AppLockScreen(viewModel: HealthViewModel, lang: String, onUnlocked: () -> Unit) {
    fun trans(key: String) = LocalStrings.get(key, lang)
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    fun tryBiometric() {
        val activity = context as? FragmentActivity ?: return
        val biometricManager = BiometricManager.from(context)
        val canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) return

        val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // User cancelled or biometric failed too many times — fall back to PIN entry silently.
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(trans("app_lock_biometric_prompt_title"))
            .setSubtitle(trans("app_lock_biometric_prompt_subtitle"))
            .setNegativeButtonText(trans("app_lock_use_pin_fallback"))
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(promptInfo)
    }

    LaunchedEffect(Unit) { tryBiometric() }

    Box(
        modifier = Modifier.fillMaxSize().background(SlateDarkBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(HighlightTeal.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = HighlightTeal, modifier = Modifier.size(32.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(trans("app_lock_enter_pin"), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(24.dp))

            // PIN dots
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(4) { i ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (i < enteredPin.length) HighlightTeal else DividerColor)
                    )
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(errorMessage, color = Color(0xFFFF6B6B), fontSize = 13.sp, textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Numeric keypad
            val keypadRows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("bio", "0", "back")
            )
            keypadRows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .clickable {
                                    when (key) {
                                        "back" -> if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                        "bio" -> tryBiometric()
                                        else -> {
                                            if (enteredPin.length < 4) {
                                                enteredPin += key
                                                if (enteredPin.length == 4) {
                                                    if (viewModel.verifyAppLockPin(enteredPin)) {
                                                        onUnlocked()
                                                    } else {
                                                        errorMessage = trans("app_lock_wrong_pin")
                                                        enteredPin = ""
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when (key) {
                                "back" -> Icon(Icons.Default.Backspace, contentDescription = null, tint = MutedText)
                                "bio" -> Icon(Icons.Default.Fingerprint, contentDescription = null, tint = HighlightTeal)
                                else -> Text(key, color = Color.White, fontSize = 24.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** PIN setup flow shown from Settings when the user turns app lock on for the first time. */
@Composable
fun SetAppLockPinDialog(viewModel: HealthViewModel, lang: String, onDone: (success: Boolean) -> Unit) {
    fun trans(key: String) = LocalStrings.get(key, lang)
    val context = LocalContext.current
    var firstPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var stage by remember { mutableStateOf(1) } // 1 = enter, 2 = confirm

    fun handleDigit(digit: String) {
        if (stage == 1) {
            if (firstPin.length < 4) firstPin += digit
            if (firstPin.length == 4) stage = 2
        } else {
            if (confirmPin.length < 4) confirmPin += digit
            if (confirmPin.length == 4) {
                if (confirmPin == firstPin) {
                    viewModel.setAppLockPin(firstPin)
                    onDone(true)
                } else {
                    Toast.makeText(context, trans("app_lock_pin_mismatch"), Toast.LENGTH_SHORT).show()
                    firstPin = ""; confirmPin = ""; stage = 1
                }
            }
        }
    }

    Dialog(onDismissRequest = { onDone(false) }) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    if (stage == 1) trans("app_lock_set_pin") else trans("app_lock_confirm_pin"),
                    fontWeight = FontWeight.Bold, fontSize = 18.sp
                )
                if (stage == 1) {
                    Text(trans("app_lock_set_pin_subtitle"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }

                val activePin = if (stage == 1) firstPin else confirmPin
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(4) { i ->
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (i < activePin.length) HighlightTeal else MaterialTheme.colorScheme.outlineVariant)
                        )
                    }
                }

                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { d ->
                            OutlinedButton(onClick = { handleDigit(d) }, modifier = Modifier.size(56.dp), shape = CircleShape) {
                                Text(d, fontSize = 18.sp)
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Spacer(modifier = Modifier.size(56.dp))
                    OutlinedButton(onClick = { handleDigit("0") }, modifier = Modifier.size(56.dp), shape = CircleShape) {
                        Text("0", fontSize = 18.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            if (stage == 1 && firstPin.isNotEmpty()) firstPin = firstPin.dropLast(1)
                            else if (stage == 2 && confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
                        },
                        modifier = Modifier.size(56.dp), shape = CircleShape
                    ) {
                        Icon(Icons.Default.Backspace, contentDescription = null)
                    }
                }

                TextButton(onClick = { onDone(false) }) { Text(trans("cancel")) }
            }
        }
    }
}
