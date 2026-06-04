package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {
    // --- Blood Pressure ---
    @Query("SELECT * FROM blood_pressure_records ORDER BY timestamp DESC")
    fun getAllBloodPressure(): Flow<List<BloodPressureRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBloodPressure(record: BloodPressureRecord)

    @Delete
    suspend fun deleteBloodPressure(record: BloodPressureRecord)

    // --- Weight ---
    @Query("SELECT * FROM weight_records ORDER BY timestamp DESC")
    fun getAllWeight(): Flow<List<WeightRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(record: WeightRecord)

    @Delete
    suspend fun deleteWeight(record: WeightRecord)

    // --- Medications ---
    @Query("SELECT * FROM medications ORDER BY name ASC")
    fun getAllMedications(): Flow<List<MedicationRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(record: MedicationRecord)

    @Delete
    suspend fun deleteMedication(record: MedicationRecord)

    // --- Symptoms ---
    @Query("SELECT * FROM symptoms ORDER BY timestamp DESC")
    fun getAllSymptoms(): Flow<List<SymptomRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptom(record: SymptomRecord)

    @Delete
    suspend fun deleteSymptom(record: SymptomRecord)

    // --- Sleep ---
    @Query("SELECT * FROM sleep_records ORDER BY timestamp DESC")
    fun getAllSleep(): Flow<List<SleepRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSleep(record: SleepRecord)

    @Delete
    suspend fun deleteSleep(record: SleepRecord)

    // --- Lab Results ---
    @Query("SELECT * FROM lab_results ORDER BY timestamp DESC")
    fun getAllLabResults(): Flow<List<LabResultRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabResult(record: LabResultRecord)

    @Delete
    suspend fun deleteLabResult(record: LabResultRecord)

    // --- Lifestyle ---
    @Query("SELECT * FROM lifestyle_records ORDER BY timestamp DESC")
    fun getAllLifestyle(): Flow<List<LifestyleRecord>>

    @Query("SELECT * FROM lifestyle_records WHERE type = :type ORDER BY timestamp DESC")
    fun getLifestyleByType(type: String): Flow<List<LifestyleRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLifestyle(record: LifestyleRecord)

    @Delete
    suspend fun deleteLifestyle(record: LifestyleRecord)

    // --- Attachments ---
    @Query("SELECT * FROM attachments ORDER BY timestamp DESC")
    fun getAllAttachments(): Flow<List<AttachmentRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(record: AttachmentRecord)

    @Delete
    suspend fun deleteAttachment(record: AttachmentRecord)

    // --- Emergency Info ---
    @Query("SELECT * FROM emergency_info WHERE id = 1 LIMIT 1")
    fun getEmergencyInfo(): Flow<EmergencyInfo?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmergencyInfo(info: EmergencyInfo)

    // --- Blood Sugar ---
    @Query("SELECT * FROM blood_sugar_records ORDER BY timestamp DESC")
    fun getAllBloodSugar(): Flow<List<BloodSugarRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBloodSugar(record: BloodSugarRecord)

    @Delete
    suspend fun deleteBloodSugar(record: BloodSugarRecord)
}
