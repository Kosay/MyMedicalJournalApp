package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.MedicalJournalApp
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.FormBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.MedicalJournalApp
import com.example.workers.HealthAlertsWorker
import com.example.workers.MedicationReminderWorker
import java.util.concurrent.TimeUnit

class HealthViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MedicalJournalApp
    private val repository = app.repository
    private val sharedPrefs = app.getSharedPreferences("medical_journal_prefs", Context.MODE_PRIVATE)

    // --- Bottom Navigation State ---
    private val _selectedTab = MutableStateFlow("home")
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    fun selectTab(tab: String) {
        _selectedTab.value = tab
    }

    // --- Settings States ---
    private val _language = MutableStateFlow(sharedPrefs.getString("lang", "en") ?: "en")
    val language: StateFlow<String> = _language.asStateFlow()

    private val _theme = MutableStateFlow(sharedPrefs.getString("theme", "dark") ?: "dark")
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _heightCm = MutableStateFlow(sharedPrefs.getFloat("heightCm", 174.0f))
    val heightCm: StateFlow<Float> = _heightCm.asStateFlow()

    private val _aiProvider = MutableStateFlow(sharedPrefs.getString("aiProvider", "Gemini") ?: "Gemini")
    val aiProvider: StateFlow<String> = _aiProvider.asStateFlow()

    private val _modelName = MutableStateFlow(sharedPrefs.getString("modelName", "gemini-3.5-flash") ?: "gemini-3.5-flash")
    val modelName: StateFlow<String> = _modelName.asStateFlow()

    private val _apiKey = MutableStateFlow(sharedPrefs.getString("apiKey", "") ?: "")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _manualDriveToken = MutableStateFlow(sharedPrefs.getString("manual_drive_token", "") ?: "")
    val manualDriveToken: StateFlow<String> = _manualDriveToken.asStateFlow()

    fun updateManualDriveToken(token: String) {
        _manualDriveToken.value = token
        sharedPrefs.edit().putString("manual_drive_token", token).apply()
    }

    private val _googleClientId = MutableStateFlow(sharedPrefs.getString("google_client_id", DEFAULT_GOOGLE_CLIENT_ID) ?: DEFAULT_GOOGLE_CLIENT_ID)
    val googleClientId: StateFlow<String> = _googleClientId.asStateFlow()

    fun updateGoogleClientId(clientId: String) {
        _googleClientId.value = clientId
        sharedPrefs.edit().putString("google_client_id", clientId).apply()
    }

    // The bundled default Client ID is a placeholder and is not registered with any
    // Google Cloud project, so Google always returns "Error 401: invalid_client" for it.
    // Users must create their own OAuth Web Application Client ID to use Drive sync.
    fun isUsingPlaceholderClientId(): Boolean {
        val current = _googleClientId.value.trim()
        return current.isEmpty() || current == DEFAULT_GOOGLE_CLIENT_ID
    }

    companion object {
        const val DEFAULT_GOOGLE_CLIENT_ID = "1046908332468-bgcrp7ve4776u26j5it93fef6n6hco2q.apps.googleusercontent.com"
        private const val NOTIF_ID_DANGER = 1000
    }

    // --- Danger alerts ---
    private val _dangerAlert = MutableStateFlow<DangerAlert?>(null)
    val dangerAlert: StateFlow<DangerAlert?> = _dangerAlert.asStateFlow()

    fun dismissDangerAlert() { _dangerAlert.value = null }

    private fun evaluateBpDanger(systolic: Int, diastolic: Int, heartRate: Int) {
        _dangerAlert.value = when {
            systolic >= 180 || diastolic >= 120 -> DangerAlert(
                DangerSeverity.CRISIS, "Hypertensive Crisis",
                "BP at $systolic/$diastolic mmHg is a medical emergency. Seek emergency care immediately."
            )
            systolic >= 140 || diastolic >= 90 -> DangerAlert(
                DangerSeverity.HIGH, "Stage 2 High Blood Pressure",
                "BP at $systolic/$diastolic mmHg is very high. Contact your doctor today."
            )
            systolic < 90 || diastolic < 60 -> DangerAlert(
                DangerSeverity.WARNING, "Low Blood Pressure",
                "BP at $systolic/$diastolic mmHg is low. Rest and monitor for dizziness or fainting."
            )
            heartRate > 130 -> DangerAlert(
                DangerSeverity.WARNING, "Elevated Heart Rate",
                "Heart rate at $heartRate bpm is high. If at rest, contact your doctor."
            )
            heartRate < 45 -> DangerAlert(
                DangerSeverity.WARNING, "Low Heart Rate",
                "Heart rate at $heartRate bpm is very low. If at rest, consult your doctor."
            )
            else -> null
        }
        _dangerAlert.value?.let { sendDangerNotification(it) }
    }

    private fun evaluateBloodSugarDanger(valueMgDl: Float) {
        _dangerAlert.value = when {
            valueMgDl < 70f -> DangerAlert(
                DangerSeverity.CRISIS, "Hypoglycemia",
                "Blood sugar at ${valueMgDl.toInt()} mg/dL is dangerously low. Consume fast-acting glucose immediately."
            )
            valueMgDl > 250f -> DangerAlert(
                DangerSeverity.HIGH, "Very High Blood Sugar",
                "Blood sugar at ${valueMgDl.toInt()} mg/dL is very high. Check for ketones and contact your doctor."
            )
            valueMgDl > 180f -> DangerAlert(
                DangerSeverity.WARNING, "Elevated Blood Sugar",
                "Blood sugar at ${valueMgDl.toInt()} mg/dL is above target range. Review recent meals and medication."
            )
            else -> null
        }
        _dangerAlert.value?.let { sendDangerNotification(it) }
    }

    private fun sendDangerNotification(alert: DangerAlert) {
        val ctx = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            if (!nm.areNotificationsEnabled()) return
        }
        val notification = NotificationCompat.Builder(ctx, MedicalJournalApp.CHANNEL_ALERTS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(alert.title)
            .setContentText(alert.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
            .setPriority(
                if (alert.severity == DangerSeverity.CRISIS) NotificationCompat.PRIORITY_MAX
                else NotificationCompat.PRIORITY_HIGH
            )
            .setAutoCancel(true)
            .build()
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID_DANGER, notification)
    }

    fun scheduleDailyPatternCheck() {
        val request = PeriodicWorkRequestBuilder<HealthAlertsWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(getApplication())
            .enqueueUniquePeriodicWork(
                HealthAlertsWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    fun scheduleMedicationReminder() {
        val request = PeriodicWorkRequestBuilder<MedicationReminderWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(getApplication())
            .enqueueUniquePeriodicWork(
                MedicationReminderWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    private val _googleAccountEmail = MutableStateFlow(sharedPrefs.getString("google_account_email", "") ?: "")
    val googleAccountEmail: StateFlow<String> = _googleAccountEmail.asStateFlow()

    private val _isGoogleSignedIn = MutableStateFlow(!sharedPrefs.getString("google_oauth2_refresh_token", "").isNullOrEmpty())
    val isGoogleSignedIn: StateFlow<Boolean> = _isGoogleSignedIn.asStateFlow()

    // --- Search query state ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Flows from DB ---
    val bloodPressureRecords: StateFlow<List<BloodPressureRecord>> = repository.allBloodPressure
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weightRecords: StateFlow<List<WeightRecord>> = repository.allWeight
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val medications: StateFlow<List<MedicationRecord>> = repository.allMedications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val symptoms: StateFlow<List<SymptomRecord>> = repository.allSymptoms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sleepRecords: StateFlow<List<SleepRecord>> = repository.allSleep
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val labResults: StateFlow<List<LabResultRecord>> = repository.allLabResults
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lifestyleRecords: StateFlow<List<LifestyleRecord>> = repository.allLifestyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attachments: StateFlow<List<AttachmentRecord>> = repository.allAttachments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val emergencyInfo: StateFlow<EmergencyInfo?> = repository.emergencyInfo
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val bloodSugarRecords: StateFlow<List<BloodSugarRecord>> = repository.allBloodSugar
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val moodRecords: StateFlow<List<MoodRecord>> = repository.allMoods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val medicationDoses: StateFlow<List<MedicationDoseRecord>> = repository.allMedicationDoses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val familyMembers: StateFlow<List<FamilyMemberProfile>> = repository.allFamilyMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appointments: StateFlow<List<AppointmentRecord>> = repository.allAppointments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Water goal (ml/day) stored in SharedPrefs
    private val _waterGoalMl = MutableStateFlow(sharedPrefs.getInt("waterGoalMl", 2000))
    val waterGoalMl: StateFlow<Int> = _waterGoalMl.asStateFlow()

    fun updateWaterGoal(ml: Int) {
        _waterGoalMl.value = ml
        sharedPrefs.edit().putInt("waterGoalMl", ml).apply()
    }

    // Weekly AI narrative
    private val _weeklyNarrative = MutableStateFlow("")
    val weeklyNarrative: StateFlow<String> = _weeklyNarrative.asStateFlow()

    private val _weeklyNarrativeLoading = MutableStateFlow(false)
    val weeklyNarrativeLoading: StateFlow<Boolean> = _weeklyNarrativeLoading.asStateFlow()

    // --- Blood Pressure pattern analysis (sleep, smoking, activity correlation) ---
    val bpCorrelationData: StateFlow<BpCorrelationData> = combine(
        bloodPressureRecords, sleepRecords, lifestyleRecords
    ) { bpList, sleepList, lifestyleList ->
        val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val sleepByDay = sleepList.groupBy { dayFormat.format(Date(it.timestamp)) }
            .mapValues { (_, recs) -> recs.sumOf { it.hours.toDouble() }.toFloat() }

        val smokingByDay = lifestyleList.filter { it.type == "smoking" }
            .groupBy { dayFormat.format(Date(it.timestamp)) }
            .mapValues { (_, recs) -> recs.sumOf { it.amount.toDouble() }.toFloat() }

        val exerciseByDay = lifestyleList.filter { it.type == "exercise" }
            .groupBy { dayFormat.format(Date(it.timestamp)) }
            .mapValues { (_, recs) -> recs.sumOf { it.amount.toDouble() }.toFloat() }

        val sleepOrder = listOf("<6h", "6-10h", ">10h")
        val smokingOrder = listOf("0-5", "6-10", "11-20", ">20")
        val activityOrder = listOf("Active", "No Activity")

        val sleepItems = mutableListOf<Pair<String, BloodPressureRecord>>()
        val smokingItems = mutableListOf<Pair<String, BloodPressureRecord>>()
        val activityItems = mutableListOf<Pair<String, BloodPressureRecord>>()
        val sleepSmokingItems = mutableListOf<Pair<String, BloodPressureRecord>>()
        val sleepActivityItems = mutableListOf<Pair<String, BloodPressureRecord>>()
        val smokingActivityItems = mutableListOf<Pair<String, BloodPressureRecord>>()
        val allThreeItems = mutableListOf<Pair<String, BloodPressureRecord>>()

        for (bp in bpList) {
            val dayKey = dayFormat.format(Date(bp.timestamp))
            val sleepHours = sleepByDay[dayKey]
            val smokingCount = smokingByDay[dayKey] ?: 0f
            val exerciseMinutes = exerciseByDay[dayKey] ?: 0f

            val smokingLabel = smokingBucketLabel(smokingCount)
            val activityLabel = activityBucketLabel(exerciseMinutes)
            smokingItems.add(smokingLabel to bp)
            activityItems.add(activityLabel to bp)
            smokingActivityItems.add("$smokingLabel + $activityLabel" to bp)

            if (sleepHours != null) {
                val sleepLabel = sleepBucketLabel(sleepHours)
                sleepItems.add(sleepLabel to bp)
                sleepSmokingItems.add("$sleepLabel + $smokingLabel" to bp)
                sleepActivityItems.add("$sleepLabel + $activityLabel" to bp)
                allThreeItems.add("$sleepLabel + $smokingLabel + $activityLabel" to bp)
            }
        }

        BpCorrelationData(
            bySleep = buildBpBuckets(sleepItems, sleepOrder),
            bySmoking = buildBpBuckets(smokingItems, smokingOrder),
            byActivity = buildBpBuckets(activityItems, activityOrder),
            bySleepSmoking = buildBpBuckets(sleepSmokingItems, sleepOrder.flatMap { s -> smokingOrder.map { c -> "$s + $c" } }),
            bySleepActivity = buildBpBuckets(sleepActivityItems, sleepOrder.flatMap { s -> activityOrder.map { a -> "$s + $a" } }),
            bySmokingActivity = buildBpBuckets(smokingActivityItems, smokingOrder.flatMap { c -> activityOrder.map { a -> "$c + $a" } }),
            byAllThree = buildBpBuckets(allThreeItems, sleepOrder.flatMap { s -> smokingOrder.flatMap { c -> activityOrder.map { a -> "$s + $c + $a" } } })
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BpCorrelationData())

    private val _lastLocalBackupTime = MutableStateFlow(sharedPrefs.getLong("last_local_backup_time", 0L))
    val lastLocalBackupTime: StateFlow<Long> = _lastLocalBackupTime.asStateFlow()

    private val _lastDriveBackupTime = MutableStateFlow(sharedPrefs.getLong("last_drive_backup_time", 0L))
    val lastDriveBackupTime: StateFlow<Long> = _lastDriveBackupTime.asStateFlow()

    private val _lastDoctorExportTime = MutableStateFlow(sharedPrefs.getLong("last_doctor_export_time", 0L))
    val lastDoctorExportTime: StateFlow<Long> = _lastDoctorExportTime.asStateFlow()

    private val _googleDriveSyncStatus = MutableStateFlow("Not Connected")
    val googleDriveSyncStatus: StateFlow<String> = _googleDriveSyncStatus.asStateFlow()

    private val _googleDriveIsSyncing = MutableStateFlow(false)
    val googleDriveIsSyncing: StateFlow<Boolean> = _googleDriveIsSyncing.asStateFlow()



    // --- Database Operations ---
    fun addBloodSugar(value: Float, unit: String = "mg/dL", category: String, notes: String, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertBloodSugar(
                BloodSugarRecord(
                    value = value,
                    unit = unit,
                    category = category,
                    notes = notes,
                    timestamp = timestamp
                )
            )
            evaluateBloodSugarDanger(normalizeBloodSugarToMgDl(value, unit))
        }
    }

    fun deleteBloodSugar(record: BloodSugarRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBloodSugar(record)
        }
    }

    fun updateBloodSugar(record: BloodSugarRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertBloodSugar(record)
            evaluateBloodSugarDanger(normalizeBloodSugarToMgDl(record.value, record.unit))
        }
    }

    fun addBloodPressure(systolic: Int, diastolic: Int, heartRate: Int, notes: String, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertBloodPressure(
                BloodPressureRecord(
                    systolic = systolic,
                    diastolic = diastolic,
                    heartRate = heartRate,
                    timestamp = timestamp,
                    notes = notes
                )
            )
            evaluateBpDanger(systolic, diastolic, heartRate)
        }
    }

    fun deleteBloodPressure(record: BloodPressureRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBloodPressure(record)
        }
    }

    fun updateBloodPressure(record: BloodPressureRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertBloodPressure(record)
            evaluateBpDanger(record.systolic, record.diastolic, record.heartRate)
        }
    }

    fun addWeight(weightKg: Float, notes: String, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertWeight(
                WeightRecord(
                    weightKg = weightKg,
                    timestamp = timestamp,
                    notes = notes
                )
            )
        }
    }

    fun deleteWeight(record: WeightRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteWeight(record)
        }
    }

    fun updateWeight(record: WeightRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertWeight(record)
        }
    }

    fun addMedication(name: String, dosage: String, frequency: String, isActive: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMedication(
                MedicationRecord(
                    name = name,
                    dosage = dosage,
                    frequency = frequency,
                    isActive = isActive
                )
            )
        }
    }

    fun deleteMedication(record: MedicationRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMedication(record)
        }
    }

    fun updateMedication(record: MedicationRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMedication(record)
        }
    }

    fun addSymptom(symptomName: String, severity: String, notes: String, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSymptom(
                SymptomRecord(
                    symptomName = symptomName,
                    severity = severity,
                    timestamp = timestamp,
                    notes = notes
                )
            )
        }
    }

    fun deleteSymptom(record: SymptomRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSymptom(record)
        }
    }

    fun updateSymptom(record: SymptomRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSymptom(record)
        }
    }

    fun addSleep(hours: Float, notes: String, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSleep(
                SleepRecord(
                    hours = hours,
                    timestamp = timestamp,
                    notes = notes
                )
            )
        }
    }

    fun deleteSleep(record: SleepRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSleep(record)
        }
    }

    fun updateSleep(record: SleepRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSleep(record)
        }
    }

    fun addLabResult(testName: String, value: Float, unit: String, referenceRange: String, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertLabResult(
                LabResultRecord(
                    testName = testName,
                    value = value,
                    unit = unit,
                    referenceRange = referenceRange,
                    timestamp = timestamp
                )
            )
        }
    }

    fun deleteLabResult(record: LabResultRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteLabResult(record)
        }
    }

    fun updateLabResult(record: LabResultRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertLabResult(record)
        }
    }

    fun quickAddWater() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertLifestyle(
                LifestyleRecord(
                    type = "water",
                    amount = 250f,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun addLifestyleEntry(type: String, amount: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertLifestyle(
                LifestyleRecord(
                    type = type,
                    amount = amount,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteLifestyle(record: LifestyleRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteLifestyle(record)
        }
    }

    fun addAttachment(title: String, fileUri: String, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertAttachment(
                AttachmentRecord(
                    title = title,
                    fileUri = fileUri,
                    notes = notes,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteAttachment(record: AttachmentRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAttachment(record)
        }
    }

    fun saveEmergencyCard(
        fullName: String,
        bloodType: String,
        chronicConditions: String,
        allergies: String,
        contactName: String,
        contactPhone: String,
        additionalNotes: String,
        sex: String,
        numberOfChildren: Int
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertEmergencyInfo(
                EmergencyInfo(
                    fullName = fullName,
                    bloodType = bloodType,
                    chronicConditions = chronicConditions,
                    allergies = allergies,
                    contactName = contactName,
                    contactPhone = contactPhone,
                    additionalNotes = additionalNotes,
                    sex = sex,
                    numberOfChildren = numberOfChildren
                )
            )
        }
    }

    // --- Mood ---
    fun addMood(score: Int, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMood(MoodRecord(score = score, notes = notes))
        }
    }

    fun deleteMood(record: MoodRecord) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteMood(record) }
    }

    // --- Medication Dose Log ---
    fun logMedicationDose(medication: com.example.data.MedicationRecord, taken: Boolean, notes: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMedicationDose(
                MedicationDoseRecord(
                    medicationId = medication.id,
                    medicationName = medication.name,
                    taken = taken,
                    notes = notes
                )
            )
        }
    }

    fun deleteMedicationDose(record: MedicationDoseRecord) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteMedicationDose(record) }
    }

    // --- Onboarding ---
    val onboardingCompleted: Boolean get() = sharedPrefs.getBoolean("onboarding_completed", false)

    fun completeOnboarding(info: EmergencyInfo, heightCm: Float, weightKg: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertEmergencyInfo(info)
            if (weightKg > 0f) {
                repository.insertWeight(WeightRecord(weightKg = weightKg, timestamp = System.currentTimeMillis(), notes = "Initial weight"))
            }
        }
        sharedPrefs.edit()
            .putBoolean("onboarding_completed", true)
            .putFloat("heightCm", heightCm)
            .apply()
        _emergencyInfo.value = info
    }

    // --- Family Members ---
    fun addFamilyMember(
        name: String, relationship: String, dateOfBirth: String, bloodType: String, notes: String,
        sex: String = "", chronicConditions: String = "", allergies: String = "",
        emergencyContactName: String = "", emergencyContactPhone: String = "",
        heightCm: Float = 0f, weightKg: Float = 0f
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertFamilyMember(
                FamilyMemberProfile(
                    name = name, relationship = relationship, dateOfBirth = dateOfBirth,
                    bloodType = bloodType, notes = notes, sex = sex,
                    chronicConditions = chronicConditions, allergies = allergies,
                    emergencyContactName = emergencyContactName, emergencyContactPhone = emergencyContactPhone,
                    heightCm = heightCm, weightKg = weightKg
                )
            )
        }
    }

    fun updateFamilyMember(profile: FamilyMemberProfile) {
        viewModelScope.launch(Dispatchers.IO) { repository.insertFamilyMember(profile) }
    }

    fun deleteFamilyMember(profile: FamilyMemberProfile) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteFamilyMember(profile) }
    }

    // --- Appointments ---
    fun addAppointment(title: String, type: String, dateTimestamp: Long, notes: String,
                       profileId: Int = 0, profileName: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertAppointment(
                AppointmentRecord(title = title, type = type, dateTimestamp = dateTimestamp,
                    notes = notes, profileId = profileId, profileName = profileName)
            )
        }
    }

    fun updateAppointment(record: AppointmentRecord) {
        viewModelScope.launch(Dispatchers.IO) { repository.updateAppointment(record) }
    }

    fun deleteAppointment(record: AppointmentRecord) {
        viewModelScope.launch(Dispatchers.IO) { repository.deleteAppointment(record) }
    }

    fun toggleAppointmentComplete(record: AppointmentRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateAppointment(record.copy(isCompleted = !record.isCompleted))
        }
    }

    // --- Per-profile export (local file share — no internet) ---
    fun exportProfileData(context: Context, familyMember: FamilyMemberProfile?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject()
                if (familyMember == null) {
                    // Main user profile
                    val info = _emergencyInfo.value
                    json.put("profile_type", "main_user")
                    json.put("name", info?.fullName ?: "")
                    json.put("blood_type", info?.bloodType ?: "")
                    json.put("sex", info?.sex ?: "")
                    json.put("chronic_conditions", info?.chronicConditions ?: "")
                    json.put("allergies", info?.allergies ?: "")
                    json.put("emergency_contact", info?.contactName ?: "")
                    json.put("emergency_phone", info?.contactPhone ?: "")
                    json.put("additional_notes", info?.additionalNotes ?: "")
                    json.put("blood_pressure", toJsonArray(bloodPressureRecords.value.map { r ->
                        JSONObject().put("systolic", r.systolic).put("diastolic", r.diastolic)
                            .put("heart_rate", r.heartRate).put("timestamp", formatDate(r.timestamp)).put("notes", r.notes)
                    }))
                    json.put("blood_sugar", toJsonArray(bloodSugarRecords.value.map { r ->
                        JSONObject().put("value", r.value).put("unit", r.unit).put("category", r.category)
                            .put("timestamp", formatDate(r.timestamp)).put("notes", r.notes)
                    }))
                    json.put("weight", toJsonArray(weightRecords.value.map { r ->
                        JSONObject().put("weight_kg", r.weightKg).put("timestamp", formatDate(r.timestamp)).put("notes", r.notes)
                    }))
                    json.put("medications", toJsonArray(medications.value.map { r ->
                        JSONObject().put("name", r.name).put("dosage", r.dosage).put("frequency", r.frequency).put("active", r.isActive)
                    }))
                    json.put("lab_results", toJsonArray(labResults.value.map { r ->
                        JSONObject().put("test", r.testName).put("value", r.value).put("unit", r.unit)
                            .put("ref_range", r.referenceRange).put("timestamp", formatDate(r.timestamp))
                    }))
                    json.put("symptoms", toJsonArray(symptoms.value.map { r ->
                        JSONObject().put("symptom", r.symptomName).put("severity", r.severity)
                            .put("timestamp", formatDate(r.timestamp)).put("notes", r.notes)
                    }))
                    json.put("sleep", toJsonArray(sleepRecords.value.map { r ->
                        JSONObject().put("hours", r.hours).put("timestamp", formatDate(r.timestamp)).put("notes", r.notes)
                    }))
                } else {
                    // Family member / child profile
                    json.put("profile_type", "family_member")
                    json.put("name", familyMember.name)
                    json.put("relationship", familyMember.relationship)
                    json.put("date_of_birth", familyMember.dateOfBirth)
                    json.put("blood_type", familyMember.bloodType)
                    json.put("sex", familyMember.sex)
                    json.put("height_cm", familyMember.heightCm)
                    json.put("weight_kg", familyMember.weightKg)
                    json.put("chronic_conditions", familyMember.chronicConditions)
                    json.put("allergies", familyMember.allergies)
                    json.put("emergency_contact_name", familyMember.emergencyContactName)
                    json.put("emergency_contact_phone", familyMember.emergencyContactPhone)
                    json.put("notes", familyMember.notes)
                }
                json.put("export_date", formatDate(System.currentTimeMillis()))
                json.put("app", "My Medical Journal")

                val profileLabel = familyMember?.name?.replace(" ", "_") ?: "main"
                val filename = "medical_export_${profileLabel}_${System.currentTimeMillis()}.json"
                val file = File(context.cacheDir, filename)
                file.writeText(json.toString(2))

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                shareFile(context, uri, "application/json", "Medical data — ${familyMember?.name ?: "My Profile"}")
            } catch (e: Exception) {
                Log.e("Export", "Profile export failed: ${e.localizedMessage}")
            }
        }
    }

    fun shareFileViaWhatsApp(context: Context, familyMember: FamilyMemberProfile?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val info = if (familyMember == null) _emergencyInfo.value else null
                val name = familyMember?.name ?: info?.fullName ?: "profile"
                val json = if (familyMember == null) generateJSON() else {
                    val obj = JSONObject()
                    obj.put("name", familyMember.name)
                    obj.put("relationship", familyMember.relationship)
                    obj.put("date_of_birth", familyMember.dateOfBirth)
                    obj.put("blood_type", familyMember.bloodType)
                    obj.put("sex", familyMember.sex)
                    obj.put("height_cm", familyMember.heightCm)
                    obj.put("weight_kg", familyMember.weightKg)
                    obj.put("chronic_conditions", familyMember.chronicConditions)
                    obj.put("allergies", familyMember.allergies)
                    obj.toString(2)
                }
                val filename = "whatsapp_share_${name.replace(" ", "_")}.json"
                val file = File(context.cacheDir, filename)
                file.writeText(json)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

                withContext(Dispatchers.Main) {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        setPackage("com.whatsapp")
                        type = "application/json"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val resolved = intent.resolveActivity(context.packageManager)
                    if (resolved != null) {
                        context.startActivity(intent)
                    } else {
                        // WhatsApp not installed — fall back to share sheet
                        shareFile(context, uri, "application/json", "Medical data — $name")
                    }
                }
            } catch (e: Exception) {
                Log.e("Export", "WhatsApp share failed: ${e.localizedMessage}")
            }
        }
    }

    private fun toJsonArray(list: List<JSONObject>): JSONArray {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        return arr
    }

    private fun shareFile(context: Context, uri: Uri, mimeType: String, subject: String) {
        viewModelScope.launch(Dispatchers.Main) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, subject)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share via...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        }
    }

    // --- Weekly AI Health Narrative ---
    fun generateWeeklyNarrative() {
        var key = _apiKey.value
        val provider = _aiProvider.value
        val model = _modelName.value

        if (key.isEmpty() && provider == "Gemini") {
            key = try {
                val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
                field.get(null) as? String ?: ""
            } catch (e: Exception) { "" }
        }

        if (key.isEmpty()) {
            _weeklyNarrative.value = "Please configure your API key in Settings to generate a weekly health summary."
            return
        }

        _weeklyNarrativeLoading.value = true
        _weeklyNarrative.value = ""

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val weekMs = 7L * 24 * 3600 * 1000
                val since = System.currentTimeMillis() - weekMs
                val sb = StringBuilder()

                sb.append("WEEKLY HEALTH SUMMARY (Last 7 Days):\n\n")

                val bpList = bloodPressureRecords.value.filter { it.timestamp >= since }
                if (bpList.isNotEmpty()) {
                    val avgSys = bpList.map { it.systolic }.average()
                    val avgDia = bpList.map { it.diastolic }.average()
                    val avgHr = bpList.map { it.heartRate }.average()
                    sb.append("Blood Pressure (${bpList.size} readings): Avg ${avgSys.toInt()}/${avgDia.toInt()} mmHg, HR ${avgHr.toInt()} bpm\n")
                }

                val sugarList = bloodSugarRecords.value.filter { it.timestamp >= since }
                if (sugarList.isNotEmpty()) {
                    val avg = sugarList.map { normalizeBloodSugarToMgDl(it.value, it.unit) }.average()
                    sb.append("Blood Sugar (${sugarList.size} readings): Avg ${avg.toInt()} mg/dL\n")
                }

                val weightList = weightRecords.value.filter { it.timestamp >= since }
                if (weightList.isNotEmpty()) {
                    sb.append("Weight: ${weightList.last().weightKg} kg → ${weightList.first().weightKg} kg (${weightList.size} entries)\n")
                }

                val sleepList = sleepRecords.value.filter { it.timestamp >= since }
                if (sleepList.isNotEmpty()) {
                    val avg = sleepList.map { it.hours }.average()
                    sb.append("Sleep: Avg ${String.format(Locale.US, "%.1f", avg)} hours/night (${sleepList.size} nights)\n")
                }

                val moodList = moodRecords.value.filter { it.timestamp >= since }
                if (moodList.isNotEmpty()) {
                    val avg = moodList.map { it.score }.average()
                    sb.append("Mood: Avg ${String.format(Locale.US, "%.1f", avg)}/5 (${moodList.size} logs)\n")
                }

                val activeMeds = medications.value.filter { it.isActive }
                if (activeMeds.isNotEmpty()) {
                    sb.append("Active Medications: ${activeMeds.joinToString { it.name }}\n")
                }

                val symptomList = symptoms.value.filter { it.timestamp >= since }
                if (symptomList.isNotEmpty()) {
                    sb.append("Symptoms this week: ${symptomList.joinToString { "${it.symptomName} (${it.severity})" }}\n")
                }

                val lifestyle = lifestyleRecords.value.filter { it.timestamp >= since }
                val waterTotal = lifestyle.filter { it.type == "water" }.sumOf { it.amount.toDouble() }
                val exerciseTotal = lifestyle.filter { it.type == "exercise" }.sumOf { it.amount.toDouble() }
                if (waterTotal > 0) sb.append("Total water intake: ${waterTotal.toInt()} ml\n")
                if (exerciseTotal > 0) sb.append("Total exercise: ${exerciseTotal.toInt()} minutes\n")

                val prompt = "You are a personal health assistant. Analyze this patient's 7-day health data and write a warm, clear, 3-4 paragraph weekly health narrative. Highlight trends, improvements, concerns, and actionable suggestions. Always remind the user that this is informational only and not a medical diagnosis.\n\n${sb}"

                val narrative = when (provider) {
                    "Gemini" -> callGeminiAPI(key, model, prompt)
                    "OpenAI", "DeepSeek" -> callChatCompletionsAPI(provider, key, model, prompt)
                    else -> "Unsupported AI Provider"
                }
                _weeklyNarrative.value = narrative
            } catch (e: Exception) {
                _weeklyNarrative.value = "Error generating summary: ${e.localizedMessage}"
            } finally {
                _weeklyNarrativeLoading.value = false
            }
        }
    }

    // --- Settings Updaters ---
    fun updateLanguage(lang: String) {
        _language.value = lang
        sharedPrefs.edit().putString("lang", lang).apply()
    }

    fun updateTheme(themeMode: String) {
        _theme.value = themeMode
        sharedPrefs.edit().putString("theme", themeMode).apply()
    }

    fun saveHeight(height: Float) {
        _heightCm.value = height
        sharedPrefs.edit().putFloat("heightCm", height).apply()
    }

    fun saveAISettings(provider: String, model: String, key: String) {
        _aiProvider.value = provider
        _modelName.value = model
        _apiKey.value = key
        sharedPrefs.edit()
            .putString("aiProvider", provider)
            .putString("modelName", model)
            .putString("apiKey", key)
            .apply()
    }

    // --- AI Assistant Query ---
    private val _aiResponse = MutableStateFlow<String>("")
    val aiResponse: StateFlow<String> = _aiResponse.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    fun queryAIAssistant(userMsg: String) {
        var key = _apiKey.value
        val provider = _aiProvider.value
        val model = _modelName.value

        // Safely check for developer's fallback GEMINI_API_KEY if the user hasn't set their own
        if (key.isEmpty() && provider == "Gemini") {
            key = try {
                val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
                field.get(null) as? String ?: ""
            } catch (e: Exception) {
                ""
            }
        }

        if (key.isEmpty()) {
            _aiResponse.value = if (_language.value == "ar") 
                "يرجى تحديد مفتاح API الخاص بك في الإعدادات أولاً لمتابعة المحادثة. (يمكنك الحصول على مفتاح Gemini مجانًا في 30 ثانية من Google AI Studio)" 
              else 
                "Please configure your API key in Settings first. (You can get a free, private Gemini API key in 30 seconds from Google AI Studio to unlock immediate local access!)"
            return
        }

        _aiLoading.value = true
        _aiResponse.value = ""

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Compile context summary to prompt
                val summary = generateContextSummary()
                val fullPrompt = "You are a highly capable offline-first Personal Health Assistant inside the 'My Medical Journal' Android app.\n" +
                        "Here is the user's latest logged health data:\n\n" +
                        summary + "\n\n" +
                        "User query: $userMsg\n\n" +
                        "Provide a concise, supportive informational response. Emphasize that your advice is not medical diagnostics or therapeutic decisions, and they should contact a healthcare professional."

                val response = when (provider) {
                    "Gemini" -> callGeminiAPI(key, model, fullPrompt)
                    "OpenAI", "DeepSeek" -> callChatCompletionsAPI(provider, key, model, fullPrompt)
                    else -> "Unsupported AI Provider"
                }

                _aiResponse.value = response
            } catch (e: Exception) {
                _aiResponse.value = "Error: ${e.localizedMessage ?: "Unknown network error or invalid API key."}"
            } finally {
                _aiLoading.value = false
            }
        }
    }

    private fun generateContextSummary(): String {
        val s = StringBuilder()
        val bp = bloodPressureRecords.value.firstOrNull()
        if (bp != null) {
            s.append("- Latest Blood Pressure: ${bp.systolic}/${bp.diastolic} mmHg (Heart Rate: ${bp.heartRate} bpm) on ${formatDate(bp.timestamp)}\n")
        }
        val w = weightRecords.value.firstOrNull()
        if (w != null) {
            val bmi = w.weightKg / ((_heightCm.value / 100f) * (_heightCm.value / 100f))
            s.append("- Latest Weight: ${w.weightKg} kg (Calculated BMI: ${String.format(Locale.US, "%.1f", bmi)}) on ${formatDate(w.timestamp)}\n")
        }
        val sl = sleepRecords.value.firstOrNull()
        if (sl != null) {
            s.append("- Latest Sleep: ${sl.hours} hours on ${formatDate(sl.timestamp)}\n")
        }
        val bs = bloodSugarRecords.value.firstOrNull()
        if (bs != null) {
            s.append("- Latest Blood Sugar: ${bs.value} ${bs.unit} (${bs.category}) on ${formatDate(bs.timestamp)}\n")
        }
        val activeMeds = medications.value.filter { it.isActive }
        if (activeMeds.isNotEmpty()) {
            s.append("- Active Medications: ")
            s.append(activeMeds.joinToString { "${it.name} (${it.dosage}, ${it.frequency})" })
            s.append("\n")
        }
        val recentSymptoms = symptoms.value.take(3)
        if (recentSymptoms.isNotEmpty()) {
            s.append("- Recent Symptoms: ")
            s.append(recentSymptoms.joinToString { "${it.symptomName} (${it.severity})" })
            s.append("\n")
        }
        val labs = labResults.value.take(4)
        if (labs.isNotEmpty()) {
            s.append("- Latest Labs: ")
            s.append(labs.joinToString { "${it.testName}: ${it.value} ${it.unit}" })
            s.append("\n")
        }
        return s.toString()
    }

    private suspend fun callGeminiAPI(apiKey: String, model: String, prompt: String): String {
        val client = OkHttpClient()
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        
        val jsonPayload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val code = response.code
            val bodyStr = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return "Gemini API error ($code): ${extractErrorMsg(bodyStr)}"
            }
            val root = JSONObject(bodyStr)
            val candidates = root.getJSONArray("candidates")
            if (candidates.length() > 0) {
                val first = candidates.getJSONObject(0)
                val content = first.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                if (parts.length() > 0) {
                    return parts.getJSONObject(0).getString("text")
                }
            }
            return "No content returned."
        }
    }

    private suspend fun callChatCompletionsAPI(provider: String, apiKey: String, model: String, prompt: String): String {
        val client = OkHttpClient()
        val url = if (provider.lowercase() == "deepseek") {
            "https://api.deepseek.com/v1/chat/completions"
        } else {
            "https://api.openai.com/v1/chat/completions"
        }

        val jsonPayload = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("temperature", 0.7)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val code = response.code
            val bodyStr = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                return "$provider error ($code): ${extractErrorMsg(bodyStr)}"
            }
            val root = JSONObject(bodyStr)
            val choices = root.getJSONArray("choices")
            if (choices.length() > 0) {
                val msg = choices.getJSONObject(0).getJSONObject("message")
                return msg.getString("content")
            }
            return "No content returned."
        }
    }

    private fun extractErrorMsg(jsonStr: String): String {
        return try {
            val obj = JSONObject(jsonStr)
            if (obj.has("error")) {
                val err = obj.get("error")
                if (err is JSONObject && err.has("message")) {
                    err.getString("message")
                } else {
                    err.toString()
                }
            } else {
                jsonStr
            }
        } catch (e: Exception) {
            jsonStr.take(150)
        }
    }

    // --- Helper Formatting ---
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDateOnly(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // --- Data Export & Sharing ---
    fun exportData(context: Context, format: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentString = when (format.uppercase()) {
                    "CSV" -> generateCSV()
                    "JSON" -> generateJSON()
                    "TXT" -> generateTXT()
                    else -> generateTXT()
                }

                val ext = format.lowercase()
                val mimeType = when (format.uppercase()) {
                    "CSV" -> "text/csv"
                    "JSON" -> "application/json"
                    "TXT" -> "text/plain"
                    else -> "text/plain"
                }

                val filename = "medical_journal_export_${System.currentTimeMillis()}.$ext"
                val cacheFile = File(context.cacheDir, filename)
                cacheFile.writeText(contentString)

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    cacheFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "My Medical Journal Export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooserIntent = Intent.createChooser(intent, "Share Report via...").apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                withContext(Dispatchers.Main) {
                    context.startActivity(chooserIntent)
                }
            } catch (e: Exception) {
                Log.e("Export", "Error exporting data: ${e.localizedMessage}")
            }
        }
    }

    fun generateHTMLContent(context: Context): String {
        val html = java.lang.StringBuilder()
        html.append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>Health Record Summary</title><style>")
        html.append("body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; color: #1E293B; background-color: #FFFFFF; padding: 30px; margin: 0; line-height: 1.5; }")
        html.append(".header-container { border-bottom: 3px solid #14B8A6; padding-bottom: 12px; margin-bottom: 24px; }")
        html.append(".header-title { font-size: 26px; font-weight: 800; color: #0F172A; text-transform: uppercase; margin: 0; letter-spacing: 0.5px; }")
        html.append(".header-sub { font-size: 13px; color: #64748B; margin: 4px 0 0 0; }")
        html.append(".section-card { background: #F8FAFC; border: 1px solid #E2E8F0; border-radius: 8px; padding: 16px; margin-bottom: 20px; }")
        html.append(".section-title { font-size: 16px; font-weight: 700; color: #0F172A; border-bottom: 2px solid #E2E8F0; padding-bottom: 6px; margin-top: 0; margin-bottom: 12px; }")
        html.append("table { width: 100%; border-collapse: collapse; margin-top: 8px; margin-bottom: 8px; }")
        html.append("th, td { padding: 8px 12px; font-size: 13px; text-align: left; border-bottom: 1px solid #E2E8F0; }")
        html.append("th { background-color: #0F172A; color: #FFFFFF; font-weight: 600; text-transform: uppercase; font-size: 11px; letter-spacing: 0.5px; }")
        html.append("tr:nth-child(even) { background-color: #F8FAFC; }")
        html.append(".badge { display: inline-block; padding: 2px 6px; font-size: 11px; font-weight: bold; border-radius: 4px; text-transform: uppercase; }")
        html.append(".badge-active { background-color: #D1FAE5; color: #065F46; }")
        html.append(".badge-inactive { background-color: #FEE2E2; color: #991B1B; }")
        html.append(".badge-elevated { background-color: #FEF3C7; color: #92400E; }")
        html.append(".badge-critical { background-color: #FEE2E2; color: #991B1B; }")
        html.append(".badge-normal { background-color: #D1FAE5; color: #065F46; }")
        html.append(".meta-info { margin-bottom: 20px; font-size: 12px; color: #64748B; display: flex; justify-content: space-between; }")
        html.append(".footer { margin-top: 45px; font-size: 10px; text-align: center; color: #94A3B8; border-top: 1px solid #E2E8F0; padding-top: 12px; }")
        html.append("</style></head><body>")
        
        html.append("<div class='header-container'>")
        html.append("<h1 class='header-title'>Patient Medical History</h1>")
        html.append("<p class='header-sub'>Confidential Personal Health Record Ledger</p>")
        html.append("</div>")

        html.append("<div class='meta-info'>")
        html.append("<span><strong>Report Generated:</strong> ${formatDate(System.currentTimeMillis())}</span>")
        html.append("<span><strong>App:</strong> My Medical Journal Secure Sync</span>")
        html.append("</div>")

        // 1. Emergency Info
        emergencyInfo.value?.let { info ->
            html.append("<div class='section-card'>")
            html.append("<div class='section-title'>Emergency Contact & Information</div>")
            html.append("<table>")
            html.append("<tr><td><strong>Full Name:</strong></td><td>${info.fullName}</td><td><strong>Blood Type:</strong></td><td><span class='badge badge-critical'>${info.bloodType}</span></td></tr>")
            val sexLabel = if (info.sex.isNotEmpty()) info.sex else "Not specified"
            val childrenLabel = if (info.sex == "Female") "${info.numberOfChildren}" else "N/A"
            html.append("<tr><td><strong>Sex:</strong></td><td>$sexLabel</td><td><strong>Number of Children:</strong></td><td>$childrenLabel</td></tr>")
            html.append("<tr><td><strong>Chronic Conditions:</strong></td><td>${info.chronicConditions}</td><td><strong>Known Allergies:</strong></td><td>${info.allergies}</td></tr>")
            html.append("<tr><td><strong>Emergency Contact:</strong></td><td>${info.contactName}</td><td><strong>Contact Phone:</strong></td><td>${info.contactPhone}</td></tr>")
            if (info.additionalNotes.isNotEmpty()) {
                html.append("<tr><td><strong>Clinical Notes:</strong></td><td colspan='3'>${info.additionalNotes}</td></tr>")
            }
            html.append("</table>")
            html.append("</div>")
        }

        // 2. Blood Pressure Logs
        val bpList = bloodPressureRecords.value
        if (bpList.isNotEmpty()) {
            html.append("<div class='section-card'>")
            html.append("<div class='section-title'>Blood Pressure Logs</div>")
            html.append("<table><tr><th>Date/Time</th><th>Reading (mmHg)</th><th>Heart Rate (bpm)</th><th>Status</th><th>Clinical Comments</th></tr>")
            bpList.forEach {
                val statusBadge = if (it.systolic >= 140 || it.diastolic >= 90) {
                    "<span class='badge badge-critical'>Stage 2 Hypertension</span>"
                } else if (it.systolic >= 130 || it.diastolic >= 80) {
                    "<span class='badge badge-elevated'>Stage 1 Hypertension</span>"
                } else if (it.systolic >= 120) {
                    "<span class='badge badge-elevated'>Elevated</span>"
                } else {
                    "<span class='badge badge-normal'>Normal</span>"
                }
                html.append("<tr><td>${formatDate(it.timestamp)}</td><td><strong>${it.systolic} / ${it.diastolic}</strong></td><td>${it.heartRate} bpm</td><td>$statusBadge</td><td>${it.notes}</td></tr>")
            }
            html.append("</table>")
            html.append("</div>")
        }

        // 3. Blood Sugar Logs
        val sugarList = bloodSugarRecords.value
        if (sugarList.isNotEmpty()) {
            html.append("<div class='section-card'>")
            html.append("<div class='section-title'>Blood Sugar Logs (Glucose)</div>")
            html.append("<table><tr><th>Date/Time</th><th>Value</th><th>Category</th><th>Status</th><th>Notes</th></tr>")
            sugarList.forEach {
                val statusBadge = if (it.value >= 140) {
                    "<span class='badge badge-critical'>High (Hyperglycemia)</span>"
                } else if (it.value < 70) {
                    "<span class='badge badge-critical'>Low (Hypoglycemia)</span>"
                } else {
                    "<span class='badge badge-normal'>Normal</span>"
                }
                html.append("<tr><td>${formatDate(it.timestamp)}</td><td><strong>${it.value} ${it.unit}</strong></td><td>${it.category}</td><td>$statusBadge</td><td>${it.notes}</td></tr>")
            }
            html.append("</table>")
            html.append("</div>")
        }

        // 4. Weight & BMI logs
        val weightList = weightRecords.value
        if (weightList.isNotEmpty()) {
            html.append("<div class='section-card'>")
            html.append("<div class='section-title'>Body Weight & BMI</div>")
            html.append("<table><tr><th>Date/Time</th><th>Weight</th><th>Calculated BMI</th><th>Status</th><th>Notes</th></tr>")
            weightList.forEach {
                val heightM = heightCm.value / 100f
                val bmi = if (heightM > 0) it.weightKg / (heightM * heightM) else 0f
                val bmiStr = if (bmi > 0) String.format(Locale.US, "%.1f", bmi) else "N/A"
                val bmiStatus = if (bmi <= 0) ""
                else if (bmi < 18.5) "<span class='badge badge-elevated'>Underweight</span>"
                else if (bmi < 25.0) "<span class='badge badge-normal'>Healthy weight</span>"
                else if (bmi < 30.0) "<span class='badge badge-elevated'>Overweight</span>"
                else "<span class='badge badge-critical'>Obesity</span>"
                
                html.append("<tr><td>${formatDate(it.timestamp)}</td><td><strong>${it.weightKg} kg</strong></td><td>$bmiStr</td><td>$bmiStatus</td><td>${it.notes}</td></tr>")
            }
            html.append("</table>")
            html.append("</div>")
        }

        // 5. Medications
        val medList = medications.value
        if (medList.isNotEmpty()) {
            html.append("<div class='section-card'>")
            html.append("<div class='section-title'>Registered Medications</div>")
            html.append("<table><tr><th>Medication Name</th><th>Dosage Form</th><th>Frequency Details</th><th>Therapeutic Status</th></tr>")
            medList.forEach {
                val statusBadge = if (it.isActive) "<span class='badge badge-active'>Active</span>" else "<span class='badge badge-inactive'>Inactive</span>"
                html.append("<tr><td><strong>${it.name}</strong></td><td>${it.dosage}</td><td>${it.frequency}</td><td>$statusBadge</td></tr>")
            }
            html.append("</table>")
            html.append("</div>")
        }

        // 6. Symptoms Tracker
        val symptomList = symptoms.value
        if (symptomList.isNotEmpty()) {
            html.append("<div class='section-card'>")
            html.append("<div class='section-title'>Symptom Records</div>")
            html.append("<table><tr><th>Date/Time</th><th>Symptom Name</th><th>Reported Severity</th><th>Patient Notes</th></tr>")
            symptomList.forEach {
                val sevBadge = when(it.severity.lowercase(Locale.US)) {
                    "severe" -> "<span class='badge badge-critical'>Severe</span>"
                    "moderate" -> "<span class='badge badge-elevated'>Moderate</span>"
                    else -> "<span class='badge badge-normal'>Mild</span>"
                }
                html.append("<tr><td>${formatDate(it.timestamp)}</td><td><strong>${it.symptomName}</strong></td><td>$sevBadge</td><td>${it.notes}</td></tr>")
            }
            html.append("</table>")
            html.append("</div>")
        }

        // 7. Sleep Logs
        val sleepList = sleepRecords.value
        if (sleepList.isNotEmpty()) {
            html.append("<div class='section-card'>")
            html.append("<div class='section-title'>Sleep Tracking Logs</div>")
            html.append("<table><tr><th>Date/Time</th><th>Sleep Duration</th><th>Status</th><th>Notes</th></tr>")
            sleepList.forEach {
                val statusBadge = if (it.hours < 6f) "<span class='badge badge-critical'>Sleep Deprived</span>"
                else if (it.hours > 9f) "<span class='badge badge-elevated'>Over-slept</span>"
                else "<span class='badge badge-normal'>Restful</span>"
                html.append("<tr><td>${formatDate(it.timestamp)}</td><td><strong>${it.hours} hours</strong></td><td>$statusBadge</td><td>${it.notes}</td></tr>")
            }
            html.append("</table>")
            html.append("</div>")
        }

        // 8. Lab Results
        val labs = labResults.value
        if (labs.isNotEmpty()) {
            html.append("<div class='section-card'>")
            html.append("<div class='section-title'>Laboratory Diagnostic Results</div>")
            html.append("<table><tr><th>Date/Time</th><th>Test Parameter</th><th>Value Measured</th><th>Reference Standard</th></tr>")
            labs.forEach {
                html.append("<tr><td>${formatDate(it.timestamp)}</td><td><strong>${it.testName}</strong></td><td><strong>${it.value} ${it.unit}</strong></td><td>${it.referenceRange}</td></tr>")
            }
            html.append("</table>")
            html.append("</div>")
        }

        // 9. Lifestyle Records
        val lifestyle = lifestyleRecords.value
        if (lifestyle.isNotEmpty()) {
            html.append("<div class='section-card'>")
            html.append("<div class='section-title'>Lifestyle & Activity Records</div>")
            html.append("<table><tr><th>Date/Time</th><th>Category</th><th>Registered Amount</th></tr>")
            lifestyle.forEach {
                html.append("<tr><td>${formatDate(it.timestamp)}</td><td><strong>${it.type.replaceFirstChar { c -> c.uppercase() }}</strong></td><td>${it.amount}</td></tr>")
            }
            html.append("</table>")
            html.append("</div>")
        }

        html.append(attachmentsHtmlSection(context))

        html.append("<div class='footer'>Disclaimer: Generated securely by Patient Medical Journal App. This document is intended as a clinical summaries ledger and does not replace professional diagnostic opinion or counsel.</div>")
        html.append("</body></html>")
        return html.toString()
    }

    // Embeds attached images and PDFs (e.g. lab test reports) as inline images so they
    // appear as extra pages when this report is printed/saved to a single PDF.
    private fun attachmentsHtmlSection(context: Context): String {
        val items = attachments.value
        if (items.isEmpty()) return ""

        val sb = StringBuilder()
        sb.append("<div class='section-card' style='page-break-before: always;'>")
        sb.append("<div class='section-title'>Attached Lab Documents & Images</div>")
        items.forEach { att ->
            try {
                val uri = Uri.parse(att.fileUri)
                sb.append("<div style='margin-bottom:18px;'>")
                sb.append("<p style='font-weight:600;font-size:13px;margin:4px 0;'>${att.title} <span style='color:#64748B;font-weight:400;'>(${formatDate(att.timestamp)})</span></p>")
                if (att.notes.isNotEmpty()) {
                    sb.append("<p style='font-size:12px;color:#64748B;margin:2px 0 6px 0;'>${att.notes}</p>")
                }

                val mimeType = context.contentResolver.getType(uri) ?: ""
                if (mimeType == "application/pdf" || att.fileUri.endsWith(".pdf", ignoreCase = true)) {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        val renderer = android.graphics.pdf.PdfRenderer(pfd)
                        for (i in 0 until renderer.pageCount) {
                            val page = renderer.openPage(i)
                            val bitmap = android.graphics.Bitmap.createBitmap(
                                page.width * 2, page.height * 2, android.graphics.Bitmap.Config.ARGB_8888
                            )
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            val baos = java.io.ByteArrayOutputStream()
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, baos)
                            val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                            sb.append("<img src='data:image/png;base64,$base64' style='width:100%;border:1px solid #E2E8F0;border-radius:4px;margin-bottom:8px;' />")
                            page.close()
                            bitmap.recycle()
                        }
                        renderer.close()
                    }
                } else {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val bytes = input.readBytes()
                        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        sb.append("<img src='data:image/*;base64,$base64' style='max-width:100%;border:1px solid #E2E8F0;border-radius:4px;margin-bottom:8px;' />")
                    }
                }
                sb.append("</div>")
            } catch (e: Exception) {
                Log.e("PDFAttach", "Failed to embed attachment '${att.title}': ${e.localizedMessage}")
                sb.append("<p style='color:#991B1B;font-size:12px;'>Could not load attachment: ${att.title}</p>")
                sb.append("</div>")
            }
        }
        sb.append("</div>")
        return sb.toString()
    }

    fun printPDFReport(context: Context) {
        viewModelScope.launch {
            try {
                val htmlContent = withContext(Dispatchers.IO) { generateHTMLContent(context) }
                val webView = android.webkit.WebView(context)
                webView.webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? android.print.PrintManager
                        if (printManager != null) {
                            val jobName = "HealthSummary_${System.currentTimeMillis()}"
                            val printAdapter = webView.createPrintDocumentAdapter(jobName)
                            printManager.print(
                                jobName,
                                printAdapter,
                                android.print.PrintAttributes.Builder().build()
                            )
                        } else {
                            Log.e("PDFPrint", "PrintManager not available")
                        }
                    }
                }
                webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            } catch (e: Exception) {
                Log.e("PDFPrint", "Error creating PDF print job: ${e.localizedMessage}")
            }
        }
    }

    fun shareHTMLReport(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val html = generateHTMLContent(context)

                val filename = "medical_journal_report_${System.currentTimeMillis()}.html"
                val cacheFile = File(context.cacheDir, filename)
                cacheFile.writeText(html)

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    cacheFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/html"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Health Record HTML Summary")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                withContext(Dispatchers.Main) {
                    context.startActivity(Intent.createChooser(intent, "Share Health Summary via..."))
                }
            } catch (e: Exception) {
                Log.e("ReportHTML", e.localizedMessage ?: "HTML Share Error")
            }
        }
    }

    fun writeLocalBackupToUri(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbData = generateJSON()
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(dbData.toByteArray())
                }
                val now = System.currentTimeMillis()
                _lastLocalBackupTime.value = now
                sharedPrefs.edit().putLong("last_local_backup_time", now).apply()
                withContext(Dispatchers.Main) {
                    onResult(true, "Success")
                }
            } catch (e: Exception) {
                Log.e("Backup", "Error writing backup to URI: ${e.localizedMessage}")
                withContext(Dispatchers.Main) {
                    onResult(false, e.localizedMessage ?: "Unknown Error")
                }
            }
        }
    }

    fun writeExportToUri(context: Context, uri: Uri, format: String, onResult: (Boolean, String) -> Unit = {_,_ ->}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentString = when (format.uppercase()) {
                    "CSV" -> generateCSV()
                    "JSON" -> generateJSON()
                    "TXT" -> generateTXT()
                    else -> generateTXT()
                }
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(contentString.toByteArray())
                }
                withContext(Dispatchers.Main) {
                    onResult(true, "Success")
                }
            } catch (e: Exception) {
                Log.e("Export", "Error writing export to URI: ${e.localizedMessage}")
                withContext(Dispatchers.Main) {
                    onResult(false, e.localizedMessage ?: "Unknown Error")
                }
            }
        }
    }

    private fun csvField(value: String): String {
        val needsQuoting = value.contains(',') || value.contains('"') || value.contains('\n')
        return if (needsQuoting) "\"${value.replace("\"", "\"\"")}\"" else value
    }

    private fun generateCSV(): String {
        val s = StringBuilder()
        s.append("Category,Date_Time,Value1,Value2,Value3,Unit,Notes\n")
        bloodPressureRecords.value.forEach {
            s.append("BloodPressure,${formatDate(it.timestamp)},${it.systolic},${it.diastolic},${it.heartRate},mmHg,${csvField(it.notes)}\n")
        }
        bloodSugarRecords.value.forEach {
            s.append("BloodSugar,${formatDate(it.timestamp)},${it.value},${csvField(it.category)},,${it.unit},${csvField(it.notes)}\n")
        }
        weightRecords.value.forEach {
            s.append("Weight,${formatDate(it.timestamp)},${it.weightKg},,,kg,${csvField(it.notes)}\n")
        }
        medications.value.forEach {
            s.append("Medication,,${csvField(it.name)},${csvField(it.dosage)},${csvField(it.frequency)},,${if (it.isActive) "Active" else "Inactive"}\n")
        }
        symptoms.value.forEach {
            s.append("Symptom,${formatDate(it.timestamp)},${csvField(it.symptomName)},${csvField(it.severity)},,,${csvField(it.notes)}\n")
        }
        sleepRecords.value.forEach {
            s.append("Sleep,${formatDate(it.timestamp)},${it.hours},,,hours,${csvField(it.notes)}\n")
        }
        labResults.value.forEach {
            s.append("LabResult,${formatDate(it.timestamp)},${csvField(it.testName)},${it.value},,${csvField(it.unit)},${csvField(it.referenceRange)}\n")
        }
        lifestyleRecords.value.forEach {
            s.append("Lifestyle,${formatDate(it.timestamp)},${csvField(it.type)},${it.amount},,,\n")
        }
        return s.toString()
    }

    // Converts a blood sugar reading to mg/dL so the value is unambiguous for clinics
    // that default to mg/dL (e.g. DataDoctorPro stores it as a plain string with no unit).
    private fun normalizeBloodSugarToMgDl(value: Float, unit: String): Float {
        return if (unit.equals("mmol/L", ignoreCase = true)) value * 18.0182f else value
    }

    // Builds a CSV scoped to the date range and to the record categories DataDoctorPro's
    // "Import Journal Data" dialog understands (BloodPressure, BloodSugar, Weight, Sleep),
    // plus Symptom/LabResult rows which are forward-compatible and safely ignored today.
    private fun generateDoctorCSV(startTime: Long, endTime: Long): String {
        val s = StringBuilder()
        s.append("Category,Date_Time,Value1,Value2,Value3,Unit,Notes\n")
        bloodPressureRecords.value.filter { it.timestamp in startTime..endTime }.forEach {
            s.append("BloodPressure,${formatDate(it.timestamp)},${it.systolic},${it.diastolic},${it.heartRate},mmHg,${csvField(it.notes)}\n")
        }
        bloodSugarRecords.value.filter { it.timestamp in startTime..endTime }.forEach {
            val mgDl = normalizeBloodSugarToMgDl(it.value, it.unit)
            val valueStr = if (mgDl == mgDl.toInt().toFloat()) mgDl.toInt().toString() else String.format(Locale.US, "%.1f", mgDl)
            s.append("BloodSugar,${formatDate(it.timestamp)},$valueStr,${csvField(it.category)},,mg/dL,${csvField(it.notes)}\n")
        }
        weightRecords.value.filter { it.timestamp in startTime..endTime }.forEach {
            s.append("Weight,${formatDate(it.timestamp)},${it.weightKg},,,kg,${csvField(it.notes)}\n")
        }
        sleepRecords.value.filter { it.timestamp in startTime..endTime }.forEach {
            s.append("Sleep,${formatDate(it.timestamp)},${it.hours},,,hours,${csvField(it.notes)}\n")
        }
        symptoms.value.filter { it.timestamp in startTime..endTime }.forEach {
            s.append("Symptom,${formatDate(it.timestamp)},${csvField(it.symptomName)},${csvField(it.severity)},,,${csvField(it.notes)}\n")
        }
        labResults.value.filter { it.timestamp in startTime..endTime }.forEach {
            s.append("LabResult,${formatDate(it.timestamp)},${csvField(it.testName)},${it.value},,${csvField(it.unit)},${csvField(it.referenceRange)}\n")
        }
        return s.toString()
    }

    // rangeOption: "last30", "last90", "all", "sinceLast"
    fun exportForDoctor(context: Context, rangeOption: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val day = 24L * 60L * 60L * 1000L
                val startTime = when (rangeOption) {
                    "last30" -> now - (30 * day)
                    "last90" -> now - (90 * day)
                    "sinceLast" -> _lastDoctorExportTime.value
                    else -> 0L
                }

                val csv = generateDoctorCSV(startTime, now)

                val filename = "DoctorExport_${System.currentTimeMillis()}.csv"
                val cacheFile = File(context.cacheDir, filename)
                cacheFile.writeText(csv)

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    cacheFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Health Records for Doctor")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooserIntent = Intent.createChooser(intent, "Send to Doctor via...").apply {
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                _lastDoctorExportTime.value = now
                sharedPrefs.edit().putLong("last_doctor_export_time", now).apply()

                withContext(Dispatchers.Main) {
                    context.startActivity(chooserIntent)
                }
            } catch (e: Exception) {
                Log.e("DoctorExport", "Error exporting for doctor: ${e.localizedMessage}")
            }
        }
    }

    private fun generateJSON(): String {
        val root = JSONObject()
        val bpArr = JSONArray()
        bloodPressureRecords.value.forEach {
            bpArr.put(JSONObject().apply {
                put("systolic", it.systolic)
                put("diastolic", it.diastolic)
                put("heartRate", it.heartRate)
                put("timestamp", it.timestamp)
                put("notes", it.notes)
            })
        }
        root.put("blood_pressure", bpArr)

        val wArr = JSONArray()
        weightRecords.value.forEach {
            wArr.put(JSONObject().apply {
                put("weightKg", it.weightKg)
                put("timestamp", it.timestamp)
                put("notes", it.notes)
            })
        }
        root.put("weight", wArr)

        val medArr = JSONArray()
        medications.value.forEach {
            medArr.put(JSONObject().apply {
                put("name", it.name)
                put("dosage", it.dosage)
                put("frequency", it.frequency)
                put("isActive", it.isActive)
            })
        }
        root.put("medications", medArr)

        val sArr = JSONArray()
        symptoms.value.forEach {
            sArr.put(JSONObject().apply {
                put("symptomName", it.symptomName)
                put("severity", it.severity)
                put("timestamp", it.timestamp)
                put("notes", it.notes)
            })
        }
        root.put("symptoms", sArr)

        val slArr = JSONArray()
        sleepRecords.value.forEach {
            slArr.put(JSONObject().apply {
                put("hours", it.hours)
                put("timestamp", it.timestamp)
                put("notes", it.notes)
            })
        }
        root.put("sleep", slArr)

        val sugarArr = JSONArray()
        bloodSugarRecords.value.forEach {
            sugarArr.put(JSONObject().apply {
                put("value", it.value)
                put("unit", it.unit)
                put("category", it.category)
                put("timestamp", it.timestamp)
                put("notes", it.notes)
            })
        }
        root.put("blood_sugar", sugarArr)

        val labArr = JSONArray()
        labResults.value.forEach {
            labArr.put(JSONObject().apply {
                put("testName", it.testName)
                put("value", it.value)
                put("unit", it.unit)
                put("referenceRange", it.referenceRange)
                put("timestamp", it.timestamp)
            })
        }
        root.put("lab_results", labArr)

        emergencyInfo.value?.let { info ->
            root.put("emergency_info", JSONObject().apply {
                put("fullName", info.fullName)
                put("bloodType", info.bloodType)
                put("chronicConditions", info.chronicConditions)
                put("allergies", info.allergies)
                put("contactName", info.contactName)
                put("contactPhone", info.contactPhone)
                put("additionalNotes", info.additionalNotes)
                put("sex", info.sex)
                put("numberOfChildren", info.numberOfChildren)
            })
        }

        val moodArr = JSONArray()
        moodRecords.value.forEach {
            moodArr.put(JSONObject().apply {
                put("score", it.score)
                put("notes", it.notes)
                put("timestamp", it.timestamp)
            })
        }
        root.put("mood_records", moodArr)

        val familyArr = JSONArray()
        familyMembers.value.forEach {
            familyArr.put(JSONObject().apply {
                put("name", it.name)
                put("relationship", it.relationship)
                put("dateOfBirth", it.dateOfBirth)
                put("bloodType", it.bloodType)
                put("notes", it.notes)
            })
        }
        root.put("family_members", familyArr)

        return root.toString(2)
    }

    private fun generateTXT(): String {
        val s = StringBuilder()
        s.append("MY MEDICAL JOURNAL - EXPORTED HEALTH RECORD STATUS\n")
        s.append("==================================================\n")
        s.append("Exported: ${formatDate(System.currentTimeMillis())}\n\n")

        s.append("BLOOD PRESSURE READINGS:\n")
        bloodPressureRecords.value.forEach {
            s.append("- ${formatDate(it.timestamp)}: ${it.systolic}/${it.diastolic} mmHg (HR: ${it.heartRate} bpm) | Notes: ${it.notes}\n")
        }
        s.append("\nWEIGHT LOGS:\n")
        weightRecords.value.forEach {
            s.append("- ${formatDate(it.timestamp)}: ${it.weightKg} kg | Notes: ${it.notes}\n")
        }
        s.append("\nMEDICATIONS:\n")
        medications.value.forEach {
            s.append("- ${it.name} (${it.dosage} - ${it.frequency}) | Active: ${it.isActive}\n")
        }
        s.append("\nSYMPTOMS:\n")
        symptoms.value.forEach {
            s.append("- ${formatDate(it.timestamp)}: ${it.symptomName} [${it.severity}] | Notes: ${it.notes}\n")
        }
        s.append("\nSLEEP RECORDS:\n")
        sleepRecords.value.forEach {
            s.append("- ${formatDate(it.timestamp)}: ${it.hours} hours | Notes: ${it.notes}\n")
        }
        s.append("\nLAB RESULTS:\n")
        labResults.value.forEach {
            s.append("- ${formatDate(it.timestamp)}: ${it.testName} = ${it.value} ${it.unit} (Ref: ${it.referenceRange})\n")
        }
        s.append("\nBLOOD SUGAR RECORDS:\n")
        bloodSugarRecords.value.forEach {
            s.append("- ${formatDate(it.timestamp)}: ${it.value} ${it.unit} (${it.category}) | Notes: ${it.notes}\n")
        }
        return s.toString()
    }

    fun backupZIP(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbData = generateJSON()
                val zipFile = File(context.cacheDir, "medical_journal_backup_${System.currentTimeMillis()}.zip")
                
                ZipOutputStream(zipFile.outputStream()).use { zos ->
                    // Add backup.json
                    val entry = ZipEntry("backup.json")
                    zos.putNextEntry(entry)
                    zos.write(dbData.toByteArray())
                    zos.closeEntry()
                }

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    zipFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "My Medical Journal ZIP Backup")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                withContext(Dispatchers.Main) {
                    context.startActivity(Intent.createChooser(intent, "Save Backup ZIP..."))
                }
            } catch (e: Exception) {
                Log.e("Backup", e.localizedMessage ?: "ZIP Error")
            }
        }
    }

    fun backupLocally(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dbData = generateJSON()
                val backupFile = File(context.cacheDir, "MyMedicalJournalBackup.json")
                backupFile.writeText(dbData)

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    backupFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "My Medical Journal Local Backup")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                withContext(Dispatchers.Main) {
                    context.startActivity(Intent.createChooser(intent, "Save Local Backup JSON..."))
                }

                val now = System.currentTimeMillis()
                _lastLocalBackupTime.value = now
                sharedPrefs.edit().putLong("last_local_backup_time", now).apply()
            } catch (e: Exception) {
                Log.e("Backup", e.localizedMessage ?: "Local Backup Error")
            }
        }
    }

    fun importLocalBackup(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val jsonStr = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().use { it.readText() }
                    }
                }
                if (jsonStr.isNullOrEmpty()) {
                    onResult(false, "File is empty")
                    return@launch
                }
                
                val result = restoreFromJSON(jsonStr)
                if (result) {
                    val now = System.currentTimeMillis()
                    _lastLocalBackupTime.value = now
                    sharedPrefs.edit().putLong("last_local_backup_time", now).apply()
                    onResult(true, "Data imported successfully!")
                } else {
                    onResult(false, "Failed to restore. Invalid file format.")
                }
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Unknown Error")
            }
        }
    }

    // --- Google OAuth 2.0 PKCE Flow Support ---
    private fun generateCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val code = ByteArray(32)
        secureRandom.nextBytes(code)
        return Base64.encodeToString(code, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(Charsets.US_ASCII)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val digest = messageDigest.digest(bytes)
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    fun getGoogleAuthUrl(): String {
        val verifier = generateCodeVerifier()
        sharedPrefs.edit().putString("google_oauth2_code_verifier", verifier).apply()
        
        val challenge = generateCodeChallenge(verifier)
        val clientId = _googleClientId.value.trim()
        val redirectUri = "http://localhost"
        
        val scope = java.net.URLEncoder.encode(
            "https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/userinfo.email",
            "UTF-8"
        )
        
        return "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=$clientId" +
                "&redirect_uri=$redirectUri" +
                "&response_type=code" +
                "&scope=$scope" +
                "&code_challenge=$challenge" +
                "&code_challenge_method=S256" +
                "&access_type=offline" +
                "&prompt=consent"
    }

    fun completeGoogleSignIn(code: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _googleDriveIsSyncing.value = true
                _googleDriveSyncStatus.value = "Exchanging code for tokens..."
                
                val verifier = sharedPrefs.getString("google_oauth2_code_verifier", "") ?: ""
                val clientId = _googleClientId.value.trim()
                
                if (verifier.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "Authentication flow verifier missing. Retrying.")
                    }
                    _googleDriveSyncStatus.value = "Sign-in failed."
                    _googleDriveIsSyncing.value = false
                    return@launch
                }
                
                val client = OkHttpClient()
                val formBody = FormBody.Builder()
                    .add("client_id", clientId)
                    .add("code_verifier", verifier)
                    .add("code", code)
                    .add("redirect_uri", "http://localhost")
                    .add("grant_type", "authorization_code")
                    .build()
                
                val request = Request.Builder()
                    .url("https://oauth2.googleapis.com/token")
                    .post(formBody)
                    .build()
                
                client.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string() ?: ""
                    if (response.isSuccessful && bodyStr.isNotEmpty()) {
                        val json = JSONObject(bodyStr)
                        val accessToken = json.optString("access_token")
                        val refreshToken = json.optString("refresh_token")
                        val expiresIn = json.optLong("expires_in", 3600)
                        val expiryTime = System.currentTimeMillis() + (expiresIn * 1000L)
                        
                        sharedPrefs.edit()
                            .putString("google_oauth2_access_token", accessToken)
                            .putString("google_oauth2_refresh_token", refreshToken)
                            .putLong("google_oauth2_token_expiry", expiryTime)
                            .apply()
                        
                        _isGoogleSignedIn.value = true
                        _googleDriveSyncStatus.value = "Retrieving account details..."
                        
                        fetchGoogleProfileEmail(accessToken)
                        
                        _googleDriveSyncStatus.value = "Logged in successfully!"
                        withContext(Dispatchers.Main) {
                            onResult(true, "Signed in successfully!")
                        }
                    } else {
                        Log.e("OAuth", "Code exchange failed: $bodyStr")
                        _googleDriveSyncStatus.value = "Sign-in exchange failed."
                        withContext(Dispatchers.Main) {
                            onResult(false, "OAuth Code exchange failed. Double check your Client ID.")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("OAuth", "Error Completing Login: ${e.localizedMessage}")
                _googleDriveSyncStatus.value = "Connection error."
                withContext(Dispatchers.Main) {
                    onResult(false, "Connection error: ${e.localizedMessage}")
                }
            } finally {
                _googleDriveIsSyncing.value = false
            }
        }
    }

    private suspend fun getOrRefreshToken(): String? {
        val refreshToken = sharedPrefs.getString("google_oauth2_refresh_token", "") ?: ""
        if (refreshToken.isEmpty()) {
            return null
        }
        
        val expiry = sharedPrefs.getLong("google_oauth2_token_expiry", 0L)
        val accessToken = sharedPrefs.getString("google_oauth2_access_token", "") ?: ""
        
        // If still valid (with 5 min safety buffer), return current access token
        if (System.currentTimeMillis() < expiry - 300_000L && accessToken.isNotEmpty()) {
            return accessToken
        }
        
        // Needs automatic refresh!
        try {
            _googleDriveSyncStatus.value = "Refreshing connection token..."
            val clientId = _googleClientId.value.trim()
            val client = OkHttpClient()
            val formBody = FormBody.Builder()
                .add("client_id", clientId)
                .add("refresh_token", refreshToken)
                .add("grant_type", "refresh_token")
                .build()
            
            val request = Request.Builder()
                .url("https://oauth2.googleapis.com/token")
                .post(formBody)
                .build()
            
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful && bodyStr.isNotEmpty()) {
                    val json = JSONObject(bodyStr)
                    val newAccessToken = json.optString("access_token")
                    val expiresIn = json.optLong("expires_in", 3600)
                    val expiryTime = System.currentTimeMillis() + (expiresIn * 1000L)
                    
                    val optNewRefreshToken = json.optString("refresh_token", "")
                    
                    val editor = sharedPrefs.edit()
                    editor.putString("google_oauth2_access_token", newAccessToken)
                    editor.putLong("google_oauth2_token_expiry", expiryTime)
                    if (optNewRefreshToken.isNotEmpty()) {
                        editor.putString("google_oauth2_refresh_token", optNewRefreshToken)
                    }
                    editor.apply()
                    
                    return newAccessToken
                } else {
                    Log.e("OAuth", "Refresh failed: $bodyStr")
                }
            }
        } catch (e: Exception) {
            Log.e("OAuth", "Refresh network error: ${e.localizedMessage}")
        }
        
        return null
    }

    private fun fetchGoogleProfileEmail(accessToken: String) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://www.googleapis.com/oauth2/v3/userinfo")
            .addHeader("Authorization", "Bearer $accessToken")
            .build()
        
        try {
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful && bodyStr.isNotEmpty()) {
                    val json = JSONObject(bodyStr)
                    val email = json.optString("email", "")
                    if (email.isNotEmpty()) {
                        _googleAccountEmail.value = email
                        sharedPrefs.edit().putString("google_account_email", email).apply()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("OAuth", "Profile fetch failed: ${e.localizedMessage}")
        }
    }

    fun googleSignOut() {
        sharedPrefs.edit()
            .remove("google_oauth2_access_token")
            .remove("google_oauth2_refresh_token")
            .remove("google_oauth2_token_expiry")
            .remove("google_oauth2_code_verifier")
            .remove("google_account_email")
            .apply()
        
        _googleAccountEmail.value = ""
        _isGoogleSignedIn.value = false
        _googleDriveSyncStatus.value = "Disconnected"
    }

    fun syncToGoogleDrive(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _googleDriveIsSyncing.value = true
            _googleDriveSyncStatus.value = "Authenticating..."
            delay(1000)

            try {
                val dbJson = generateJSON()
                
                // Try dynamic auto token first
                var oauthToken = getOrRefreshToken() ?: ""
                
                // Fallback to manual custom token or BuildConfig compile token
                if (oauthToken.isEmpty()) {
                    oauthToken = _manualDriveToken.value
                    if (oauthToken.isEmpty()) {
                        oauthToken = try {
                            val field = BuildConfig::class.java.getField("GOOGLE_DRIVE_OAUTH_TOKEN")
                            field.get(null) as? String ?: ""
                        } catch (e: Exception) {
                            ""
                        }
                    }
                }

                if (oauthToken.isNotEmpty() && oauthToken != "null") {
                    _googleDriveSyncStatus.value = "Uploading to Google Drive..."
                    val client = OkHttpClient()
                    val mediaType = "application/json; charset=UTF-8".toMediaType()

                    val query = "name='MyMedicalJournalBackup.json' and trashed=false"
                    val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                    val searchRequest = Request.Builder()
                        .url("https://www.googleapis.com/drive/v3/files?q=$encodedQuery")
                        .addHeader("Authorization", "Bearer $oauthToken")
                        .build()

                    var existingFileId: String? = null
                    client.newCall(searchRequest).execute().use { response ->
                        if (response.isSuccessful) {
                            val resData = response.body?.string() ?: ""
                            val resJson = JSONObject(resData)
                            val files = resJson.optJSONArray("files")
                            if (files != null && files.length() > 0) {
                                existingFileId = files.getJSONObject(0).optString("id")
                            }
                        }
                    }

                    val url = if (existingFileId != null) {
                        "https://www.googleapis.com/upload/drive/v3/files/$existingFileId?uploadType=media"
                    } else {
                        "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
                    }

                    val requestBody = if (existingFileId != null) {
                        dbJson.toRequestBody(mediaType)
                    } else {
                        val boundary = "MyCustomBoundary"
                        val multipartContent = buildString {
                            append("--$boundary\r\n")
                            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                            append("{\"name\": \"MyMedicalJournalBackup.json\", \"mimeType\": \"application/json\"}\r\n")
                            append("--$boundary\r\n")
                            append("Content-Type: application/json\r\n\r\n")
                            append(dbJson)
                            append("\r\n--$boundary--\r\n")
                        }
                        multipartContent.toRequestBody("multipart/related; boundary=$boundary".toMediaType())
                    }

                    val uploadRequest = Request.Builder()
                        .url(url)
                        .method(if (existingFileId != null) "PATCH" else "POST", requestBody)
                        .addHeader("Authorization", "Bearer $oauthToken")
                        .build()

                    client.newCall(uploadRequest).execute().use { response ->
                        if (response.isSuccessful) {
                            val now = System.currentTimeMillis()
                            _lastDriveBackupTime.value = now
                            sharedPrefs.edit().putLong("last_drive_backup_time", now).apply()
                            _googleDriveSyncStatus.value = "Synced successfully!"
                        } else {
                            val errorMsg = response.body?.string() ?: ""
                            Log.e("Upload", "Google Drive Upload Failed: $errorMsg")
                            _googleDriveSyncStatus.value = "Sync failed: expired connection or server error."
                        }
                    }
                } else {
                    _googleDriveSyncStatus.value = "Sign-in required to sync."
                }
            } catch (e: Exception) {
                _googleDriveSyncStatus.value = "Sync failed: ${e.localizedMessage ?: "Unknown network error."}"
            } finally {
                _googleDriveIsSyncing.value = false
            }
        }
    }

    fun restoreFromGoogleDrive(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _googleDriveIsSyncing.value = true
            _googleDriveSyncStatus.value = "Searching backup in Drive..."
            delay(1000)

            try {
                // Try dynamic auto token first
                var oauthToken = getOrRefreshToken() ?: ""
                
                // Fallback to manual custom token or BuildConfig compile token
                if (oauthToken.isEmpty()) {
                    oauthToken = _manualDriveToken.value
                    if (oauthToken.isEmpty()) {
                        oauthToken = try {
                            val field = BuildConfig::class.java.getField("GOOGLE_DRIVE_OAUTH_TOKEN")
                            field.get(null) as? String ?: ""
                        } catch (e: Exception) {
                            ""
                        }
                    }
                }

                if (oauthToken.isNotEmpty() && oauthToken != "null") {
                    val client = OkHttpClient()

                    val query = "name='MyMedicalJournalBackup.json' and trashed=false"
                    val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                    val searchRequest = Request.Builder()
                        .url("https://www.googleapis.com/drive/v3/files?q=$encodedQuery")
                        .addHeader("Authorization", "Bearer $oauthToken")
                        .build()

                    var fileId: String? = null
                    client.newCall(searchRequest).execute().use { response ->
                        if (response.isSuccessful) {
                            val resJson = JSONObject(response.body?.string() ?: "")
                            val files = resJson.optJSONArray("files")
                            if (files != null && files.length() > 0) {
                                fileId = files.getJSONObject(0).optString("id")
                            }
                        }
                    }

                    if (fileId != null) {
                        _googleDriveSyncStatus.value = "Downloading backup logs..."
                        val downloadRequest = Request.Builder()
                            .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
                            .addHeader("Authorization", "Bearer $oauthToken")
                            .build()

                        client.newCall(downloadRequest).execute().use { response ->
                            if (response.isSuccessful) {
                                val jsonStr = response.body?.string() ?: ""
                                val parsed = restoreFromJSON(jsonStr)
                                if (parsed) {
                                    val now = System.currentTimeMillis()
                                    _lastDriveBackupTime.value = now
                                    sharedPrefs.edit().putLong("last_drive_backup_time", now).apply()
                                    _googleDriveSyncStatus.value = "Logs restored completely from Drive!"
                                } else {
                                    _googleDriveSyncStatus.value = "Restore failed: Invalid backup format."
                                }
                            } else {
                                _googleDriveSyncStatus.value = "Download failed."
                            }
                        }
                    } else {
                        _googleDriveSyncStatus.value = "No backup found in Google Drive."
                    }
                } else {
                    _googleDriveSyncStatus.value = "Sign-in required to restore."
                }
            } catch (e: Exception) {
                _googleDriveSyncStatus.value = "Restore failed: ${e.localizedMessage ?: "Unknown network error."}"
            } finally {
                _googleDriveIsSyncing.value = false
            }
        }
    }

    private suspend fun restoreFromJSON(jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonStr)
            
            // 1. Blood Pressure
            root.optJSONArray("blood_pressure")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    repository.insertBloodPressure(
                        BloodPressureRecord(
                            id = obj.optInt("id", 0),
                            systolic = obj.optInt("systolic"),
                            diastolic = obj.optInt("diastolic"),
                            heartRate = obj.optInt("heartRate"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            notes = obj.optString("notes")
                        )
                    )
                }
            }

            // 2. Weight
            root.optJSONArray("weight")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    repository.insertWeight(
                        WeightRecord(
                            id = obj.optInt("id", 0),
                            weightKg = obj.optDouble("weightKg").toFloat(),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            notes = obj.optString("notes")
                        )
                    )
                }
            }

            // 3. Medications
            root.optJSONArray("medications")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    repository.insertMedication(
                        MedicationRecord(
                            id = obj.optInt("id", 0),
                            name = obj.optString("name"),
                            dosage = obj.optString("dosage"),
                            frequency = obj.optString("frequency"),
                            isActive = obj.optBoolean("isActive", true)
                        )
                    )
                }
            }

            // 4. Symptoms
            root.optJSONArray("symptoms")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    repository.insertSymptom(
                        SymptomRecord(
                            id = obj.optInt("id", 0),
                            symptomName = obj.optString("symptomName"),
                            severity = obj.optString("severity"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            notes = obj.optString("notes")
                        )
                    )
                }
            }

            // 5. Sleep
            root.optJSONArray("sleep")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    repository.insertSleep(
                        SleepRecord(
                            id = obj.optInt("id", 0),
                            hours = obj.optDouble("hours").toFloat(),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            notes = obj.optString("notes")
                        )
                    )
                }
            }

            // 6. Blood Sugar
            root.optJSONArray("blood_sugar")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    repository.insertBloodSugar(
                        BloodSugarRecord(
                            id = obj.optInt("id", 0),
                            value = obj.optDouble("value").toFloat(),
                            unit = obj.optString("unit", "mg/dL"),
                            category = obj.optString("category", "Fasting"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            notes = obj.optString("notes")
                        )
                    )
                }
            }

            // 7. Lab Results
            root.optJSONArray("lab_results")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    repository.insertLabResult(
                        LabResultRecord(
                            id = obj.optInt("id", 0),
                            testName = obj.optString("testName"),
                            value = obj.optDouble("value").toFloat(),
                            unit = obj.optString("unit"),
                            referenceRange = obj.optString("referenceRange"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            // 8. Emergency Info
            root.optJSONObject("emergency_info")?.let { obj ->
                repository.insertEmergencyInfo(
                    EmergencyInfo(
                        id = obj.optInt("id", 1),
                        fullName = obj.optString("fullName"),
                        bloodType = obj.optString("bloodType"),
                        allergies = obj.optString("allergies"),
                        chronicConditions = obj.optString("chronicConditions"),
                        contactName = obj.optString("contactName"),
                        contactPhone = obj.optString("contactPhone"),
                        additionalNotes = obj.optString("additionalNotes"),
                        sex = obj.optString("sex"),
                        numberOfChildren = obj.optInt("numberOfChildren", 0)
                    )
                )
            }

            // 9. Mood Records
            root.optJSONArray("mood_records")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    repository.insertMood(
                        MoodRecord(
                            score = obj.optInt("score", 3),
                            notes = obj.optString("notes"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            // 10. Family Members
            root.optJSONArray("family_members")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    repository.insertFamilyMember(
                        FamilyMemberProfile(
                            name = obj.optString("name"),
                            relationship = obj.optString("relationship"),
                            dateOfBirth = obj.optString("dateOfBirth"),
                            bloodType = obj.optString("bloodType"),
                            notes = obj.optString("notes")
                        )
                    )
                }
            }

            true
        } catch (e: Exception) {
            Log.e("Restore", "Failed to parse json backup", e)
            false
        }
    }
}

enum class DangerSeverity { CRISIS, HIGH, WARNING }

data class DangerAlert(
    val severity: DangerSeverity,
    val title: String,
    val message: String
)

data class BpCorrelationBucket(
    val label: String,
    val avgSystolic: Float,
    val avgDiastolic: Float,
    val count: Int
)

data class BpCorrelationData(
    val bySleep: List<BpCorrelationBucket> = emptyList(),
    val bySmoking: List<BpCorrelationBucket> = emptyList(),
    val byActivity: List<BpCorrelationBucket> = emptyList(),
    val bySleepSmoking: List<BpCorrelationBucket> = emptyList(),
    val bySleepActivity: List<BpCorrelationBucket> = emptyList(),
    val bySmokingActivity: List<BpCorrelationBucket> = emptyList(),
    val byAllThree: List<BpCorrelationBucket> = emptyList()
)

private fun sleepBucketLabel(hours: Float): String = when {
    hours < 6f -> "<6h"
    hours <= 10f -> "6-10h"
    else -> ">10h"
}

private fun smokingBucketLabel(cigarettes: Float): String = when {
    cigarettes <= 5f -> "0-5"
    cigarettes <= 10f -> "6-10"
    cigarettes <= 20f -> "11-20"
    else -> ">20"
}

private fun activityBucketLabel(exerciseMinutes: Float): String =
    if (exerciseMinutes > 0f) "Active" else "No Activity"

private fun buildBpBuckets(
    items: List<Pair<String, BloodPressureRecord>>,
    order: List<String>
): List<BpCorrelationBucket> {
    val grouped = items.groupBy({ it.first }, { it.second })
    return order.mapNotNull { label ->
        val recs = grouped[label]
        if (recs.isNullOrEmpty()) return@mapNotNull null
        BpCorrelationBucket(
            label = label,
            avgSystolic = recs.map { it.systolic }.average().toFloat(),
            avgDiastolic = recs.map { it.diastolic }.average().toFloat(),
            count = recs.size
        )
    }
}

class HealthViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HealthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HealthViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
