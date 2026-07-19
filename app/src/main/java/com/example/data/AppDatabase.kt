package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope

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
        FamilyMemberProfile::class,
        AppointmentRecord::class
    ],
    version = 6,
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

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE family_members ADD COLUMN sex TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE family_members ADD COLUMN chronicConditions TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE family_members ADD COLUMN allergies TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE family_members ADD COLUMN emergencyContactName TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE family_members ADD COLUMN emergencyContactPhone TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE family_members ADD COLUMN heightCm REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE family_members ADD COLUMN weightKg REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS appointments (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "title TEXT NOT NULL, " +
                    "type TEXT NOT NULL DEFAULT 'Doctor', " +
                    "dateTimestamp INTEGER NOT NULL, " +
                    "notes TEXT NOT NULL DEFAULT '', " +
                    "profileId INTEGER NOT NULL DEFAULT 0, " +
                    "profileName TEXT NOT NULL DEFAULT '', " +
                    "isCompleted INTEGER NOT NULL DEFAULT 0)"
                )
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medical_journal_database"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
