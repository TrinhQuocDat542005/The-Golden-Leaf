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
video_pid=''
video_mode=''
video_file=''
stop_video() {
  if [[ "$video_mode" == host ]]; then
    timeout 10 "$adb_bin" -s emulator-5580 emu screenrecord stop > build/week9/screenrecord-stop.log 2>&1 || true
    video_mode=''
  fi
  if [[ -n "$video_pid" ]]; then
    timeout 5 "$adb_bin" -s emulator-5580 shell pkill -2 screenrecord 2>/dev/null || true
    wait "$video_pid" 2>/dev/null || true
    video_pid=''
    timeout 30 "$adb_bin" -s emulator-5580 pull /sdcard/week9-demo.mp4 build/week9/android-demo.mp4 >/dev/null 2>&1 || true
  fi
}
cleanup() {
  stop_video
  if [[ -n "$emulator_pid" ]]; then timeout 5 "$adb_bin" -s emulator-5580 logcat -d -t 2000 > build/week8/android-logcat.txt 2>&1 || true; fi
  if [[ -n "$emulator_pid" ]]; then
    for shot in slot-selection network-error; do
      timeout 5 "$adb_bin" -s emulator-5580 exec-out run-as com.example.giaodien cat "files/week8-$shot-window.txt" > "build/week8/android-$shot-window.txt" 2>/dev/null || true
    done
    for shot in home payment history invoice menu review; do
      timeout 5 "$adb_bin" -s emulator-5580 exec-out run-as com.example.giaodien.demo cat "files/week9-$shot-window.txt" > "build/week9/android-$shot-window.txt" 2>/dev/null || true
    done
  fi
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
echo 'Installing isolated application APK (bounded, non-streaming)'
timeout 120 "$adb_bin" -s "$ANDROID_SERIAL" install --no-streaming -r app/build/outputs/apk/debug/app-debug.apk
echo 'Installing instrumentation APK (bounded, non-streaming)'
timeout 120 "$adb_bin" -s "$ANDROID_SERIAL" install --no-streaming -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
# Native JUnit avoids AGP UTP's incompatible emulator-console handshake; tests are unchanged.
echo 'Running four native instrumentation tests (bounded)'
timeout 180 "$adb_bin" -s "$ANDROID_SERIAL" shell am instrument -w -r -e demoBaseUrl http://10.0.2.2:18082 com.example.giaodien.test/com.example.giaodien.Week8TestRunner | tee build/week8/instrumentation-results.txt
node scripts/check-android-instrumentation.mjs
for shot in slot-selection network-error; do
  "$adb_bin" -s "$ANDROID_SERIAL" exec-out run-as com.example.giaodien cat "files/week8-$shot.png" > "build/week8/android-$shot.png"
done
"$adb_bin" -s "$ANDROID_SERIAL" logcat -d -t 2000 > build/week8/android-logcat.txt

# Real MyApp/MainActivity customer UI. Firebase stays disabled only in the separate demo APK.
mkdir -p build/week9
bash gradlew --no-daemon -PdemoInstrumentation=true -PDEMO_API_BASE_URL=http://10.0.2.2:18082/ assembleDemo assembleDemoAndroidTest
timeout 120 "$adb_bin" -s "$ANDROID_SERIAL" install --no-streaming -r app/build/outputs/apk/demo/app-demo.apk
timeout 120 "$adb_bin" -s "$ANDROID_SERIAL" install --no-streaming -r app/build/outputs/apk/androidTest/demo/app-demo-androidTest.apk
if "$adb_bin" -s "$ANDROID_SERIAL" shell test -x /system/bin/screenrecord; then
  video_file=build/week9/android-demo.mp4
  timeout 190 "$adb_bin" -s "$ANDROID_SERIAL" shell screenrecord --size 720x1280 --bit-rate 1500000 --time-limit 180 /sdcard/week9-demo.mp4 > build/week9/screenrecord.log 2>&1 &
  video_pid=$!
else
  # API 25 Google API image has no guest screenrecord binary. Record the same
  # live display using the documented host console, not a stitched screenshot video.
  video_file=build/week9/android-demo.webm
  video_mode=host
  timeout 10 "$adb_bin" -s "$ANDROID_SERIAL" emu screenrecord start --time-limit 180 "$PWD/$video_file" > build/week9/screenrecord.log 2>&1
  grep -q '^OK' build/week9/screenrecord.log && ! grep -q '^KO' build/week9/screenrecord.log || { echo 'Host recorder did not start'; exit 1; }
fi
timeout 300 "$adb_bin" -s "$ANDROID_SERIAL" shell am instrument -w -r -e class com.example.giaodien.DemoAppTest com.example.giaodien.demo.test/androidx.test.runner.AndroidJUnitRunner | tee build/week9/instrumentation-results.txt
node scripts/check-android-instrumentation.mjs build/week9/instrumentation-results.txt build/week9/instrumentation-results.xml 3
stop_video
[[ -s "$video_file" ]] || { echo 'Native demo recording is missing'; exit 1; }
# Decode every input frame. Normalize only null-output timestamps to avoid
# screenrecord's variable-rate timestamps being rounded to duplicate muxer DTS.
# The original recording is never transcoded or modified.
ffmpeg -hide_banner -v error -xerror -i "$video_file" -map 0:v:0 -vf 'setpts=N/(30*TB)' -fps_mode passthrough -enc_time_base 1:30 -f null - > build/week9/video-validation.log 2>&1
ffprobe -v error -select_streams v:0 -show_entries stream=codec_name,width,height,duration:format=duration -of json "$video_file" > build/week9/video-metadata.json
for shot in home payment history invoice menu review; do
  "$adb_bin" -s "$ANDROID_SERIAL" exec-out run-as com.example.giaodien.demo cat "files/week9-$shot.png" > "build/week9/android-$shot.png"
  [[ -s "build/week9/android-$shot.png" ]] || { echo "Missing native demo screenshot: $shot"; exit 1; }
done
"$adb_bin" -s "$ANDROID_SERIAL" logcat -d -t 2000 > build/week9/android-logcat.txt
