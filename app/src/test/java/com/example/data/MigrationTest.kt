package com.example.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Regression tests for the AppDatabase migration chain (v2 -> v9).
 *
 * These deliberately avoid androidx.room:room-testing's MigrationTestHelper, since that API
 * requires exported schema JSON files (exportSchema = true + a wired room.schemaLocation KSP
 * arg + the resulting assets on the test classpath) which this project does not currently
 * produce (exportSchema = false) and which can't be safely introduced and verified without a
 * working Gradle/KSP build. Instead, each test hand-builds the historical schema at an older
 * version with plain SQLite, then opens it through the real Room.databaseBuilder(...)
 * production entry point with the same Migration objects used by the app. Room performs its
 * own unconditional internal schema validation on open — if a migration doesn't produce the
 * exact schema Room expects from the current @Entity definitions, build()/first access throws,
 * which is exactly the failure mode a real user would hit after an app update.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MigrationTest {

    private val dbName = "migration-test.db"
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getDatabasePath(dbName).let { if (it.exists()) it.delete() }
    }

    @After
    fun tearDown() {
        context.getDatabasePath(dbName).let { if (it.exists()) it.delete() }
    }

    /** Historical v2 schema: the 10 tables that predate every migration in the current chain. */
    private fun createV2Database(): File {
        val dbFile = context.getDatabasePath(dbName)
        val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        db.execSQL("CREATE TABLE blood_pressure_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, systolic INTEGER NOT NULL, diastolic INTEGER NOT NULL, heartRate INTEGER NOT NULL, timestamp INTEGER NOT NULL, notes TEXT NOT NULL DEFAULT '')")
        db.execSQL("CREATE TABLE weight_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, weightKg REAL NOT NULL, timestamp INTEGER NOT NULL, notes TEXT NOT NULL DEFAULT '')")
        db.execSQL("CREATE TABLE medications (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, dosage TEXT NOT NULL, frequency TEXT NOT NULL, isActive INTEGER NOT NULL DEFAULT 1, timestamp INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE symptoms (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, symptomName TEXT NOT NULL, severity TEXT NOT NULL, timestamp INTEGER NOT NULL, notes TEXT NOT NULL DEFAULT '')")
        db.execSQL("CREATE TABLE sleep_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, hours REAL NOT NULL, timestamp INTEGER NOT NULL, notes TEXT NOT NULL DEFAULT '')")
        db.execSQL("CREATE TABLE lab_results (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, testName TEXT NOT NULL, value REAL NOT NULL, unit TEXT NOT NULL, timestamp INTEGER NOT NULL, referenceRange TEXT NOT NULL DEFAULT '')")
        db.execSQL("CREATE TABLE lifestyle_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT NOT NULL, amount REAL NOT NULL, timestamp INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE attachments (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, title TEXT NOT NULL, fileUri TEXT NOT NULL, notes TEXT NOT NULL DEFAULT '', timestamp INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE emergency_info (id INTEGER PRIMARY KEY NOT NULL, fullName TEXT NOT NULL DEFAULT '', bloodType TEXT NOT NULL DEFAULT '', chronicConditions TEXT NOT NULL DEFAULT '', allergies TEXT NOT NULL DEFAULT '', contactName TEXT NOT NULL DEFAULT '', contactPhone TEXT NOT NULL DEFAULT '', additionalNotes TEXT NOT NULL DEFAULT '')")
        db.execSQL("CREATE TABLE blood_sugar_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, value REAL NOT NULL, unit TEXT NOT NULL DEFAULT 'mg/dL', category TEXT NOT NULL DEFAULT 'Fasting', timestamp INTEGER NOT NULL, notes TEXT NOT NULL DEFAULT '')")

        // Sample rows to confirm data survives the full migration chain untouched.
        db.execSQL("INSERT INTO blood_pressure_records (id, systolic, diastolic, heartRate, timestamp, notes) VALUES (1, 120, 80, 70, 1700000000000, 'pre-migration reading')")
        db.execSQL("INSERT INTO emergency_info (id, fullName, bloodType) VALUES (1, 'Test Patient', 'O+')")
        db.execSQL("INSERT INTO medications (id, name, dosage, frequency, isActive, timestamp) VALUES (1, 'Aspirin', '100mg', 'Daily', 1, 1700000000000)")

        db.version = 2
        db.close()
        return dbFile
    }

    private fun openMigratedDatabase(dbFile: File): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, dbFile.absolutePath)
            .addMigrations(
                AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9
            )
            // Deliberately no fallbackToDestructiveMigration() — a missing/bad migration must
            // throw here, not silently wipe data, exactly like it must in production.
            .build()

    @Test
    fun `full migration chain v2 to v9 preserves data and produces a valid schema`() {
        val dbFile = createV2Database()
        val db = openMigratedDatabase(dbFile)

        // Opening the readable database forces Room to run the migrations and validate the
        // resulting schema against its compiled expectations right now, not lazily later.
        val sqliteDb = db.openHelper.writableDatabase

        // Pre-existing data survived the chain.
        sqliteDb.query("SELECT systolic, diastolic, notes, profileId FROM blood_pressure_records WHERE id = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(120, c.getInt(0))
            assertEquals(80, c.getInt(1))
            assertEquals("pre-migration reading", c.getString(2))
            assertEquals(0, c.getInt(3)) // profileId defaulted to 0 by MIGRATION_6_7
        }
        sqliteDb.query("SELECT fullName, sex, numberOfChildren FROM emergency_info WHERE id = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Test Patient", c.getString(0))
            assertEquals("", c.getString(1)) // sex defaulted to '' by MIGRATION_2_3
            assertEquals(0, c.getInt(2))
        }
        // This round's migration (MIGRATION_8_9): new medication columns with correct defaults.
        sqliteDb.query("SELECT name, reminderTimeMinutes, reminderEnabled FROM medications WHERE id = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Aspirin", c.getString(0))
            assertTrue(c.isNull(1)) // reminderTimeMinutes is nullable, no default -> NULL
            assertEquals(1, c.getInt(2)) // reminderEnabled defaults to 1 (true)
        }

        // Tables created by later migrations all exist and are queryable.
        listOf(
            "mood_records", "medication_doses", "family_members", "appointments",
            "height_records", "period_records", "vaccination_records"
        ).forEach { table ->
            sqliteDb.query("SELECT COUNT(*) FROM $table").use { c ->
                assertTrue("table $table should exist and be queryable", c.moveToFirst())
            }
        }

        db.close()
    }

    @Test
    fun `MIGRATION_2_3 alone adds sex and numberOfChildren with defaults`() {
        val dbFile = context.getDatabasePath(dbName)
        val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        db.execSQL("CREATE TABLE emergency_info (id INTEGER PRIMARY KEY NOT NULL, fullName TEXT NOT NULL DEFAULT '')")
        db.execSQL("INSERT INTO emergency_info (id, fullName) VALUES (1, 'Jane Doe')")
        db.version = 2
        db.close()

        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbFile.absolutePath)
            .addMigrations(
                AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9
            )
            .build()

        roomDb.openHelper.writableDatabase.query("SELECT fullName, sex, numberOfChildren FROM emergency_info WHERE id = 1").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("Jane Doe", c.getString(0))
            assertEquals("", c.getString(1))
            assertEquals(0, c.getInt(2))
        }
        roomDb.close()
    }
}
