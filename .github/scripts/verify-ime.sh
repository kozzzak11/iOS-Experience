#!/usr/bin/env bash
# Keep the test exit status while collecting diagnostics before the emulator stops.
set -u
status=0
./gradlew --no-daemon connectedDebugAndroidTest || status=$?
mkdir -p app/build/device-diagnostics
adb pull /sdcard/Android/data/com.kozzzak.ios/files/ime-diagnostics app/build/device-diagnostics || true
adb logcat -d -v threadtime > app/build/device-diagnostics/logcat.txt || true
exit "$status"
