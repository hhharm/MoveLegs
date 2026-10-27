package app.movelegs

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.time.LocalDateTime

private const val CHANNEL = "nudges"
private const val NOTIF_ID = 1
private const val INTERVAL_MIN = 30L
private const val SNOOZE_MIN = 10L

private fun pi(ctx: Context, cls: Class<*>, action: String? = null, code: Int = 0): PendingIntent =
    PendingIntent.getBroadcast(
        ctx, code, Intent(ctx, cls).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

object Reminders {
    /** Следующее напоминание через [afterMin] минут, но не вне окна активности. */
    fun schedule(ctx: Context, afterMin: Long = INTERVAL_MIN) {
        val s = Store(ctx)
        val am = ctx.getSystemService(AlarmManager::class.java)
        val intent = pi(ctx, ReminderReceiver::class.java)
        if (!s.enabled) { am.cancel(intent); return }

        var at = LocalDateTime.now().plusMinutes(afterMin)
        if (at.hour >= s.endHour) at = at.plusDays(1).withHour(s.startHour).withMinute(0)
        else if (at.hour < s.startHour) at = at.withHour(s.startHour).withMinute(0)

        // Неточный будильник, который переживает Doze: точные права не нужны,
        // а сдвиг на пару минут для напоминания про ноги не страшен.
        val ms = at.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, intent)
    }

    fun notify(ctx: Context, c: Challenge) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Напоминания", NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 180, 120, 180)
            },
        )
        if (notificationsDenied(ctx)) return

        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentTitle(c.title)
            .setContentText(c.amount)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${c.how}\n${c.amount}"))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            // Кнопки автоматически появятся и на часах Galaxy Watch.
            .addAction(0, "Готово", pi(ctx, ActionReceiver::class.java, ActionReceiver.DONE, 1))
            .addAction(0, "Через 10 мин", pi(ctx, ActionReceiver::class.java, ActionReceiver.SNOOZE, 2))
            .build()
        NotificationManagerCompat.from(ctx).notify(NOTIF_ID, n)
    }

    fun cancelNotification(ctx: Context) = NotificationManagerCompat.from(ctx).cancel(NOTIF_ID)

    private fun notificationsDenied(ctx: Context) =
        android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val s = Store(ctx)
        if (!s.enabled) return
        Reminders.notify(ctx, s.pickNext())
        Reminders.schedule(ctx)
        WearSync.push(ctx)
    }
}

class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Reminders.cancelNotification(ctx)
        when (intent.action) {
            DONE -> {
                Store(ctx).apply { markDone(); pickNext() }
                Reminders.schedule(ctx)
            }
            SNOOZE -> Reminders.schedule(ctx, SNOOZE_MIN)
        }
        WearSync.push(ctx)
    }

    companion object {
        const val DONE = "app.movelegs.DONE"
        const val SNOOZE = "app.movelegs.SNOOZE"
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) Reminders.schedule(ctx)
    }
}
