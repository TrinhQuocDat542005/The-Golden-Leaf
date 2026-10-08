# Tuần 7 — Portfolio, demo và bàn giao

Đây là snapshot nghiệm thu tuần 7. Kết quả và thay đổi mới hơn nằm trong [báo cáo tuần 8](week-8-quality.md); giữ nguyên số liệu lịch sử bên dưới.

## Mục tiêu đã điều chỉnh

The Golden Leaf là **dự án portfolio GitHub**, không phải job triển khai cho nhà hàng thật. Tuần 7 ưu tiên bản demo có thể chạy lại, bằng chứng kỹ thuật, ảnh giao diện thật và tài liệu trình diễn. Không mua hosting/domain, tạo tài khoản ngân hàng, public backend hoặc bật giao dịch thật. Tài liệu production tuần 6 là một hạng mục kỹ thuật tham khảo, không phải yêu cầu hoàn thành portfolio.

## Chạy demo từ source

Yêu cầu: Git, JDK 17; lần đầu cần Internet để Maven tải dependency. Sau build không cần MySQL, Docker, Firebase, API key hoặc tài khoản người dùng.

PowerShell từ thư mục repository:

```powershell
cd The-Golden-Leaf-server
./mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=demo'
```

Linux/macOS:

```bash
cd The-Golden-Leaf-server
bash mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Mở [demo khách](http://127.0.0.1:8080/demo.html), [dashboard](http://127.0.0.1:8080/staff.html), [Swagger](http://127.0.0.1:8080/swagger-ui.html). Database H2 memory chạy Flyway V1–V4 và Hibernate validate, không tạo một schema demo lệch contract. Dừng process/Ctrl+C rồi chạy lại để reset; không có endpoint reset/drop DB.

Nếu cần đổi port:

```powershell
./mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=demo' '-Dspring-boot.run.arguments=--server.port=18082'
```

Nếu startup guard từ chối: bỏ active profiles khác, public binding và các CLI/SPRING_APPLICATION_JSON override DB/auth/Firebase. **Không sửa guard hoặc mở rộng quyền secret để chạy demo.** Chế độ demo chỉ chấp nhận fixed H2 memory + loopback + auth bật + Firebase/push tắt. Demo không nhận `DB_URL` thật.

## Bundle không cần Maven

Sau một CI main thành công, mở **Actions → CI → run của commit mong muốn → Artifacts → portfolio-demo-bundle**. Cần quyền đọc repository và GitHub có thể yêu cầu đăng nhập để tải artifact. Giải nén, đọc REVISION/README, kiểm checksum rồi chạy JAR bằng JDK 17. Artifact giữ **14 ngày**, không phải GitHub Release lâu dài hoặc ứng dụng signed. Không commit JAR/APK vào source.

Hướng dẫn độc lập trong [bundle guide](demo-bundle-guide.md). CI cũng có `android-debug-and-reports` (APK debug riêng) và `demo-browser-report-and-video` (HTML report, video Chromium, trace nếu lỗi). Không gọi video browser này là video Android, không gọi APK debug là release production.

## Dữ liệu và ranh giới an toàn

| Thành phần | Demo |
|---|---|
| Accounts | 4 persona tổng hợp: khách, khách khác, STAFF, ADMIN; email `.invalid` |
| Sessions | Opaque token sinh ngẫu nhiên theo process; endpoint chỉ tồn tại trong profile demo |
| Roles/ownership | Vẫn dùng AuthService, DB roles, security filter và owner checks thật |
| Catalog/inventory | 6 món, 4 bàn 8 ghế, 28 slots cho 7 ngày theo Asia/Ho_Chi_Minh |
| Booking hôm nay | Đơn đã phân bàn/đối soát **giả lập**, phục vụ thử check-in/complete |
| Payment | Destination `NOT-A-REAL-ACCOUNT`; chỉ ghi trạng thái, không chuyển tiền/QR/gateway |
| Notifications | Inbox chạy thật; Firebase initialization và push delivery không chạy |
| Uploads | Không ghi ảnh và không serve thư mục uploads local trong demo |
| Reset | Restart process; dữ liệu/token mất, không thao tác schema dev/prod |

Server không tin role, UID hoặc email tùy ý từ session request. Persona được giới hạn; roles vẫn đọc DB mỗi request. Không cố định password hoặc token admin trong repository. Demo session endpoint không có trên dev/test/prod; mixed profile hoặc real DB/public bind bị chặn trước bean DB/Firebase initialize.

Đây không phải identity provider an toàn để public hosting: bất kỳ ai trên máy local cũng có thể chọn persona demo. Không reverse-proxy/tunnel demo ra Internet, không mở firewall. Browser chỉ giữ token trong memory, không localStorage/sessionStorage. Nút đối soát vẫn cần xác nhận, nhưng trong demo là **giao dịch giả lập**.

Android vẫn dùng Firebase thật: week 7 không thay LoginViewModel/FCM bằng tài khoản demo và không tuyên bố mobile E2E offline. Favorites/reviews/weather live, payment gateway, HA, load capacity, signed store release và operator UAT không được tự nhận là hoàn tất vì có schema/template.

## Kịch bản trình diễn 5–7 phút

1. **30 giây — Tổng quan:** mở README, chỉ ra Android + Spring API + dashboard, demo local và trạng thái CI. Nói rõ portfolio, không khách/tiền thật.
2. **90 giây — Khách:** chọn ngày mai/4 khách/2 salad → tạo đơn → xác nhận → tạo payment. Tổng 340.000 VND do server tính. Đặt bàn giữ chỗ trước khi confirm và idempotency chống retry trùng.
3. **90 giây — Nhân viên:** đăng nhập demo STAFF → đối soát với `DEMO-RECEIPT-<ID>` và đúng số tiền → phân bàn. Customer refresh thấy ASSIGNED/PAID và inbox.
4. **60 giây — Hủy/hoàn:** khách hủy; staff ghi refund với mã khác, đúng tổng tiền. Không khôi phục booking đã hủy chỉ vì có tiền, không hoàn bằng cách xóa invoice.
5. **30 giây — Nhận khách:** seeded booking hôm nay → check-in → complete; booking mới ngày mai không check-in hôm nay.
6. **60 giây — Kỹ thuật:** admin audit; giới thiệu tests concurrency/MySQL, demo safety, browser E2E, CI/build/backup. Không chạy restore/chaos trên dữ liệu thật.

Nếu thao tác nhầm hoặc seed đã hoàn tất, restart demo rồi quay lại. Không kỳ vọng ID luôn cố định khi đã thử nhiều đơn. Mỗi tab customer demo giữ một lượt walkthrough; reload mất token/đơn đang chọn, nhưng data server vẫn có đến khi backend dừng.

## Case study ngắn cho người review

**Bài toán:** nhiều khách đặt cùng khung giờ, retry request do mạng chậm, chỉnh giá món sau khi chốt và chuyển khoản đến sau khi hủy có thể làm sai sức chứa/tiền/quyền.

**Thiết kế:** giữ lock slot trước booking; transaction + unique idempotency key; server gắn owner UID; tiền BigDecimal/DECIMAL; invoice/item/bank destination snapshots; payment/booking state machine; staff xác nhận cùng audit; outbox lease/retry cho push.

**Trade-off:** nguyên tử và khóa DB ưu tiên tính đúng hơn throughput; database là nguồn roles nên request có thể revoke ngay nhưng phải query; limiter per-instance thay distributed; đối soát thủ công thay gateway. H2 giúp onboarding, MySQL disposable tests kiểm chứng engine thực; H2 không thay kiểm thử concurrency/production trên MySQL.

**Bằng chứng:** test backend mới kiểm profile isolation, token lifecycle, role matrix, ownership, idempotent booking, refund/check-in/complete và uploads disabled. Chromium E2E không mock API: customer → staff → refund → admin audit; seed hôm nay; viewport 390 px không overflow. Ảnh README do test chụp từ app thật, không phải ảnh AI/mockup.

## Chạy kiểm thử demo

```powershell
cd The-Golden-Leaf-server
./mvnw.cmd verify
cd ../tools/demo-browser
npm ci
npx playwright install chromium
npm test
```

Playwright tự khởi động JAR mới với demo, port 18082, không reuse server đang có; readiness gồm DB; tắt process sau test. Cần package/verify trước để JAR khớp source. Bộ test MySQL chỉ chạy khi cung cấp URL fixture riêng theo CI, nếu không các suite đó được skip rõ ràng.

Chụp lại ảnh khi thay giao diện: `$env:CAPTURE_DEMO='true'; npm test` (Linux: `CAPTURE_DEMO=true npm test`). PNG dưới `docs/assets`, videos/reports trong thư mục ignored của test. Review ảnh trước commit; không chụp dữ liệu/tài khoản thật. Chromium desktop + mobile-width là scope đã test, không chứng nhận Safari/Firefox/Android OEM.

## Checklist portfolio

Kiểm chứng local ngày 08/10/2026: **180 backend tests** (bao gồm MySQL disposable), **0 failure/error/skip**; **3 Chromium E2E đạt** trên API thật. Workflow/actionlint và JavaScript syntax đạt. Android không đổi source trong tuần 7; job Android hiện có tiếp tục là gate CI trước khi tạo demo bundle. Kết quả CI remote cần kiểm tra trên đúng revision đã push, không suy ra từ kết quả local.

- [x] Demo không cần secret ngoài, guard ngăn nhầm dev/prod/DB thật.
- [x] Walkthrough khách/nhân viên/admin dùng API thật và dataset synthetic.
- [x] Screenshot desktop/mobile thật, không mockup.
- [x] README phân biệt demo, Android Firebase và production templates.
- [x] Browser E2E + report/video artifact và demo JAR bundle trên CI.
- [x] Kịch bản trình diễn, case study và hướng dẫn đóng góp/báo lỗi an toàn.
- [ ] Tùy chọn của tác giả: video có thuyết minh, signed Android, GitHub Release dài hạn, license sử dụng.

Không chọn license thay chủ repository hoặc phát hành GitHub Release/public hosting tự động. Tác giả có thể giữ source public để review; quyền tái sử dụng cần license do tác giả chọn.

Browser tooling: [Playwright web server](https://playwright.dev/docs/test-webserver), [traces](https://playwright.dev/docs/trace-viewer).
