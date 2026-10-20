# Архитектура

Один APK, пакет com.kozzzak.ios, minSdk 28, target/compileSdk 36.

- `settings/SettingsActivity`: Compose Material3, статус IME через InputMethodManager, официальный ACTION_INPUT_METHOD_SETTINGS, официальный picker, настройки/превью, ручной словарь, тестовые редакторы.
- `keyboard/IosInputMethodService`: lifecycle IME, текущая InputConnection, composing, действия редактора, RU/EN, слова/emoji. Не использует foreground service и не работает при отсутствии активной IME-сессии.
- `keyboard/EditorPolicy`: классификация password / private / email / URI / numeric. Private policy запрещает запрос контекста.
- `keyboard/KeyView`: лёгкий View вместо Compose в горячем пути клавиатуры. Cancellable repeat и long press, тёплое состояние paint, key feedback. Отмена при ACTION_CANCEL/detach.
- `keyboard/KeyboardSurface`: свайп вниз только внутри собственной клавиатуры.
- `keyboard/WordEngine`: детерминированный офлайн baseline. Дистанция редактирования с транспозицией, префиксы, сохранение регистра; автоматическая замена только при однозначном кандидате.
- `keyboard/EmojiCatalog`: локальные Unicode 17.0 metadata, поиск, recent и skin tones. Для списка используется GridView, чтобы не создавать все ячейки сразу.
- `keyboard/KeyboardPrefs`: настройки, вручную добавленные слова, опциональные recent emoji. Не принимает и не сохраняет произвольный ввод из редакторов.

IME и settings — компоненты одного app-модуля. Launcher/HOME, widget host и возможные жесты следует добавлять изолированными пакетами и включать независимо. Не выделять дополнительные устанавливаемые приложения.

Внутренние границы платформы и источники — PLATFORM-LIMITS.md. План развития — ROADMAP.md. Приватные ключи подписи, APK и SDK не должны попадать в исходный репозиторий.
