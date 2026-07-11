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

class HealthAlertsWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = (context.applicationContext as MedicalJournalApp).repository

        val allBp = repository.allBloodPressure.first()
        if (allBp.size < 3) return Result.success()

        val latest = allBp.first()
        val cutoff = System.currentTimeMillis() - 24L * 3600 * 1000
        if (latest.timestamp < cutoff) return Result.success()  // no reading today

        val fourteenDaysCutoff = System.currentTimeMillis() - 14L * 24 * 3600 * 1000
        val baseline = allBp.drop(1).filter { it.timestamp >= fourteenDaysCutoff }
        if (baseline.size < 2) return Result.success()

        val avgSystolic = baseline.map { it.systolic }.average().toFloat()
        val diff = latest.systolic - avgSystolic

        if (diff >= 15f) {
            sendPatternNotification(latest.systolic, avgSystolic.toInt(), diff.toInt())
        }

        return Result.success()
    }

    private fun sendPatternNotification(latestSystolic: Int, avgSystolic: Int, diff: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, MedicalJournalApp.CHANNEL_PATTERNS)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Unusual Blood Pressure Pattern")
            .setContentText("Today's BP ($latestSystolic mmHg systolic) is $diff points above your 14-day average ($avgSystolic mmHg).")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Today's systolic reading of $latestSystolic mmHg is $diff points above your recent 14-day average of $avgSystolic mmHg. Consider checking in with your doctor if this trend continues."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID_PATTERN, notification)
    }

    companion object {
        const val WORK_NAME = "health_pattern_check"
        const val NOTIFICATION_ID_PATTERN = 1001
    }
}
