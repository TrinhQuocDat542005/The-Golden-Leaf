# Tuần 10 — Polish Android và bản bàn giao portfolio

Ngày 09/10/2026. Phạm vi: demo local cho GitHub, không go-live, không ngân hàng/Firebase/FCM thật. **Đang triển khai/nghiệm thu; chưa tuyên bố release đã xuất bản hoặc CI tuần 10 đạt.**

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

- [ ] Android unit/build/lint cuối và static privacy/budget đạt.
- [ ] Backend H2/MySQL CI và browser E2E đạt trên source tuần 10.
- [ ] Native API 25/35, raw results/JUnit, ảnh và video tuần 10 được kiểm.
- [ ] Packaging tests, bundle checksums, launcher PowerShell và JAR smoke đạt.
- [ ] GitHub prerelease public đúng revision, đủ assets và kiểm download/checksum.
- [ ] README/guide/report dẫn đúng run, release và giới hạn; commit/push sạch.

## Giới hạn còn lại

Hai CVE `CVE-2026-47884/47890`, đúng spring-webmvc 6.2.19, vẫn chưa vá; acceptance kết thúc **08/11/2026 00:00 UTC**. Không gia hạn hoặc đổi severity để release. Public hosting, real transfers, Google/Firebase/FCM, signed/store Android và phone UAT ngoài scope. Bundle launchers enforce deadline; dùng APK/direct Java không tự enforce, người review vẫn phải tuân thủ giới hạn. Linux/macOS launchers cần kiểm trên môi trường tương ứng trước khi gọi là runtime đã nghiệm thu.

Hướng dẫn [Android demo](android-demo.md), [combined bundle](demo-bundle-guide.md), [release notes](week-10-release-notes.md). Workflow dùng [GitHub reusable workflows](https://docs.github.com/en/actions/how-tos/reuse-automations/reuse-workflows) và [gh release create](https://cli.github.com/manual/gh_release_create); nội dung release vẫn là prerelease local-only.
