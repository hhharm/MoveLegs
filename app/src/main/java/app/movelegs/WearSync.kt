package app.movelegs

import android.content.Context
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService

/** Телефон -> часы: текущий челлендж и счётчики. Должен совпадать с wear/WatchApp.kt. */
object WearSync {
    const val STATE = "/state"
    const val DONE = "/done"
    const val SNOOZE = "/snooze"

    fun push(ctx: Context) {
        val s = Store(ctx)
        val c = challengeById(s.currentId)
        val req = PutDataMapRequest.create(STATE).apply {
            dataMap.putString("title", c.title)
            dataMap.putString("how", c.how)
            dataMap.putString("amount", c.amount)
            dataMap.putInt("today", s.todayCount)
            dataMap.putInt("goal", DAILY_GOAL)
            dataMap.putInt("streak", s.streak)
            // Без метки времени одинаковые данные не вызовут обновление на часах.
            dataMap.putLong("ts", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        Wearable.getDataClient(ctx.applicationContext).putDataItem(req)
    }
}

/** Часы -> телефон: «Готово» и «Отложить». */
class WearListener : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        val s = Store(this)
        when (event.path) {
            WearSync.DONE -> {
                s.markDone()
                s.pickNext()
                Reminders.cancelNotification(this)
                Reminders.schedule(this)
            }
            WearSync.SNOOZE -> {
                Reminders.cancelNotification(this)
                Reminders.schedule(this, 10L)
            }
            else -> return
        }
        WearSync.push(this)
    }
}
