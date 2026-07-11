package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.LocalStrings
import com.example.ui.HealthViewModel
import com.example.ui.DangerAlert
import com.example.ui.DangerSeverity
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: HealthViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val currentLang by viewModel.language.collectAsState()
    
    // Quick translation helper
    fun trans(key: String): String = LocalStrings.get(key, currentLang)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = trans(selectedTab),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 8.dp
            ) {
                listOf("home", "track", "lifestyle", "more", "settings").forEach { tab ->
                    val isSelected = selectedTab == tab
                    val label = when (tab) {
                        "home" -> trans("home")
                        "track" -> trans("track")
                        "lifestyle" -> trans("lifestyle")
                        "more" -> trans("more")
                        else -> trans("settings")
                    }
                    val icon = when (tab) {
                        "home" -> Icons.Default.Home
                        "track" -> Icons.Default.Favorite
                        "lifestyle" -> Icons.Default.DirectionsRun
                        "more" -> Icons.Default.Menu
                        else -> Icons.Default.Settings
                    }

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(tab) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        icon = { Icon(icon, contentDescription = label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            indicatorColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                "home" -> DashboardScreen(viewModel, currentLang)
                "track" -> TrackScreen(viewModel, currentLang)
                "lifestyle" -> LifestyleScreen(viewModel, currentLang)
                "more" -> MoreScreen(viewModel, currentLang)
                "settings" -> SettingsScreen(viewModel, currentLang)
            }
        }
    }
}

// ==========================================
// 1. DASHBOARD SCREEN
// ==========================================
@Composable
fun DashboardScreen(viewModel: HealthViewModel, lang: String) {
    val bpList by viewModel.bloodPressureRecords.collectAsState()
    val weightList by viewModel.weightRecords.collectAsState()
    val sleepList by viewModel.sleepRecords.collectAsState()
    val labList by viewModel.labResults.collectAsState()
    val sugarList by viewModel.bloodSugarRecords.collectAsState()
    val heightCm by viewModel.heightCm.collectAsState()

    fun trans(key: String): String = LocalStrings.get(key, lang)

    val bpLatest = bpList.firstOrNull()
    val weightLatest = weightList.firstOrNull()
    val sleepLatest = sleepList.firstOrNull()
    val cholesterolLatest = labList.firstOrNull { it.testName.lowercase() == "cholesterol" } ?: labList.firstOrNull { it.testName.lowercase().contains("chol") }
    val trigLatest = labList.firstOrNull { it.testName.lowercase() == "triglycerides" } ?: labList.firstOrNull { it.testName.lowercase().contains("trig") }
    val sugarLatest = sugarList.firstOrNull()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // High fidelity 2x3 Grid Layout of medical metrics matched to Screenshot 1!
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Blood Pressure Card
                DashboardCard(
                    title = trans("blood_pressure"),
                    value = bpLatest?.let { "${it.systolic}/${it.diastolic}" } ?: "120/79",
                    unit = "mmHg",
                    statusText = bpLatest?.let { getBloodPressureStatus(it.systolic, it.diastolic, lang) } ?: trans("elevated"),
                    statusColor = bpLatest?.let { getBloodPressureStatusColor(it.systolic, it.diastolic) } ?: WarningYellow,
                    modifier = Modifier.weight(1f)
                )
                // Weight & Dynamic BMI Card
                val wVal = weightLatest?.weightKg ?: 78.5f
                val bmi = wVal / ((heightCm / 100f) * (heightCm / 100f))
                val bmiText = "BMI ${String.format(Locale.US, "%.1f", bmi)}"
                DashboardCard(
                    title = trans("weight"),
                    value = "$wVal kg",
                    statusText = bmiText,
                    statusColor = if (bmi in 18.5..24.9) HighlightTeal else WarningYellow,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Heart Rate Card
                DashboardCard(
                    title = trans("heart_rate"),
                    value = bpLatest?.let { "${it.heartRate} bpm" } ?: "90 bpm",
                    statusText = if ((bpLatest?.heartRate ?: 90) in 60..100) trans("normal") else trans("elevated"),
                    statusColor = HighlightTeal,
                    modifier = Modifier.weight(1f)
                )
                // Sleep Card
                DashboardCard(
                    title = trans("sleep"),
                    value = sleepLatest?.let { "${it.hours} h" } ?: "7.5 h",
                    statusText = if ((sleepLatest?.hours ?: 7.5f) >= 7.0f) trans("normal") else trans("low"),
                    statusColor = HighlightTeal,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Cholesterol (Standard range < 200 mg/dL)
                val cholVal = cholesterolLatest?.value ?: 195f
                val cholStatus = if (cholVal < 200) trans("normal") else trans("high")
                val cholColor = if (cholVal < 200) HighlightTeal else AlertRed
                DashboardCard(
                    title = trans("cholesterol"),
                    value = "${cholVal.toInt()}",
                    unit = "mg/dL",
                    statusText = cholStatus,
                    statusColor = cholColor,
                    modifier = Modifier.weight(1f)
                )
                // Triglycerides (Matches the 160 with warning icon from Screenshot 1!)
                val trigVal = trigLatest?.value ?: 160f
                val isTrigHigh = trigVal >= 150
                DashboardCard(
                    title = trans("triglycerides"),
                    value = "${trigVal.toInt()}",
                    unit = "mg/dL",
                    statusText = if (isTrigHigh) trans("high") else trans("normal"),
                    statusColor = if (isTrigHigh) AlertRed else HighlightTeal,
                    showWarningIcon = isTrigHigh,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            val sugarVal = sugarLatest?.value ?: 95f
            val sugarCat = sugarLatest?.category ?: "Fasting"
            val sugarUnit = sugarLatest?.unit ?: "mg/dL"
            val sugarStatus = getBloodSugarStatus(sugarVal, sugarCat, lang)
            val sugarColor = getBloodSugarStatusColor(sugarVal, sugarCat)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DashboardCard(
                    title = trans("blood_sugar"),
                    value = "${sugarVal.toInt()}",
                    unit = sugarUnit,
                    statusText = "$sugarCat - $sugarStatus",
                    statusColor = sugarColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    value: String,
    unit: String = "",
    statusText: String,
    statusColor: Color,
    showWarningIcon: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.SemiBold
            )
            
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = value,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (unit.isNotEmpty()) {
                    Text(
                        text = unit,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (showWarningIcon) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = AlertRed,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = statusText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = statusColor
                )
            }
        }
    }
}

// Helpers for BP Classifications
fun getBloodPressureStatus(systolic: Int, diastolic: Int, lang: String): String {
    val en = lang == "en"
    return when {
        systolic < 120 && diastolic < 80 -> if (en) "NORMAL" else "طبيعي"
        systolic in 120..129 && diastolic < 80 -> if (en) "ELEVATED" else "مرتفع نسبيًا"
        else -> if (en) "HIGH" else "مرتفع"
    }
}

fun getBloodPressureStatusColor(systolic: Int, diastolic: Int): Color {
    return when {
        systolic < 120 && diastolic < 80 -> HighlightTeal
        systolic in 120..129 && diastolic < 80 -> WarningYellow
        else -> AlertRed
    }
}

// Helpers for Blood Sugar Classifications
fun getBloodSugarStatus(value: Float, category: String, lang: String): String {
    val en = lang == "en"
    return when (category.lowercase(Locale.US)) {
        "fasting" -> {
            when {
                value < 70 -> if (en) "LOW" else "منخفض"
                value < 100 -> if (en) "NORMAL" else "طبيعي"
                value < 126 -> if (en) "PREDIABETIC" else "مرحلة ما قبل السكري"
                else -> if (en) "DIABETIC" else "مرتفع (سكري)"
            }
        }
        "post-prandial", "after meal", "after breakfast", "after lunch", "after dinner" -> {
            when {
                value < 70 -> if (en) "LOW" else "منخفض"
                value < 140 -> if (en) "NORMAL" else "طبيعي"
                value < 200 -> if (en) "PREDIABETIC" else "مرحلة ما قبل السكري"
                else -> if (en) "DIABETIC" else "مرتفع (سكري)"
            }
        }
        else -> {
            when {
                value < 70 -> if (en) "LOW" else "منخفض"
                value < 140 -> if (en) "NORMAL" else "طبيعي"
                else -> if (en) "HIGH" else "مرتفع"
            }
        }
    }
}

fun getBloodSugarStatusColor(value: Float, category: String): Color {
    return when (category.lowercase(Locale.US)) {
        "fasting" -> {
            when {
                value < 70 -> AlertRed
                value < 100 -> HighlightTeal
                value < 126 -> WarningYellow
                else -> AlertRed
            }
        }
        "post-prandial", "after meal", "after breakfast", "after lunch", "after dinner" -> {
            when {
                value < 70 -> AlertRed
                value < 140 -> HighlightTeal
                value < 200 -> WarningYellow
                else -> AlertRed
            }
        }
        else -> {
            when {
                value < 70 -> AlertRed
                value < 140 -> HighlightTeal
                else -> AlertRed
            }
        }
    }
}

// ==========================================
// 2. TRACK SCREEN (Minds Screenshot 3)
// ==========================================
enum class TrackSection { NONE, BP, BLOOD_SUGAR, WEIGHT, MEDS, SYMPTOMS, SLEEP, LAB_RESULTS }

@Composable
fun TrackScreen(viewModel: HealthViewModel, lang: String) {
    var activeSection by remember { mutableStateOf(TrackSection.NONE) }
    fun trans(key: String): String = LocalStrings.get(key, lang)

    if (activeSection == TrackSection.NONE) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val list = listOf(
                Pair("blood_pressure", TrackSection.BP),
                Pair("blood_sugar", TrackSection.BLOOD_SUGAR),
                Pair("weight", TrackSection.WEIGHT),
                Pair("medications", TrackSection.MEDS),
                Pair("symptoms", TrackSection.SYMPTOMS),
                Pair("sleep", TrackSection.SLEEP),
                Pair("lab_results", TrackSection.LAB_RESULTS)
            )

            items(list) { (stringKey, section) ->
                TrackMenuItemCard(
                    title = trans(stringKey),
                    onClick = { activeSection = section }
                )
            }
        }
    } else {
        // Render Detail View for the active section with a lovely floating overlay
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header back navigation row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { activeSection = TrackSection.NONE }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when(activeSection) {
                            TrackSection.BP -> trans("blood_pressure")
                            TrackSection.BLOOD_SUGAR -> trans("blood_sugar")
                            TrackSection.WEIGHT -> trans("weight")
                            TrackSection.MEDS -> trans("medications")
                            TrackSection.SYMPTOMS -> trans("symptoms")
                            TrackSection.SLEEP -> trans("sleep")
                            else -> trans("lab_results")
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Inner content matching the track category
                Box(modifier = Modifier.weight(1f)) {
                    when(activeSection) {
                        TrackSection.BP -> BpDetailView(viewModel, lang)
                        TrackSection.BLOOD_SUGAR -> BloodSugarDetailView(viewModel, lang)
                        TrackSection.WEIGHT -> WeightDetailView(viewModel, lang)
                        TrackSection.MEDS -> MedicationDetailView(viewModel, lang)
                        TrackSection.SYMPTOMS -> SymptomDetailView(viewModel, lang)
                        TrackSection.SLEEP -> SleepDetailView(viewModel, lang)
                        TrackSection.LAB_RESULTS -> LabResultDetailView(viewModel, lang)
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun TrackMenuItemCard(title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

// ------------------------------------------
// DANGER ALERT BANNER
// ------------------------------------------
@Composable
fun DangerAlertBanner(alert: DangerAlert, onDismiss: () -> Unit) {
    val (bgColor, borderColor) = when (alert.severity) {
        DangerSeverity.CRISIS -> Color(0xFFFFCDD2) to Color(0xFFD32F2F)
        DangerSeverity.HIGH   -> Color(0xFFFFE0B2) to Color(0xFFE65100)
        DangerSeverity.WARNING -> Color(0xFFFFF9C4) to Color(0xFFF9A825)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = borderColor,
                modifier = Modifier.padding(top = 2.dp, end = 8.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = borderColor
                )
                Text(
                    text = alert.message,
                    fontSize = 13.sp,
                    color = Color(0xFF333333)
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = borderColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ------------------------------------------
// CATEGORY DETAILS VIEWS & INPUT OVERLAYS
// ------------------------------------------
@Composable
fun BpDetailView(viewModel: HealthViewModel, lang: String) {
    val records by viewModel.bloodPressureRecords.collectAsState()
    val correlationData by viewModel.bpCorrelationData.collectAsState()
    val dangerAlert by viewModel.dangerAlert.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<com.example.data.BloodPressureRecord?>(null) }
    var isPatternAnalysisExpanded by remember { mutableStateOf(false) }

    var systolicStr by remember { mutableStateOf("") }
    var diastolicStr by remember { mutableStateOf("") }
    var hrStr by remember { mutableStateOf("") }
    var notesStr by remember { mutableStateOf("") }
    var customTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        dangerAlert?.let { alert ->
            DangerAlertBanner(alert = alert, onDismiss = { viewModel.dismissDangerAlert() })
        }

        if (records.size >= 2) {
            val systolicPoints = records.take(7).reversed().map { it.systolic.toFloat() }
            val diastolicPoints = records.take(7).reversed().map { it.diastolic.toFloat() }
            DualTrendLineChart(
                points1 = systolicPoints, label1 = "Systolic",
                points2 = diastolicPoints, label2 = "Diastolic"
            )
        }

        // Expandable pattern analysis section (sleep / smoking / activity correlation)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isPatternAnalysisExpanded = !isPatternAnalysisExpanded }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Pattern Analysis (Sleep, Smoking, Activity)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = if (isPatternAnalysisExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isPatternAnalysisExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                BpCorrelationBarChart("Blood Pressure vs Sleep Duration", correlationData.bySleep)
                BpCorrelationBarChart("Blood Pressure vs Smoking (cigarettes/day)", correlationData.bySmoking)
                BpCorrelationBarChart("Blood Pressure vs Activity", correlationData.byActivity)
                BpCorrelationBarChart("Sleep + Smoking", correlationData.bySleepSmoking)
                BpCorrelationBarChart("Sleep + Activity", correlationData.bySleepActivity)
                BpCorrelationBarChart("Smoking + Activity", correlationData.bySmokingActivity)
                BpCorrelationBarChart("Sleep + Smoking + Activity", correlationData.byAllThree)
            }
        }

        Button(
            onClick = {
                editingRecord = null
                systolicStr = ""; diastolicStr = ""; hrStr = ""; notesStr = ""
                customTimestamp = System.currentTimeMillis()
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal)
        ) {
            Text(LocalStrings.get("add_record", lang), color = SlateDarkBg, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(records) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${record.systolic}/${record.diastolic} mmHg (HR: ${record.heartRate} bpm)",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (record.notes.isNotEmpty()) {
                                Text(record.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = viewModel.formatDate(record.timestamp),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        Row {
                            IconButton(onClick = {
                                editingRecord = record
                                systolicStr = record.systolic.toString()
                                diastolicStr = record.diastolic.toString()
                                hrStr = record.heartRate.toString()
                                notesStr = record.notes
                                customTimestamp = record.timestamp
                                showDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = HighlightTeal)
                            }
                            IconButton(onClick = { viewModel.deleteBloodPressure(record) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false; editingRecord = null }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        if (editingRecord != null) "Edit Blood Pressure" else "Record Blood Pressure",
                        fontWeight = FontWeight.Bold, fontSize = 16.sp
                    )
                    OutlinedTextField(
                        value = systolicStr, onValueChange = { systolicStr = it },
                        label = { Text("Systolic (mmHg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = diastolicStr, onValueChange = { diastolicStr = it },
                        label = { Text("Diastolic (mmHg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = hrStr, onValueChange = { hrStr = it },
                        label = { Text("Heart Rate (bpm)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notesStr, onValueChange = { notesStr = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DateTimePickerInline(
                        context = context,
                        timestamp = customTimestamp,
                        onTimestampChange = { customTimestamp = it }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showDialog = false; editingRecord = null }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val s = systolicStr.toIntOrNull() ?: 120
                                val d = diastolicStr.toIntOrNull() ?: 80
                                val h = hrStr.toIntOrNull() ?: 70
                                val rec = editingRecord
                                if (rec != null) {
                                    viewModel.updateBloodPressure(rec.copy(systolic = s, diastolic = d, heartRate = h, notes = notesStr, timestamp = customTimestamp))
                                } else {
                                    viewModel.addBloodPressure(s, d, h, notesStr, customTimestamp)
                                }
                                showDialog = false; editingRecord = null
                                systolicStr = ""; diastolicStr = ""; hrStr = ""; notesStr = ""
                            }
                        ) {
                            Text(if (editingRecord != null) "Update" else "Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BloodSugarDetailView(viewModel: HealthViewModel, lang: String) {
    val records by viewModel.bloodSugarRecords.collectAsState()
    val dangerAlert by viewModel.dangerAlert.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<com.example.data.BloodSugarRecord?>(null) }

    var sugarStr by remember { mutableStateOf("") }
    var categorySelection by remember { mutableStateOf("Fasting") }
    val categories = listOf("Fasting", "Post-Prandial", "Random", "Bedtime")
    var notesStr by remember { mutableStateOf("") }
    var customTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        dangerAlert?.let { alert ->
            DangerAlertBanner(alert = alert, onDismiss = { viewModel.dismissDangerAlert() })
        }

        if (records.isNotEmpty()) {
            val chartPoints = records.take(7).reversed().map { it.value }
            TrendLineChart(points = chartPoints, label = "Blood Sugar (${records.firstOrNull()?.unit ?: "mg/dL"})")
        }

        Button(
            onClick = {
                editingRecord = null
                sugarStr = ""; categorySelection = "Fasting"; notesStr = ""
                customTimestamp = System.currentTimeMillis()
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal)
        ) {
            Text(LocalStrings.get("add_record", lang), color = SlateDarkBg, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(records) { record ->
                val statusString = getBloodSugarStatus(record.value, record.category, lang)
                val statusColor = getBloodSugarStatusColor(record.value, record.category)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${record.value.toInt()} ${record.unit}",
                                    fontWeight = FontWeight.Bold, fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = statusString, color = statusColor,
                                        fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(record.category, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (record.notes.isNotEmpty()) {
                                Text(record.notes, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                            }
                            Text(viewModel.formatDate(record.timestamp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                        Row {
                            IconButton(onClick = {
                                editingRecord = record
                                sugarStr = record.value.toString()
                                categorySelection = record.category
                                notesStr = record.notes
                                customTimestamp = record.timestamp
                                showDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = HighlightTeal)
                            }
                            IconButton(onClick = { viewModel.deleteBloodSugar(record) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false; editingRecord = null }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        if (editingRecord != null) "Edit Blood Sugar" else "Record Blood Sugar Level",
                        fontWeight = FontWeight.Bold, fontSize = 16.sp
                    )
                    OutlinedTextField(
                        value = sugarStr, onValueChange = { sugarStr = it },
                        label = { Text("Blood Sugar (mg/dL)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Measurement Type / Category", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.forEach { cat ->
                            val isSelected = categorySelection == cat
                            Card(
                                modifier = Modifier.weight(1f).clickable { categorySelection = cat },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isSelected) HighlightTeal else MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(cat, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) SlateDarkBg else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = notesStr, onValueChange = { notesStr = it },
                        label = { Text("Notes") }, modifier = Modifier.fillMaxWidth()
                    )
                    DateTimePickerInline(context = context, timestamp = customTimestamp, onTimestampChange = { customTimestamp = it })
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showDialog = false; editingRecord = null }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val s = sugarStr.toFloatOrNull() ?: 100f
                                val rec = editingRecord
                                if (rec != null) {
                                    viewModel.updateBloodSugar(rec.copy(value = s, category = categorySelection, notes = notesStr, timestamp = customTimestamp))
                                } else {
                                    viewModel.addBloodSugar(s, categorySelection, notesStr, customTimestamp)
                                }
                                showDialog = false; editingRecord = null
                                sugarStr = ""; notesStr = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal)
                        ) {
                            Text(if (editingRecord != null) "Update" else "Save", color = SlateDarkBg, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeightDetailView(viewModel: HealthViewModel, lang: String) {
    val records by viewModel.weightRecords.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<com.example.data.WeightRecord?>(null) }

    var weightStr by remember { mutableStateOf("") }
    var notesStr by remember { mutableStateOf("") }
    var customTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        if (records.isNotEmpty()) {
            val chartPoints = records.take(7).reversed().map { it.weightKg }
            TrendLineChart(points = chartPoints, label = "Weight (kg)")
        }

        Button(
            onClick = {
                editingRecord = null; weightStr = ""; notesStr = ""
                customTimestamp = System.currentTimeMillis()
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal)
        ) {
            Text(LocalStrings.get("add_record", lang), color = SlateDarkBg, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(records) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${record.weightKg} kg", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            if (record.notes.isNotEmpty()) {
                                Text(record.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(viewModel.formatDate(record.timestamp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                        Row {
                            IconButton(onClick = {
                                editingRecord = record
                                weightStr = record.weightKg.toString()
                                notesStr = record.notes
                                customTimestamp = record.timestamp
                                showDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = HighlightTeal)
                            }
                            IconButton(onClick = { viewModel.deleteWeight(record) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false; editingRecord = null }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(if (editingRecord != null) "Edit Weight" else "Add Weight Record", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    OutlinedTextField(
                        value = weightStr, onValueChange = { weightStr = it },
                        label = { Text("Weight (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notesStr, onValueChange = { notesStr = it },
                        label = { Text("Notes") }, modifier = Modifier.fillMaxWidth()
                    )
                    DateTimePickerInline(context = context, timestamp = customTimestamp, onTimestampChange = { customTimestamp = it })
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showDialog = false; editingRecord = null }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val w = weightStr.toFloatOrNull() ?: 70.0f
                            val rec = editingRecord
                            if (rec != null) {
                                viewModel.updateWeight(rec.copy(weightKg = w, notes = notesStr, timestamp = customTimestamp))
                            } else {
                                viewModel.addWeight(w, notesStr, customTimestamp)
                            }
                            showDialog = false; editingRecord = null
                            weightStr = ""; notesStr = ""
                        }) {
                            Text(if (editingRecord != null) "Update" else "Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MedicationDetailView(viewModel: HealthViewModel, lang: String) {
    val records by viewModel.medications.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<com.example.data.MedicationRecord?>(null) }

    var nameStr by remember { mutableStateOf("") }
    var dosageStr by remember { mutableStateOf("") }
    var freqStr by remember { mutableStateOf("") }
    var isActiveState by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = {
                editingRecord = null; nameStr = ""; dosageStr = ""; freqStr = ""; isActiveState = true
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal)
        ) {
            Text(LocalStrings.get("add_record", lang), color = SlateDarkBg, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(records) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(record.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Dosage: ${record.dosage} | Frequency: ${record.frequency}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                val activeColor = if (record.isActive) HighlightTeal else WarningYellow
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(activeColor))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (record.isActive) "Active" else "Inactive", fontSize = 11.sp, color = activeColor, fontWeight = FontWeight.Bold)
                            }
                        }
                        Row {
                            IconButton(onClick = {
                                editingRecord = record
                                nameStr = record.name; dosageStr = record.dosage
                                freqStr = record.frequency; isActiveState = record.isActive
                                showDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = HighlightTeal)
                            }
                            IconButton(onClick = { viewModel.deleteMedication(record) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false; editingRecord = null }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(if (editingRecord != null) "Edit Medication" else "Add Medication", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    OutlinedTextField(value = nameStr, onValueChange = { nameStr = it }, label = { Text("Medication Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = dosageStr, onValueChange = { dosageStr = it }, label = { Text("Dosage (e.g. 10mg / 500mg)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = freqStr, onValueChange = { freqStr = it }, label = { Text("Frequency (e.g. Once daily / evening)") }, modifier = Modifier.fillMaxWidth())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = isActiveState, onCheckedChange = { isActiveState = it }, colors = SwitchDefaults.colors(checkedThumbColor = HighlightTeal, checkedTrackColor = HighlightTeal.copy(alpha = 0.4f)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isActiveState) "Active" else "Inactive", fontSize = 13.sp, color = if (isActiveState) HighlightTeal else WarningYellow)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showDialog = false; editingRecord = null }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val rec = editingRecord
                            if (rec != null) {
                                viewModel.updateMedication(rec.copy(name = nameStr, dosage = dosageStr, frequency = freqStr, isActive = isActiveState))
                            } else {
                                viewModel.addMedication(nameStr, dosageStr, freqStr, isActiveState)
                            }
                            showDialog = false; editingRecord = null
                            nameStr = ""; dosageStr = ""; freqStr = ""
                        }) {
                            Text(if (editingRecord != null) "Update" else "Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SymptomDetailView(viewModel: HealthViewModel, lang: String) {
    val records by viewModel.symptoms.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<com.example.data.SymptomRecord?>(null) }

    var symptomStr by remember { mutableStateOf("") }
    var severityStr by remember { mutableStateOf("Mild") }
    var notesStr by remember { mutableStateOf("") }
    var customTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = {
                editingRecord = null; symptomStr = ""; severityStr = "Mild"; notesStr = ""
                customTimestamp = System.currentTimeMillis()
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal)
        ) {
            Text(LocalStrings.get("add_record", lang), color = SlateDarkBg, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(records) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(record.symptomName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = record.severity, fontSize = 11.sp,
                                    color = when(record.severity.lowercase()) { "mild" -> HighlightTeal; "moderate" -> WarningYellow; else -> AlertRed },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (record.notes.isNotEmpty()) { Text(record.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Text(viewModel.formatDate(record.timestamp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                        Row {
                            IconButton(onClick = {
                                editingRecord = record
                                symptomStr = record.symptomName; severityStr = record.severity
                                notesStr = record.notes; customTimestamp = record.timestamp
                                showDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = HighlightTeal)
                            }
                            IconButton(onClick = { viewModel.deleteSymptom(record) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false; editingRecord = null }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(if (editingRecord != null) "Edit Symptom" else "Record Symptom", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    OutlinedTextField(value = symptomStr, onValueChange = { symptomStr = it }, label = { Text("Symptom Description") }, modifier = Modifier.fillMaxWidth())
                    Text("Severity Status", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Mild", "Moderate", "Severe").forEach { m ->
                            val active = m == severityStr
                            Button(onClick = { severityStr = m }, colors = ButtonDefaults.buttonColors(containerColor = if (active) HighlightTeal else MaterialTheme.colorScheme.surfaceVariant)) {
                                Text(m, color = if (active) SlateDarkBg else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                            }
                        }
                    }
                    OutlinedTextField(value = notesStr, onValueChange = { notesStr = it }, label = { Text("Additional notes") }, modifier = Modifier.fillMaxWidth())
                    DateTimePickerInline(context = context, timestamp = customTimestamp, onTimestampChange = { customTimestamp = it })
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showDialog = false; editingRecord = null }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val rec = editingRecord
                            if (rec != null) {
                                viewModel.updateSymptom(rec.copy(symptomName = symptomStr, severity = severityStr, notes = notesStr, timestamp = customTimestamp))
                            } else {
                                viewModel.addSymptom(symptomStr, severityStr, notesStr, customTimestamp)
                            }
                            showDialog = false; editingRecord = null
                            symptomStr = ""; notesStr = ""
                        }) {
                            Text(if (editingRecord != null) "Update" else "Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SleepDetailView(viewModel: HealthViewModel, lang: String) {
    val records by viewModel.sleepRecords.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<com.example.data.SleepRecord?>(null) }

    var hoursStr by remember { mutableStateOf("") }
    var notesStr by remember { mutableStateOf("") }
    var customTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        if (records.isNotEmpty()) {
            val chartPoints = records.take(7).reversed().map { it.hours }
            TrendLineChart(points = chartPoints, label = "Sleep Duration (hours)")
        }

        Button(
            onClick = {
                editingRecord = null; hoursStr = ""; notesStr = ""
                customTimestamp = System.currentTimeMillis()
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal)
        ) {
            Text(LocalStrings.get("add_record", lang), color = SlateDarkBg, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(records) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${record.hours} hours slept", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            if (record.notes.isNotEmpty()) { Text(record.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            Text(viewModel.formatDate(record.timestamp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                        Row {
                            IconButton(onClick = {
                                editingRecord = record
                                hoursStr = record.hours.toString(); notesStr = record.notes
                                customTimestamp = record.timestamp
                                showDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = HighlightTeal)
                            }
                            IconButton(onClick = { viewModel.deleteSleep(record) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false; editingRecord = null }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(if (editingRecord != null) "Edit Sleep" else "Log Sleep", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    OutlinedTextField(value = hoursStr, onValueChange = { hoursStr = it }, label = { Text("Hours slept") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notesStr, onValueChange = { notesStr = it }, label = { Text("Notes (e.g. felt deep rested / interrupted)") }, modifier = Modifier.fillMaxWidth())
                    DateTimePickerInline(context = context, timestamp = customTimestamp, onTimestampChange = { customTimestamp = it })
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showDialog = false; editingRecord = null }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val h = hoursStr.toFloatOrNull() ?: 8.0f
                            val rec = editingRecord
                            if (rec != null) {
                                viewModel.updateSleep(rec.copy(hours = h, notes = notesStr, timestamp = customTimestamp))
                            } else {
                                viewModel.addSleep(h, notesStr, customTimestamp)
                            }
                            showDialog = false; editingRecord = null
                            hoursStr = ""; notesStr = ""
                        }) {
                            Text(if (editingRecord != null) "Update" else "Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LabResultDetailView(viewModel: HealthViewModel, lang: String) {
    val records by viewModel.labResults.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<com.example.data.LabResultRecord?>(null) }

    var testNameStr by remember { mutableStateOf("") }
    var valueStr by remember { mutableStateOf("") }
    var unitStr by remember { mutableStateOf("mg/dL") }
    var refStr by remember { mutableStateOf("") }
    var customTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = {
                editingRecord = null; testNameStr = ""; valueStr = ""; unitStr = "mg/dL"; refStr = ""
                customTimestamp = System.currentTimeMillis()
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal)
        ) {
            Text(LocalStrings.get("add_record", lang), color = SlateDarkBg, fontWeight = FontWeight.Bold)
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(records) { record ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${record.testName}: ${record.value} ${record.unit}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            if (record.referenceRange.isNotEmpty()) {
                                Text("Ref Range: ${record.referenceRange}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(viewModel.formatDate(record.timestamp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                        Row {
                            IconButton(onClick = {
                                editingRecord = record
                                testNameStr = record.testName; valueStr = record.value.toString()
                                unitStr = record.unit; refStr = record.referenceRange
                                customTimestamp = record.timestamp
                                showDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = HighlightTeal)
                            }
                            IconButton(onClick = { viewModel.deleteLabResult(record) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false; editingRecord = null }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(if (editingRecord != null) "Edit Lab Result" else "Add Lab Result Record", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    OutlinedTextField(value = testNameStr, onValueChange = { testNameStr = it }, label = { Text("Test Name (e.g. Cholesterol)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = valueStr, onValueChange = { valueStr = it }, label = { Text("Measurement Value") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = unitStr, onValueChange = { unitStr = it }, label = { Text("Unit (e.g. mg/dL)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = refStr, onValueChange = { refStr = it }, label = { Text("Reference Range (e.g. < 200 mg/dL)") }, modifier = Modifier.fillMaxWidth())
                    DateTimePickerInline(context = context, timestamp = customTimestamp, onTimestampChange = { customTimestamp = it })
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showDialog = false; editingRecord = null }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            val v = valueStr.toFloatOrNull() ?: 100f
                            val rec = editingRecord
                            if (rec != null) {
                                viewModel.updateLabResult(rec.copy(testName = testNameStr, value = v, unit = unitStr, referenceRange = refStr, timestamp = customTimestamp))
                            } else {
                                viewModel.addLabResult(testNameStr, v, unitStr, refStr, customTimestamp)
                            }
                            showDialog = false; editingRecord = null
                            testNameStr = ""; valueStr = ""; unitStr = "mg/dL"; refStr = ""
                        }) {
                            Text(if (editingRecord != null) "Update" else "Save")
                        }
                    }
                }
            }
        }
    }
}

// Single-series trend chart
@Composable
fun TrendLineChart(points: List<Float>, label: String) {
    if (points.size < 2) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Needs at least 2 logs to draw chart graph", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val maxVal = points.maxOrNull() ?: 1f
    val minVal = points.minOrNull() ?: 0f
    val range = if (maxVal == minVal) 1f else maxVal - minVal

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            val width = size.width
            val height = size.height
            val segmentWidth = width / (points.size - 1)

            val path = Path()
            points.forEachIndexed { i, p ->
                val x = i * segmentWidth
                val y = height - ((p - minVal) / range) * height
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(path = path, color = HighlightTeal, style = Stroke(width = 3.dp.toPx()))

            points.forEachIndexed { i, p ->
                val x = i * segmentWidth
                val y = height - ((p - minVal) / range) * height
                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(x, y))
                drawCircle(color = HighlightTeal, radius = 2.dp.toPx(), center = Offset(x, y))
            }
        }
    }
}

// Dual-series trend chart for records with two related values (e.g. systolic/diastolic BP)
@Composable
fun DualTrendLineChart(
    points1: List<Float>,
    label1: String,
    points2: List<Float>,
    label2: String,
    color1: Color = HighlightTeal,
    color2: Color = AlertRed
) {
    if (points1.size < 2) {
        Card(
            modifier = Modifier.fillMaxWidth().height(130.dp).padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Needs at least 2 logs to draw chart", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val allValues = points1 + points2
    val maxVal = allValues.maxOrNull() ?: 1f
    val minVal = allValues.minOrNull() ?: 0f
    val range = if (maxVal == minVal) 1f else maxVal - minVal

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color1))
                Spacer(modifier = Modifier.width(4.dp))
                Text(label1, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color1)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color2))
                Spacer(modifier = Modifier.width(4.dp))
                Text(label2, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color2)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
            val width = size.width
            val height = size.height
            val count = maxOf(points1.size, points2.size)
            val segW = if (count > 1) width / (count - 1) else width

            fun drawSeries(pts: List<Float>, seriesColor: Color) {
                if (pts.size < 2) return
                val path = Path()
                pts.forEachIndexed { i, p ->
                    val x = i * segW
                    val y = height - ((p - minVal) / range) * height
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, seriesColor, style = Stroke(width = 3.dp.toPx()))
                pts.forEachIndexed { i, p ->
                    val x = i * segW
                    val y = height - ((p - minVal) / range) * height
                    drawCircle(Color.White, radius = 4.dp.toPx(), center = Offset(x, y))
                    drawCircle(seriesColor, radius = 2.dp.toPx(), center = Offset(x, y))
                }
            }

            drawSeries(points1, color1)
            drawSeries(points2, color2)
        }
    }
}

// Bar chart comparing average systolic/diastolic BP across lifestyle-pattern buckets
@Composable
fun BpCorrelationBarChart(
    title: String,
    buckets: List<com.example.ui.BpCorrelationBucket>,
    color1: Color = HighlightTeal,
    color2: Color = AlertRed
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(4.dp))

        if (buckets.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.CenterStart) {
                Text("Not enough data yet", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Column
        }

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color1))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Systolic", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color1)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color2))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Diastolic", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color2)
            }
        }

        val maxVal = buckets.maxOf { it.avgSystolic }.coerceAtLeast(1f)

        buckets.forEach { bucket ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(
                    "${bucket.label}  (n=${bucket.count})",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                // Systolic bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .fillMaxWidth(fraction = (bucket.avgSystolic / maxVal).coerceIn(0f, 1f))
                            .background(color1, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${bucket.avgSystolic.toInt()}", fontSize = 10.sp, color = color1)
                }
                Spacer(modifier = Modifier.height(2.dp))
                // Diastolic bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .fillMaxWidth(fraction = (bucket.avgDiastolic / maxVal).coerceIn(0f, 1f))
                            .background(color2, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${bucket.avgDiastolic.toInt()}", fontSize = 10.sp, color = color2)
                }
            }
        }
    }
}

// ==========================================
// 3. LIFESTYLE SCREEN (Minds Screenshot 4)
// ==========================================
enum class LifeSection { NONE, SMOKING, WATER, EXERCISE, MOOD }

@Composable
fun LifestyleScreen(viewModel: HealthViewModel, lang: String) {
    val records by viewModel.lifestyleRecords.collectAsState()
    var activeSub by remember { mutableStateOf(LifeSection.NONE) }
    
    fun trans(key: String): String = LocalStrings.get(key, lang)

    val waterToday = records.filter { it.type == "water" && isToday(it.timestamp) }.sumOf { it.amount.toDouble() }.toFloat()
    val smokingToday = records.filter { it.type == "smoking" && isToday(it.timestamp) }.sumOf { it.amount.toDouble() }.toInt()
    val activeMinsToday = records.filter { it.type == "exercise" && isToday(it.timestamp) }.sumOf { it.amount.toDouble() }.toInt()

    if (activeSub == LifeSection.NONE) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                LifestyleMenuButton(
                    title = trans("smoking"),
                    subtitle = "Today: $smokingToday pieces",
                    onClick = { activeSub = LifeSection.SMOKING }
                )
            }

            item {
                LifestyleMenuButton(
                    title = trans("water_intake"),
                    subtitle = "Today: ${waterToday.toInt()} ml",
                    onClick = { activeSub = LifeSection.WATER }
                )
            }

            item {
                LifestyleMenuButton(
                    title = trans("exercise"),
                    subtitle = "Today: $activeMinsToday mins",
                    onClick = { activeSub = LifeSection.EXERCISE }
                )
            }

            item {
                val moodList by viewModel.moodRecords.collectAsState()
                val todayMood = moodList.firstOrNull { isToday(it.timestamp) }
                val moodEmoji = when(todayMood?.score) {
                    1 -> "😢"; 2 -> "😕"; 3 -> "😐"; 4 -> "🙂"; 5 -> "😄"
                    else -> null
                }
                LifestyleMenuButton(
                    title = trans("mood"),
                    subtitle = if (moodEmoji != null) "Today: $moodEmoji (${todayMood!!.score}/5)" else "Not logged today",
                    onClick = { activeSub = LifeSection.MOOD }
                )
            }

            // High aesthetic match cyan "+250ml water" pill button!
            item {
                Button(
                    onClick = { viewModel.quickAddWater() },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal),
                    modifier = Modifier
                        .wrapContentWidth()
                        .height(48.dp)
                        .padding(top = 8.dp)
                ) {
                    Text(trans("quick_water"), color = SlateDarkBg, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else if (activeSub == LifeSection.MOOD) {
        MoodDetailView(viewModel = viewModel, lang = lang, onBack = { activeSub = LifeSection.NONE })
    } else {
        // Detailed log list for selected category inside Lifestyle Tracker
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { activeSub = LifeSection.NONE }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when(activeSub) {
                        LifeSection.SMOKING -> trans("smoking")
                        LifeSection.WATER -> trans("water_intake")
                        else -> trans("exercise")
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Water goal progress bar
            if (activeSub == LifeSection.WATER) {
                val waterGoal by viewModel.waterGoalMl.collectAsState()
                val waterToday2 = records.filter { it.type == "water" && isToday(it.timestamp) }.sumOf { it.amount.toDouble() }.toFloat()
                val progress = (waterToday2 / waterGoal).coerceIn(0f, 1f)
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${waterToday2.toInt()} ml", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HighlightTeal)
                        Text("Goal: ${waterGoal} ml", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    androidx.compose.material3.LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = HighlightTeal,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text("${(progress * 100).toInt()}% of daily goal", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Input Row
            var numStr by remember { mutableStateOf("") }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = numStr,
                    onValueChange = { numStr = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text("Log amount") },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        val v = numStr.toFloatOrNull() ?: 1f
                        val typeStr = when(activeSub) {
                            LifeSection.SMOKING -> "smoking"
                            LifeSection.WATER -> "water"
                            else -> "exercise"
                        }
                        viewModel.addLifestyleEntry(typeStr, v)
                        numStr = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal)
                ) {
                    Text("Add", color = SlateDarkBg, fontWeight = FontWeight.Bold)
                }
            }

            // History List
            val subRecords = records.filter {
                it.type == when(activeSub) {
                    LifeSection.SMOKING -> "smoking"
                    LifeSection.WATER -> "water"
                    else -> "exercise"
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(subRecords) { r ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val amountLabel = when(activeSub) {
                                    LifeSection.SMOKING -> "${r.amount.toInt()} pieces"
                                    LifeSection.WATER -> "${r.amount.toInt()} ml"
                                    else -> "${r.amount.toInt()} mins"
                                }
                                Text(amountLabel, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(viewModel.formatDate(r.timestamp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.deleteLifestyle(r) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoodDetailView(viewModel: HealthViewModel, lang: String, onBack: () -> Unit) {
    val records by viewModel.moodRecords.collectAsState()
    var selectedScore by remember { mutableStateOf(3) }
    var notesStr by remember { mutableStateOf("") }
    val moodEmojis = listOf("😢", "😕", "😐", "🙂", "😄")
    val moodLabels = listOf("Very Bad", "Bad", "Neutral", "Good", "Great")

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Mood Log", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("How are you feeling?", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    moodEmojis.forEachIndexed { index, emoji ->
                        val score = index + 1
                        val isSelected = selectedScore == score
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) HighlightTeal.copy(alpha = 0.2f) else Color.Transparent)
                                .clickable { selectedScore = score }
                                .padding(8.dp)
                        ) {
                            Text(emoji, fontSize = 28.sp)
                            Text(score.toString(), fontSize = 10.sp, color = if (isSelected) HighlightTeal else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Text(moodLabels[selectedScore - 1], fontSize = 13.sp, color = HighlightTeal, fontWeight = FontWeight.Medium, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                OutlinedTextField(
                    value = notesStr,
                    onValueChange = { notesStr = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = {
                        viewModel.addMood(selectedScore, notesStr)
                        notesStr = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal)
                ) {
                    Text("Log Mood", color = SlateDarkBg, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(records) { r ->
                val emoji = moodEmojis.getOrElse(r.score - 1) { "😐" }
                val label = moodLabels.getOrElse(r.score - 1) { "Neutral" }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(emoji, fontSize = 24.sp)
                            Column {
                                Text("$label (${r.score}/5)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                if (r.notes.isNotEmpty()) Text(r.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(viewModel.formatDate(r.timestamp), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }
                        IconButton(onClick = { viewModel.deleteMood(r) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LifestyleMenuButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Open", tint = HighlightTeal)
        }
    }
}

fun isToday(timestamp: Long): Boolean {
    val cal = Calendar.getInstance()
    val todayDay = cal.get(Calendar.DAY_OF_YEAR)
    val todayYear = cal.get(Calendar.YEAR)
    cal.timeInMillis = timestamp
    return cal.get(Calendar.DAY_OF_YEAR) == todayDay && cal.get(Calendar.YEAR) == todayYear
}

// ==========================================
// 4. MORE SCREEN (Minds Screenshot 2)
// ==========================================
enum class MoreSection { NONE, ATTACHMENTS, EMERGENCY, SUMMARY, SEARCH, AI_ASSISTANT, FAMILY }

@Composable
fun MoreScreen(viewModel: HealthViewModel, lang: String) {
    var activeSub by remember { mutableStateOf(MoreSection.NONE) }
    fun trans(key: String): String = LocalStrings.get(key, lang)

    if (activeSub == MoreSection.NONE) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val sections = listOf(
                Pair("attachments", MoreSection.ATTACHMENTS),
                Pair("emergency_info", MoreSection.EMERGENCY),
                Pair("family_members", MoreSection.FAMILY),
                Pair("health_summary", MoreSection.SUMMARY),
                Pair("search", MoreSection.SEARCH),
                Pair("ai_health_assistant", MoreSection.AI_ASSISTANT)
            )

            items(sections) { (stringKey, section) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .clickable { activeSub = section },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(trans(stringKey), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Icon(Icons.Default.ChevronRight, contentDescription = "Chevron", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    } else {
        // Overlay container showing detailed menu item flows
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { activeSub = MoreSection.NONE }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(modifier = Modifier.width(8.dp))
                val titleString = when(activeSub) {
                    MoreSection.ATTACHMENTS -> trans("attachments")
                    MoreSection.EMERGENCY -> trans("emergency_info")
                    MoreSection.FAMILY -> trans("family_members")
                    MoreSection.SUMMARY -> trans("health_summary")
                    MoreSection.SEARCH -> trans("search")
                    else -> trans("ai_health_assistant")
                }
                Text(titleString, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }

            Box(modifier = Modifier.weight(1f)) {
                when(activeSub) {
                    MoreSection.ATTACHMENTS -> DocumentAttachmentsView(viewModel, lang)
                    MoreSection.EMERGENCY -> EmergencyCardView(viewModel, lang)
                    MoreSection.FAMILY -> FamilyMembersView(viewModel, lang)
                    MoreSection.SUMMARY -> HealthSummaryView(viewModel, lang)
                    MoreSection.SEARCH -> SearchJournalView(viewModel, lang)
                    MoreSection.AI_ASSISTANT -> AIHealthAssistantView(viewModel, lang)
                    else -> {}
                }
            }
        }
    }
}

// ------------------------------------------
// SUB-VIEWS FOR MORE SCREEN
// ------------------------------------------

// Document Attachments
@Composable
fun DocumentAttachmentsView(viewModel: HealthViewModel, lang: String) {
    val context = LocalContext.current
    val list by viewModel.attachments.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var attachTitle by remember { mutableStateOf("") }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                // Grant persistent permission if possible or store string
                try {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: Exception) {}
                selectedUri = uri
                showDialog = true
            }
        }
    )

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } catch (e: Exception) {}
                selectedUri = uri
                showDialog = true
            }
        }
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    pickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal)
            ) {
                Text(LocalStrings.get("select_image", lang), color = SlateDarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Button(
                onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal)
            ) {
                Text("Select PDF", color = SlateDarkBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        if (list.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No attachments loaded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(list) { r ->
                    Card(
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            val isPdf = remember(r.fileUri) {
                                r.fileUri.endsWith(".pdf", ignoreCase = true) ||
                                    context.contentResolver.getType(Uri.parse(r.fileUri)) == "application/pdf"
                            }
                            if (isPdf) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = r.title,
                                        tint = AlertRed,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            } else {
                                AsyncImage(
                                    model = r.fileUri,
                                    contentDescription = r.title,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp)
                                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                        .background(Color.Black)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(r.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                    Text(viewModel.formatDateOnly(r.timestamp), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { viewModel.deleteAttachment(r) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Enter Attachment Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    OutlinedTextField(
                        value = attachTitle,
                        onValueChange = { attachTitle = it },
                        label = { Text("Title / Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                selectedUri?.let { uri ->
                                    val title = if (attachTitle.isEmpty()) "Medical Attachment" else attachTitle
                                    viewModel.addAttachment(title, uri.toString(), "")
                                }
                                showDialog = false
                                attachTitle = ""
                                selectedUri = null
                            }
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

// Emergency Details Card Form
@Composable
fun EmergencyCardView(viewModel: HealthViewModel, lang: String) {
    val info by viewModel.emergencyInfo.collectAsState()

    var name by remember { mutableStateOf("") }
    var blood by remember { mutableStateOf("") }
    var conditions by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var cName by remember { mutableStateOf("") }
    var cPhone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var childrenStr by remember { mutableStateOf("0") }

    LaunchedEffect(info) {
        info?.let {
            name = it.fullName
            blood = it.bloodType
            conditions = it.chronicConditions
            allergies = it.allergies
            cName = it.contactName
            cPhone = it.contactPhone
            notes = it.additionalNotes
            sex = it.sex
            childrenStr = it.numberOfChildren.toString()
        }
    }

    fun trans(key: String): String = LocalStrings.get(key, lang)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "This card displays critical medical context if first responders need to view your records instantly.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(trans("full_name")) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = blood,
                onValueChange = { blood = it },
                label = { Text(trans("blood_type")) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text("Sex", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Male", "Female").forEach { option ->
                    val selected = sex == option
                    OutlinedButton(
                        onClick = { sex = option },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) HighlightTeal.copy(alpha = 0.15f) else Color.Transparent,
                            contentColor = if (selected) HighlightTeal else MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(
                            1.5.dp,
                            if (selected) HighlightTeal else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Text(option, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }

        if (sex == "Female") {
            item {
                OutlinedTextField(
                    value = childrenStr,
                    onValueChange = { if (it.all { c -> c.isDigit() }) childrenStr = it },
                    label = { Text("Number of Children") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            OutlinedTextField(
                value = conditions,
                onValueChange = { conditions = it },
                label = { Text(trans("chronic_conditions")) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = allergies,
                onValueChange = { allergies = it },
                label = { Text(trans("allergies")) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = cName,
                onValueChange = { cName = it },
                label = { Text(trans("emergency_contact")) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = cPhone,
                onValueChange = { cPhone = it },
                label = { Text(trans("emergency_phone")) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(trans("additional_notes")) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
        }

        item {
            Button(
                onClick = {
                    viewModel.saveEmergencyCard(
                        name, blood, conditions, allergies, cName, cPhone, notes,
                        sex = sex,
                        numberOfChildren = childrenStr.toIntOrNull() ?: 0
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal)
            ) {
                Text("Save Changes", color = SlateDarkBg, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Health Summary
@Composable
fun HealthSummaryView(viewModel: HealthViewModel, lang: String) {
    val context = LocalContext.current
    val bpList by viewModel.bloodPressureRecords.collectAsState()
    val weightList by viewModel.weightRecords.collectAsState()
    val sleepList by viewModel.sleepRecords.collectAsState()
    val medications by viewModel.medications.collectAsState()

    val bpLatest = bpList.firstOrNull()
    val weightLatest = weightList.firstOrNull()
    val sleepLatest = sleepList.firstOrNull()

    fun trans(key: String): String = LocalStrings.get(key, lang)

    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .verticalScroll(rememberScrollState()),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Compiled Diagnostic Summary", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = HighlightTeal)
                Text("LATEST DIAGNOSTIC VITAL METRICS:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                Text(
                    text = "• Blood Pressure: ${bpLatest?.let { "${it.systolic}/${it.diastolic} mmHg (HR: ${it.heartRate} bpm)" } ?: "Not logged"}\n" +
                           "• Body Weight: ${weightLatest?.let { "${it.weightKg} kg" } ?: "Not logged"}\n" +
                           "• Daily Sleep: ${sleepLatest?.let { "${it.hours} hours" } ?: "Not logged"}",
                    fontSize = 14.sp
                )

                Text("REGISTERED CONTINUOUS DRUG DOSAGE:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                val activeMeds = medications.filter { it.isActive }
                if (activeMeds.isEmpty()) {
                    Text("• No active medications registered in database.", fontSize = 14.sp)
                } else {
                    activeMeds.forEach { m ->
                        Text("• ${m.name}: Dosage: ${m.dosage} | Freq: ${m.frequency}", fontSize = 14.sp)
                    }
                }

                Button(
                    onClick = { viewModel.printPDFReport(context) },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal)
                ) {
                    Icon(Icons.Default.Print, contentDescription = "Print/Save PDF", tint = SlateDarkBg)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Print / Save as Polished PDF", color = SlateDarkBg, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { viewModel.shareHTMLReport(context) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HighlightTeal)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = HighlightTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share HTML Report", color = HighlightTeal, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { viewModel.exportData(context, "CSV") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HighlightTeal)
                ) {
                    Icon(Icons.Default.GridOn, contentDescription = "Export CSV", tint = HighlightTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export CSV (Excel)", color = HighlightTeal, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Search across all items
@Composable
fun SearchJournalView(viewModel: HealthViewModel, lang: String) {
    val q by viewModel.searchQuery.collectAsState()
    
    val bpList by viewModel.bloodPressureRecords.collectAsState()
    val weightList by viewModel.weightRecords.collectAsState()
    val symptomsList by viewModel.symptoms.collectAsState()
    val medsList by viewModel.medications.collectAsState()

    fun trans(key: String): String = LocalStrings.get(key, lang)

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = q,
            onValueChange = { viewModel.updateSearchQuery(it) },
            label = { Text("Search comments / medication logs / symptoms...") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (q.trim().isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Type any term above to search instantly.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Search in blood pressure comments
                val filteredBp = bpList.filter { it.notes.lowercase(Locale.ROOT).contains(q.lowercase(Locale.ROOT)) }
                if (filteredBp.isNotEmpty()) {
                    item { Text("Blood Pressure Comments", fontWeight = FontWeight.Bold, color = HighlightTeal, fontSize = 12.sp) }
                    items(filteredBp) { r ->
                        SearchMatchRow(title = "BP Reading: ${r.systolic}/${r.diastolic}", text = r.notes, date = viewModel.formatDate(r.timestamp))
                    }
                }

                // Search in Weight Logs
                val filteredWeight = weightList.filter { it.notes.lowercase(Locale.ROOT).contains(q.lowercase(Locale.ROOT)) }
                if (filteredWeight.isNotEmpty()) {
                    item { Text("Weight Entry Comments", fontWeight = FontWeight.Bold, color = HighlightTeal, fontSize = 12.sp) }
                    items(filteredWeight) { r ->
                        SearchMatchRow(title = "Weight: ${r.weightKg} kg", text = r.notes, date = viewModel.formatDate(r.timestamp))
                    }
                }

                // Search in Medicines
                val filteredMeds = medsList.filter { it.name.lowercase(Locale.ROOT).contains(q.lowercase(Locale.ROOT)) || it.dosage.lowercase(Locale.ROOT).contains(q.lowercase(Locale.ROOT)) }
                if (filteredMeds.isNotEmpty()) {
                    item { Text("Medication Matches", fontWeight = FontWeight.Bold, color = HighlightTeal, fontSize = 12.sp) }
                    items(filteredMeds) { r ->
                        SearchMatchRow(title = r.name, text = "Dosage: ${r.dosage} | Frequency: ${r.frequency}", date = "")
                    }
                }

                // Search in Symptoms
                val filteredSymptoms = symptomsList.filter { it.symptomName.lowercase(Locale.ROOT).contains(q.lowercase(Locale.ROOT)) || it.notes.lowercase(Locale.ROOT).contains(q.lowercase(Locale.ROOT)) }
                if (filteredSymptoms.isNotEmpty()) {
                    item { Text("Symptom Logs", fontWeight = FontWeight.Bold, color = HighlightTeal, fontSize = 12.sp) }
                    items(filteredSymptoms) { r ->
                        SearchMatchRow(title = "${r.symptomName} (${r.severity})", text = r.notes, date = viewModel.formatDate(r.timestamp))
                    }
                }
            }
        }
    }
}

@Composable
fun SearchMatchRow(title: String, text: String, date: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (text.isNotEmpty()) {
                Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (date.isNotEmpty()) {
                Text(date, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
    }
}

// AI Health Assistant View (Supports Gemini, OpenAI, DeepSeek per settings selection!)
@Composable
fun FamilyMembersView(viewModel: HealthViewModel, lang: String) {
    val members by viewModel.familyMembers.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<com.example.data.FamilyMemberProfile?>(null) }
    var nameStr by remember { mutableStateOf("") }
    var relStr by remember { mutableStateOf("") }
    var dobStr by remember { mutableStateOf("") }
    var bloodStr by remember { mutableStateOf("") }
    var notesStr by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = {
                editingMember = null
                nameStr = ""; relStr = ""; dobStr = ""; bloodStr = ""; notesStr = ""
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal)
        ) {
            Text("Add Family Member", color = SlateDarkBg, fontWeight = FontWeight.Bold)
        }

        if (members.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No family members yet.\nTap + to add one.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(members) { m ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(m.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                if (m.relationship.isNotEmpty()) Text(m.relationship, fontSize = 13.sp, color = HighlightTeal)
                                if (m.dateOfBirth.isNotEmpty()) Text("DOB: ${m.dateOfBirth}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (m.bloodType.isNotEmpty()) Text("Blood: ${m.bloodType}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (m.notes.isNotEmpty()) Text(m.notes, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                            }
                            Row {
                                IconButton(onClick = {
                                    editingMember = m
                                    nameStr = m.name; relStr = m.relationship; dobStr = m.dateOfBirth
                                    bloodStr = m.bloodType; notesStr = m.notes
                                    showDialog = true
                                }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = HighlightTeal)
                                }
                                IconButton(onClick = { viewModel.deleteFamilyMember(m) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AlertRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        Dialog(onDismissRequest = { showDialog = false; editingMember = null }) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (editingMember != null) "Edit Member" else "Add Family Member", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    OutlinedTextField(value = nameStr, onValueChange = { nameStr = it }, label = { Text("Full Name *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = relStr, onValueChange = { relStr = it }, label = { Text("Relationship (e.g. Son, Spouse)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = dobStr, onValueChange = { dobStr = it }, label = { Text("Date of Birth (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bloodStr, onValueChange = { bloodStr = it }, label = { Text("Blood Type") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = notesStr, onValueChange = { notesStr = it }, label = { Text("Health Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showDialog = false; editingMember = null }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (nameStr.trim().isNotEmpty()) {
                                    val existing = editingMember
                                    if (existing != null) {
                                        viewModel.updateFamilyMember(existing.copy(name = nameStr, relationship = relStr, dateOfBirth = dobStr, bloodType = bloodStr, notes = notesStr))
                                    } else {
                                        viewModel.addFamilyMember(nameStr, relStr, dobStr, bloodStr, notesStr)
                                    }
                                    showDialog = false; editingMember = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal)
                        ) { Text(if (editingMember != null) "Update" else "Save", color = SlateDarkBg) }
                    }
                }
            }
        }
    }
}

@Composable
fun AIHealthAssistantView(viewModel: HealthViewModel, lang: String) {
    val response by viewModel.aiResponse.collectAsState()
    val loading by viewModel.aiLoading.collectAsState()
    val weeklyNarrative by viewModel.weeklyNarrative.collectAsState()
    val weeklyLoading by viewModel.weeklyNarrativeLoading.collectAsState()
    val provider by viewModel.aiProvider.collectAsState()

    var userQueryInput by remember { mutableStateOf("") }
    var showWeekly by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Weekly summary toggle row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { showWeekly = false },
                border = BorderStroke(1.dp, if (!showWeekly) HighlightTeal else MaterialTheme.colorScheme.outline),
                modifier = Modifier.weight(1f)
            ) { Text("Chat", color = if (!showWeekly) HighlightTeal else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
            OutlinedButton(
                onClick = { showWeekly = true },
                border = BorderStroke(1.dp, if (showWeekly) HighlightTeal else MaterialTheme.colorScheme.outline),
                modifier = Modifier.weight(1f)
            ) { Text("Weekly Summary", color = if (showWeekly) HighlightTeal else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (showWeekly) {
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    if (weeklyLoading) {
                        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = HighlightTeal)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Generating your 7-day health summary...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Text("AI Weekly Health Report", fontWeight = FontWeight.Black, fontSize = 12.sp, color = HighlightTeal, modifier = Modifier.padding(bottom = 8.dp))
                            if (weeklyNarrative.isEmpty()) {
                                Text("Tap \"Generate\" below to get a personalised 7-day health narrative powered by AI.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Text(weeklyNarrative, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { viewModel.generateWeeklyNarrative() },
                enabled = !weeklyLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal)
            ) {
                Text("Generate Weekly Summary", color = SlateDarkBg, fontWeight = FontWeight.Bold)
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = HighlightTeal)
                    } else {
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Text(
                                text = if (provider.isNotEmpty()) "Assistant Powered By $provider" else "Health GPT Assistant",
                                fontWeight = FontWeight.Black, fontSize = 12.sp, color = HighlightTeal,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            if (response.isEmpty()) {
                                Text(
                                    text = "Ask anything about your health records, metrics, trend indicators or suggest health tips.\n\n" +
                                           "Sample Prompts:\n" +
                                           "• \"What does my latest Blood Pressure reading indicate?\"\n" +
                                           "• \"What is my dynamic BMI and how can I bring it to normal range?\"\n" +
                                           "• \"Analyze my lipid logs (cholesterol & triglycerides).\"",
                                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(text = response, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = userQueryInput,
                    onValueChange = { userQueryInput = it },
                    label = { Text("Ask your health assistant...") },
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { viewModel.queryAIAssistant(userQueryInput); userQueryInput = "" },
                    enabled = userQueryInput.trim().isNotEmpty() && !loading,
                    colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal)
                ) { Text("Send", color = SlateDarkBg, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

// ==========================================
// 5. SETTINGS SCREEN (Minds Screenshot 5)
// ==========================================
@Composable
fun SettingsScreen(viewModel: HealthViewModel, lang: String) {
    val context = LocalContext.current
    val currentLang by viewModel.language.collectAsState()
    val themeMode by viewModel.theme.collectAsState()
    val heightVal by viewModel.heightCm.collectAsState()
    
    val providerState by viewModel.aiProvider.collectAsState()
    val modelNameState by viewModel.modelName.collectAsState()
    val apiKeyVal by viewModel.apiKey.collectAsState()

    var heightInput by remember { mutableStateOf(heightVal.toString()) }
    var keyInput by remember { mutableStateOf(apiKeyVal) }
    var modelInput by remember { mutableStateOf(modelNameState) }
    var selectProvider by remember { mutableStateOf(providerState) }

    var importStatusMessage by remember { mutableStateOf("") }
    var showImportStatus by remember { mutableStateOf(false) }

    val jsonPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importLocalBackup(context, uri) { success, msg ->
                importStatusMessage = if (success) "Restore successful: All medical entries have been imported!" else "Restore failed: $msg"
                showImportStatus = true
            }
        }
    }

    val localBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.writeLocalBackupToUri(context, uri) { success, msg ->
                importStatusMessage = if (success) "Backup saved successfully!" else "Backup failed: $msg"
                showImportStatus = true
            }
        }
    }

    if (showImportStatus) {
        AlertDialog(
            onDismissRequest = { showImportStatus = false },
            title = { Text("Restore Status", color = HighlightTeal, fontWeight = FontWeight.Bold) },
            text = { Text(importStatusMessage, color = Color.White) },
            confirmButton = {
                TextButton(onClick = { showImportStatus = false }) {
                    Text("OK", color = HighlightTeal, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = SlateCardBg
        )
    }

    fun trans(key: String): String = LocalStrings.get(key, lang)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Theme setting header & selector matching Screenshot 5!
        item {
            Text(trans("theme"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("SYSTEM", "LIGHT", "DARK").forEach { mode ->
                    val active = themeMode.uppercase() == mode
                    Button(
                        onClick = { viewModel.updateTheme(mode.lowercase()) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(mode, color = if (active) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                }
            }
        }

        // Language toggle buttons matching Screenshot 5!
        item {
            Text(trans("language"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Pair("English", "en").let { (lbl, code) ->
                    val active = currentLang == code
                    Button(
                        onClick = { viewModel.updateLanguage(code) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(lbl, color = if (active) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                }
                Pair("العربية", "ar").let { (lbl, code) ->
                    val active = currentLang == code
                    Button(
                        modifier = Modifier.widthIn(min = 100.dp),
                        onClick = { viewModel.updateLanguage(code) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(lbl, color = if (active) Color.White else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                }
            }
        }

        // Height BMI setup row matching Screenshot 5!
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(trans("height_bmi"), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    )
                    Button(
                        onClick = {
                            heightInput.toFloatOrNull()?.let { viewModel.saveHeight(it) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text(trans("save_height"), color = SlateDarkBg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // AI Provider vertical selector matches layout from Screenshot 5!
        item {
            Text(trans("ai_provider"), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("OpenAI", "DeepSeek", "Gemini").forEach { prov ->
                    val active = selectProvider == prov
                    Button(
                        onClick = {
                            selectProvider = prov
                            modelInput = when(prov) {
                                "OpenAI" -> "gpt-4o-mini"
                                "DeepSeek" -> "deepseek-chat"
                                else -> "gemini-3.5-flash"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(prov, color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        // Model Name & Secure Text API Input Fields matching Screenshot 5!
        item {
            OutlinedTextField(
                value = modelInput,
                onValueChange = { modelInput = it },
                label = { Text(trans("model_name")) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text(trans("api_key")) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation()
            )
        }

        item {
            Button(
                onClick = { viewModel.saveAISettings(selectProvider, modelInput, keyInput) },
                colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(trans("save_api_key"), color = SlateDarkBg, fontWeight = FontWeight.Bold)
            }
        }

        // Export Data Section
        item {
            Text(trans("export_data"), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HighlightTeal)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { viewModel.exportData(context, "TXT") }, modifier = Modifier.weight(1f)) { Text("TXT", fontSize = 11.sp) }
                Button(onClick = { viewModel.exportData(context, "CSV") }, modifier = Modifier.weight(1f)) { Text("CSV", fontSize = 11.sp) }
                Button(onClick = { viewModel.exportData(context, "JSON") }, modifier = Modifier.weight(1f)) { Text("JSON", fontSize = 11.sp) }
                Button(onClick = { viewModel.printPDFReport(context) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("PDF", fontSize = 11.sp) }
            }
        }

        // Export for Doctor (DataDoctorPro-compatible CSV)
        item {
            Text("Export for Doctor", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HighlightTeal)

            val lastDoctorExportTime by viewModel.lastDoctorExportTime.collectAsState()

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Share a CSV of your Blood Pressure, Blood Sugar, Weight, Sleep, Symptom and Lab Result records with your doctor (e.g. for import into DataDoctorPro).",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = if (lastDoctorExportTime > 0L) "Last sent: ${viewModel.formatDate(lastDoctorExportTime)}" else "Last sent: Never",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = { viewModel.exportForDoctor(context, "last30") }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)) {
                            Text("30 Days", fontSize = 10.sp)
                        }
                        OutlinedButton(onClick = { viewModel.exportForDoctor(context, "last90") }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)) {
                            Text("90 Days", fontSize = 10.sp)
                        }
                        OutlinedButton(onClick = { viewModel.exportForDoctor(context, "all") }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)) {
                            Text("All Time", fontSize = 10.sp)
                        }
                    }
                    Button(
                        onClick = { viewModel.exportForDoctor(context, "sinceLast") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = SlateDarkBg)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send New Records Since Last Export", color = SlateDarkBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }

        // WhatsApp-Style Backup & Sync Center
        item {
            Text("Backup & Sync (WhatsApp Style)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HighlightTeal)
            
            val lastLocalTime by viewModel.lastLocalBackupTime.collectAsState()
            val lastDriveTime by viewModel.lastDriveBackupTime.collectAsState()
            val syncStatus by viewModel.googleDriveSyncStatus.collectAsState()
            val isSyncing by viewModel.googleDriveIsSyncing.collectAsState()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCardBg),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Backup,
                            contentDescription = "Backup Center",
                            tint = HighlightTeal,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Backup & Restore options for your medical journal records.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            lineHeight = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    // Local Backup Section
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Local Device Backup",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        val localTimeStr = if (lastLocalTime > 0L) viewModel.formatDate(lastLocalTime) else "Never"
                        Text(
                            text = "Last backup: $localTimeStr",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { localBackupLauncher.launch("MyMedicalJournal_backup_${System.currentTimeMillis()}.json") },
                                colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp), tint = SlateDarkBg)
                                    Text("Back Up", color = SlateDarkBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Button(
                                onClick = { jsonPickerLauncher.launch("application/json") },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                    Text("Restore", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    // Google Drive Backup Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Google Drive Backup & Restore",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                        val driveTimeStr = if (lastDriveTime > 0L) viewModel.formatDate(lastDriveTime) else "Never"
                        Text(
                            text = "Last sync: $driveTimeStr",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Google Drive Connection: $syncStatus",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = HighlightTeal
                        )

                        val isGoogleSignedIn by viewModel.isGoogleSignedIn.collectAsState()
                        val googleAccountEmail by viewModel.googleAccountEmail.collectAsState()
                        val googleClientId by viewModel.googleClientId.collectAsState()
                        val manualDriveToken by viewModel.manualDriveToken.collectAsState()

                        var showGoogleAuthWebView by remember { mutableStateOf(false) }
                        var showOAuthSetupDialog by remember { mutableStateOf(false) }
                        var isAdvancedOptionsExpanded by remember { mutableStateOf(false) }

                        if (showOAuthSetupDialog) {
                            AlertDialog(
                                onDismissRequest = { showOAuthSetupDialog = false },
                                title = { Text("Google Sign-In Setup Required") },
                                text = {
                                    Column {
                                        Text(
                                            "The built-in Google Client ID is just a placeholder and isn't registered with Google, " +
                                            "so sign-in fails with \"Error 401: invalid_client\".\n\n" +
                                            "To enable Google Drive backup, create your own free OAuth Client ID:\n\n" +
                                            "1. Go to console.cloud.google.com and create (or select) a project.\n" +
                                            "2. Enable the \"Google Drive API\".\n" +
                                            "3. Go to \"APIs & Services\" > \"Credentials\" > \"Create Credentials\" > \"OAuth client ID\".\n" +
                                            "4. Choose \"Web application\".\n" +
                                            "5. Under \"Authorized redirect URIs\", add: http://localhost\n" +
                                            "6. Copy the generated Client ID and paste it below in \"Advanced Drive Settings\" > \"Private OAuth Web Client ID (PKCE)\".",
                                            fontSize = 13.sp
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = {
                                        isAdvancedOptionsExpanded = true
                                        showOAuthSetupDialog = false
                                    }) { Text("Open Advanced Settings") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showOAuthSetupDialog = false }) { Text("Close") }
                                }
                            )
                        }

                        if (showGoogleAuthWebView) {
                            val authUrl = remember { viewModel.getGoogleAuthUrl() }
                            Dialog(
                                onDismissRequest = { showGoogleAuthWebView = false },
                                properties = DialogProperties(
                                    dismissOnBackPress = true,
                                    dismissOnClickOutside = false,
                                    usePlatformDefaultWidth = false
                                )
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = SlateDarkBg),
                                    border = BorderStroke(1.dp, HighlightTeal.copy(alpha = 0.5f))
                                ) {
                                    Column(modifier = Modifier.fillMaxSize()) {
                                        // Header
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Sign in with Google",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp
                                            )
                                            IconButton(onClick = { showGoogleAuthWebView = false }) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Close",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                                        // WebView
                                        AndroidView(
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                            factory = { ctx ->
                                                WebView(ctx).apply {
                                                    settings.apply {
                                                        javaScriptEnabled = true
                                                        domStorageEnabled = true
                                                        databaseEnabled = true
                                                        cacheMode = WebSettings.LOAD_DEFAULT
                                                        userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"
                                                    }
                                                    webViewClient = object : WebViewClient() {
                                                        private var authCodeHandled = false

                                                        override fun shouldOverrideUrlLoading(
                                                            view: WebView?,
                                                            request: WebResourceRequest?
                                                        ): Boolean {
                                                            val url = request?.url?.toString() ?: ""
                                                            if (url.startsWith("http://localhost")) {
                                                                val code = request?.url?.getQueryParameter("code")
                                                                if (code != null && !authCodeHandled) {
                                                                    authCodeHandled = true
                                                                    showGoogleAuthWebView = false
                                                                    viewModel.completeGoogleSignIn(code) { success, msg ->
                                                                        importStatusMessage = if (success) "Google account connected! You can now sync to Drive." else "Sign-in failed: $msg"
                                                                        showImportStatus = true
                                                                    }
                                                                }
                                                                return true
                                                            }
                                                            return false
                                                        }

                                                        override fun onPageFinished(view: WebView?, url: String?) {
                                                            super.onPageFinished(view, url)
                                                            if (authCodeHandled) return
                                                            view?.evaluateJavascript(
                                                                "document.body ? document.body.innerText.includes('invalid_client') : false"
                                                            ) { result ->
                                                                if (result == "true") {
                                                                    showGoogleAuthWebView = false
                                                                    showOAuthSetupDialog = true
                                                                }
                                                            }
                                                        }
                                                    }
                                                    loadUrl(authUrl)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Connected Account status or Sign-In button
                        if (isGoogleSignedIn) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                border = BorderStroke(1.dp, HighlightTeal.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(HighlightTeal)
                                        )
                                        Column {
                                            Text(
                                                text = "Connected Account",
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                            Text(
                                                text = googleAccountEmail.ifEmpty { "Patient Drive Storage" },
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    
                                    Button(
                                        onClick = { viewModel.googleSignOut() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.15f)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                                    ) {
                                        Text("Disconnect", color = Color.Red, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (viewModel.isUsingPlaceholderClientId()) {
                                        showOAuthSetupDialog = true
                                    } else {
                                        showGoogleAuthWebView = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "G",
                                        color = Color(0xFF4285F4),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "Sign In with Google",
                                        color = SlateDarkBg,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Sync Up and Pull Down trigger row
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.syncToGoogleDrive(context) },
                                enabled = !isSyncing,
                                colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = SlateDarkBg, strokeWidth = 1.5.dp)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(14.dp), tint = SlateDarkBg)
                                        Text("Sync Up", color = SlateDarkBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Button(
                                onClick = { viewModel.restoreFromGoogleDrive(context) },
                                enabled = !isSyncing,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 1.5.dp)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                        Text("Pull Down", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Expandable Advanced Google Settings
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isAdvancedOptionsExpanded = !isAdvancedOptionsExpanded }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Advanced Drive Settings",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                fontWeight = FontWeight.SemiBold
                            )
                            Icon(
                                imageVector = if (isAdvancedOptionsExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (isAdvancedOptionsExpanded) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Custom Client ID
                                    OutlinedTextField(
                                        value = googleClientId,
                                        onValueChange = { viewModel.updateGoogleClientId(it) },
                                        label = { Text("Private OAuth Web Client ID (PKCE)", fontSize = 10.sp) },
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = HighlightTeal,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                        )
                                    )
                                    
                                    // Backwards-compatible manual token
                                    var isTokenVisible by remember { mutableStateOf(false) }
                                    OutlinedTextField(
                                        value = manualDriveToken,
                                        onValueChange = { viewModel.updateManualDriveToken(it) },
                                        label = { Text("Manual Google Drive Access Token", fontSize = 10.sp) },
                                        visualTransformation = if (isTokenVisible) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                                        trailingIcon = {
                                            IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                                                val icon = if (isTokenVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                                Icon(icon, contentDescription = "Toggle Visibility", modifier = Modifier.size(16.dp))
                                            }
                                        },
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = HighlightTeal,
                                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f)
                                        )
                                    )

                                    Text(
                                        text = "To guarantee maximum personal backup privacy, you can configure your own OAuth Web Application Client ID in your Google Cloud Developer Console. Make sure to add authorized redirect URI 'http://localhost' (no client secret is needed). This isolates your data entirely from other app instances.",
                                        fontSize = 9.sp,
                                        lineHeight = 11.sp,
                                        color = Color.White.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    // Media ZIP Attachment Export
                    Button(
                        onClick = { viewModel.backupZIP(context) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Export Media Support ZIP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }



        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun DateTimePickerInline(
    context: android.content.Context,
    timestamp: Long,
    onTimestampChange: (Long) -> Unit,
    label: String = "Record Date & Time"
) {
    val calendar = remember { java.util.Calendar.getInstance() }.apply { timeInMillis = timestamp }
    
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = HighlightTeal,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Date Button
            OutlinedButton(
                onClick = {
                    android.app.DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            calendar.set(java.util.Calendar.YEAR, year)
                            calendar.set(java.util.Calendar.MONTH, month)
                            calendar.set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth)
                            onTimestampChange(calendar.timeInMillis)
                        },
                        calendar.get(java.util.Calendar.YEAR),
                        calendar.get(java.util.Calendar.MONTH),
                        calendar.get(java.util.Calendar.DAY_OF_MONTH)
                    ).show()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = HighlightTeal)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = "Date", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                val dateSdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                Text(dateSdf.format(calendar.time), fontSize = 13.sp)
            }
            
            // Time Button
            OutlinedButton(
                onClick = {
                    android.app.TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                            calendar.set(java.util.Calendar.MINUTE, minute)
                            onTimestampChange(calendar.timeInMillis)
                        },
                        calendar.get(java.util.Calendar.HOUR_OF_DAY),
                        calendar.get(java.util.Calendar.MINUTE),
                        true
                    ).show()
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = HighlightTeal)
            ) {
                Icon(Icons.Default.Schedule, contentDescription = "Time", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                val timeSdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                Text(timeSdf.format(calendar.time), fontSize = 13.sp)
            }
        }
    }
}
