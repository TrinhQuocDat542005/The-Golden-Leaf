# Phát triển và cấu hình

[← The Golden Leaf](../README.md)

Hướng dẫn backend, Android, biến môi trường, API và kiểm thử. Để chỉ xem demo, bắt đầu từ README hoặc [demo bundle guide](demo-bundle-guide.md).

### 3. Development backend bằng Docker (tùy chọn)

```powershell
# Chạy từ thư mục backend; không cần bước này nếu chỉ xem demo H2.
Copy-Item .env.example .env
docker compose up --build
```

Khi container đã healthy, các dịch vụ mặc định sẽ có tại:

| Dịch vụ | Địa chỉ |
| --- | --- |
| REST API | `http://localhost:8080` |
| Health check | `http://localhost:8080/actuator/health` |
| Dashboard nhân viên | `http://localhost:8080/staff.html` (cần cấu hình Firebase và quyền) |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| MySQL từ máy host | `localhost:3308` |

Khởi động thêm phpMyAdmin khi cần quan sát database:

```powershell
docker compose --profile tools up --build
```

phpMyAdmin sẽ chạy tại `http://localhost:8082`.

### 4. Cấu hình và build Android (tùy chọn)

Quay lại thư mục gốc, tạo cấu hình SDK local:

```powershell
Copy-Item local.properties.example local.properties
```

Sửa `sdk.dir` trong `local.properties` cho đúng Android SDK trên máy. Bản debug mặc định gọi `http://10.0.2.2:8080/`, phù hợp với Android Emulator.

```powershell
# Chỉ cần đổi URL khi chạy trên thiết bị thật hoặc môi trường khác.
$env:API_BASE_URL='http://192.168.1.10:8080/'
$env:WEATHER_API_KEY='your-local-key'
./gradlew.bat assembleDebug
```

Mở project bằng Android Studio và chạy configuration `app` để sử dụng emulator hoặc thiết bị thật.

**Thử Android không cần Firebase:** bật backend profile `demo`, chọn Build Variant **`demo`** rồi chạy trên emulator. APK riêng có hai tài khoản khách và dữ liệu tổng hợp, không nhận tiền. Xem [Android demo guide](../docs/android-demo.md); `assembleDemo` xuất `app-demo.apk` (không dùng `app-debug.apk` để đăng nhập mẫu).

## Chạy backend không dùng Docker

Khởi động một MySQL instance trước, sau đó chạy:

```powershell
cd The-Golden-Leaf-server
$env:DB_URL='jdbc:mysql://localhost:3308/datban_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
$env:DB_USERNAME='datban'
$env:DB_PASSWORD='local-datban-password'
./mvnw.cmd spring-boot:run
```

Backend sử dụng profile `dev` mặc định. Có thể đặt `SPRING_PROFILES_ACTIVE` để chuyển profile.

## Cấu hình môi trường

### Backend

| Biến | Mặc định local | Mô tả |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` | Profile Spring đang chạy |
| `DB_URL` | MySQL tại `localhost:3308` | JDBC URL của database |
| `DB_USERNAME` | `datban` | Tài khoản database |
| `DB_PASSWORD` | `local-datban-password` | Mật khẩu database |
| `SERVER_PORT` | `8080` | Port của backend |
| `FIREBASE_ENABLED` | `false` | Khởi tạo Firebase Admin SDK |
| `REQUIRE_AUTH` | `false` | Bắt buộc Bearer token cho API |
| `BOOKING_HOLD_DURATION` | `PT15M` | Thời gian giữ chỗ |
| `RESTAURANT_TIME_ZONE` | `Asia/Ho_Chi_Minh` | Múi giờ nhà hàng |
| `BOOKING_TABLE_FEE` | `200000.00` | Phí bàn trên mỗi đơn |
| `FIREBASE_CREDENTIALS_PATH` | đường dẫn local | Service-account JSON bên trong runtime |
| `FIREBASE_CREDENTIALS_HOST_PATH` | file mẫu | File được mount vào container |
| `FIREBASE_WEB_API_KEY` | Rỗng | Firebase Web API key cho đăng nhập dashboard, cùng project Android/Admin SDK |
| `PAYMENT_BANK_NAME` | Rỗng | Tên ngân hàng thực tế của nhà hàng |
| `PAYMENT_ACCOUNT_NUMBER` | Rỗng | Số tài khoản nhận tiền |
| `PAYMENT_ACCOUNT_NAME` | Rỗng | Chủ tài khoản nhận tiền |
| `MONITORING_TOKEN` | Rỗng | Bearer riêng ≥32 ký tự cho private Prometheus, không cấp quyền business |
| `PUSH_DELIVERY_ENABLED` | `true` | Tắt trên rehearsal DB restore để không gửi push thật |

Các API payment/inbox/lịch sử/nhân viên/admin **luôn yêu cầu xác thực**, kể cả khi `REQUIRE_AUTH=false`. Chế độ này chỉ giữ tương thích development cho một số API cũ; không dùng công khai. Để trống cấu hình ngân hàng sẽ chặn tạo yêu cầu thanh toán mới, không trả tài khoản/QR mẫu.

### Android

| Biến | Mặc định debug | Mô tả |
| --- | --- | --- |
| `API_BASE_URL` | `http://10.0.2.2:8080/` | Base URL cho debug build |
| `PRODUCTION_API_BASE_URL` | Không có | Bắt buộc khi build release |
| `WEATHER_API_KEY` | Chuỗi rỗng | API key cho tính năng thời tiết |

Ví dụ build release:

```powershell
$env:PRODUCTION_API_BASE_URL='https://api.example.com/'
$env:WEATHER_API_KEY='your-production-key'
./gradlew.bat assembleRelease
```

### Spring profiles

| Profile | Database | Firebase/Auth | Mục đích |
| --- | --- | --- | --- |
| `dev` | MySQL | Tắt mặc định | Phát triển local và Docker Compose |
| `test` | H2 in-memory | Tắt | Test tự động, không phụ thuộc dịch vụ ngoài |
| `demo` | H2 memory riêng, không đổi URL | Persona mẫu; Firebase/push tắt, auth/roles vẫn bật | Portfolio local-only, không trộn profile |
| `prod` | Nhận hoàn toàn từ env | Bắt buộc | Môi trường production |

## API hiện có

| Method | Endpoint | Chức năng |
| :---: | --- | --- |
| `POST` | `/api/auth/sync` | Đồng bộ hồ sơ Firebase với backend |
| `GET` | `/api/thucdon` | Lấy danh sách thực đơn |
| `GET` | `/api/thucdon/{id}` | Lấy chi tiết món ăn |
| `GET` | `/api/ban-slot` | Tra cứu bàn theo ngày, giờ và khu vực |
| `GET / POST / DELETE` | `/api/yeu-thich/list`, `/add`, `/remove` | Danh sách/thêm/xóa yêu thích của principal, không nhận quyền từ client UID |
| `GET / POST` | `/api/binhluan/{id}`, `/api/binhluan/add` | Đọc đánh giá ẩn danh / cập nhật điểm 1–5 và nội dung có giới hạn |
| `POST` | `/api/datban/save` | Tạo đơn và giữ sức chứa, cần `Idempotency-Key` |
| `GET` | `/api/datban/{id}` | Đọc đúng đơn đặt bàn theo ID |
| `POST` | `/api/datban/{id}/confirm` | Xác nhận đặt bàn, đóng băng giỏ hàng |
| `POST` | `/api/datban/{id}/cancel` | Hủy đơn và trả sức chứa đúng một lần |
| `GET` | `/api/datban/latest` | Lấy đặt bàn gần nhất của người dùng |
| `PUT` | `/api/giohang/{idDat}` | Thay thế toàn bộ giỏ hàng, có thể rỗng |
| `POST` | `/api/giohang/datmon` | API cũ, nay thay thế toàn bộ giỏ hàng |
| `POST` | `/api/hoadon/create` | Tạo hóa đơn |
| `GET` | `/api/payments/bookings/{id}/quote` | Xem tổng tiền từ server, không tạo hóa đơn/thanh toán |
| `POST` | `/api/payments/bookings/{id}` | Tạo/replay yêu cầu chuyển khoản |
| `GET` | `/api/notifications` | Inbox riêng của tài khoản |
| `GET` | `/api/staff/bookings` | Hàng đợi vận hành STAFF/ADMIN |
| `POST` | `/api/staff/bookings/{id}/verify-payment` | Ghi nhận tiền thực nhận theo sao kê |

Request, response và schema lỗi chuẩn được mô tả trong [REST API contract](../docs/api-contract.md). Khi backend đang chạy, Swagger UI là nguồn tương tác nhanh nhất để thử từng endpoint.

Hai API sửa sức chứa trực tiếp `/api/ban-slot/dat` và `/api/ban-slot/tra` đã được ngừng thao tác dữ liệu; chúng trả `409 BOOKING_REQUIRED`. Giữ/trả chỗ phải đi qua lifecycle của đơn. Xác nhận đặt bàn hoặc tạo hóa đơn chưa phải xác nhận thanh toán.

## Database

Các nguyên tắc dữ liệu hiện tại:

- Flyway quản lý phiên bản schema; Hibernate chỉ chạy ở chế độ `validate`.
- Tiền tệ lưu bằng `DECIMAL` trong MySQL và `BigDecimal` trong Java.
- Thời gian được chuẩn hóa theo UTC ở backend và JDBC.
- Khóa ngoại, unique constraint và index phục vụ các truy vấn nghiệp vụ chính.
- Cột version được chuẩn bị cho optimistic locking ở dữ liệu tranh chấp.
- Schema V1 gồm 17 bảng cho người dùng, thực đơn, bàn, đặt bàn, hóa đơn, thanh toán, đánh giá, yêu thích, thông báo và audit.
- V2 bảo vệ booking; V3 bổ sung mã đối soát/hoàn tiền và outbox delivery; V4 lưu snapshot tài khoản nhận tiền. Không sửa migration đã áp dụng.

> [!WARNING]
> Database được tạo từ bản prototype trước khi có Flyway cần được sao lưu rồi migrate hoặc tạo mới trước khi chạy `V1__initial_production_schema.sql`.

Xem đầy đủ tại [tài liệu thiết kế database](../docs/database-schema.md).

## Kiểm thử

### Backend

```powershell
cd The-Golden-Leaf-server
./mvnw.cmd test
```

Test mặc định dùng H2 in-memory, không cần Firebase. Test MySQL dùng schema riêng trong CI; khi chưa cấu hình MySQL local, các test này được bỏ qua.

### Browser E2E

```powershell
cd The-Golden-Leaf-server
./mvnw.cmd verify
cd ../tools/demo-browser
npm ci
npx playwright install chromium
npm test
```

JDK 17 + Node 24; lần đầu cần tải Chromium. Test tự khởi động JAR với demo tại port 18082, không mock API, không reuse server khác, và kiểm customer/staff/admin journey + mobile-width. HTML report/video có trong artifact **demo-browser-report-and-video** của CI. [Hướng dẫn chụp lại screenshot và trình diễn](../docs/week-7-portfolio.md).

### Android

```powershell
./gradlew.bat testDebugUnitTest
./gradlew.bat assembleDebug
./gradlew.bat lintDebug
node scripts/check-android-quality.mjs
```

Trước khi mở pull request, nên chạy cả test backend lẫn build Android để phát hiện sớm lỗi contract giữa hai phía.

Ở [CI của v0.10.0](https://github.com/TrinhQuocDat542005/The-Golden-Leaf/actions/runs/37958479705): 191 backend tests, 48 Android unit tests, 3 browser E2E và 14 test cho các script kiểm tra. Android Emulator API 25/35 chạy 8 test mỗi API, gồm đặt bàn/thanh toán, yêu thích/đánh giá, đổi tài khoản và tải lại ảnh. Lint còn 94 warnings và 8 hints, không có errors. Chưa thử trên điện thoại thật hoặc với Firebase/FCM và giao dịch thật.
