# Ноги в ход

Приложение для людей с варикозом: каждые 30 минут в дневное окно приходит небольшой челлендж
(стопы-качалки, подъёмы на носки, пройтись), за выполнение копятся стикеры и серия дней.

## Статус

MVP, **ещё не собирался**: в среде разработки нет Android SDK. Откройте проект в Android Studio
(Ladybug+), синхронизируйте Gradle и запустите `app` на телефоне или эмуляторе.

## Часы Samsung Galaxy Watch (модуль `wear`)

Wear OS-приложение с тем же `applicationId`, что у телефона. Это нужно, чтобы работал Data Layer.
Телефон шлёт часам текущий челлендж и счётчики (`WearSync.push`), часы показывают их на экране
в том же бумажном стиле и шлют обратно «Готово» или «Позже» (`WearListener` на телефоне).
Уведомления с кнопками по-прежнему приходят на часы через обычный мост уведомлений.

Часы работают только в паре с телефоном. Оба приложения должны быть подписаны одним ключом.
Для запуска: `:app` на телефон, `:wear` на часы или эмулятор Wear OS, парные через Android Studio.

## Как устроено

- `Content.kt` — каталог упражнений и стикеров.
- `Store.kt` — состояние в SharedPreferences: счётчики, серия, открытые стикеры.
- `Reminders.kt` — `AlarmManager.setAndAllowWhileIdle` раз в 30 минут внутри окна, уведомление с действиями, перезапуск после перезагрузки.
- `MainActivity.kt` — экран в стиле бумаги и штампов на Compose.

Серия дней растёт, когда за день выполнено `DAILY_GOAL` (4) челленджей.

## Источники упражнений

- [Iowa Clinic — двигайтесь чаще](https://iowaclinic.com/blog/articles/dont-just-stand-there-move-often-to-prevent-pain-from-varicose-veins)
- [Vein Directory — упражнения для вен](https://www.veindirectory.org/article/prevention-management/the-top-4-exercises-to-boost-vein-health-blood-circulation)
- [Medanta — что полезно и чего избегать](https://www.medanta.org/patient-education-blog/varicose-veins-exercises-activities-that-help-and-those-to-avoid)

Приложение не лечит и не заменяет врача. Боль, отёк или изменение цвета вен — к сосудистому хирургу.
