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

    // --- Mood ---
    @Query("SELECT * FROM mood_records ORDER BY timestamp DESC")
    fun getAllMoods(): Flow<List<MoodRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMood(record: MoodRecord)

    @Delete
    suspend fun deleteMood(record: MoodRecord)

    // --- Medication Doses ---
    @Query("SELECT * FROM medication_doses ORDER BY timestamp DESC")
    fun getAllMedicationDoses(): Flow<List<MedicationDoseRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicationDose(record: MedicationDoseRecord)

    @Delete
    suspend fun deleteMedicationDose(record: MedicationDoseRecord)

    // --- Family Members ---
    @Query("SELECT * FROM family_members ORDER BY name ASC")
    fun getAllFamilyMembers(): Flow<List<FamilyMemberProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyMember(profile: FamilyMemberProfile)

    @Delete
    suspend fun deleteFamilyMember(profile: FamilyMemberProfile)

    // --- Height (child growth) ---
    @Query("SELECT * FROM height_records ORDER BY timestamp DESC")
    fun getAllHeights(): Flow<List<HeightRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeight(record: HeightRecord)

    @Delete
    suspend fun deleteHeight(record: HeightRecord)

    // --- Period tracking ---
    @Query("SELECT * FROM period_records ORDER BY startTimestamp DESC")
    fun getAllPeriods(): Flow<List<PeriodRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriod(record: PeriodRecord)

    @Delete
    suspend fun deletePeriod(record: PeriodRecord)

    // --- Appointments ---
    @Query("SELECT * FROM appointments ORDER BY dateTimestamp ASC")
    fun getAllAppointments(): Flow<List<AppointmentRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppointment(record: AppointmentRecord)

    @Update
    suspend fun updateAppointment(record: AppointmentRecord)

    @Delete
    suspend fun deleteAppointment(record: AppointmentRecord)
}
