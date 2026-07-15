package com.example.workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.MedicalJournalApp
import kotlinx.coroutines.flow.first

class MedicationReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = (context.applicationContext as MedicalJournalApp).repository
        val activeMeds = repository.allMedications.first().filter { it.isActive }
        if (activeMeds.isEmpty()) return Result.success()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val medList = activeMeds.joinToString("\n") { "• ${it.name} ${it.dosage} (${it.frequency})" }
        val notification = NotificationCompat.Builder(context, MedicalJournalApp.CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Medication Reminder")
            .setContentText("Time to take your medications")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Don't forget your medications:\n$medList"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID_REMINDER, notification)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "medication_daily_reminder"
        const val NOTIFICATION_ID_REMINDER = 1002
    }
}
