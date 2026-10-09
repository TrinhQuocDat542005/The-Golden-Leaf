# Tuần 10 — Polish Android và bản bàn giao portfolio

Ngày 09/10/2026. Phạm vi: demo local cho GitHub, không go-live, không ngân hàng/Firebase/FCM thật. **Code, CI và combined bundle đã đạt; prerelease public còn chờ chủ repository chạy lại job xuất bản. Chưa tuyên bố hoàn tất toàn bộ bàn giao tuần 10.**

Source nghiệm thu cuối: `893b3e4f6cf44fa63eb0fc04a9dc4a4df336146c`, [CI run 37958479705](https://github.com/TrinhQuocDat542005/The-Golden-Leaf/actions/runs/37958479705), toàn bộ jobs thành công. Job `portfolio-release` lúc đó là no-op có chủ đích vì chưa có tag. Sau nghiệm thu bundle đã push lightweight tag `portfolio-v0.10.0` đúng SHA trên; connector chạy lại job trả 403 Resource not accessible by integration, trình duyệt chưa đăng nhập. Không đổi permission, đọc token hoặc bỏ gate để vượt giới hạn. Chủ repository cần mở job này và chọn Re-run job; sau đó còn phải kiểm public release và checksum tải về.

Run trước `5df5655`, [37955018052](https://github.com/TrinhQuocDat542005/The-Golden-Leaf/actions/runs/37955018052), đạt unit/build/lint, nhưng budget gate chặn `NewerVersionAvailable: 13 > 12`: MockWebServer 4.12.0 được khai báo lặp ở unit và instrumentation. Đưa OkHttp core/logging/MockWebServer về một version catalog chung **vẫn 4.12.0**, một khai báo MockWebServer dùng cho cả hai suite. Cảnh báo version mới vẫn giữ trong lint; không suppression, không nâng budget, không đổi dependency major. Source cuối CI đã đạt `NewerVersionAvailable: 12`, `UseTomlInstead: 17` và mọi per-ID budget giữ nguyên.

## Kết quả đã xác nhận

| Kiểm chứng | Kết quả |
|---|---|
| Backend CI | 191 tests passed, 0 failures/errors/skipped; H2 + MySQL disposable |
| Backend restart CI/local | JAR process restart thật: token cũ 401, fresh session 200; không phải Android outage E2E |
| Android local | 48 unit tests passed, 0 failures/errors/skipped; demo APK và instrumentation build được |
| Android CI | 48 unit tests passed, 0 failures/ignored; build/debug/demo/instrumentation thành công; lint 94 warnings / 8 hints, không Error/Fatal; budget/static privacy đạt |
| Infrastructure/security CI | 14 gate tests + actionlint/ShellCheck/container/backup-restore đạt; scan giữ 6 dependency findings và 36 image findings, 0 blocking sau hai exception đúng scope có hạn |
| Node gates local | 14 passed: 8 quality/security/instrumentation + 6 packaging |
| PowerShell guard local | Bundle prototype đúng được nhận; file bị sửa và exception hết hạn bị từ chối |
| PowerShell startup local | Bundle CI cuối chạy trong PowerShell 7/JDK 17, đúng profile demo/port 18082; config demo=true, public menu 12 món, web 200; dừng process riêng sau kiểm |
| Native API 25/35 | Mỗi API 4 regressions + 3 customer journeys + 1 image retry, 0 failures/errors/skips; raw results/JUnit đã kiểm |
| Browser/Linux launcher CI | 3 browser E2E; launcher Bash của bundle thật đạt config demo, menu 12 và web 200 |
| Combined bundle local | Verify toàn bộ manifest/checksum/source đạt; APK SHA-256 `96c66050eef6a6801e91baabce9b2c24a5ae89ca1b9490c96cb10db78899b25f`, trùng APK cài/test API 35 |
| Prerelease public | Chưa xuất bản: cần chạy lại job publish sau tag; chưa có bằng chứng download public |

Windows PowerShell 5 mặc định trên máy chặn chạy script theo execution policy; không thay policy hệ thống hoặc bypass guard. Kiểm PowerShell 7 trong phiên đang cho phép script, không suy ra đã nghiệm thu mọi policy/phiên bản Windows. Prototype local dùng để kiểm launcher, không được xuất bản làm binary đã nghiệm thu. Linux launcher runtime kiểm trong CI; macOS chỉ có syntax/compatibility review nếu chưa có runner thật.

## Ảnh và video final

Đã kiểm trực tiếp 12 PNG gốc của cả API 25/35; copy nguyên bản vào `docs/assets/week10-android-api{25,35}-{home,menu,payment,history,invoice,review}.png` và so SHA-256 nguồn/đích, không crop/chỉnh sửa. Ảnh tuần 8/9 giữ nguyên. Giá menu có dấu phân cách Việt Nam; history dùng nhãn xử lý/sắp tới; ảnh món và bình luận thật đã tải. Nội dung review fixture vẫn nhắc tuần 9 vì được tái sử dụng, không phải ảnh tuần 9.

Video final qua full-decode gate CI và frame sample đã xem: API 35 H264 MP4 720×1280, 32.547367 giây; API 25 VP9 WebM 1080×1920, 31.683 giây. Recording chỉ phần booking, không chứng minh toàn bộ suite hay phone UAT. Artifact giữ 14 ngày; ảnh gốc lưu trong Git. Hóa đơn còn dùng formatter VNĐ cũ, một số header tiếng Anh/ảnh vị trí còn đơn giản; không gọi đây là redesign toàn app hoặc đã thống nhất mọi typography/locale.

## Thay đổi

- Định dạng tiền Việt Nam độc lập locale thiết bị trên home/menu/detail/choose/favorites, không còn `70000.0 VND`. Tổng tiền authoritative vẫn do backend tính, không thay đổi tiền hay nghiệp vụ.
- Nhóm lịch sử đổi thành “Đơn đang xử lý / sắp tới”, không gọi đơn PAID/ASSIGNED là chờ xác nhận. Tên và giá chi tiết món xếp dọc để tránh chen ngang trên màn hẹp.
- Ảnh món giữ geometry, có loading, lỗi và nút tải lại; native screenshot chỉ chụp sau khi ảnh hiện và không còn loading/error. Review chờ bình luận thật xuất hiện, không chỉ chờ API thành công.
- Tách demo auth interceptor để test transport thực với OkHttp/MockWebServer loopback. 401 chỉ vô hiệu đúng phiên hiện tại; callback trên main thread kiểm lại identity. Network error/timeout/403/503 không tự đăng xuất, không replay/refresh demo token.
- Bundle có JAR + emulator demo APK từ cùng run/source, full SHA-256 manifest, revision, metadata, security policy và launcher Windows/Linux/macOS. Launcher kiểm checksum và hạn security exception trước khởi động; không mở binding public.
- Từ tuần 10, native runner disposable dùng backend **8080** và APK default origin thay vì rebuild client 18082. APK xuất sang artifact chỉ sau test/video gates; combined bundle lấy **đúng APK đã cài/test trên API 35**, không build lại sau nghiệm thu. Backend/browser fixture vẫn có thể dùng 18082 riêng; không reuse server người dùng.
- Publish prerelease chỉ khi toàn bộ dependency jobs đạt và lightweight tag `portfolio-v0.10.0` trỏ đúng source run trên main. Chỉ job publish có contents:write; không token dài hạn/keystore/service account. Tạo draft, kiểm đủ bốn assets rồi mới public; không overwrite release cũ.

## Phân biệt bằng chứng

10 unit cases mới dùng transport loopback: bearer header, current 401, 403, 503/retry, connection loss/retry, timeout, stale 401 sau đổi tài khoản, queued logout, old-token/new-session sequence và tiền tệ. Mock 401 **không** thay runtime restart của MainActivity/backend thật. Có thêm smoke script khởi động, dừng và restart hai process JAR demo/H2 riêng trên 18083: private request ban đầu 200, token cũ sau restart 401, fresh session 200. Không dùng/kill backend của người dùng; đây là backend runtime, chưa phải Android end-to-end outage test. Native API 25/35 vẫn gồm 4 regressions và 3 customer journeys, thêm assertions giá/nhãn và ảnh/bình luận đã tải.

Thêm một native component test trên MainActivity với surface ảnh 120dp: HTTP 503 → nút “Tải lại ảnh” → SVG thật từ MockWebServer → loaded, đúng hai request, không Firebase. Test này không phải full customer journey hoặc smoke toàn app trên thiết bị nhỏ. Mỗi API tuần 10 tổng cộng **4 regressions + 3 customer journeys + 1 image retry component**; parser phải có đủ 4 events của demo suite, không nhận output thiếu test mới.

6 packaging unit tests dùng ZIP-header fixtures, không gọi fixture giả này là APK/JAR chạy được. CI build binary thật, kiểm bundle lại trước release. Local và CI phải báo riêng MySQL skip, lint warnings, security findings, test counts và source revision.

## Checklist nghiệm thu

Run đầu trên `10afcaf`, [37953912589](https://github.com/TrinhQuocDat542005/The-Golden-Leaf/actions/runs/37953912589), đạt backend 191/no skip, infrastructure và security; Android có 1/48 unit failure ở retry fixture. Stack trace chỉ rõ `localhost/[::1]` connection refused sau disconnect, trong khi MockWebServer bind IPv4. Sửa fixture bind **và URL** thành `127.0.0.1`, giữ nguyên simulated socket disconnect, assertions và số tests; không sửa production DNS/network policy hoặc skip failure. Native image fixture cũng pin cùng loopback IPv4. Phải nghiệm thu lại source mới, không dùng run lỗi này làm bằng chứng hoàn thành.

Run `8ca446a`, [37956310101](https://github.com/TrinhQuocDat542005/The-Golden-Leaf/actions/runs/37956310101), đạt backend/unit/build/lint/infrastructure/security; mỗi API native đạt 4 regressions và 3 customer journeys, nhưng component retry fixture gọi `MockWebServer.url()` trong composition, gây reverse DNS/NetworkOnMainThreadException. Dựng URL IPv4 literal từ port trước khi vào UI thread; không tắt StrictMode hoặc bỏ test. Assertion backend restart cũng chỉ in boolean/message nếu fail, không dump ephemeral token values. Cần run mới có đủ 4 successful demo events trước release.

- [x] Android unit/build/lint cuối và static privacy/budget đạt.
- [x] Backend H2/MySQL CI và browser E2E đạt trên source tuần 10.
- [x] Native API 25/35, raw results/JUnit, ảnh và video tuần 10 được kiểm.
- [x] Packaging tests, bundle checksums, launcher PowerShell 7/Linux và JAR smoke đạt.
- [ ] GitHub prerelease public đúng revision, đủ assets và kiểm download/checksum.
- [x] README/guide/report dẫn đúng run, ảnh final và giới hạn; ghi rõ release pending, kiểm 45 local links/assets và diff whitespace; commit/push tài liệu riêng không thay source binary đã test.

## Giới hạn còn lại

Hai CVE `CVE-2026-47884/47890`, đúng spring-webmvc 6.2.19, vẫn chưa vá; acceptance kết thúc **08/11/2026 00:00 UTC**. Không gia hạn hoặc đổi severity để release. Public hosting, real transfers, Google/Firebase/FCM, signed/store Android và phone UAT ngoài scope. Bundle launchers enforce deadline; dùng APK/direct Java không tự enforce, người review vẫn phải tuân thủ giới hạn. Linux runtime đã kiểm trong CI; macOS mới review syntax/compatibility, chưa runtime trên máy Mac.

Hướng dẫn [Android demo](android-demo.md), [combined bundle](demo-bundle-guide.md), [release notes](week-10-release-notes.md). Workflow dùng [GitHub reusable workflows](https://docs.github.com/en/actions/how-tos/reuse-automations/reuse-workflows) và [gh release create](https://cli.github.com/manual/gh_release_create); nội dung release vẫn là prerelease local-only.
