package com.lugat.kelime.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lugat.kelime.MainActivity
import com.lugat.kelime.R
import com.lugat.kelime.data.content.ContentSource
import com.lugat.kelime.domain.WordOfDay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object DailyWordNotifications {
    const val CHANNEL_ID = "daily_word"
    const val EXTRA_OPEN_WOTD = "open_word_of_day"
    private const val WORK_NAME = "daily_word"
    private const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Günün Kelimesi", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Her gün bir İngilizce kelime ve ilginç bir bilgi"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Seçilen saatte günde bir kez çalışacak işi (yeniden) planlar. */
    fun schedule(context: Context, enabled: Boolean, hour: Int, minute: Int) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(WORK_NAME)
            return
        }
        val now = LocalDateTime.now()
        var next = LocalDateTime.of(LocalDate.now(), LocalTime.of(hour, minute))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMillis()
        val request = PeriodicWorkRequestBuilder<DailyWordWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
    }

    fun show(context: Context) {
        if (!hasPermission(context)) return
        val content = ContentSource.get(context)
        val fact = content.facts.getOrNull(WordOfDay.index(content.facts.size, LocalDate.now().toEpochDay())) ?: return
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_WOTD, true)
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_word)
            .setContentTitle("Günün kelimesi: ${fact.en}")
            .setContentText("${fact.tr} · ${fact.fact}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("${fact.tr}\n\n${fact.fact}"))
            .setColor(0xFFFF6B3D.toInt())
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // İzin çalışma anında geri alınmış olabilir.
        }
    }
}

class DailyWordWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        DailyWordNotifications.show(applicationContext)
        return Result.success()
    }
}
