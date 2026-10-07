# Tuần 3 — Tính toàn vẹn luồng đặt bàn

Phạm vi lấy từ mục tiếp theo của roadmap: transaction đặt bàn, chống đặt trùng, idempotency, kiểm thử concurrency và đồng bộ Android. Phân quyền staff/admin, physical table assignment, payment callback, notification outbox và CI/CD thuộc các giai đoạn tiếp theo.

## Kết quả triển khai

- Flyway V2 bổ sung idempotency key, số bàn đã giữ, index hết hạn và CHECK constraint; giữ nguyên V1 đã phát hành.
- Tạo đơn và trừ sức chứa trong một transaction; khóa slot trước booking và dùng READ_COMMITTED để tránh đọc snapshot cũ sau khi chờ trên MySQL.
- Giữ chỗ 15 phút; xác nhận/hủy có state transition rõ ràng; expiry job theo batch 100, mỗi đơn một transaction. Tạo đơn mới cũng thu hồi hold đã hết hạn trong cùng slot.
- Chặn hai API sửa sức chứa trực tiếp, chuyển việc giữ/trả về lifecycle của đơn.
- Giỏ hàng được thay thế toàn bộ trong transaction, giới hạn số món/số lượng, kiểm tra món còn phục vụ và giữ snapshot giá phía server khi retry.
- Hóa đơn dùng tiền món và phí bàn phía server; retry không tạo hóa đơn thứ hai; lập hóa đơn không đồng nghĩa xác nhận thanh toán.
- Android lưu key, ID đơn và giỏ hàng qua SavedStateHandle, hiển thị deadline/error, chặn gửi lặp, hỗ trợ hủy và xác nhận đơn không đặt món.
- Bổ sung integration tests cho cạnh tranh nhiều transaction, REST flow, rollback, terminal-state retries, ownership, giá món và timezone.

## Cấu hình

| Biến môi trường | Mặc định | Ý nghĩa |
| --- | --- | --- |
| `BOOKING_HOLD_DURATION` | `PT15M` | Khoảng thời gian giữ chỗ, ISO-8601 duration dương |
| `RESTAURANT_TIME_ZONE` | `Asia/Ho_Chi_Minh` | Ngày/giờ nhà hàng, không phụ thuộc timezone máy chủ |
| `BOOKING_EXPIRY_DELAY_MS` | `60000` | Chu kỳ quét hold hết hạn |
| `BOOKING_TABLE_FEE` | `200000.00` | Phí bàn cố định trên một đơn |

API contract và chính sách nâng cấp dữ liệu V1 được ghi trong [API contract](api-contract.md) và [database schema](database-schema.md).

## Kiểm thử tự động

```powershell
cd The-Golden-Leaf-server
./mvnw.cmd test
```

Bộ test mặc định dùng H2. `MySqlBookingIntegrityTests` chạy lại cùng các ca nghiệp vụ trên MySQL, chỉ bật khi biến `MYSQL_TEST_URL` trỏ đúng port/database test riêng. Bộ test xóa dữ liệu fixture nên tuyệt đối không dùng database đang phục vụ dự án.

```powershell
# Từ thư mục backend: tạo MySQL test riêng, dùng dữ liệu tạm trong RAM.
docker run --rm -d --name golden-leaf-week3-test --label golden-leaf.purpose=week3-test --publish 127.0.0.1:13308:3306 --tmpfs /var/lib/mysql -e MYSQL_ROOT_PASSWORD=local-test-password -e MYSQL_DATABASE=golden_leaf_week3_test mysql:8.0
# Chờ MySQL sẵn sàng trước khi chạy Maven.
docker exec golden-leaf-week3-test mysqladmin ping -h 127.0.0.1 -uroot -plocal-test-password
$env:MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:13308/golden_leaf_week3_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
$env:MYSQL_TEST_PASSWORD='local-test-password'
./mvnw.cmd test -Dtest=MySqlBookingIntegrityTests
docker stop golden-leaf-week3-test
Remove-Item Env:MYSQL_TEST_URL, Env:MYSQL_TEST_PASSWORD
```

Android (từ thư mục gốc):

```powershell
./gradlew.bat testDebugUnitTest assembleDebug
```

## Kết quả xác minh ngày 07/10/2026

- `mvnw.cmd verify` với MySQL test bật: **42 test pass**, không fail/error/skip (19 ca H2, 19 ca MySQL, 4 ca nền tảng/API).
- MySQL thực tế: **8.0.46**, Flyway chạy V1 → V2 từ database trống và validate thành công ở lần chạy tiếp theo.
- Các ca cạnh tranh sử dụng 8–12 worker và transaction riêng; xác minh số đơn, dòng món, hóa đơn và sức chứa cuối cùng.
- Android: `testDebugUnitTest assembleDebug` thành công; unit suite hiện là smoke test có sẵn, chưa thay thế kiểm thử UI/thiết bị thật.
- `docker compose config --quiet` và `git diff --check` thành công.
- Chưa chạy end-to-end trên emulator hoặc xác nhận thanh toán từ provider; hai phần này không được tính là đã nghiệm thu production.

## Giới hạn còn lại

- Sức chứa hiện là tổng số bàn trong khung giờ, 8 khách/bàn. Khu vực là nguyện vọng; chưa phân bàn vật lý theo từng khu vực.
- Booking đã CONFIRMED giữ suất cho lịch hẹn đến khi hủy. Quy trình check-in, phân bàn và hoàn tất phục vụ sẽ được bổ sung cùng phân quyền nhân viên.
- Chế độ dev tắt xác thực vẫn cho phép gọi API không có token; bật prod/Firebase trước khi phục vụ bên ngoài.
- Android còn hiển thị phí bàn 200.000 VND hiện tại. Khi đổi cấu hình phí server, cần đồng bộ app hoặc triển khai quote endpoint ở giai đoạn thanh toán.
- SavedStateHandle phục hồi trong lifecycle Android; force-stop hoặc xóa dữ liệu ứng dụng không bảo đảm phục hồi intent đang gửi.
- Các đơn legacy không được tự suy đoán trạng thái giữ chỗ. Cần reconciliation trước khi nâng cấp dữ liệu có sẵn.
