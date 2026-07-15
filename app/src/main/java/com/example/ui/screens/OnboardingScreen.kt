package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EmergencyInfo
import com.example.ui.HealthViewModel
import com.example.ui.LocalStrings

private val SlateDarkBg = Color(0xFF1A2332)
private val SlateCardBg  = Color(0xFF243447)
private val HighlightTeal = Color(0xFF00BFA5)
private val MutedText    = Color(0xFF8899AA)
private val DividerColor = Color(0xFF2E4057)

@Composable
fun OnboardingScreen(
    viewModel: HealthViewModel,
    lang: String,
    onFinished: () -> Unit
) {
    fun trans(key: String) = LocalStrings.get(key, lang)

    var step by remember { mutableStateOf(1) }
    val totalSteps = 4

    // Step 2 – Personal Info
    var fullName        by remember { mutableStateOf("") }
    var sex             by remember { mutableStateOf("") }
    var dateOfBirth     by remember { mutableStateOf("") }
    var heightCmText    by remember { mutableStateOf("") }
    var weightKgText    by remember { mutableStateOf("") }

    // Step 3 – Medical Info
    var bloodType       by remember { mutableStateOf("") }
    var conditions      by remember { mutableStateOf("") }
    var allergies       by remember { mutableStateOf("") }

    // Step 4 – Emergency Contact
    var contactName     by remember { mutableStateOf("") }
    var contactPhone    by remember { mutableStateOf("") }
    var additionalNotes by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress dots
            if (step > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    (1..totalSteps).forEach { i ->
                        Box(
                            modifier = Modifier
                                .size(if (i == step) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(if (i == step) HighlightTeal else DividerColor)
                        )
                    }
                }
                // Step label
                Text(
                    text = trans("step_of")
                        .replace("%1", step.toString())
                        .replace("%2", totalSteps.toString()),
                    color = MutedText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            when (step) {
                1 -> WelcomeStep(trans = ::trans, onGetStarted = { step = 2 })
                2 -> PersonalInfoStep(
                    trans = ::trans,
                    fullName = fullName, onFullNameChange = { fullName = it },
                    sex = sex, onSexChange = { sex = it },
                    dateOfBirth = dateOfBirth, onDateOfBirthChange = { dateOfBirth = it },
                    heightCmText = heightCmText, onHeightChange = { heightCmText = it },
                    weightKgText = weightKgText, onWeightChange = { weightKgText = it }
                )
                3 -> MedicalInfoStep(
                    trans = ::trans,
                    bloodType = bloodType, onBloodTypeChange = { bloodType = it },
                    conditions = conditions, onConditionsChange = { conditions = it },
                    allergies = allergies, onAllergiesChange = { allergies = it }
                )
                4 -> EmergencyContactStep(
                    trans = ::trans,
                    contactName = contactName, onContactNameChange = { contactName = it },
                    contactPhone = contactPhone, onContactPhoneChange = { contactPhone = it },
                    additionalNotes = additionalNotes, onAdditionalNotesChange = { additionalNotes = it }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Navigation buttons (shown for steps 2-4)
            if (step > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HighlightTeal),
                        border = androidx.compose.foundation.BorderStroke(1.dp, HighlightTeal)
                    ) {
                        Text(trans("previous"))
                    }

                    Button(
                        onClick = {
                            if (step < totalSteps) {
                                step++
                            } else {
                                val heightCm = heightCmText.toFloatOrNull() ?: 0f
                                val weightKg = weightKgText.toFloatOrNull() ?: 0f
                                val info = EmergencyInfo(
                                    fullName = fullName,
                                    bloodType = bloodType,
                                    chronicConditions = conditions,
                                    allergies = allergies,
                                    contactName = contactName,
                                    contactPhone = contactPhone,
                                    additionalNotes = additionalNotes,
                                    sex = sex
                                )
                                viewModel.completeOnboarding(info, heightCm, weightKg)
                                onFinished()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal)
                    ) {
                        Text(
                            text = if (step < totalSteps) trans("next_step") else trans("finish_setup"),
                            color = Color.White
                        )
                    }
                }

                // Skip option
                if (step < totalSteps) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = trans("skip"),
                        color = MutedText,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable { step++ }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(trans: (String) -> String, onGetStarted: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // App icon placeholder
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(HighlightTeal.copy(alpha = 0.15f))
                .border(2.dp, HighlightTeal, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "♥", fontSize = 40.sp, color = HighlightTeal)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = trans("onboarding_welcome"),
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = trans("onboarding_subtitle"),
            color = MutedText,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(56.dp))

        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = trans("get_started"),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PersonalInfoStep(
    trans: (String) -> String,
    fullName: String, onFullNameChange: (String) -> Unit,
    sex: String, onSexChange: (String) -> Unit,
    dateOfBirth: String, onDateOfBirthChange: (String) -> Unit,
    heightCmText: String, onHeightChange: (String) -> Unit,
    weightKgText: String, onWeightChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = trans("personal_info"),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OnboardingTextField(
            value = fullName,
            onValueChange = onFullNameChange,
            label = trans("full_name")
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = trans("sex"), color = MutedText, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Male", "Female").forEach { option ->
                val label = if (option == "Male") trans("male") else trans("female")
                val selected = sex == option
                FilterChip(
                    selected = selected,
                    onClick = { onSexChange(option) },
                    label = { Text(label, color = if (selected) Color.White else MutedText) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = HighlightTeal,
                        containerColor = SlateCardBg
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        selectedBorderColor = HighlightTeal,
                        borderColor = DividerColor
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OnboardingTextField(
            value = dateOfBirth,
            onValueChange = onDateOfBirthChange,
            label = trans("date_of_birth"),
            placeholder = "DD/MM/YYYY"
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                OnboardingTextField(
                    value = heightCmText,
                    onValueChange = onHeightChange,
                    label = trans("height_cm"),
                    keyboardType = KeyboardType.Decimal
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                OnboardingTextField(
                    value = weightKgText,
                    onValueChange = onWeightChange,
                    label = trans("weight_kg"),
                    keyboardType = KeyboardType.Decimal
                )
            }
        }
    }
}

@Composable
private fun MedicalInfoStep(
    trans: (String) -> String,
    bloodType: String, onBloodTypeChange: (String) -> Unit,
    conditions: String, onConditionsChange: (String) -> Unit,
    allergies: String, onAllergiesChange: (String) -> Unit
) {
    val bloodTypes = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = trans("medical_info"),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(text = trans("blood_type_select"), color = MutedText, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(8.dp))

        // 2×4 grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            bloodTypes.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { bt ->
                        val selected = bloodType == bt
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) HighlightTeal else SlateCardBg)
                                .border(1.dp, if (selected) HighlightTeal else DividerColor, RoundedCornerShape(8.dp))
                                .clickable { onBloodTypeChange(bt) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = bt,
                                color = if (selected) Color.White else MutedText,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        OnboardingTextField(
            value = conditions,
            onValueChange = onConditionsChange,
            label = trans("chronic_conditions"),
            placeholder = trans("conditions_hint"),
            singleLine = false,
            minLines = 2
        )

        Spacer(modifier = Modifier.height(16.dp))

        OnboardingTextField(
            value = allergies,
            onValueChange = onAllergiesChange,
            label = trans("allergies"),
            placeholder = trans("allergies_hint"),
            singleLine = false,
            minLines = 2
        )
    }
}

@Composable
private fun EmergencyContactStep(
    trans: (String) -> String,
    contactName: String, onContactNameChange: (String) -> Unit,
    contactPhone: String, onContactPhoneChange: (String) -> Unit,
    additionalNotes: String, onAdditionalNotesChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = trans("emergency_contact"),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = trans("emergency_info"),
            color = MutedText,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OnboardingTextField(
            value = contactName,
            onValueChange = onContactNameChange,
            label = trans("emergency_contact")
        )

        Spacer(modifier = Modifier.height(16.dp))

        OnboardingTextField(
            value = contactPhone,
            onValueChange = onContactPhoneChange,
            label = trans("emergency_phone"),
            keyboardType = KeyboardType.Phone
        )

        Spacer(modifier = Modifier.height(16.dp))

        OnboardingTextField(
            value = additionalNotes,
            onValueChange = onAdditionalNotesChange,
            label = trans("additional_notes"),
            singleLine = false,
            minLines = 3
        )
    }
}

@Composable
private fun OnboardingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = MutedText, fontSize = 13.sp) },
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder, color = MutedText.copy(alpha = 0.5f), fontSize = 13.sp) }
        } else null,
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = HighlightTeal,
            unfocusedBorderColor = DividerColor,
            cursorColor = HighlightTeal,
            focusedContainerColor = SlateCardBg,
            unfocusedContainerColor = SlateCardBg
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    )
}
