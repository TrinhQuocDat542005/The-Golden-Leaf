# Tuần 4–5 — Bảo mật, chuyển khoản và vận hành

## Phạm vi và trạng thái

Milestone này tiếp nối tuần 3: tuần 4 hoàn thiện xác thực, ownership và phân quyền; tuần 5 triển khai chuyển khoản **do nhân viên xác nhận**, thông báo và quy trình vận hành. Đây là phạm vi đã triển khai trong source và kiểm thử local, không phải tuyên bố đã triển khai/nghiệm thu production.

| Hạng mục | Kết quả |
|---|---|
| Xác thực | Firebase ID token, kiểm tra token bị thu hồi, email đã xác minh, tài khoản ACTIVE |
| Ownership | UID do server gắn vào booking; email xác minh chỉ fallback cho dữ liệu cũ không UID |
| Phân quyền | CUSTOMER / STAFF / ADMIN từ DB, có hiệu lực request kế tiếp; bảo vệ admin hoạt động cuối cùng |
| Android | Token cache + refresh một lần khi 401; xóa dữ liệu đơn/cart/inbox/payment khi đổi tài khoản |
| Thanh toán | Server tính tiền; snapshot hóa đơn và tài khoản ngân hàng; không QR mẫu, không tự đánh dấu đã thu |
| Đối soát | Ghi nhận tiền thực nhận, hoàn đủ tiền, chống mã giao dịch trùng, retry idempotent, xử lý tiền đến muộn |
| Thông báo | Inbox riêng, unread count toàn bộ feed, đăng ký thiết bị, outbox per-device, lease/retry và xử lý token lỗi |
| Nhân viên | Dashboard web: lọc ngày, chi tiết, phân bàn, nhận khách, hoàn tất, hủy, đối soát và thử lại push |
| Quản trị | Quyền/khóa tài khoản, menu/ảnh an toàn, bàn thực tế, reconcile sức chứa và audit |

Không có gateway VNPay/MoMo, webhook, chuyển tiền/hoàn tiền tự động, thanh toán một phần hoặc công cụ đọc sao kê tự động. Không bổ sung các module favorites/reviews chỉ vì schema có bảng. CI/CD, TLS/public hosting, backup/restore, giám sát và kiểm thử tải vẫn là việc triển khai tiếp theo.

## Cấu hình chạy thật

1. Sao lưu DB và kiểm tra dữ liệu prototype/tuần 3 trước khi nâng cấp. Flyway chạy V3/V4; không chỉnh checksum migration đã áp dụng. Thử migration trên bản sao trước, giữ backup để phục hồi theo quy trình DBA.
2. Dùng cùng Firebase project cho Android (`google-services.json`), Admin service account và Web API key. Bật provider Email/Password cho dashboard; xác minh email nhân viên/admin trước khi đăng nhập. Android hỗ trợ gửi thư xác minh khi email chưa xác minh.
3. Đặt biến môi trường dưới đây. Không commit `.env` thật, service-account JSON, token, mật khẩu hoặc sao kê.

```dotenv
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://your-private-db:3306/datban_db?serverTimezone=UTC
DB_USERNAME=your-runtime-user
DB_PASSWORD=your-secret
FIREBASE_CREDENTIALS_PATH=/run/secrets/firebase-service-account.json
FIREBASE_WEB_API_KEY=your-firebase-web-api-key
PAYMENT_BANK_NAME=your-actual-bank
PAYMENT_ACCOUNT_NUMBER=your-actual-account
PAYMENT_ACCOUNT_NAME=your-actual-account-holder
RESTAURANT_TIME_ZONE=Asia/Ho_Chi_Minh
BOOKING_TABLE_FEE=200000.00
```

Khi dùng Compose, `FIREBASE_CREDENTIALS_HOST_PATH` trỏ tới file thật để mount vào container. Profile prod bắt buộc Firebase/auth và bàn thực tế, không phụ thuộc cờ dev tắt auth. Compose hiện là template local: **không đưa cổng MySQL/phpMyAdmin ra Internet**; chuẩn bị network riêng, reverse proxy HTTPS, TLS DB phù hợp hạ tầng, quyền DB và secret manager trước khi public.

Web API key là cấu hình client đăng nhập, không phải thông tin Admin SDK và không cấp quyền STAFF/ADMIN. Giới hạn key theo cấu hình Firebase/Google Cloud phù hợp project. Dashboard giữ token trong bộ nhớ, không lưu localStorage/sessionStorage; reload hoặc token hết hạn yêu cầu đăng nhập lại. Không nhập token vào URL.

Android release cần `PRODUCTION_API_BASE_URL=https://.../` và cấu hình ký bản phát hành riêng. Network body logging tắt, Authorization được redact; runtime Android 13+ hỏi quyền thông báo. Inbox vẫn dùng được nếu người dùng không cho phép push.

## Khởi tạo admin đầu tiên

Không có tài khoản admin mặc định hoặc cơ chế nhận quyền từ email/JSON client. Chủ hệ thống tạo và xác minh một tài khoản Firebase, đăng nhập/sync để có row `users`, rồi DBA xác nhận **UID thực** và trạng thái ACTIVE. Chỉ trong bước bootstrap đã được chủ hệ thống phê duyệt, cấp quyền cho UID đó và ghi audit trong cùng transaction:

```sql
START TRANSACTION;
SELECT uid, email, status FROM users WHERE uid = 'VERIFIED_FIREBASE_UID' FOR UPDATE;
SELECT id FROM roles WHERE code = 'ADMIN' FOR UPDATE;
INSERT INTO user_roles(user_uid, role_id)
SELECT u.uid, r.id FROM users u CROSS JOIN roles r
WHERE u.uid = 'VERIFIED_FIREBASE_UID' AND u.status = 'ACTIVE' AND r.code = 'ADMIN'
AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_uid = u.uid AND ur.role_id = r.id);
INSERT INTO audit_logs(actor_uid, action, entity_type, entity_id, new_value)
SELECT uid, 'BOOTSTRAP_ADMIN', 'USER', uid, 'ADMIN: approved initial provisioning'
FROM users WHERE uid = 'VERIFIED_FIREBASE_UID' AND status = 'ACTIVE';
COMMIT;
```

Đọc kết quả SELECT, dừng nếu UID/email/trạng thái không đúng; thay placeholder trước khi chạy. Sau bootstrap quản lý quyền bằng dashboard/API admin, không sửa role tùy tiện bằng SQL. Nên có ít nhất hai admin tin cậy; hệ thống không cho gỡ quyền/khóa admin hoạt động cuối cùng. Thu hồi quyền hoặc khóa user áp dụng từ request kế tiếp, không cần đợi token hết hạn.

## Bàn thực tế và vòng đời phục vụ

1. Đăng nhập `/staff.html` với quyền ADMIN. Khai báo mã bàn duy nhất và khu vực hoạt động, dùng bàn tối thiểu 8 ghế để cấp đơn vị sức chứa hiện tại.
2. Chạy “Đối chiếu sức chứa” để đồng bộ bảy ngày với bàn thực tế. Các chỗ đang giữ được bảo toàn; giảm dưới số đã giữ bị từ chối và rollback.
3. Production không tạo giả 30 bàn như dev. Không có bàn hợp lệ hoặc slot lớn hơn bàn thực tế thì tạo booking mới bị chặn. Nếu nâng cấp từ dev/prototype, reconcile trước khi nhận khách thật.
4. Khách giữ chỗ → chọn món → CONFIRMED. Nhân viên phân đúng số bàn đã giữ, đủ ghế, không trùng ngày/khung giờ. Khu vực khách chọn là ưu tiên, chưa phải cam kết bàn vật lý.
5. Ghi nhận đủ tiền trước khi nhận khách. ASSIGNED → SEATED chỉ đúng ngày đặt theo múi giờ nhà hàng; SEATED → COMPLETED trả chỗ/bàn đúng một lần.
6. Hủy HOLDING/CONFIRMED/ASSIGNED trả chỗ. Không customer-cancel SEATED/COMPLETED. Đơn đã thu tiền khi hủy chuyển chờ hoàn; đừng reset thủ công sức chứa.

Model sức chứa hiện tại là `ceil(số khách/8)` bàn, không phải tối ưu ghép bàn theo từng loại. Bàn nhỏ hơn 8 ghế không cấp một đơn vị sức chứa; nhóm tối đa 80 khách. Quy trình ngoại lệ/sửa ngày sau check-in cần xử lý nghiệp vụ riêng, không giả trạng thái qua database.

## Quy trình chuyển khoản và hoàn tiền

1. GET quote chỉ xem tổng tiền, không sinh invoice/payment. POST payment tạo hóa đơn từ phí bàn cấu hình + giá snapshot món đã chốt; client không quyết định tổng.
2. App hiển thị ngân hàng, chủ tài khoản, số tài khoản, số tiền và nội dung `TGL{id}` từ server. Cấu hình ngân hàng trống sẽ chặn tạo yêu cầu mới, không hiện dữ liệu mẫu. Tài khoản nhận tiền được lưu tại thời điểm tạo intent; thay env không đổi hướng dẫn của đơn cũ.
3. Khách thực hiện chuyển khoản bằng ứng dụng ngân hàng của họ. **PENDING không có nghĩa đã trả tiền.** Không dựa vào ảnh biên lai đơn thuần để ghi nhận PAID.
4. Nhân viên kiểm tra giao dịch đã vào tài khoản nhà hàng bằng sao kê/ngân hàng: đúng đơn, đúng số tiền, mã giao dịch thực. Nhập mã và số tiền vào “Đối soát”; chỉ lúc này server ghi PAID và audit. Mã thực 4–128 ký tự chữ/số/`_`/`-`, chuẩn hóa uppercase, không dùng lại cho đơn khác.
5. Nếu đã hủy mà tiền mới đến, tra cứu đơn đã đóng theo ID và ghi nhận giao dịch đến muộn: booking vẫn CANCELLED, payment REFUND_REQUIRED. Không nhận lại khách bằng cách mở lại đơn.
6. Nhà hàng chuyển hoàn đủ tiền **bên ngoài ứng dụng**, rồi ghi mã giao dịch đi và đúng số tiền ở “Hoàn tiền”. Server mới ghi REFUNDED. API không thực hiện chuyển tiền thay nhà hàng.
7. Retry đúng mã/số tiền trả trạng thái hiện tại, không phát thêm thông báo. Khác mã/số tiền hoặc mã trùng bị từ chối. Audit ghi actor, hành động và bằng chứng đối soát.

Không hỗ trợ ghi nhận thu/hoàn từng phần. Các trường hợp thiếu/thừa tiền, đối soát không rõ đơn hoặc miễn toàn bộ phí cần quy trình kế toán thủ công trước khi mở rộng state machine. Giữ tổng phải thu dương cho luồng chuyển khoản hiện tại. Payment cũ không có snapshot tài khoản phải đối chiếu thủ công; không tự suy đoán tài khoản từ env mới.

## Thông báo và xử lý lỗi

- Sự kiện booking/payment ghi inbox và delivery outbox trong transaction nghiệp vụ. Rollback không để lại thông báo “đã thu tiền” giả.
- Worker claim delivery bằng lease 120 giây, gửi ngoài transaction, finalize theo lease key. Poll 15 giây, batch 20; lỗi tạm thời retry có backoff tối đa năm lần gửi, worker chết có thể reclaim lease. Retry thủ công chỉ cho FAILED và có audit.
- DELIVERED nghĩa FCM đã chấp nhận request, **không phải** người dùng đã đọc/thiết bị chắc chắn hiển thị. Delivery at-least-once; Android dùng notification ID ổn định để giảm hiển thị trùng.
- Token UNREGISTERED bị deactivate; token chuyển sang user khác loại bỏ các delivery cũ. Payload có UID và Android chỉ hiển thị cho user đang đăng nhập. Logout/đổi tài khoản xóa cache và binding thiết bị tương ứng.
- Khi Firebase tắt trong dev, job không giả gửi thành công; inbox vẫn tồn tại. Người dùng mở app có polling inbox và unread count toàn bộ feed, không chỉ 100 dòng đang hiển thị.
- Sau khi thanh toán đã được xử lý, worker bỏ reminder PAYMENT_PENDING lỗi thời. Dashboard theo dõi FAILED/last_error và thử lại sau khi sửa cấu hình; không log token hoặc raw credential.

## Kiểm thử và nghiệm thu

Kết quả chạy cuối ngày 08/10/2026 (local): `mvnw verify` thành công, **140 backend tests, 0 failure/error/skip** gồm H2 và MySQL 8.0.46; Android **10 unit tests**, `assembleDebug` và `lintDebug` thành công. Lint còn **92 warnings / 7 hints**, không lỗi; chưa xử lý toàn bộ cảnh báo legacy/dependency/deprecation trong milestone này.

Backend có suites BookingIntegrity, Operations trên cả H2/MySQL, thêm ProductionInventory, ImageStorage và application smoke. Bao phủ IDOR/UID, token giả/thu hồi/chưa xác minh, role/khóa user, admin cuối cùng, số tiền/mã trùng, retry/concurrency/rollback, tiền đến muộn, hoàn tiền, bàn trùng/thiếu ghế, check-in/completion, private inbox/device switch, lease/backoff/token lỗi, upload ảnh và ngân hàng snapshot.

```powershell
cd The-Golden-Leaf-server
# Chỉ trỏ tới database TEST riêng trên localhost; các suites reset fixture dữ liệu.
$env:MYSQL_TEST_URL='jdbc:mysql://127.0.0.1:13308/golden_leaf_week3_test?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
$env:MYSQL_TEST_PASSWORD='your-local-test-password'
./mvnw.cmd verify
```

Không đặt biến MySQL thì chạy H2, các suite MySQL được skip; tuyệt đối không trỏ biến test vào dữ liệu thật. Đọc report `target/surefire-reports` để biết tests/skips thực tế.

```powershell
# Từ repository root, JDK 17 và Android SDK đã cài.
./gradlew.bat testDebugUnitTest assembleDebug lintDebug
node --check The-Golden-Leaf-server/src/main/resources/static/staff.js
```

Android unit tests kiểm tra contract/payment/quote/notification/history/nhãn trạng thái; không thay thế instrumentation/E2E. Đã sửa API thời gian để tương thích minSdk 25 bằng ThreeTen. UI dashboard được kiểm tra qua computer-use bằng fixture **chỉ đọc**, không đăng nhập thật/gửi tiền. Có thể xem bằng `node scripts/preview-staff-ui.cjs`, mở `http://127.0.0.1:18086/staff.html`; POST fixture bị chặn. Script này không thuộc backend production và không chứng minh Firebase login đã nghiệm thu.

Checklist người vận hành phải hoàn tất trước khi public:

- [ ] Xác nhận migration/backup và thử restore trên môi trường riêng.
- [ ] Firebase cùng project, credential secret mount, email verification, admin bootstrap và staff đúng quyền.
- [ ] Khai báo/reconcile đúng bàn thực tế; kiểm tra không oversell với hai thiết bị.
- [ ] Xác nhận bank/account/name, phí bàn và quy trình sao kê; UAT một giao dịch giá trị nhỏ được chủ nhà hàng cho phép.
- [ ] UAT hủy sau trả tiền, tiền đến muộn, thực hiện hoàn tiền bên ngoài rồi ghi nhận đúng mã.
- [ ] Trên thiết bị Android thật: token expiry/revocation, đổi tài khoản, quyền push từ chối/chấp nhận, background FCM và inbox.
- [ ] Chạy release signing/HTTPS, kiểm thử dashboard trên browser và staging với Firebase thật.
- [ ] Rate limit, log/metrics/alert, backup định kỳ, secret rotation, DB private network, CI/CD và rollback runbook.

Chưa thực hiện live Firebase/FCM, giao dịch ngân hàng thật, instrumentation Android hoặc deployment production trong milestone này; cần cấu hình và nghiệm thu các mục trên. Không có secret/merchant account mới được tạo.

## Tài liệu chuẩn tham khảo

- [Firebase — verify ID tokens](https://firebase.google.com/docs/auth/admin/verify-id-tokens) và [manage sessions/revocation](https://firebase.google.com/docs/auth/admin/manage-sessions).
- [Firebase Auth REST — password sign-in](https://firebase.google.com/docs/reference/rest/auth).
- [FCM — manage tokens](https://firebase.google.com/docs/cloud-messaging/manage-tokens) và [delivery error codes](https://firebase.google.com/docs/cloud-messaging/error-codes).
