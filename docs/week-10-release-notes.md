# Local portfolio demo — v0.10.0

Native Android demo + Spring Boot web/API sandbox, with synthetic data only. **No money transfers, Firebase credentials, MySQL server or hosting required.** This is a prerelease, not a production/store release.

Download **golden-leaf-portfolio-demo.zip** and **RELEASE-SHA256SUMS**. Verify the ZIP's SHA-256, extract to a new directory, then read README.md. Use JDK 17 and Start-Demo.ps1 (Windows) or `bash Start-Demo.sh` (Linux/macOS); install the included demo APK on an Android Emulator API 25+. Backend must stay on local port 8080 for the shipped emulator APK.

Also available separately: golden-leaf-demo.apk and golden-leaf-demo.jar. The combined archive includes launchers, source REVISION, manifest, checksum list, security policy and exact exception metadata. Separate binaries are intended for experienced reviewers; they do not include launcher safety checks.

Week 10 improves money formatting, active booking labels, image loading/error/retry and narrow dish-detail layout. Acceptance pipeline includes H2/MySQL, Android unit/lint, customer native API 25/35, browser E2E, security inventory and packaging gates. Assets are taken from that same verified workflow run; publishing is gated by the explicit tag matching its source commit. Native staff operations are API calls, not a staff screen in Android.

**Known security limitation:** CVE-2026-47884 and CVE-2026-47890 in spring-webmvc 6.2.19 are not patched. Exact exceptions are accepted only for local portfolio until **2026-11-08 00:00 UTC**. Launchers block after expiry. Do not deploy publicly or use with real restaurant/customer/payment data. No clean-vulnerability or go-live claim is made.

APK uses an ephemeral CI debug certificate. Installing over another debug build may require manually uninstalling only the demo package first. No signing keystore is included. Physical devices, real Google/Firebase/FCM, banking and store release are not certified.

[Run/build guide](https://github.com/TrinhQuocDat542005/The-Golden-Leaf/blob/main/docs/android-demo.md) · [Week 10 acceptance report](https://github.com/TrinhQuocDat542005/The-Golden-Leaf/blob/main/docs/week-10-portfolio-release.md)
