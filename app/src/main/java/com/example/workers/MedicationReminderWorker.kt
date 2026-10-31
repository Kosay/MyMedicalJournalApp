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
import com.example.ui.LocalStrings
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MedicationReminderWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    // Default time-of-day (minutes since midnight) for medications with no custom reminder
    // time set — preserves the original "once a day" reminder for existing medications.
    private val defaultReminderMinutes = 9 * 60 // 9:00 AM

    override suspend fun doWork(): Result {
        val repository = (context.applicationContext as MedicalJournalApp).repository
        val activeMeds = repository.allMedications.first().filter { it.isActive && it.reminderEnabled }
        if (activeMeds.isEmpty()) return Result.success()

        val prefs = context.getSharedPreferences("medical_journal_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("lang", "en") ?: "en"
        fun trans(key: String) = LocalStrings.get(key, lang)

        val now = Calendar.getInstance()
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(now.time)

        val dueMeds = activeMeds.filter { med ->
            val targetMinutes = med.reminderTimeMinutes ?: defaultReminderMinutes
            val alreadyNotifiedToday = prefs.getBoolean("med_notified_${med.id}_$today", false)
            !alreadyNotifiedToday && nowMinutes >= targetMinutes && nowMinutes < targetMinutes + 15
        }
        if (dueMeds.isEmpty()) return Result.success()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        val prefsEditor = prefs.edit()

        // Medications with their own custom time get their own notification; the rest
        // (default-time medications) are grouped into one notification like before.
        val (timed, defaultTimed) = dueMeds.partition { it.reminderTimeMinutes != null }

        timed.forEach { med ->
            val notification = NotificationCompat.Builder(context, MedicalJournalApp.CHANNEL_REMINDERS)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(trans("medication_reminder_title"))
                .setContentText("${med.name} ${med.dosage} (${med.frequency})")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            manager.notify(NOTIFICATION_ID_REMINDER + med.id, notification)
            prefsEditor.putBoolean("med_notified_${med.id}_$today", true)
        }

        if (defaultTimed.isNotEmpty()) {
            val medList = defaultTimed.joinToString("\n") { "• ${it.name} ${it.dosage} (${it.frequency})" }
            val notification = NotificationCompat.Builder(context, MedicalJournalApp.CHANNEL_REMINDERS)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setContentTitle(trans("medication_reminder_title"))
                .setContentText(trans("medication_reminder_subtitle"))
                .setStyle(NotificationCompat.BigTextStyle().bigText("${trans("medication_reminder_list_prefix")}\n$medList"))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            manager.notify(NOTIFICATION_ID_REMINDER, notification)
            defaultTimed.forEach { prefsEditor.putBoolean("med_notified_${it.id}_$today", true) }
        }

        prefsEditor.apply()
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "medication_daily_reminder"
        const val NOTIFICATION_ID_REMINDER = 1002
    }
}
