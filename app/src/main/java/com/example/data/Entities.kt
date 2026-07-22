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
    val notes: String = "",
    val profileId: Int = 0
)

@Entity(tableName = "weight_records")
data class WeightRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val weightKg: Float,
    val timestamp: Long,
    val notes: String = "",
    val profileId: Int = 0
)

@Entity(tableName = "medications")
data class MedicationRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val dosage: String,
    val frequency: String,
    val isActive: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val profileId: Int = 0
)

@Entity(tableName = "symptoms")
data class SymptomRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val symptomName: String,
    val severity: String, // e.g. Mild, Moderate, Severe
    val timestamp: Long,
    val notes: String = "",
    val profileId: Int = 0
)

@Entity(tableName = "sleep_records")
data class SleepRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hours: Float,
    val timestamp: Long,
    val notes: String = "",
    val profileId: Int = 0
)

@Entity(tableName = "lab_results")
data class LabResultRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val testName: String, // e.g. Cholesterol, Triglycerides, Blood Sugar
    val value: Float,
    val unit: String, // e.g. mg/dL, mmol/L
    val timestamp: Long,
    val referenceRange: String = "",
    val profileId: Int = 0
)

@Entity(tableName = "lifestyle_records")
data class LifestyleRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "smoking", "water", "exercise"
    val amount: Float, // count of cigarettes, ml of water, mins of exercise
    val timestamp: Long,
    val profileId: Int = 0
)

@Entity(tableName = "attachments")
data class AttachmentRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val fileUri: String, // URI of image/attachment
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val profileId: Int = 0
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
    val additionalNotes: String = "",
    val sex: String = "",           // "Male" or "Female"
    val numberOfChildren: Int = 0
)

@Entity(tableName = "blood_sugar_records")
data class BloodSugarRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val value: Float,
    val unit: String = "mg/dL", // e.g. "mg/dL" or "mmol/L"
    val category: String = "Fasting", // e.g. "Fasting", "Post-Prandial", "Random", "Bedtime"
    val timestamp: Long,
    val notes: String = "",
    val profileId: Int = 0
)

@Entity(tableName = "mood_records")
data class MoodRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val score: Int,   // 1 = very bad … 5 = great
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val profileId: Int = 0
)

@Entity(tableName = "medication_doses")
data class MedicationDoseRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicationId: Int = 0,
    val medicationName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val taken: Boolean = true,
    val notes: String = "",
    val profileId: Int = 0
)

@Entity(tableName = "appointments")
data class AppointmentRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val type: String = "Doctor",    // "Doctor" | "Medication" | "Lab" | "Other"
    val dateTimestamp: Long,
    val notes: String = "",
    val profileId: Int = 0,         // 0 = main user, else FamilyMemberProfile.id
    val profileName: String = "",   // display name for multi-profile context
    val isCompleted: Boolean = false
)

@Entity(tableName = "height_records")
data class HeightRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val heightCm: Float,
    val timestamp: Long,
    val notes: String = "",
    val profileId: Int = 0
)

@Entity(tableName = "family_members")
data class FamilyMemberProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val relationship: String = "",
    val dateOfBirth: String = "",
    val bloodType: String = "",
    val notes: String = "",
    val sex: String = "",
    val chronicConditions: String = "",
    val allergies: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val heightCm: Float = 0f,
    val weightKg: Float = 0f
)
