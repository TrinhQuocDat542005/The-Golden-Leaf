# Tuần 9 — Android demo native và bàn giao portfolio

## Phạm vi và trạng thái

Ngày 09/10/2026. Tuần 9 hoàn thiện phần Android demo bổ sung sau tuần 8: một APK riêng không Firebase, dùng API/backend demo thật và dữ liệu tổng hợp. Không triển khai nhà hàng thật, không nhận tiền, không mở hosting và không nghiệm thu Google login/FCM thật.

**Đã nghiệm thu trên source `4401d02a72f9d990c848557f8906a4d7dcc3a4b3`: cả 7 jobs thành công** tại [CI run 37916305353](https://github.com/TrinhQuocDat542005/The-Golden-Leaf/actions/runs/37916305353). Đã đọc raw results/JUnit, kiểm 12 PNG gốc và frame thực tế của hai video; không dùng CI tuần 8 thay bằng chứng cho demo mới. Commit bàn giao sau revision này chỉ sửa tài liệu/ảnh, không đổi source đã test.

## Lịch sử chẩn đoán (không phải trạng thái hiện tại)

Source revision `2da58538d7b0c5d47ef132393c310c800779473c`, [CI run 37899286958](https://github.com/TrinhQuocDat542005/The-Golden-Leaf/actions/runs/37899286958): backend **191 passed, 0 skipped**; Android build/unit/lint budget và infrastructure đạt. Native API 25/35 đều có **3 failures tại bước mở phiên demo**; browser job bị skip bởi dependencies. Security gate đang chặn **CVE-2026-47890**, không được xem toàn bộ CI là xanh.

Video lỗi API 35 đã giải mã được bằng FFmpeg 7.1: màn chọn persona báo lỗi mở phiên sau request session trả 200. Chưa có bộ sáu ảnh thành công để đưa vào README. Bản sửa tiếp theo chuyển `review-draft` về TextField thật và bổ sung chẩn đoán demo chỉ gồm tên lớp/hàm/line, không exception message/token/body; native test fail-fast nếu màn login báo lỗi. Những sửa này chưa được tính là sửa xong lỗi đăng nhập trước khi đọc runtime mới.

Trivy ghi severity CRITICAL (`ghsa`) cho `spring-webmvc:6.2.19`, fixed version 7.0.9. [Advisory chính thức Spring](https://spring.io/security/cve-2026-47890/) ghi LOW, yêu cầu SSE với view fragments và dữ liệu attacker kiểm soát; bản sửa 6.2.20 là enterprise-only. Rà source hiện tại không thấy SSE/view fragments. Chủ project đã đồng ý ngoại lệ đúng CVE/package/version đến **08/11/2026 00:00 UTC**, chỉ cho portfolio local. Guard quét Java/HTML/JS/config trong backend main source, từ chối SSE/emitter/event-stream/fragments API và EventSource. Không xóa finding hoặc đổi severity; regex chỉ là guard phụ, không thay reachability audit. Cả hai CVE-2026-47884/47890 vẫn chưa được vá, cần reassess trước hết hạn, đổi dependency hoặc public deployment.

Run chẩn đoán `da92f50`, [37901459802](https://github.com/TrinhQuocDat542005/The-Golden-Leaf/actions/runs/37901459802) xác định `LifecycleRegistry.enforceMainThreadIfNeeded` khi Navigation pop login. Bản sửa gọi navigation trên `Dispatchers.Main.immediate` sau sign-in; runtime các run tiếp theo xác nhận đăng nhập thành công.

Run `4f440cd`, [37914153839](https://github.com/TrinhQuocDat542005/The-Golden-Leaf/actions/runs/37914153839): cả API 25/35 **3 native customer tests passed**, nhưng API 25 thiếu `/system/bin/screenrecord` nên gate bằng chứng fail. Kiểm ảnh API 35 thấy resolver ghép sai `/demo-assets` thành `/uploads/demo-assets`. Source cuối giữ đúng static path, bổ sung unit assertion, dùng host WebM khi thiếu guest recorder theo [Android documentation](https://developer.android.com/studio/run/emulator-record-screen), và yêu cầu FFmpeg giải mã toàn bộ video. ShellCheck cũng được sửa bằng điều kiện `if` tường minh; không nới gate để lấy CI xanh.

## Những gì đã thay đổi

- APK `demo` có package `.demo`, tên riêng, banner DEMO trên mọi màn hình; bản `debug`/`release` giữ Firebase. Provider auto-init, device binding, quyền push và messaging service tắt trong demo.
- Hai persona khách, bearer token do backend demo sinh, giữ trong RAM, không password/token cố định hoặc đăng nhập production giả. 401 của đúng session hiện tại đưa app về màn chọn tài khoản; response của session cũ không đăng xuất tài khoản mới.
- Origin demo chỉ emulator/loopback, port 8080 hoặc fixture 18082; không cho URL Internet/LAN, không đổi guard loopback của backend.
- 12 món/ba nhóm, 7 hình minh họa SVG tự viết, 4 bàn × 8 ghế, lịch 7 ngày, đơn hôm nay, hai lịch sử hoàn tất/hủy, hai favorites và hai reviews tổng hợp. Seed chỉ ở profile demo, không có migration thay database thật.
- Nút yêu thích ở home lưu qua API thật; bỏ giá gạch ngang/khuyến mãi và điểm 4.5 tự tạo. Hiển thị retry khi menu/favorite lỗi.
- Màn số khách có thể cuộn, giới hạn theo đơn vị sức chứa 8 ghế/bàn, payment và login cuộn được; safe drawing insets và adjustResize hỗ trợ màn nhỏ/bàn phím.
- Ảnh lớn dùng Coil đo kích thước thay vì eager bitmap decoding; thêm SVG decoder dùng chung. Lịch sử chặn kết quả cũ sau đổi tài khoản; inbox có loading, lỗi hiển thị bền và retry, không Toast mỗi lần recomposition.

## Test và bằng chứng

| Kiểm chứng | Trạng thái |
|---|---|
| Backend CI H2 + MySQL | **191 passed, 0 failures/errors/skipped** trên source cuối |
| Backend local `mvnw verify` | 191 ca khai báo; **122 chạy, 0 failure/error, 69 MySQL skip** do Docker local không chạy |
| Android unit/build | **38 passed, 0 failures/skipped** local và CI; demo APK/instrumentation APK và debug build thành công. Có 6 ca mới cho history/inbox/invoice retry và account isolation |
| Android quality | CI **95 warnings / 8 hints**, local 93 warnings / 8 hints, không Error/Fatal; budget/static log gate đạt, không tăng budget. Không tuyên bố lint sạch |
| Node gates | **8 passed**, gồm guard SSE/XSLT và kiểm đúng ba native events; fail/crash/skip/partial vẫn bị từ chối |
| Browser E2E | **3 passed** local và CI, gồm customer/staff/admin/refund, check-in và màn hình mobile; fixture 12 món |
| Native API 25/35 | **4 regression + 3 customer UI tests passed trên mỗi API**, 0 failures/errors/skipped. Ba test mới dùng MyApp/MainActivity thật, không Firebase |
| Screenshot/video | **6 PNG gốc/mỗi API**, đã kiểm trực quan; MP4 API 35 và WebM API 25 giải mã toàn bộ thành công |
| Security | Gate đạt với **2 ngoại lệ đúng CVE/package/version có hạn**; inventory vẫn giữ 6 dependency findings và 36 image findings. Không phải zero vulnerabilities hoặc đã vá CVE |

### Ảnh/video native đã kiểm

| Màn hình | API 25 | API 35 |
|---|---|---|
| Home | [PNG gốc](assets/week9-android-api25-home.png) | [PNG gốc](assets/week9-android-api35-home.png) |
| Menu | [PNG gốc](assets/week9-android-api25-menu.png) | [PNG gốc](assets/week9-android-api35-menu.png) |
| Payment | [PNG gốc](assets/week9-android-api25-payment.png) | [PNG gốc](assets/week9-android-api35-payment.png) |
| History | [PNG gốc](assets/week9-android-api25-history.png) | [PNG gốc](assets/week9-android-api35-history.png) |
| Invoice | [PNG gốc](assets/week9-android-api25-invoice.png) | [PNG gốc](assets/week9-android-api35-invoice.png) |
| Review | [PNG gốc](assets/week9-android-api25-review.png) | [PNG gốc](assets/week9-android-api35-review.png) |

12 PNG 1080×1920 được copy nguyên vẹn từ artifact source cuối, SHA-256 source/destination trùng; không crop, sửa nội dung hoặc dùng mockup. API 35 hiện minh họa món ở home/menu/review. API 25 menu/review hiện SVG đúng; home còn placeholder trong snapshot đầu và review chụp lúc danh sách bình luận đang tải. Đây là giới hạn thời điểm chụp bất đồng bộ, không dùng ảnh đó để tuyên bố mọi nội dung đã tải xong. Các màn dài có cuộn; screenshot không đại diện toàn bộ nội dung ngoài viewport. Giá menu còn định dạng legacy `70000.0 VND`, nhóm lịch sử vẫn có nhãn legacy; chưa phải polish giao diện tuyệt đối.

Video API 35: H.264 MP4, **720×1280, 30,61 giây**. API 25: VP9 WebM host recording, **1080×1920, 28,35 giây**. Cả hai giữ bản gốc, không audio, qua full decode gate và đã xem frame ở giây 10 để xác nhận display native thật. Không mô tả video ngắn này là bao phủ mọi bước của cả ba test; raw results/JUnit là bằng chứng test đầy đủ. Trong [run nghiệm thu](https://github.com/TrinhQuocDat542005/The-Golden-Leaf/actions/runs/37916305353), tải artifact emulator tương ứng để xem video, metadata, raw events và JUnit; artifact giữ 14 ngày. Ảnh trong Git không phụ thuộc thời hạn artifact.

Ba native tests:

1. Chọn khách → ngày mai → khung giờ → sơ đồ sức chứa → vị trí → 4 khách → hai salad → giỏ hàng → xác nhận → payment PENDING, tổng 340.000 VND từ server. Staff API đối soát/phân bàn; UI refresh hiển thị PAID, lịch sử đúng đơn và tổng hóa đơn 340.000 VNĐ.
2. Khách khác → thực đơn → favorite lưu thật → chi tiết món → gửi review → draft được xóa sau thành công; API trả tác giả ẩn danh.
3. Đăng xuất → khách khác có lịch sử riêng rỗng → đăng nhập lại khách chính giữ lịch sử; Firebase không được khởi tạo.

Staff dashboard đã có browser E2E riêng. Native tests không giả định rằng Android có màn staff/admin, không mô tả các calls staff API là thao tác UI. Điện thoại vật lý, phiên token hết hạn sau restart backend và mạng offline trên full app cần smoke thủ công; unit tests có error/retry, không thay nghiệm thu thiết bị thật.

## Chạy lại

```powershell
# Tại repository root, Android SDK/JDK 17 đã cấu hình.
./gradlew.bat testDebugUnitTest assembleDebug assembleDemo lintDebug
node scripts/check-android-quality.mjs
node --test scripts/check-android-quality.test.mjs scripts/check-security-report.test.mjs scripts/check-android-instrumentation.test.mjs

# Sau khi bật backend profile demo trên 8080 và emulator online:
./gradlew.bat -PdemoInstrumentation=true connectedDemoAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.example.giaodien.DemoAppTest'
```

Linux/KVM disposable runner: `ANDROID_TEST_API=25 bash ops/scripts/test-android-emulator.sh`, rồi API 35. Script tự tạo backend H2 trên 18082 và AVD riêng; không reuse backend/device người dùng. Parser xuất JUnit từ raw native events, không dùng exit code ADB làm bằng chứng. Video chỉ ghi app/data giả lập; đầu video là customer happy path, độ dài có giới hạn 180 giây.

## Checklist bàn giao

- [x] Tách demo APK khỏi Firebase debug/release và origin public.
- [x] Seed dữ liệu/illustration local, không dữ liệu nhà hàng thật.
- [x] Source ba native journeys và gate đúng số test, không skip.
- [x] Bổ sung unit regression history/inbox và cập nhật browser fixture.
- [x] Build/unit/lint trên source cuối, MySQL regression CI không skip.
- [x] Native API 25 và 35 thành công trên cùng source revision.
- [x] Kiểm tra đủ ảnh gốc, video giải mã và frame thực tế; lưu ảnh bàn giao.
- [x] README/báo cáo dẫn đúng revision/run và hướng dẫn tải demo APK.
- [x] Source nghiệm thu đã commit/push; tài liệu/ảnh bàn giao commit riêng, không đổi source được test.

Hướng dẫn cho người thử app: [Android demo](android-demo.md). CVE exception/lint warnings còn lại của tuần 8 vẫn phải theo dõi; tuần 9 không được coi là vá CVE hoặc lint sạch.
