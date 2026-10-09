# The Golden Leaf — Local portfolio demo

This bundle is a **local-only portfolio sandbox**, not a live restaurant service.
No Firebase project, database server, domain, account registration or bank account is required.

## Run

1. Install a JDK 17 and check `java -version`.
2. Extract the bundle into a new folder. Obtain it from the trusted CI run linked to the desired commit, not an unknown mirror.
3. Check `REVISION` and the JAR checksum. Checksums detect damaged files, not an untrusted publisher.

PowerShell:

```powershell
Get-Content REVISION
Get-Content SHA256SUMS
Get-FileHash golden-leaf-demo.jar -Algorithm SHA256
java -jar golden-leaf-demo.jar --spring.profiles.active=demo
```

Linux/macOS:

```bash
cat REVISION
sha256sum -c SHA256SUMS
java -jar golden-leaf-demo.jar --spring.profiles.active=demo
```

4. After startup, open `http://127.0.0.1:8080/demo.html`.
5. Dashboard: `http://127.0.0.1:8080/staff.html`; choose **Nhân viên** or **Quản trị viên** in the demo form.
6. Stop with Ctrl+C. Restart to get a fresh in-memory database and new session tokens. Never point demo at a real database or bind it publicly.

Port busy? Add `--server.port=18082` and use that port in the URLs. Do not combine demo with dev/prod/test profiles.

## Try the journey

- Select tomorrow, four guests, two **Salad vườn xanh**. Create booking, confirm, then create demo payment.
- Total is **340,000 VND**: 200,000 table fee + two 70,000 menu items, computed by the server.
- Open the dashboard in another tab. As staff, click **Đối soát** on the new booking and enter a fake reference such as `DEMO-RECEIPT-2`. Confirm the simulated transaction, then assign a table.
- Refresh the customer page to see the updated status/inbox. Cancel to make the paid demo amount refund-required.
- Staff: enter the cancelled booking ID, same amount, a new fake reference and **Đã hoàn tiền**. Customer refresh shows refunded.
- The seeded booking for today supports **Nhận khách → Hoàn tất**. Tomorrow's booking cannot be checked in today; this is deliberate validation.
- Admin can inspect the audit log and edit sample catalog/roles. Image uploads are disabled in demo; existing local upload files are not served.

All accounts end in `@example.invalid`. All financial entries are synthetic; there is no valid bank destination or QR. No push is delivered. Sessions stay in browser memory and disappear on reload; changing account roles still takes effect on the next API request.

Week 10 bundles also contain **golden-leaf-demo.apk**, a separate native Android client without Firebase. Older week 7–9 JAR-only bundles do not contain this APK. Ordinary `debug`/`release` Android builds remain Firebase-authenticated. This is not an offline app: the demo APK needs this backend running on the computer.

## Week 10 combined bundle

Download `golden-leaf-portfolio-demo.zip` and `RELEASE-SHA256SUMS` from the trusted [portfolio prerelease](https://github.com/TrinhQuocDat542005/The-Golden-Leaf/releases/tag/portfolio-v0.10.0). Check the archive's SHA-256 against `RELEASE-SHA256SUMS` before extracting into a new folder. GitHub Release assets persist beyond CI's 14-day retention; a downloadable asset is not proof that its security acceptance is still current.

The archive contains the demo JAR/APK, `REVISION`, `manifest.json`, checksums, security policy, exact exceptions and launchers. It contains no Firebase service account, merchant destination, production database or signing keystore. APK is **debug-signed**, not store-ready; different CI runs may use different debug certificates. If Android reports an incompatible update, manually uninstall only `com.example.giaodien.demo` before installing the new demo, not the ordinary app.

PowerShell (after extracting and checking publisher/archive):

```powershell
./Verify-Demo.ps1
./Start-Demo.ps1
```

Linux/macOS:

```bash
bash Start-Demo.sh
```

Launchers verify all packaged file checksums, reject expired security exceptions and run only profile `demo` on loopback. JDK 17 is required; no Node, Gradle, Maven, MySQL or Firebase setup is needed to run the bundle. Do not disable system execution-policy protections globally; if a downloaded script is blocked, inspect it and follow your machine's policy, or perform the checks above and use the explicit `java -jar` command. Checksums detect corruption, **not authenticity**; download only from the trusted project release.

Start an Android Emulator API 25+; install `golden-leaf-demo.apk` by dragging it into the emulator or `adb install golden-leaf-demo.apk`. Keep backend on **8080**: the APK uses `http://10.0.2.2:8080/`. Choose **Khách demo · có đơn mẫu**. Menu contains 12 dishes, favorites/reviews, today's booking, history and fake payment data. Web customer/staff/admin and Android use the same in-memory database. See the [Android walkthrough](https://github.com/TrinhQuocDat542005/The-Golden-Leaf/blob/main/docs/android-demo.md).

For USB physical devices, build the loopback APK and use `adb reverse` as described in that guide; the shipped emulator APK is not a LAN client. If 8080 is busy, stop your own conflicting service or run web-only on 18082 (`./Start-Demo.ps1 -Port 18082`); the default APK will not follow a port change. Do not expose the service publicly.

## Recovery and security limits

- Connection loss/timeout: keep the backend running, restore connectivity and use the visible retry controls. Connection failures/403/503 must not automatically log out a valid demo session.
- Backend restart: synthetic data and tokens reset. A private request with the old token returns 401 and brings Android back to persona selection; sign in again. Token lasts for that backend process, not a configured timed session. Closing the Android process also clears its memory-only token.
- Two unpatched Spring WebMVC CVEs are accepted **only for local portfolio until 2026-11-08 00:00 UTC**. Bundled launchers refuse startup after that date. Obtain a newly reviewed version; do not bypass the guard or treat the prerelease as production-safe. The APK/direct JAR commands do not enforce this deadline themselves.
- Native CI covers API 25/35 customer journeys; transport recovery additionally has unit tests with real OkHttp/loopback. A mock 401 sequence is not evidence of physically restarting a live Android backend. Physical phone, true offline full-app recovery, Firebase/FCM and real payments remain outside acceptance.

Source, architecture and limits: [GitHub repository](https://github.com/TrinhQuocDat542005/The-Golden-Leaf) and [week 7 guide](https://github.com/TrinhQuocDat542005/The-Golden-Leaf/blob/main/docs/week-7-portfolio.md).
