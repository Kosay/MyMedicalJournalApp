package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        BloodPressureRecord::class,
        WeightRecord::class,
        MedicationRecord::class,
        SymptomRecord::class,
        SleepRecord::class,
        LabResultRecord::class,
        LifestyleRecord::class,
        AttachmentRecord::class,
        EmergencyInfo::class,
        BloodSugarRecord::class,
        MoodRecord::class,
        MedicationDoseRecord::class,
        FamilyMemberProfile::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun healthDao(): HealthDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE emergency_info ADD COLUMN sex TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE emergency_info ADD COLUMN numberOfChildren INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS mood_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, score INTEGER NOT NULL, notes TEXT NOT NULL DEFAULT '', timestamp INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS medication_doses (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, medicationId INTEGER NOT NULL DEFAULT 0, medicationName TEXT NOT NULL, timestamp INTEGER NOT NULL, taken INTEGER NOT NULL DEFAULT 1, notes TEXT NOT NULL DEFAULT '')")
                db.execSQL("CREATE TABLE IF NOT EXISTS family_members (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, relationship TEXT NOT NULL DEFAULT '', dateOfBirth TEXT NOT NULL DEFAULT '', bloodType TEXT NOT NULL DEFAULT '', notes TEXT NOT NULL DEFAULT '')")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medical_journal_database"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseSeederCallback(context, scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseSeederCallback(
        private val context: Context,
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.healthDao()
                    val now = System.currentTimeMillis()
                    val dayMs = 24 * 60 * 60 * 1000L

                    // 1. Seed Blood Pressure
                    dao.insertBloodPressure(BloodPressureRecord(systolic = 120, diastolic = 79, heartRate = 90, timestamp = now - 2 * 60 * 60 * 1000L, notes = "Resting"))
                    dao.insertBloodPressure(BloodPressureRecord(systolic = 125, diastolic = 82, heartRate = 92, timestamp = now - dayMs, notes = "After evening walk"))
                    dao.insertBloodPressure(BloodPressureRecord(systolic = 118, diastolic = 78, heartRate = 88, timestamp = now - 2 * dayMs, notes = "Morning check"))

                    // 2. Seed Weight
                    dao.insertWeight(WeightRecord(weightKg = 78.5f, timestamp = now - 1 * 60 * 60 * 1000L, notes = "Morning weight"))
                    dao.insertWeight(WeightRecord(weightKg = 79.0f, timestamp = now - 3 * dayMs, notes = "Post-workout"))
                    dao.insertWeight(WeightRecord(weightKg = 79.2f, timestamp = now - 5 * dayMs, notes = "Felt slightly bloated"))

                    // 3. Seed Medications
                    dao.insertMedication(MedicationRecord(name = "Lisinopril", dosage = "10mg", frequency = "Once daily (morning)", isActive = true))
                    dao.insertMedication(MedicationRecord(name = "Metformin", dosage = "500mg", frequency = "Twice daily with meals", isActive = true))
                    dao.insertMedication(MedicationRecord(name = "Atorvastatin", dosage = "20mg", frequency = "At bedtime", isActive = true))

                    // 4. Seed Symptoms
                    dao.insertSymptom(SymptomRecord(symptomName = "Headache", severity = "Mild", timestamp = now - 5 * 60 * 60 * 1000L, notes = "Resolved after drinking water"))
                    dao.insertSymptom(SymptomRecord(symptomName = "Fatigue", severity = "Moderate", timestamp = now - 2 * dayMs, notes = "Busy day at office"))

                    // 5. Seed Sleep
                    dao.insertSleep(SleepRecord(hours = 7.5f, timestamp = now - 10 * 60 * 60 * 1000L, notes = "Felt rested and refreshed"))
                    dao.insertSleep(SleepRecord(hours = 6.8f, timestamp = now - dayMs - 10 * 60 * 60 * 1000L, notes = "Slightly interrupted sleep"))
                    dao.insertSleep(SleepRecord(hours = 8.0f, timestamp = now - 2 * dayMs - 10 * 60 * 60 * 1000L, notes = "Excellent deep sleep"))

                    // 6. Seed Lab Results (including Screenshot 1's values!)
                    dao.insertLabResult(LabResultRecord(testName = "Cholesterol", value = 195f, unit = "mg/dL", timestamp = now - 12 * dayMs, referenceRange = "< 200 mg/dL"))
                    dao.insertLabResult(LabResultRecord(testName = "Triglycerides", value = 160f, unit = "mg/dL", timestamp = now - 12 * dayMs, referenceRange = "< 150 mg/dL"))
                    dao.insertLabResult(LabResultRecord(testName = "HbA1c", value = 5.6f, unit = "%", timestamp = now - 12 * dayMs, referenceRange = "4.0 - 5.6 %"))
                    dao.insertLabResult(LabResultRecord(testName = "Blood Glucose", value = 95f, unit = "mg/dL", timestamp = now - 12 * dayMs, referenceRange = "70 - 100 mg/dL"))

                    // 7. Seed Lifestyle
                    dao.insertLifestyle(LifestyleRecord(type = "water", amount = 1500f, timestamp = now - 4 * 60 * 60 * 1000L))
                    dao.insertLifestyle(LifestyleRecord(type = "exercise", amount = 30f, timestamp = now - 3 * 60 * 60 * 1000L))
                    dao.insertLifestyle(LifestyleRecord(type = "smoking", amount = 0f, timestamp = now))

                    // 8. Seed Emergency Info
                    dao.insertEmergencyInfo(
                        EmergencyInfo(
                            fullName = "Kosay Hatem",
                            bloodType = "A+",
                            chronicConditions = "Hypertension, Seasonal Allergies",
                            allergies = "Sulfonamides",
                            contactName = "Jane Doe (Spouse)",
                            contactPhone = "+1-555-0199",
                            additionalNotes = "In emergency, please search wallet for donor card.",
                            sex = "Male",
                            numberOfChildren = 0
                        )
                    )

                    // 9. Seed Blood Sugar records
                    dao.insertBloodSugar(BloodSugarRecord(value = 95f, category = "Fasting", timestamp = now - 4 * 60 * 60 * 1000L, notes = "Normal waking glucose"))
                    dao.insertBloodSugar(BloodSugarRecord(value = 122f, category = "Post-Prandial", timestamp = now - 2 * 60 * 60 * 1000L, notes = "2 hours after breakfast"))
                    dao.insertBloodSugar(BloodSugarRecord(value = 104f, category = "Random", timestamp = now - dayMs, notes = "Mid-afternoon general check"))
                }
            }
        }
    }
}
