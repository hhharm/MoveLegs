package app.movelegs

import android.content.Context
import java.time.LocalDate

class Store(ctx: Context) {
    private val p = ctx.applicationContext.getSharedPreferences("movelegs", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = p.getBoolean("enabled", true)
        set(v) = p.edit().putBoolean("enabled", v).apply()

    var startHour: Int
        get() = p.getInt("startHour", 9)
        set(v) = p.edit().putInt("startHour", v.coerceIn(0, 22)).apply()

    var endHour: Int
        get() = p.getInt("endHour", 21)
        set(v) = p.edit().putInt("endHour", v.coerceIn(1, 23)).apply()

    var currentId: String?
        get() = p.getString("currentId", null)
        set(v) = p.edit().putString("currentId", v).apply()

    val total: Int get() = p.getInt("total", 0)
    val bestStreak: Int get() = p.getInt("best", 0)

    val todayCount: Int
        get() = if (p.getString("todayDate", null) == today()) p.getInt("todayCount", 0) else 0

    /** Серия жива, если цель достигнута сегодня или вчера. */
    val streak: Int
        get() {
            val last = p.getString("lastGoalDate", null) ?: return 0
            val ok = last == today() || last == LocalDate.now().minusDays(1).toString()
            return if (ok) p.getInt("streak", 0) else 0
        }

    val unlocked: Set<String> get() = p.getStringSet("stickers", emptySet()) ?: emptySet()

    fun pickNext(): Challenge {
        val pool = CHALLENGES.filter { it.id != currentId }
        return pool.random().also { currentId = it.id }
    }

    /** Возвращает новые стикеры, открытые этим действием. */
    fun markDone(): List<Sticker> {
        val today = today()
        var count = todayCount + 1
        val total = total + 1
        val e = p.edit().putString("todayDate", today).putInt("todayCount", count).putInt("total", total)

        var streak = p.getInt("streak", 0)
        if (count == DAILY_GOAL) {
            val last = p.getString("lastGoalDate", null)
            streak = if (last == LocalDate.now().minusDays(1).toString()) streak + 1 else 1
            e.putString("lastGoalDate", today).putInt("streak", streak)
            if (streak > bestStreak) e.putInt("best", streak)
        }

        val have = unlocked
        val earned = buildSet {
            if (total >= 1) add("first")
            if (count >= DAILY_GOAL) add("day_goal")
            if (streak >= 3) add("streak3")
            if (streak >= 7) add("streak7")
            if (total >= 25) add("total25")
            if (total >= 100) add("total100")
        }
        val fresh = earned - have
        e.putStringSet("stickers", have + earned).apply()
        return STICKERS.filter { it.id in fresh }
    }

    private fun today() = LocalDate.now().toString()
}
