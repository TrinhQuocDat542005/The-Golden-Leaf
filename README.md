          🧩 Giới thiệu

 Dự án cung cấp giải pháp đặt bàn cho khách hàng và công cụ quản lý cho nhà hàng.
Khách có thể xem thực đơn, đặt bàn theo ngày giờ, chọn vị trí, đặt món, thanh toán, theo dõi trạng thái đơn và nhận thông báo realtime.
Nhà hàng có thể phân bàn, trả bàn, xem lịch đặt và quản lý hoạt động phục vụ.

          🏗️ Kiến trúc tổng quan

Frontend (Mobile App):

Kotlin Jetpack Compose

Firebase Authentication (OTP SMS/Email)

Firebase Messaging (Realtime Notification)

Retrofit + Coroutines

Navigation Compose

ViewModel + StateFlow

Backend (Server):

Spring Boot 3

Rest API

Spring Data JPA

MySQL/PostgreSQL

WebSocket / Firebase Cloud Messaging

Docker + Docker Compose

DevOps:

Docker Image cho Backend & Database

CI/CD (có thể mở rộng)

          📱 Tính năng chính
👤 1. Xem món ăn / Thực đơn

Xem danh sách món: nổi bật, món mới, món giảm giá.

Tìm kiếm theo tên hoặc nhóm món.

Xem mô tả món: hình ảnh, giá, thành phần, đánh giá.

Đánh giá & bình luận (sau khi đăng nhập).

Thêm món yêu thích.

           🪑 2. Đặt bàn (Use Case chính)

Quy trình đặt bàn gồm 8 bước:

Chọn ngày giờ (7 ngày tiếp theo, mỗi ngày 4 khung giờ).

Xem sơ đồ bàn và tình trạng bàn (trống/đã đặt).

Chọn vị trí (sông, hồ, tầng thượng...).

Nhập số lượng khách & ghi chú (trẻ em, thú cưng, sinh nhật…).

Chọn món ăn → đưa vào Giỏ hàng.

Xem lại hóa đơn tổng.

Thanh toán bằng mã QR theo phương thức chọn.

Nhận thông báo đặt thành công.

           🧭 3. Theo dõi đơn

Xem danh sách đơn Chờ xác nhận (đã thanh toán nhưng chưa phân bàn).

Xem mục Lịch sử đơn (đơn đã được phân bàn).

Xem chi tiết hóa đơn.

Hủy đơn (trước khi phân bàn).

Nhận thông báo realtime khi:

Đặt bàn thành công

Nhà hàng phân bàn

Nhà hàng trả bàn

          🛎️ 4. Phân bàn (Nhà hàng)

Nhà hàng xem danh sách đơn đã thanh toán.

Kiểm tra bàn trống theo ngày/giờ.

Gán bàn phù hợp (mã bàn, vị trí).

Gửi thông báo cho khách.

Bàn chuyển sang trạng thái Đã đặt.

        🧹 5. Trả bàn

Khi khách dùng xong, nhân viên chọn chức năng Trả bàn.

Bàn chuyển về trạng thái Trống.

Khách nhận thông báo trả bàn thành công.



🔌 API chính (Spring Boot)

- `POST /api/auth/sync`
- `GET /api/thucdon`, `GET /api/thucdon/{id}`
- `GET /api/ban-slot`, `POST /api/ban-slot/dat`, `POST /api/ban-slot/tra`
- `POST /api/datban/save`, `GET /api/datban/latest`
- `POST /api/giohang/datmon`
- `POST /api/hoadon/create`

Contract chi tiết và schema lỗi chuẩn nằm trong [tài liệu REST API](docs/api-contract.md).

## Chạy dự án local

### Yêu cầu

- JDK 17 (không dùng JDK 25 để chạy Gradle của Android).
- Android Studio/Android SDK, compile SDK 36.
- Docker Desktop nếu chạy backend bằng container.

### Backend bằng Docker

```powershell
cd The-Golden-Leaf-server
Copy-Item .env.example .env
docker compose up --build
```

Backend chạy tại `http://localhost:8080`. Health check nằm tại
`http://localhost:8080/actuator/health`. Swagger UI nằm tại
`http://localhost:8080/swagger-ui.html`. phpMyAdmin là công cụ tùy chọn:

```powershell
docker compose --profile tools up --build
```

Profile local mặc định tắt Firebase authentication để backend có thể khởi động
không cần secret. Khi cần kiểm tra Firebase, đặt `FIREBASE_ENABLED=true`,
`REQUIRE_AUTH=true` và cập nhật `FIREBASE_CREDENTIALS_HOST_PATH` trong `.env`.
Không commit `.env` hoặc service-account JSON.

### Backend không dùng Docker

Khởi động MySQL theo `The-Golden-Leaf-server/docker-compose.yml`, sau đó:

```powershell
cd The-Golden-Leaf-server
$env:DB_URL='jdbc:mysql://localhost:3308/datban_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
$env:DB_USERNAME='datban'
$env:DB_PASSWORD='local-datban-password'
.\mvnw.cmd spring-boot:run
```

Chạy test backend không cần MySQL bên ngoài:

```powershell
.\mvnw.cmd test
```

### Android

Sao chép `local.properties.example` thành `local.properties` và sửa `sdk.dir`.
App debug mặc định gọi `http://10.0.2.2:8080/`, phù hợp Android Emulator.

```powershell
# Có thể đổi URL cho thiết bị thật hoặc staging.
$env:API_BASE_URL='http://192.168.1.10:8080/'
$env:WEATHER_API_KEY='your-local-key'
.\gradlew.bat assembleDebug
```

Release không dùng URL local. Trước khi build phải cung cấp URL production:

```powershell
$env:PRODUCTION_API_BASE_URL='https://api.example.com/'
$env:WEATHER_API_KEY='your-production-key'
.\gradlew.bat assembleRelease
```

### Profiles backend

- `dev`: MySQL, cho phép cấu hình bằng biến môi trường, authentication mặc định tắt.
- `test`: H2 in-memory, không cần MySQL/Firebase.
- `prod`: bắt buộc nhận database và Firebase credentials từ môi trường; schema chỉ được validate.

Các biến môi trường mẫu nằm trong `The-Golden-Leaf-server/.env.example`.

Schema do Flyway quản lý và Hibernate chỉ kiểm tra độ khớp. Database từ bản prototype
trước Flyway cần được sao lưu rồi migrate hoặc tạo lại trước khi chạy migration `V1`.
Xem [thiết kế database](docs/database-schema.md) và [REST API contract](docs/api-contract.md).
