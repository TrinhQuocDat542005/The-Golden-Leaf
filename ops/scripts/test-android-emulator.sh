#!/usr/bin/env bash
set -euo pipefail
# Disposable CI emulator and synthetic loopback demo. Never reuse a user's server/device.
readonly api="${ANDROID_TEST_API:-35}"
[[ "$api" == 25 || "$api" == 35 ]] || { echo 'Only API 25/35 fixtures are allowed'; exit 1; }
readonly sdk="${ANDROID_HOME:?ANDROID_HOME is required}"
readonly fixture="golden-leaf-week8-${api}"
readonly image="system-images;android-${api};google_apis;x86_64"
readonly emulator_bin="$sdk/emulator/emulator"
readonly adb_bin="$sdk/platform-tools/adb"
# Use the same explicit Android-scoped locations for new CLI tools and emulator.
# Do not override HOME or reuse a user's AVD directory.
export ANDROID_USER_HOME="$PWD/build/week8/android-user-$api"
export ANDROID_EMULATOR_HOME="$ANDROID_USER_HOME"
export ANDROID_AVD_HOME="$ANDROID_USER_HOME/avd"
mkdir -p "$ANDROID_AVD_HOME"
mkdir -p build/week8
java -jar The-Golden-Leaf-server/target/datban-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo --server.port=18082 > build/week8/android-demo.log 2>&1 &
demo_pid=$!
emulator_pid=''
cleanup() {
  if [[ -n "$emulator_pid" ]]; then timeout 5 "$adb_bin" -s emulator-5580 logcat -d -t 2000 > build/week8/android-logcat.txt 2>&1 || true; fi
  [[ -z "$emulator_pid" ]] || kill "$emulator_pid" 2>/dev/null || true
  kill "$demo_pid" 2>/dev/null || true
  [[ -z "$emulator_pid" ]] || wait "$emulator_pid" 2>/dev/null || true
  wait "$demo_pid" 2>/dev/null || true
}
trap cleanup EXIT
for _ in $(seq 1 90); do
  if curl --fail --silent http://127.0.0.1:18082/actuator/health/readiness > /dev/null; then break; fi
  kill -0 "$demo_pid" || { echo 'Demo exited before readiness'; exit 1; }
  sleep 1
done
curl --fail --silent http://127.0.0.1:18082/api/demo/config | grep -q '"demo":true'
"$sdk/cmdline-tools/latest/bin/sdkmanager" "platforms;android-36" "build-tools;36.0.0" "$image" emulator platform-tools
"$sdk/cmdline-tools/latest/bin/avdmanager" create avd --force --name "$fixture" --path "$ANDROID_AVD_HOME/$fixture.avd" --package "$image" --device pixel <<< no
[[ -f "$ANDROID_AVD_HOME/$fixture.ini" ]] || { echo 'AVD registry missing at the explicit fixture location'; "$sdk/cmdline-tools/latest/bin/avdmanager" list avd; exit 1; }
"$emulator_bin" -avd "$fixture" -no-window -no-audio -no-snapshot -no-boot-anim -gpu swiftshader_indirect -port 5580 > build/week8/emulator.log 2>&1 &
emulator_pid=$!
export ANDROID_SERIAL=emulator-5580
for _ in $(seq 1 180); do
  if [[ "$("$adb_bin" -s "$ANDROID_SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]]; then break; fi
  kill -0 "$emulator_pid" || { echo 'Emulator exited before boot'; exit 1; }
  sleep 2
done
[[ "$("$adb_bin" -s "$ANDROID_SERIAL" shell getprop sys.boot_completed | tr -d '\r')" == 1 ]] || { echo 'Emulator boot timeout'; exit 1; }
"$adb_bin" -s "$ANDROID_SERIAL" shell input keyevent 82
"$adb_bin" -s "$ANDROID_SERIAL" shell settings put global window_animation_scale 0
"$adb_bin" -s "$ANDROID_SERIAL" shell settings put global transition_animation_scale 0
"$adb_bin" -s "$ANDROID_SERIAL" shell settings put global animator_duration_scale 0
# Headless Google API images can leave an ANR dialog from their unused home launcher.
# Disable only known launcher packages in this disposable AVD, never on a user's device.
for launcher in com.google.android.apps.nexuslauncher com.android.launcher3; do
  if "$adb_bin" -s "$ANDROID_SERIAL" shell pm path "$launcher" | grep -q '^package:'; then
    "$adb_bin" -s "$ANDROID_SERIAL" shell am force-stop "$launcher"
    "$adb_bin" -s "$ANDROID_SERIAL" shell pm disable-user --user 0 "$launcher"
  fi
done
bash gradlew --no-daemon assembleDebug assembleDebugAndroidTest -Pweek8IsolatedTests=true
"$adb_bin" -s "$ANDROID_SERIAL" install -r app/build/outputs/apk/debug/app-debug.apk
"$adb_bin" -s "$ANDROID_SERIAL" install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
# Native JUnit avoids AGP UTP's incompatible emulator-console handshake; tests are unchanged.
"$adb_bin" -s "$ANDROID_SERIAL" shell am instrument -w -r -e demoBaseUrl http://10.0.2.2:18082 com.example.giaodien.test/com.example.giaodien.Week8TestRunner | tee build/week8/instrumentation-results.txt
node scripts/check-android-instrumentation.mjs
for shot in slot-selection network-error; do
  "$adb_bin" -s "$ANDROID_SERIAL" exec-out run-as com.example.giaodien cat "files/week8-$shot.png" > "build/week8/android-$shot.png"
done
"$adb_bin" -s "$ANDROID_SERIAL" logcat -d -t 2000 > build/week8/android-logcat.txt
