# Android demo — thử app không cần Firebase

Bản `demo` là APK debug riêng (`com.example.giaodien.demo`), cài cạnh app thường. Bản `debug`/`release` vẫn dùng Firebase. Đây là **app native gọi backend thật**, không phải demo offline hay dịch vụ nhận tiền.

## Chạy trong Android Studio

1. Bật backend từ terminal ở thư mục repository:

   ```powershell
   cd The-Golden-Leaf-server
   ./mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=demo'
   ```

2. Mở repository bằng Android Studio, cấu hình Android SDK trong `local.properties` nếu cần. Trong **Build Variants**, chọn module `app` → **demo**. Sync nếu chưa thấy variant.
3. Chạy configuration `app` trên **Android Emulator** (API 25 trở lên). Mở app **Golden Leaf DEMO**, chọn **Khách demo · có đơn mẫu**. Không cần tạo Firebase account, mật khẩu, merchant hoặc weather key.

APK cũng có thể build bằng `./gradlew.bat assembleDemo`, xuất tại `app/build/outputs/apk/demo/app-demo.apk`. Không chọn bản `app-debug.apk` nếu muốn đăng nhập mẫu.

Backend chỉ bind loopback; emulator dùng `http://10.0.2.2:8080/` để truy cập máy tính. Không mở firewall/tunnel/public hosting. Chạy web demo và Android dùng chung dữ liệu của cùng backend. Port 18082 dành cho fixture CI, không cần đổi khi thử bằng Android Studio.

Điện thoại Android nối USB có thể dùng `adb reverse tcp:8080 tcp:8080`, rồi build `./gradlew.bat assembleDemo '-PDEMO_API_BASE_URL=http://127.0.0.1:8080/'`. Chỉ chấp nhận host emulator/loopback và port 8080/18082; không hỗ trợ URL LAN/Internet. Lệnh reverse chỉ tạo đường tới backend local, không public dịch vụ. Thiết bị thật chưa được nghiệm thu trong CI.

## Dữ liệu có sẵn

- 12 món, chia đều khai vị/món chính/tráng miệng; salad vẫn 70.000 VND để tái hiện tổng mẫu 340.000 VND.
- 7 minh họa SVG local, dùng chung theo nhóm món; không phải ảnh chụp món ăn thật, không tải ảnh từ dịch vụ ngoài.
- 4 bàn, mỗi bàn 8 ghế; 28 khung giờ cho 7 ngày theo múi giờ TP.HCM.
- Khách demo có đơn hôm nay đã phân bàn/đối soát giả lập, hai đơn lịch sử hoàn tất/hủy, hai món yêu thích và thông báo chào mừng.
- Có hai đánh giá tổng hợp của khách khác; tác giả API vẫn ẩn danh, không lộ UID/email.
- Khách khác có dữ liệu riêng; thích món/đánh giá/đơn mới được ghi qua API thật.
- STAFF/ADMIN dùng dashboard web, không có màn hình nhân viên native.

## Kịch bản thử

1. Xem thực đơn, chi tiết món, thêm/xóa yêu thích và gửi đánh giá.
2. Bấm dấu **+** → chọn **ngày mai**, khung giờ, vị trí và 4 khách → chọn 2 salad → giỏ hàng → xác nhận đơn → tạo thanh toán. Tổng mẫu 340.000 VND do server tính.
3. **Không chuyển tiền**: thông tin nhận tiền là giả. Trên máy tính mở `http://127.0.0.1:8080/staff.html`, chọn Nhân viên, đối soát đúng số tiền bằng mã biên nhận giả riêng rồi phân bàn.
4. Trong app kiểm tra trạng thái thanh toán, tài khoản/lịch sử và inbox. Đơn hôm nay cho phép nhân viên nhận khách/hoàn tất; đơn ngày mai không nhận khách hôm nay.
5. Đăng xuất ở Tài khoản → chọn Khách khác để kiểm tra đơn/yêu thích riêng. Quay lại Khách demo để xem dữ liệu cũ.

Token chỉ giữ trong RAM: đóng process app thì đăng nhập lại. Dừng rồi khởi động backend để reset toàn bộ dữ liệu H2/token; app gặp 401 sẽ quay về chọn tài khoản. Không có endpoint xóa database thật. FCM/Firebase tự khởi tạo, đăng ký thiết bị và quyền push đều tắt trong demo; inbox vẫn lấy từ backend.

## Kiểm chứng

Trạng thái mới nhất nằm trong [báo cáo tuần 9](week-9-native-demo.md). Bài instrumentation dùng MainActivity/MyApp thật, không dùng application fixture tuần 8. Ba test kiểm customer UI đặt bàn/giỏ hàng/thanh toán/lịch sử, yêu thích/đánh giá và đăng xuất/đổi tài khoản; nhân viên đối soát/phân bàn qua API demo thật, **không phải staff UI native**. Firebase initialization phải trống trong suốt bài test.

```powershell
./gradlew.bat -PdemoInstrumentation=true connectedDemoAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.example.giaodien.DemoAppTest'
```

Cần emulator online và backend demo tại 8080. CI dùng origin 18082, chạy native `am instrument`, kiểm đủ ba successful events, xuất screenshot PNG gốc và video `screenrecord` tối đa 180 giây. APK demo dùng thử tải từ artifact **android-debug-and-reports**; artifact emulator là bằng chứng, không phải APK cho backend 8080. Không suy ra runtime từ việc build thành công.
