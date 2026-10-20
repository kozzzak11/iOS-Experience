# Проверенные границы платформы

## Клавиатура
InputMethodService получает InputConnection только текущего редактора. Активацию IME подтверждает пользователь в системных настройках. Приложение не может назначить себя клавиатурой без этого шага. Скрытие свайпом реализуется только в области собственной клавиатуры; глобальные свайпы сторонних приложений не перехватываются.

Включение клавиатуры показывает стандартное предупреждение Android: любая IME технически получает ввод. В этом проекте нет INTERNET, аналитики, журналирования ввода или автоматического обучения. Парольные поля и IME_FLAG_NO_PERSONALIZED_LEARNING отключают composing, подсказки, исправления и чтение окружающего текста. В обычных полях текущее набираемое слово кратковременно хранится в RAM для composing, затем очищается. Пользовательский словарь заполняется только вручную. Recent emoji сохраняются отдельно и могут быть отключены/очищены.

## Emoji
Каталог содержит 3944 fully-qualified последовательности Unicode Emoji 17.0. Данные Unicode используются по Unicode License v3 (см. licenses/UNICODE.txt). Рисунки Apple и шрифт Apple Color Emoji не включены. Каталог и поддержка глифа — разные вещи: новые символы могут не отображаться установленным шрифтом телефона.

EmojiCompat настраивает отображение только в интегрировавшем его приложении. Клавиатура отправляет Unicode, не шрифт. Telegram, WhatsApp, Chrome, ChatGPT могут использовать собственные шрифты или графику. Обычному APK недоступна запись в системные разделы и подмена шрифтов других приложений. Поэтому глобальная Apple-замена без root в проекте не заявляется.

Официальные инструкции Nothing о выборе Roboto / Inter не подтверждают замену emoji-шрифта на Apple. На физическом Nothing Phone (4a) приложение ещё не проверялось. Пункты меню Nothing OS здесь не угадываются: используется ACTION_INPUT_METHOD_SETTINGS.

## Launcher и жесты — будущие этапы
Launcher будет отдельной HOME activity того же APK, RoleManager.ROLE_HOME, LauncherApps, AppWidgetHost. Это не даёт права заменить SystemUI, экран блокировки или экран недавних приложений. Badges потребуют добровольно включённого NotificationListenerService, содержимое уведомлений сохраняться не должно.

AccessibilityService может выполнять GLOBAL_ACTION_BACK после отдельного включения пользователем, но не предоставляет универсальной команды «Вперёд». В этом выпуске AccessibilityService, overlay и перехват системных краёв экрана отсутствуют. Жест «Вперёд» можно реализовать только там, где приложение само владеет историей.

## Root — необязательное исследование
Системная подмена шрифта через модификацию прошивки/модуль root иногда технически возможна, но зависит от конкретной прошивки и прав на ресурс. Root не исправляет приложения, которые рисуют emoji самостоятельно. Разблокировка загрузчика может стирать данные; модификации могут нарушать OTA, проверку целостности и запуск банковских приложений, создавать риск bootloop. Никаких root-действий и неподтверждённых инструкций для Nothing (4a) этот проект не выполняет.

## Первичные источники
- https://developer.android.com/reference/android/inputmethodservice/InputMethodService
- https://developer.android.com/reference/android/view/inputmethod/InputConnection
- https://developer.android.com/develop/ui/views/text-and-emoji/emoji2
- https://developer.android.com/reference/android/app/role/RoleManager#ROLE_HOME
- https://developer.android.com/reference/android/content/pm/LauncherApps
- https://developer.android.com/reference/android/appwidget/AppWidgetHost
- https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
- https://support.nothing.tech/hc/en-us/articles/43227863608209-How-do-I-change-fonts-on-the-Phone
- https://www.unicode.org/Public/17.0.0/emoji/emoji-test.txt
