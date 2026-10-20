# Проверка 0.1.0

Выполнено 20 октября 2026:
- Kotlin / Android SDK 36 compilation: PASS.
- testDebugUnitTest: 15 тестов, 0 ошибок, 0 пропущенных. Robolectric 4.16, Android SDK 35, JDK 17.
- lintDebug: 0 ошибок, 11 предупреждений (обновления зависимостей, предложения KTX и рекомендация fullBackupContent для старых Android; allowBackup=false установлен).
- assembleDebug: PASS.
- assembleDebugAndroidTest: PASS — тестовый APK скомпилирован, не запущен.
- apksigner verify: PASS, v2, один development signer.
- Manifest проверен aapt: нет INTERNET, microphone, storage, AccessibilityService, overlay permissions.
- Светлая и тёмная клавиатура отрисованы в native graphics Robolectric и визуально просмотрены. Это не скриншоты телефона.

Проверяемые сценарии: RU/EN composing и commit; удаление при composing; замена выделения; удаление семейного ZWJ emoji целиком; отсутствие чтения/compose/persistence в password; типы password/PIN/email/incognito; смена поля; IME_ACTION_SEND и multiline; перенос курсора; офлайн correction/prefix; регистр; словарь; каталог и оттенки кожи.

Физический Nothing Phone (4a): **не тестировался**. Android 16 emulator: **не запускался**. Telegram, WhatsApp, Chrome и ChatGPT: **не тестировались**.

GitHub: публичный репозиторий kozzzak11/iOS-Experience создан пользователем; публичная публикация разрешена. Workflow запускает build и Android 16 emulator при push в main. Локальные результаты выше не заменяют статус Actions.

SHA-256 APK: `ff22c3f8df23e6648cd46daf449f8a8823c2fd71f13bd9a9579a3c4f0060de0f`

Подробные результаты: verification/results.json, lint.txt. Изображения: verification/keyboard-light.png и keyboard-dark.png.
