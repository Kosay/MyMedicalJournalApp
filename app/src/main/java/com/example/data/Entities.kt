package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blood_pressure_records")
data class BloodPressureRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val systolic: Int,
    val diastolic: Int,
    val heartRate: Int,
    val timestamp: Long,
    val notes: String = ""
)

@Entity(tableName = "weight_records")
data class WeightRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val weightKg: Float,
    val timestamp: Long,
    val notes: String = ""
)

@Entity(tableName = "medications")
data class MedicationRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dosage: String,
    val frequency: String,
    val isActive: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "symptoms")
data class SymptomRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symptomName: String,
    val severity: String, // e.g. Mild, Moderate, Severe
    val timestamp: Long,
    val notes: String = ""
)

@Entity(tableName = "sleep_records")
data class SleepRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hours: Float,
    val timestamp: Long,
    val notes: String = ""
)

@Entity(tableName = "lab_results")
data class LabResultRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val testName: String, // e.g. Cholesterol, Triglycerides, Blood Sugar
    val value: Float,
    val unit: String, // e.g. mg/dL, mmol/L
    val timestamp: Long,
    val referenceRange: String = ""
)

@Entity(tableName = "lifestyle_records")
data class LifestyleRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "smoking", "water", "exercise"
    val amount: Float, // count of cigarettes, ml of water, mins of exercise
    val timestamp: Long
)

@Entity(tableName = "attachments")
data class AttachmentRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val fileUri: String, // URI of image/attachment
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "emergency_info")
data class EmergencyInfo(
    @PrimaryKey val id: Int = 1,
    val fullName: String = "",
    val bloodType: String = "",
    val chronicConditions: String = "",
    val allergies: String = "",
    val contactName: String = "",
    val contactPhone: String = "",
    val additionalNotes: String = ""
)

@Entity(tableName = "blood_sugar_records")
data class BloodSugarRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val value: Float,
    val unit: String = "mg/dL", // e.g. "mg/dL" or "mmol/L"
    val category: String = "Fasting", // e.g. "Fasting", "Post-Prandial", "Random", "Bedtime"
    val timestamp: Long,
    val notes: String = ""
)
