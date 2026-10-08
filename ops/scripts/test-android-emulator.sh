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
mkdir -p build/week8
java -jar The-Golden-Leaf-server/target/datban-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo --server.port=18082 > build/week8/android-demo.log 2>&1 &
demo_pid=$!
emulator_pid=''
cleanup() {
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
"$sdk/cmdline-tools/latest/bin/avdmanager" create avd --force --name "$fixture" --package "$image" --device pixel <<< no
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
bash gradlew --no-daemon connectedDebugAndroidTest -Pweek8IsolatedTests=true -Pandroid.testInstrumentationRunnerArguments.demoBaseUrl=http://10.0.2.2:18082
"$adb_bin" -s "$ANDROID_SERIAL" logcat -d -t 2000 > build/week8/android-logcat.txt
