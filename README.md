<div align="center">
  <img src="app/src/main/res/drawable/logo.png" alt="The Golden Leaf Restaurant" width="220" />

  # The Golden Leaf

  **Nền tảng đặt bàn và gọi món dành cho nhà hàng, kết nối trải nghiệm khách hàng trên Android với hệ thống vận hành tập trung.**

  [![Android](https://img.shields.io/badge/Android-Jetpack_Compose-3DDC84?logo=android&logoColor=white)](https://developer.android.com/compose)
  [![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
  [![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.6-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
  [![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
  [![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
  [![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)

  [Bắt đầu nhanh](#bắt-đầu-nhanh) · [Kiến trúc](#kiến-trúc-hệ-thống) · [API](#api-hiện-có) · [Roadmap](#roadmap) · [Tài liệu](#tài-liệu-kỹ-thuật)
</div>

---

## Tổng quan

The Golden Leaf hướng tới số hóa toàn bộ hành trình dùng bữa: khách hàng xem thực đơn, chọn thời gian và khu vực, đặt bàn, gọi món và theo dõi hóa đơn ngay trên ứng dụng Android; phía nhà hàng có API và giao diện web nền tảng để quản lý thực đơn cùng hoạt động đặt bàn.

Repository được tổ chức theo mô hình monorepo, gồm ứng dụng Android, Spring Boot REST API và hạ tầng MySQL chạy bằng Docker Compose. Schema dữ liệu được quản lý bằng Flyway, contract API được chuẩn hóa bằng DTO và có OpenAPI/Swagger để kiểm thử tích hợp.

> [!NOTE]
> Đã triển khai milestone tuần 1–6: đặt bàn nguyên tử, xác thực/phân quyền, chuyển khoản thủ công, thông báo/dashboard, CI và bộ cấu hình triển khai an toàn. Chưa public production hoặc nghiệm thu Firebase/ngân hàng thật; xem [runbook tuần 6](docs/week-6-production-readiness.md) và checklist cấu hình tuần 4–5.

## Trạng thái phát triển

| Hạng mục | Trạng thái | Kết quả |
| --- | :---: | --- |
| Nền tảng phát triển | ✅ Hoàn thành | Build tái lập, Docker Compose, environment template, health check |
| Dữ liệu & API contract | ✅ Hoàn thành | Flyway V1, 17 bảng nghiệp vụ, DTO, lỗi API chuẩn, Swagger |
| Luồng đặt bàn an toàn | ✅ Đã triển khai | Transaction, giữ chỗ có hạn, chống đặt trùng, idempotency, test concurrency |
| Xác thực & phân quyền | ✅ Đã triển khai | Firebase ID token, kiểm tra thu hồi, email xác minh, ownership UID, CUSTOMER/STAFF/ADMIN |
| Chuyển khoản & hoàn tiền | ✅ Đã triển khai | Hóa đơn từ server, tài khoản nhận tiền snapshot, đối soát thủ công, audit và chống ghi nhận trùng |
| Thông báo & vận hành | ✅ Đã triển khai | Inbox, FCM outbox/lease/retry, phân bàn, nhận khách, hoàn tất và dashboard nhân viên |
| Release engineering | ✅ Đã triển khai | CI H2/MySQL/Android, manual delivery bundle, HTTPS template, hardening, metrics và backup mã hóa |
| Go-live thực tế | 📋 Chờ cấu hình/UAT | Hosting/domain/secrets, signed Android, monitoring/backup off-host và nghiệm thu live |

## Tính năng cốt lõi

### Trải nghiệm khách hàng

- Duyệt thực đơn và xem thông tin món ăn.
- Chọn ngày, khung giờ và khu vực ngồi.
- Tạo yêu cầu đặt bàn và thêm món vào đơn.
- Xem hóa đơn tính bởi server và tài khoản chuyển khoản thực tế; theo dõi chờ đối soát, đã thu, chờ hoàn/đã hoàn.
- Lịch sử đơn theo tài khoản và hộp thư thông báo; push FCM khi đã cấu hình.
- Giao diện Android hiện đại xây dựng bằng Jetpack Compose.
- Tích hợp nền tảng Firebase cho xác thực và thông báo.

### Vận hành nhà hàng

- Dashboard `/staff.html`: lọc ngày, chi tiết đơn, phân bàn, nhận khách, hoàn tất và hủy.
- Nhân viên ghi nhận chuyển khoản/hoàn tiền sau khi kiểm tra sao kê; không tự động chuyển tiền.
- Admin quản lý món/ảnh, bàn thực tế, quyền truy cập, khóa tài khoản và xem audit.
- Đồng bộ sức chứa với bàn thực tế, bảo toàn chỗ đang giữ; theo dõi và thử lại push lỗi.
- Database migration có phiên bản, dễ tái tạo trên môi trường mới.
- Health check và tài liệu API phục vụ triển khai, tích hợp.

## Kiến trúc hệ thống

```mermaid
flowchart LR
    U[Khách hàng] --> A[Android App<br/>Jetpack Compose]
    S[Nhân viên nhà hàng] --> W[Staff Dashboard<br/>Bearer token]

    A -->|REST / JSON| B[Spring Boot API]
    W --> B
    A <-->|Auth / FCM| F[Firebase]
    B <-->|Token verification| F
    B -->|Spring Data JPA| D[(MySQL 8)]
    M[Flyway migrations] --> D

    subgraph Docker Compose
        B
        D
    end
```

Luồng dữ liệu chính:

1. Ứng dụng xác thực qua Firebase và gửi ID token; production bắt buộc token hợp lệ, email xác minh và tài khoản đang hoạt động.
2. Android gọi REST API bằng Retrofit; backend xác thực, kiểm tra nghiệp vụ và trả DTO ổn định.
3. Spring Data JPA truy cập MySQL; Flyway là nguồn duy nhất quản lý thay đổi schema.
4. Swagger UI mô tả contract thực tế để mobile, backend và QA cùng kiểm tra.

## Công nghệ sử dụng

| Khu vực | Công nghệ chính |
| --- | --- |
| Android | Kotlin 2.0.21, Jetpack Compose, Material 3, Navigation Compose |
| Kiến trúc mobile | ViewModel, StateFlow, Coroutines, Hilt |
| Kết nối mobile | Retrofit 2, OkHttp 4, Gson, Kotlin Serialization |
| Backend | Java 17, Spring Boot 3.5.6, Spring Web, Validation, Security |
| Dữ liệu | Spring Data JPA, MySQL 8, Flyway, H2 cho test |
| Dịch vụ | Firebase Authentication, Firebase Cloud Messaging |
| API & vận hành | OpenAPI 3, Swagger UI, Spring Boot Actuator |
| Hạ tầng local | Docker, Docker Compose, phpMyAdmin tùy chọn |

## Cấu trúc repository

```text
The-Golden-Leaf/
├── app/                         # Ứng dụng Android Jetpack Compose
│   └── src/main/java/...        # UI, ViewModel, repository, network
├── The-Golden-Leaf-server/      # Spring Boot backend
│   ├── src/main/java/...        # Controller, service, repository, model
│   ├── src/main/resources/
│   │   ├── db/migration/        # Flyway migrations
│   │   ├── static/staff.*      # Dashboard nhân viên/admin
│   │   └── templates/           # Giao diện quản trị Thymeleaf
│   ├── Dockerfile
│   └── docker-compose.yml
├── docs/
│   ├── api-contract.md          # REST contract và quy ước lỗi
│   └── database-schema.md       # Thiết kế database
├── build.gradle.kts             # Cấu hình Gradle cấp project
└── local.properties.example     # Mẫu cấu hình Android SDK
```

## Bắt đầu nhanh

### Yêu cầu môi trường

- Git.
- JDK 17.
- Android Studio và Android SDK 36.
- Docker Desktop nếu chạy backend bằng container.

> [!IMPORTANT]
> Dùng JDK 17 cho cả backend và Gradle Android. Không đưa `.env`, keystore hoặc Firebase service-account JSON thật vào Git.

### 1. Clone repository

```powershell
git clone https://github.com/TrinhQuocDat542005/The-Golden-Leaf.git
cd The-Golden-Leaf
```

### 2. Khởi động backend bằng Docker

```powershell
cd The-Golden-Leaf-server
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

### 3. Cấu hình và build Android

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
| `prod` | Nhận hoàn toàn từ env | Bắt buộc | Môi trường production |

## API hiện có

| Method | Endpoint | Chức năng |
| :---: | --- | --- |
| `POST` | `/api/auth/sync` | Đồng bộ hồ sơ Firebase với backend |
| `GET` | `/api/thucdon` | Lấy danh sách thực đơn |
| `GET` | `/api/thucdon/{id}` | Lấy chi tiết món ăn |
| `GET` | `/api/ban-slot` | Tra cứu bàn theo ngày, giờ và khu vực |
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

Request, response và schema lỗi chuẩn được mô tả trong [REST API contract](docs/api-contract.md). Khi backend đang chạy, Swagger UI là nguồn tương tác nhanh nhất để thử từng endpoint.

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

Xem đầy đủ tại [tài liệu thiết kế database](docs/database-schema.md).

## Kiểm thử

### Backend

```powershell
cd The-Golden-Leaf-server
./mvnw.cmd test
```

Test backend dùng H2 in-memory nên không yêu cầu MySQL hoặc Firebase bên ngoài.

### Android

```powershell
./gradlew.bat testDebugUnitTest
./gradlew.bat assembleDebug
./gradlew.bat lintDebug
```

Trước khi mở pull request, nên chạy cả test backend lẫn build Android để phát hiện sớm lỗi contract giữa hai phía.

## Roadmap

- [x] Chuẩn hóa Gradle/Maven wrapper và cấu hình JDK 17.
- [x] Docker hóa backend, MySQL và health check.
- [x] Tách secret khỏi source, bổ sung environment template.
- [x] Thiết lập Flyway V1 và chuẩn hóa kiểu dữ liệu tiền tệ.
- [x] Bổ sung DTO, global error response và OpenAPI/Swagger.
- [x] Làm luồng đặt bàn nguyên tử, chống double-booking và hỗ trợ idempotency.
- [x] Firebase authentication, ownership UID và phân quyền khách/nhân viên/admin.
- [x] Payment state machine và đối soát chuyển khoản/hoàn tiền thủ công (không gateway/callback).
- [x] Notification outbox, retry, lease recovery và FCM delivery tracking.
- [x] Dashboard vận hành, audit và kiểm tra sức chứa bàn thực tế.
- [x] Test tích hợp H2/MySQL, concurrency, bảo mật và Android contract; build/lint debug.
- [ ] Nghiệm thu end-to-end trên thiết bị, Firebase và tài khoản ngân hàng thật.
- [x] CI và manual release delivery bundle; log có cấu trúc, metrics, HTTPS/hardening template.
- [x] Backup DB/uploads mã hóa, diễn tập restore và runbook phát hành/rollback.
- [ ] Go-live: hosting/domain/secrets, image/dependency scan, alerts/backup off-host và nghiệm thu operator.

## Tài liệu kỹ thuật

- [REST API contract](docs/api-contract.md) — endpoint, payload, validation và error envelope.
- [Database schema](docs/database-schema.md) — bảng, quan hệ, kiểu dữ liệu và chiến lược migration.
- [Tuần 3 — Booking integrity](docs/week-3-booking-integrity.md) — lifecycle, cấu hình, kiểm thử concurrency và giới hạn triển khai.
- [Tuần 4–5 — Security & operations](docs/weeks-4-5-security-operations.md) — phạm vi hoàn thành, phân quyền, thanh toán thủ công, thông báo và checklist đưa vào vận hành.
- [Tuần 6 — Production readiness](docs/week-6-production-readiness.md) — CI/release, TLS/private metrics, backup mã hóa, restore/rollback và các gate go-live.
- [Environment template](The-Golden-Leaf-server/.env.example) — biến môi trường dùng với Docker Compose.
- [OpenAPI configuration](The-Golden-Leaf-server/src/main/java/com/example/datban/config/OpenApiConfig.java) — metadata tài liệu API.

## Quy ước đóng góp

1. Tạo branch theo phạm vi thay đổi, ví dụ `feat/booking-integrity` hoặc `fix/invoice-total`.
2. Giữ commit nhỏ, có chủ đích và sử dụng Conventional Commits.
3. Không commit secret, file `.env`, service-account JSON hoặc keystore thật.
4. Cập nhật contract/migration khi thay đổi API hoặc database.
5. Chạy test backend và build Android trước khi mở pull request.

Ví dụ commit:

```text
feat(booking): prevent duplicate reservations
fix(invoice): preserve monetary precision
docs(readme): improve project onboarding
```

---

<div align="center">
  <sub>Built with Kotlin, Spring Boot and a focus on reliable restaurant operations.</sub>
  <br />
  <sub>© The Golden Leaf contributors</sub>
</div>
