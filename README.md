<div align="center">
  <img src="app/src/main/res/drawable/logo.png" alt="The Golden Leaf Restaurant" width="220" />

  # The Golden Leaf

  **Full-stack portfolio: đặt bàn nguyên tử, đối soát chuyển khoản và vận hành nhà hàng — Android, Spring Boot và web dashboard.**

  [![CI](https://github.com/TrinhQuocDat542005/The-Golden-Leaf/actions/workflows/ci.yml/badge.svg)](https://github.com/TrinhQuocDat542005/The-Golden-Leaf/actions/workflows/ci.yml)

  [![Android](https://img.shields.io/badge/Android-Jetpack_Compose-3DDC84?logo=android&logoColor=white)](https://developer.android.com/compose)
  [![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
  [![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.16-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
  [![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
  [![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
  [![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)

  [Chạy demo](#2-demo-portfolio-không-cần-secret) · [Ảnh thật](#giao-diện-demo) · [Case study](docs/week-7-portfolio.md#case-study-ngắn-cho-người-review) · [API](#api-hiện-có) · [Tài liệu](#tài-liệu-kỹ-thuật)
</div>

---

## Tổng quan

The Golden Leaf là dự án portfolio mô hình hóa hành trình dùng bữa: khách xem thực đơn, đặt bàn, gọi món và theo dõi hóa đơn; nhân viên đối soát, phân bàn, nhận khách và hoàn tất. Trọng tâm không chỉ là CRUD mà là tính đúng của sức chứa, tiền và quyền truy cập khi request bị retry hoặc nhiều người thao tác đồng thời.

**For reviewers:** a local web/API sandbox runs with JDK 17, without Firebase credentials, MySQL or a bank account. Android also has a separate native `demo` APK using this sandbox; ordinary `debug`/`release` clients remain Firebase-authenticated. This repository demonstrates engineering workflows; it is not a live restaurant service or a published mobile app.

Repository được tổ chức theo mô hình monorepo, gồm ứng dụng Android, Spring Boot REST API và hạ tầng MySQL chạy bằng Docker Compose. Schema dữ liệu được quản lý bằng Flyway, contract API được chuẩn hóa bằng DTO và có OpenAPI/Swagger để kiểm thử tích hợp.

> [!NOTE]
> Tuần 1–7 đã có source, kiểm thử, CI và bản demo portfolio. Demo dùng dữ liệu tổng hợp, không nhận tiền hoặc gửi push thật. Không cần mua hosting/domain để chạy thử. Bộ cấu hình production là tài liệu kỹ thuật tham khảo, **không phải chứng nhận đã go-live**. Bắt đầu với [hướng dẫn demo & kịch bản trình diễn](docs/week-7-portfolio.md).

Tuần 8 bổ sung yêu thích/đánh giá theo tài khoản, trạng thái lỗi/retry Android, lịch theo múi giờ nhà hàng, test ViewModel và quality/security gates. [Báo cáo tuần 8](docs/week-8-quality.md) phân biệt kết quả local, native emulator và screenshot đã kiểm tra; không coi build APK là UI E2E đã đạt. Xem đúng revision ở [CI](https://github.com/TrinhQuocDat542005/The-Golden-Leaf/actions/workflows/ci.yml).

## Giao diện demo

Ảnh chụp từ ứng dụng đang chạy qua Chromium E2E, không phải mockup. Ngày/ID có thể khác khi chạy lại.

![Portfolio demo — trải nghiệm khách trên API thật](docs/assets/demo-overview.png)

<details>
  <summary>Đơn đặt bàn, dashboard nhân viên và giao diện mobile-width</summary>

  <p><img src="docs/assets/demo-booking.png" alt="Đơn demo với tổng tiền do server tính" width="900" /></p>
  <p><img src="docs/assets/demo-dashboard.png" alt="Dashboard demo sau đối soát và phân bàn" width="900" /></p>
  <p><img src="docs/assets/demo-mobile.png" alt="Web demo ở viewport 390 px, không phải screenshot Android" width="280" /></p>
</details>

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
| Portfolio demo | ✅ Đã triển khai | H2 memory, 4 persona, walkthrough API thật, screenshots, browser E2E/video và JAR bundle |
| Go-live thực tế | Ngoài phạm vi portfolio | Chưa public hosting, chưa signed Android/live Firebase/FCM/ngân hàng; không cần để review demo |

## Tính năng cốt lõi

### Trải nghiệm khách hàng

- Duyệt thực đơn và xem thông tin món ăn.
- Yêu thích riêng theo tài khoản; đánh giá 1–5 sao, cập nhật một đánh giá cho mỗi món, không công khai email/UID.
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
│   ├── database-schema.md       # Thiết kế database
│   ├── week-7-portfolio.md      # Demo, case study và script trình diễn
│   └── assets/                 # Screenshot thật từ browser test
├── tools/demo-browser/         # Playwright E2E, video/report
├── ops/                        # Production/backup templates (không auto-deploy)
├── .github/workflows/           # Backend/Android/infrastructure/demo CI
├── build.gradle.kts             # Cấu hình Gradle cấp project
└── local.properties.example     # Mẫu cấu hình Android SDK
```

## Bắt đầu nhanh

### Yêu cầu môi trường

- Git.
- JDK 17.
- **Demo web/API:** chỉ Git và JDK 17. Lần build đầu cần Internet.
- **Android riêng:** Android Studio và Android SDK 36.
- **Development MySQL/kiểm tra hạ tầng:** Docker Desktop nếu chạy bằng container.

> [!IMPORTANT]
> Dùng JDK 17 cho cả backend và Gradle Android. Không đưa `.env`, keystore hoặc Firebase service-account JSON thật vào Git.

### 1. Clone repository

```powershell
git clone https://github.com/TrinhQuocDat542005/The-Golden-Leaf.git
cd The-Golden-Leaf
```

### 2. Demo portfolio không cần secret

```powershell
cd The-Golden-Leaf-server
./mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=demo'
```

Linux/macOS: `bash mvnw spring-boot:run -Dspring-boot.run.profiles=demo` trong thư mục backend.

Mở **[http://127.0.0.1:8080/demo.html](http://127.0.0.1:8080/demo.html)**. Chọn ngày mai, 4 khách và 2 salad → tạo đơn → xác nhận → tạo payment demo. Tổng mẫu **340.000 VND** do server tính. Mở `/staff.html` ở tab khác, chọn persona nhân viên để đối soát/phân bàn; admin xem audit. Không cần mật khẩu hay Firebase.

DB tạm có 6 món, 4 bàn 8 ghế, 28 slots, 4 tài khoản `.invalid` và một đơn hôm nay để thử nhận khách/hoàn tất. Restart backend để reset. Không chuyển tiền, không gửi FCM, không ghi/serve uploads local. Demo chỉ bind loopback và từ chối trộn profile/DB thật.

Muốn chạy không cần Maven? Tải **portfolio-demo-bundle** từ một [CI run main thành công](https://github.com/TrinhQuocDat542005/The-Golden-Leaf/actions/workflows/ci.yml), kiểm checksum và chạy JAR với JDK 17. Artifact giữ 14 ngày, có thể cần GitHub login; không phải GitHub Release lâu dài. Xem [bundle guide](docs/demo-bundle-guide.md).

**Phạm vi:** bundle trên là demo web/API; ảnh web mobile-width không thay nghiệm thu Android. Để thử app native không Firebase, chọn **Build Variant `demo`** theo [Android demo guide](docs/android-demo.md). Bản Android `debug`/`release` thường vẫn cần Firebase.

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

**Thử Android không cần Firebase:** bật backend profile `demo`, chọn Build Variant **`demo`** rồi chạy trên emulator. APK riêng có hai tài khoản khách và dữ liệu tổng hợp, không nhận tiền. Xem [Android demo guide](docs/android-demo.md); `assembleDemo` xuất `app-demo.apk` (không dùng `app-debug.apk` để đăng nhập mẫu).

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

Test mặc định dùng H2 in-memory và không yêu cầu Firebase. Các suite MySQL opt-in dùng **schema disposable riêng** theo CI; khi chưa cấu hình fixture chúng được skip, không gọi đây là đã test MySQL local.

### Browser E2E và bằng chứng demo

```powershell
cd The-Golden-Leaf-server
./mvnw.cmd verify
cd ../tools/demo-browser
npm ci
npx playwright install chromium
npm test
```

JDK 17 + Node 24; lần đầu cần tải Chromium. Test tự khởi động JAR với demo tại port 18082, không mock API, không reuse server khác, và kiểm customer/staff/admin journey + mobile-width. HTML report/video có trong artifact **demo-browser-report-and-video** của CI. [Hướng dẫn chụp lại screenshot và trình diễn](docs/week-7-portfolio.md).

### Android

```powershell
./gradlew.bat testDebugUnitTest
./gradlew.bat assembleDebug
./gradlew.bat lintDebug
node scripts/check-android-quality.mjs
```

Trước khi mở pull request, nên chạy cả test backend lẫn build Android để phát hiện sớm lỗi contract giữa hai phía.

Tuần 8 đã chạy **190 backend tests (H2 + MySQL), 32 Android unit tests, 3 Chromium E2E và 6 quality-gate tests**. Lint local hiện còn **96 warnings / 8 hints, 0 errors**; CI Linux fresh metadata có 98 warnings / 8 hints, được hiệu chỉnh theo từng loại với evidence, chưa phải lint sạch. CI có native instrumentation matrix API 25/35, raw test events/JUnit và screenshot Android thật; đọc trạng thái đúng commit trong workflow. Test dùng application fixture không Firebase, không thay thế nghiệm thu toàn bộ app hoặc Google/Firebase login thật. Chi tiết/reproduce trong [báo cáo tuần 8](docs/week-8-quality.md).

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
- [x] Demo portfolio riêng: H2/roles fixture, walkthrough thật, screenshot, E2E browser/video và bundle JAR.
- [x] API yêu thích/đánh giá, Android retry/error/account isolation, restaurant timezone, ViewModel tests và quality/security gate.
- [x] Nghiệm thu instrumentation API 25/35: 4 tests mỗi API và bốn screenshot native đã kiểm tra; [toàn bộ CI xanh ở source revision 7876e77](https://github.com/TrinhQuocDat542005/The-Golden-Leaf/actions/runs/37804794761).
- [x] CI và manual release delivery bundle; log có cấu trúc, metrics, HTTPS/hardening template.
- [x] Backup DB/uploads mã hóa, diễn tập restore và runbook phát hành/rollback.
- Ngoài scope hiện tại: hosting/domain, giao dịch/Firebase/FCM thật, signed Android/store release, HA và operator UAT.
- Tác giả tùy chọn: video có thuyết minh, license và GitHub Release dài hạn; không coi đây là việc bắt buộc để demo local.

## Tài liệu kỹ thuật

- [REST API contract](docs/api-contract.md) — endpoint, payload, validation và error envelope.
- [Database schema](docs/database-schema.md) — bảng, quan hệ, kiểu dữ liệu và chiến lược migration.
- [Tuần 3 — Booking integrity](docs/week-3-booking-integrity.md) — lifecycle, cấu hình, kiểm thử concurrency và giới hạn triển khai.
- [Tuần 4–5 — Security & operations](docs/weeks-4-5-security-operations.md) — phạm vi hoàn thành, phân quyền, thanh toán thủ công, thông báo và checklist đưa vào vận hành.
- [Tuần 6 — Production readiness](docs/week-6-production-readiness.md) — CI/release, TLS/private metrics, backup mã hóa, restore/rollback và các gate go-live.
- [Tuần 7 — Portfolio demo](docs/week-7-portfolio.md) — chạy không cần secret, case study, kịch bản 5–7 phút và giới hạn đã kiểm chứng.
- [Tuần 8 — Quality & regression](docs/week-8-quality.md) — nghiệm thu CI, screenshot Android thật, lint budget và CVE exception có hạn.
- [Tuần 9 — Native Android demo](docs/week-9-native-demo.md) — APK riêng, dữ liệu minh họa, customer UI journeys, screenshot/video và trạng thái nghiệm thu.
- [Demo bundle guide](docs/demo-bundle-guide.md) — chạy JAR độc lập bằng JDK 17.
- [Contributing](CONTRIBUTING.md) · [Security policy](SECURITY.md) — workflow đóng góp và báo lỗi không lộ dữ liệu.
- [Environment template](The-Golden-Leaf-server/.env.example) — biến môi trường dùng với Docker Compose.
- [OpenAPI configuration](The-Golden-Leaf-server/src/main/java/com/example/datban/config/OpenApiConfig.java) — metadata tài liệu API.

## Quy ước đóng góp

1. Tạo branch theo phạm vi thay đổi, ví dụ `feat/booking-integrity` hoặc `fix/invoice-total`; Codex-assisted dùng `codex/<scope>`.
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
