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
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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

    private val _lastLocalBackupTime = MutableStateFlow(sharedPrefs.getLong("last_local_backup_time", 0L))
    val lastLocalBackupTime: StateFlow<Long> = _lastLocalBackupTime.asStateFlow()

    private val _lastDriveBackupTime = MutableStateFlow(sharedPrefs.getLong("last_drive_backup_time", 0L))
    val lastDriveBackupTime: StateFlow<Long> = _lastDriveBackupTime.asStateFlow()

    private val _googleDriveSyncStatus = MutableStateFlow("Not Connected")
    val googleDriveSyncStatus: StateFlow<String> = _googleDriveSyncStatus.asStateFlow()

    private val _googleDriveIsSyncing = MutableStateFlow(false)
    val googleDriveIsSyncing: StateFlow<Boolean> = _googleDriveIsSyncing.asStateFlow()



    // --- Database Operations ---
    fun addBloodSugar(value: Float, category: String, notes: String, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertBloodSugar(
                BloodSugarRecord(
                    value = value,
                    category = category,
                    notes = notes,
                    timestamp = timestamp
                )
            )
        }
    }

    fun deleteBloodSugar(record: BloodSugarRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBloodSugar(record)
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
        }
    }

    fun deleteBloodPressure(record: BloodPressureRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteBloodPressure(record)
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
        additionalNotes: String
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
                    additionalNotes = additionalNotes
                )
            )
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
                val filename = "medical_journal_export_${System.currentTimeMillis()}.$ext"
                val cacheFile = File(context.cacheDir, filename)
                cacheFile.writeText(contentString)

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    cacheFile
                )

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "My Medical Journal Export")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                withContext(Dispatchers.Main) {
                    context.startActivity(Intent.createChooser(intent, "Share Report via..."))
                }
            } catch (e: Exception) {
                Log.e("Export", "Error exporting data: ${e.localizedMessage}")
            }
        }
    }

    fun shareHTMLReport(context: Context) {
        // Generates beautiful responsive health record summary as HTML for PDF-like styling!
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val html = StringBuilder()
                html.append("<html><head><style>")
                html.append("body { font-family: sans-serif; color: #1E293B; padding: 24px; }")
                html.append("h1 { color: #0F172A; border-bottom: 2px solid #E2E8F0; padding-bottom: 8px; }")
                html.append("h2 { color: #2563EB; margin-top: 24px; }")
                html.append("table { width: 100%; border-collapse: collapse; margin-top: 12px; }")
                html.append("th, td { border: 1px solid #E2E8F0; padding: 10px; text-align: left; }")
                html.append("th { background-color: #F8FAFC; font-weight: bold; }")
                html.append(".footer { margin-top: 40px; font-size: 11px; text-align: center; color: #64748B; }")
                html.append("</style></head><body>")
                
                html.append("<h1>My Medical Journal - Health Summary</h1>")
                html.append("<p>Report generated on: ${formatDate(System.currentTimeMillis())}</p>")

                // Emergency Info
                emergencyInfo.value?.let { info ->
                    html.append("<h2>Emergency Information</h2>")
                    html.append("<p><strong>Name:</strong> ${info.fullName}<br/>")
                    html.append("<strong>Blood Type:</strong> ${info.bloodType}<br/>")
                    html.append("<strong>Conditions:</strong> ${info.chronicConditions}<br/>")
                    html.append("<strong>Allergies:</strong> ${info.allergies}<br/>")
                    html.append("<strong>Emergency Contact:</strong> ${info.contactName} (${info.contactPhone})</p>")
                }

                // BP History
                html.append("<h2>Blood Pressure Logs</h2>")
                html.append("<table><tr><th>Date</th><th>Reading</th><th>Pulse</th><th>Notes</th></tr>")
                bloodPressureRecords.value.forEach {
                    html.append("<tr><td>${formatDate(it.timestamp)}</td><td>${it.systolic}/${it.diastolic} mmHg</td><td>${it.heartRate} bpm</td><td>${it.notes}</td></tr>")
                }
                html.append("</table>")

                // Weight History
                html.append("<h2>Weight Logs</h2>")
                html.append("<table><tr><th>Date</th><th>Weight</th><th>Notes</th></tr>")
                weightRecords.value.forEach {
                    html.append("<tr><td>${formatDate(it.timestamp)}</td><td>${it.weightKg} kg</td><td>${it.notes}</td></tr>")
                }
                html.append("</table>")

                // Medication History
                html.append("<h2>Medications</h2>")
                html.append("<table><tr><th>Name</th><th>Dosage</th><th>Frequency</th><th>Status</th></tr>")
                medications.value.forEach {
                    val status = if (it.isActive) "Active" else "Inactive"
                    html.append("<tr><td>${it.name}</td><td>${it.dosage}</td><td>${it.frequency}</td><td>$status</td></tr>")
                }
                html.append("</table>")

                html.append("<div class='footer'>Disclaimer: Generated by my medical journal app. Not a replacement for professional healthcare counsel.</div>")
                html.append("</body></html>")

                val filename = "medical_journal_report_${System.currentTimeMillis()}.html"
                val cacheFile = File(context.cacheDir, filename)
                cacheFile.writeText(html.toString())

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

    private fun generateCSV(): String {
        val s = StringBuilder()
        s.append("Category,Metric,Metric2,Unit,Timestamp,Notes\n")
        bloodPressureRecords.value.forEach {
            s.append("BloodPressure,${it.systolic}/${it.diastolic},${it.heartRate},mmHg,${formatDate(it.timestamp)},\"${it.notes}\"\n")
        }
        weightRecords.value.forEach {
            s.append("Weight,${it.weightKg},,kg,${formatDate(it.timestamp)},\"${it.notes}\"\n")
        }
        medications.value.forEach {
            s.append("Medication,${it.name},${it.dosage},${it.frequency},,${if (it.isActive) "Active" else "Inactive"}\n")
        }
        symptoms.value.forEach {
            s.append("Symptom,${it.symptomName},${it.severity},,${formatDate(it.timestamp)},\"${it.notes}\"\n")
        }
        sleepRecords.value.forEach {
            s.append("Sleep,${it.hours},,hours,${formatDate(it.timestamp)},\"${it.notes}\"\n")
        }
        labResults.value.forEach {
            s.append("LabResult,${it.testName},${it.value},${it.unit},${formatDate(it.timestamp)},\"${it.referenceRange}\"\n")
        }
        return s.toString()
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

    fun syncToGoogleDrive(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _googleDriveIsSyncing.value = true
            _googleDriveSyncStatus.value = "Authenticating..."
            delay(1000)

            try {
                val dbJson = generateJSON()
                
                val oauthToken = try {
                    val field = BuildConfig::class.java.getField("GOOGLE_DRIVE_OAUTH_TOKEN")
                    field.get(null) as? String ?: ""
                } catch (e: Exception) {
                    ""
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
                            _googleDriveSyncStatus.value = "Sync failed: Server error or expired token."
                        }
                    }
                } else {
                    _googleDriveSyncStatus.value = "Waiting for connection setup..."
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
                val oauthToken = try {
                    val field = BuildConfig::class.java.getField("GOOGLE_DRIVE_OAUTH_TOKEN")
                    field.get(null) as? String ?: ""
                } catch (e: Exception) {
                    ""
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
                    _googleDriveSyncStatus.value = "Connection pending."
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
                        additionalNotes = obj.optString("additionalNotes")
                    )
                )
            }

            true
        } catch (e: Exception) {
            Log.e("Restore", "Failed to parse json backup", e)
            false
        }
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
