package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.AppDatabase
import com.example.data.HealthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class MedicalJournalApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob())

    val database by lazy { AppDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { HealthRepository(database.healthDao()) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ALERTS,
                    "Health Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = "Danger zone warnings for critical health readings" }
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_PATTERNS,
                    "Pattern Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Unusual pattern alerts based on your health trends" }
            )
        }
    }

    companion object {
        const val CHANNEL_ALERTS = "health_alerts"
        const val CHANNEL_PATTERNS = "health_patterns"
    }
}
