# Tuần 9 — Android demo native và bàn giao portfolio

## Phạm vi và trạng thái

Ngày 09/10/2026. Tuần 9 hoàn thiện phần Android demo bổ sung sau tuần 8: một APK riêng không Firebase, dùng API/backend demo thật và dữ liệu tổng hợp. Không triển khai nhà hàng thật, không nhận tiền, không mở hosting và không nghiệm thu Google login/FCM thật.

**Source đã triển khai; nghiệm thu native/ảnh/video và CI trên revision được push còn chờ.** Các ô runtime bên dưới chỉ đánh dấu sau khi đọc raw results, kiểm ảnh gốc và video thực tế. Không dùng CI tuần 8 để chứng minh bản demo mới.

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
| Backend local `mvnw verify` | 191 ca khai báo; **122 chạy, 0 failure/error, 69 MySQL skip** do Docker local không chạy |
| Android unit/build | **38 passed, 0 skipped** local; demo APK/instrumentation APK và debug build thành công. Có 6 ca mới cho history/inbox/invoice retry và account isolation |
| Android quality | Lint không error; budget/static log gate đạt, không tăng budget. Coil compose/SVG đồng bộ 2.7.0 qua version catalog. Không tuyên bố lint sạch |
| Node gates | **7 passed**, có kiểm đúng ba native events; fail/crash/skip/partial vẫn bị từ chối |
| Browser E2E | **3 passed** local, gồm customer/staff/admin/refund, check-in và màn hình mobile; fixture 12 món |
| Native API 25/35 | CI dùng MyApp/MainActivity thật, ba customer UI tests riêng sau bốn regression tests tuần 8; chưa tính đạt trước khi có run thành công |
| Screenshot/video | Sáu PNG gốc/mỗi API: home, payment, history, invoice, menu, review; MP4 native tối đa 180 giây, không audio; chờ kiểm tra trực quan |

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
- [ ] Build/unit/lint trên source cuối, MySQL regression CI không skip.
- [ ] Native API 25 và 35 thành công trên cùng source revision.
- [ ] Kiểm tra đủ ảnh gốc và video native, lưu ảnh bàn giao.
- [ ] README/báo cáo dẫn đúng revision/run và hướng dẫn tải demo APK.
- [ ] Commit/push chuyên nghiệp, working tree sạch.

Hướng dẫn cho người thử app: [Android demo](android-demo.md). CVE exception/lint warnings còn lại của tuần 8 vẫn phải theo dõi; tuần 9 không được coi là vá CVE hoặc lint sạch.
