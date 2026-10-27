package app.movelegs

const val DAILY_GOAL = 4

data class Challenge(
    val id: String,
    val title: String,
    val how: String,
    val amount: String,
    val standing: Boolean = false,
)

val CHALLENGES = listOf(
    Challenge("ankle_pump", "Стопы-качалки", "Сидя: носок на себя, потом от себя. Медленно, до тепла в икрах.", "20 раз"),
    Challenge("ankle_circle", "Нарисуй кружочки", "Оторви ногу от пола и вращай стопой. Потом в другую сторону.", "10 + 10, каждая нога"),
    Challenge("calf_raise", "На цыпочки", "Встань, держась за стол. Поднимись на носки, задержись, опустись.", "15 раз, пауза 3 сек", standing = true),
    Challenge("walk", "Прогулка до окна", "Просто пройдись. Кухня, коридор, лестница — любой маршрут.", "3 минуты", standing = true),
    Challenge("heel_toe", "Перекат", "Стоя или сидя: пятка вверх, носок вверх, как будто качаешь педаль.", "20 перекатов"),
    Challenge("pedal", "Велосипед под столом", "Крути воображаемые педали. Можно вперёд и назад.", "1 минута"),
    Challenge("march", "Марш на месте", "Сидя, колени поднимаются повыше. Спина прямая.", "30 шагов"),
    Challenge("stairs", "Один пролёт", "Подъём по лестнице обычным шагом. Лифт подождёт.", "1 пролёт", standing = true),
    Challenge("wall_stretch", "Растяжка у стены", "Руки на стену, одна нога назад, пятка на полу. Тяни икру.", "30 сек на ногу", standing = true),
    Challenge("legs_up", "Ноги выше сердца", "Ляг и подними ноги на подушку или стену. Дай венам отдохнуть.", "2 минуты"),
)

fun challengeById(id: String?) = CHALLENGES.firstOrNull { it.id == id } ?: CHALLENGES.first()

data class Sticker(
    val id: String,
    val title: String,
    val hint: String,
    val glyph: String,
)

val STICKERS = listOf(
    Sticker("first", "Первый шаг", "Выполни первый челлендж", "1"),
    Sticker("day_goal", "Полный день", "$DAILY_GOAL челленджа за день", "★"),
    Sticker("streak3", "Три дня подряд", "Серия из 3 дней", "3д"),
    Sticker("streak7", "Неделя на ногах", "Серия из 7 дней", "7д"),
    Sticker("total25", "Четверть сотни", "25 челленджей всего", "25"),
    Sticker("total100", "Сотня", "100 челленджей всего", "100"),
)

val TIPS = listOf(
    "Ногам нужен перерыв примерно каждые 30 минут. Мышцы икр работают как насос для вен.",
    "Лучше часто и понемногу, чем один раз и до упора.",
    "Если вены болят, отекают или меняют цвет, покажись сосудистому хирургу. Приложение не лечит.",
)
