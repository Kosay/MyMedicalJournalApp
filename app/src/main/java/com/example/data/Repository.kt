package com.example.data

import kotlinx.coroutines.flow.Flow

class HealthRepository(private val dao: HealthDao) {
    // --- Blood Pressure ---
    val allBloodPressure: Flow<List<BloodPressureRecord>> = dao.getAllBloodPressure()
    suspend fun insertBloodPressure(record: BloodPressureRecord) = dao.insertBloodPressure(record)
    suspend fun deleteBloodPressure(record: BloodPressureRecord) = dao.deleteBloodPressure(record)

    // --- Weight ---
    val allWeight: Flow<List<WeightRecord>> = dao.getAllWeight()
    suspend fun insertWeight(record: WeightRecord) = dao.insertWeight(record)
    suspend fun deleteWeight(record: WeightRecord) = dao.deleteWeight(record)

    // --- Medications ---
    val allMedications: Flow<List<MedicationRecord>> = dao.getAllMedications()
    suspend fun insertMedication(record: MedicationRecord) = dao.insertMedication(record)
    suspend fun deleteMedication(record: MedicationRecord) = dao.deleteMedication(record)

    // --- Symptoms ---
    val allSymptoms: Flow<List<SymptomRecord>> = dao.getAllSymptoms()
    suspend fun insertSymptom(record: SymptomRecord) = dao.insertSymptom(record)
    suspend fun deleteSymptom(record: SymptomRecord) = dao.deleteSymptom(record)

    // --- Sleep ---
    val allSleep: Flow<List<SleepRecord>> = dao.getAllSleep()
    suspend fun insertSleep(record: SleepRecord) = dao.insertSleep(record)
    suspend fun deleteSleep(record: SleepRecord) = dao.deleteSleep(record)

    // --- Lab Results ---
    val allLabResults: Flow<List<LabResultRecord>> = dao.getAllLabResults()
    suspend fun insertLabResult(record: LabResultRecord) = dao.insertLabResult(record)
    suspend fun deleteLabResult(record: LabResultRecord) = dao.deleteLabResult(record)

    // --- Lifestyle ---
    val allLifestyle: Flow<List<LifestyleRecord>> = dao.getAllLifestyle()
    fun getLifestyleByType(type: String): Flow<List<LifestyleRecord>> = dao.getLifestyleByType(type)
    suspend fun insertLifestyle(record: LifestyleRecord) = dao.insertLifestyle(record)
    suspend fun deleteLifestyle(record: LifestyleRecord) = dao.deleteLifestyle(record)

    // --- Attachments ---
    val allAttachments: Flow<List<AttachmentRecord>> = dao.getAllAttachments()
    suspend fun insertAttachment(record: AttachmentRecord) = dao.insertAttachment(record)
    suspend fun deleteAttachment(record: AttachmentRecord) = dao.deleteAttachment(record)

    // --- Emergency Info ---
    val emergencyInfo: Flow<EmergencyInfo?> = dao.getEmergencyInfo()
    suspend fun insertEmergencyInfo(info: EmergencyInfo) = dao.insertEmergencyInfo(info)

    // --- Blood Sugar ---
    val allBloodSugar: Flow<List<BloodSugarRecord>> = dao.getAllBloodSugar()
    suspend fun insertBloodSugar(record: BloodSugarRecord) = dao.insertBloodSugar(record)
    suspend fun deleteBloodSugar(record: BloodSugarRecord) = dao.deleteBloodSugar(record)

    // --- Mood ---
    val allMoods: Flow<List<MoodRecord>> = dao.getAllMoods()
    suspend fun insertMood(record: MoodRecord) = dao.insertMood(record)
    suspend fun deleteMood(record: MoodRecord) = dao.deleteMood(record)

    // --- Medication Doses ---
    val allMedicationDoses: Flow<List<MedicationDoseRecord>> = dao.getAllMedicationDoses()
    suspend fun insertMedicationDose(record: MedicationDoseRecord) = dao.insertMedicationDose(record)
    suspend fun deleteMedicationDose(record: MedicationDoseRecord) = dao.deleteMedicationDose(record)

    // --- Family Members ---
    val allFamilyMembers: Flow<List<FamilyMemberProfile>> = dao.getAllFamilyMembers()
    suspend fun insertFamilyMember(profile: FamilyMemberProfile) = dao.insertFamilyMember(profile)
    suspend fun deleteFamilyMember(profile: FamilyMemberProfile) = dao.deleteFamilyMember(profile)

    // --- Height (child growth) ---
    val allHeights: Flow<List<HeightRecord>> = dao.getAllHeights()
    suspend fun insertHeight(record: HeightRecord) = dao.insertHeight(record)
    suspend fun deleteHeight(record: HeightRecord) = dao.deleteHeight(record)

    // --- Appointments ---
    val allAppointments: Flow<List<AppointmentRecord>> = dao.getAllAppointments()
    suspend fun insertAppointment(record: AppointmentRecord) = dao.insertAppointment(record)
    suspend fun updateAppointment(record: AppointmentRecord) = dao.updateAppointment(record)
    suspend fun deleteAppointment(record: AppointmentRecord) = dao.deleteAppointment(record)
}
