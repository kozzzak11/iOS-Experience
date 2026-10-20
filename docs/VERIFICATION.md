# Проверка 0.1.0

20 октября 2026: [GitHub Actions](https://github.com/kozzzak11/iOS-Experience/actions/runs/39510468341) — build, emulator и release завершились успешно.
Проверенный commit: `f6b09cddedda19a1850a2f7864ec586335093425`. Обновление этого отчёта не меняет код APK.

- Kotlin / Android SDK 36 compilation: PASS.
- testDebugUnitTest: 16 тестов, 0 ошибок, 0 пропущенных. Robolectric 4.16, Android SDK 35, JDK 17.
- lintDebug: 0 ошибок, 17 предупреждений: рекомендации KTX, обновления зависимостей/target SDK и fullBackupContent для старых Android. allowBackup=false установлен.
- assembleDebug / assembleDebugAndroidTest: PASS.
- connectedDebugAndroidTest: PASS — Android 16, API 36, x86_64, профиль Pixel 6. Реальная IME активирована системой; UI-нажатия ввели `hello привет ` в настоящий EditText, переключили язык и скрыли клавиатуру.
- Отступ нижнего ряда учитывает системную панель навигации. Регрессионный unit-тест проверяет повторную доставку отступов, трёхкнопочную и жестовую навигацию без использования высоты самой IME.
- Manifest: нет INTERNET, microphone, storage, AccessibilityService, overlay permissions.

Unit-сценарии: RU/EN composing и commit; удаление при composing; замена выделения; удаление семейного ZWJ emoji целиком; отсутствие чтения/compose/persistence в password; типы password/PIN/email/incognito; смена поля; IME_ACTION_SEND и multiline; перенос курсора; офлайн correction/prefix; регистр; словарь; каталог и оттенки кожи.

[Скачать проверенный APK](https://github.com/kozzzak11/iOS-Experience/releases/download/v0.1.0/iOS-Experience.apk) — 25364751 байт.
SHA-256: `65c932de48f35375da19e54ac0fdeb81759df5707e2f30ab61fc7d218b458ebd` (digest опубликованного GitHub asset).
Это APK с development-подписью. Постоянный ключ обновлений пока не настроен.

Физический Nothing Phone (4a): **не тестировался**. Telegram, WhatsApp, Chrome и ChatGPT: **не тестировались**. Проверка эмулятора подтверждает перечисленный сценарий, а не всю совместимость с приложениями и Nothing OS.

`verification/results.json` содержит сводку текущего CI. Отчёты unit/lint и устройства доступны в Artifacts указанного запуска Actions. `verification/lint.txt` и изображения `keyboard-light.png`, `keyboard-dark.png` относятся к первоначальной локальной проверке. Изображения отрисованы в native graphics Robolectric и визуально просмотрены; это не скриншоты телефона. Первоначальный локальный APK отдельно проверялся apksigner; для текущего CI APK эта отдельная проверка не повторялась.
