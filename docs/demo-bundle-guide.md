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

This is a **web/API demo**. The Android application is a separate Firebase-authenticated client and is not made offline by this bundle. Device/Firebase/FCM end-to-end testing is outside the local sandbox.

Source, architecture and limits: [GitHub repository](https://github.com/TrinhQuocDat542005/The-Golden-Leaf) and [week 7 guide](https://github.com/TrinhQuocDat542005/The-Golden-Leaf/blob/main/docs/week-7-portfolio.md).
