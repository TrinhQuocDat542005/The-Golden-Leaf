# Tuần 6 — Release engineering và vận hành an toàn

## Kết quả và ranh giới

Tuần 6 cung cấp pipeline kiểm thử/đóng gói, cấu hình triển khai một host bằng Docker, hardening, observability, backup mã hóa và runbook phát hành/khôi phục. **Chưa public server/domain, chưa cấu hình credential thật, chưa ký/phát hành Android và chưa nghiệm thu live Firebase/ngân hàng.** Không chuyển tiền hoặc cấp quyền admin thật trong milestone.

| Deliverable | Phạm vi |
|---|---|
| CI | PR/main/manual: H2 + MySQL disposable, Android unit/build/lint, workflow/shell lint, backup drill, Docker build |
| Release delivery | Manual trên main, cần CI thành công; image gắn commit SHA, artifact tar.gz/checksum/revision/runbook |
| Production template | TLS proxy, DB/management không publish, secret file/configtree, non-root backend, read-only/cap-drop, graceful shutdown và log rotation |
| HTTP hardening | Giới hạn header/form/thread/timeouts, Swagger production tắt, 429 gate trước Firebase, không tin forwarded IP tùy tiện |
| Android | Release HTTPS-only, build guard URL, debug HTTP tách riêng; chặn cloud/device-transfer dữ liệu tài khoản |
| Observability | JSON stdout, request ID server cấp, access log tối giản; Prometheus credential riêng; readiness gồm DB |
| Business metrics | Delivery pending/sending/failed, khoản chờ hoàn, hold quá hạn và tuổi snapshot, không có nhãn dữ liệu khách |
| Recovery | DB + uploads mã hóa age; stream không có dump plaintext trên host; restore DB mới/rỗng, diễn tập tamper/key-sai/ghi-đè |

CD hiện là **delivery bundle + promotion thủ công**, không auto-deploy vì chưa chọn hosting/domain/môi trường phê duyệt. Không publish registry, tạo GitHub secrets hoặc thay branch protection. Không coi milestone là chứng nhận đã audit mọi dependency hoặc đã sẵn sàng nhận tiền thật.

## CI / release

Workflow dùng `contents: read`, pin Actions full commit SHA, checkout không giữ credential. PR không nhận secret deployment, không dùng pull_request_target hoặc runner riêng. Password/schema MySQL là fixture riêng. Reports/debug APK giữ 14 ngày, không phải backup nghiệp vụ. Lint warnings legacy chưa trở thành gate; lỗi lint/build/test vẫn fail.

Chủ repository bật Actions và ruleset `main`: PR review, required checks backend/android/infrastructure. Dependabot khai báo Actions/Maven/Gradle, không auto-merge. Chọn **Release delivery bundle → Run workflow → main** sau review: chạy CI, build `golden-leaf:<commit-sha>`, tạo image tar.gz, SHA256SUMS, REVISION, ops và runbook. Không đưa secret/key vào artifact.

Image base dùng tag vendor chính thức, chưa khóa digest toàn bộ dependency. Trước promotion ghi image ID/digest, scan image/dependency và lưu cùng revision. Bundle chưa signed/provenance-attested; checksum chỉ kiểm tra byte file, phải tải từ workflow tin cậy.

```bash
sha256sum -c SHA256SUMS
gzip -dc backend-image.tar.gz | docker load
docker image inspect golden-leaf:YOUR_VERIFIED_COMMIT_SHA --format '{{.Id}}'
```

Build local phải test trước `docker build -t golden-leaf:YOUR_VERIFIED_COMMIT_SHA The-Golden-Leaf-server`. Giữ image cũ theo SHA, không dùng latest để rollback. Bundle không có signed Android release: chọn hệ thống phát hành/keystore riêng, không commit password/keystore.

## Chuẩn bị staging / production

1. Chọn host Linux, domain, cert hợp lệ, operator và người nhận alert. Template một host chưa giải quyết HA/multi-host/zero-downtime migrations.
2. Copy `ops/.env.production.example` → `ops/.env.production` (ignored). Điền image SHA, Firebase Web API key, ngân hàng thực, phí bàn; monitoring token ngẫu nhiên **ít nhất 32 ký tự**.
3. Tạo secret riêng `ops/secrets/db-password`, `db-root-password`, `firebase-service-account.json`. Hạn chế quyền đọc host files; DB_PASSWORD qua configtree, service-account mount read-only. Không dùng fixture passwords ở production.
4. Đặt cert/key đúng domain ở `ops/tls/fullchain.pem`, `privkey.pem`; TLS 1.2/1.3, không bypass warnings. Nếu dùng managed TLS/LB, vẫn giữ backend/DB/metrics private và rà trust proxy.
5. Template chỉ publish 443. DB network internal; backend có outbound network gọi Firebase. JDBC không TLS trong network Docker riêng **cùng host**; DB remote/multi-host phải cấu hình CA/JDBC verify-identity, không dùng nguyên mẫu này.
6. Backup và thử migration trên bản sao. Runtime user giới hạn trong schema nhưng hiện cần DDL để Flyway startup; tách migration credential/runtime DML là bước hardening thêm khi vận hành thật.

```bash
docker compose --env-file ops/.env.production -f ops/compose.production.yml config --quiet
docker compose --env-file ops/.env.production -f ops/compose.production.yml up -d
docker compose --env-file ops/.env.production -f ops/compose.production.yml ps
```

Compose secret file không phải secret manager, không tự mã hóa host disk. Kiểm tra firewall/cloud chỉ mở TLS và SSH hạn chế; không bật phpMyAdmin production. Provision secret rotation, disk alerts, cert renewal và backup off-host theo hạ tầng thực. Nginx không tự xin/gia hạn cert và chưa có ACME/HTTP redirect listener.

Admin/staff/Firebase/bàn/ngân hàng làm theo [runbook tuần 4–5](weeks-4-5-security-operations.md). Prod chưa có bàn thì không nhận booking. Giao dịch UAT nhỏ chỉ thực hiện khi chủ nhà hàng cho phép.

## Rate limit và Android

Nginx theo IP kết nối: API 10 req/s burst 20, auth 5 req/phút burst 10, vượt trả 429. Sau CDN/LB có thể thấy IP proxy; chỉ trust real-IP từ CIDR proxy đã xác minh, không tin header bất kỳ. Template không mở trust X-Forwarded-For.

Backend gate fixed-window một phút trước Firebase, map tối đa 10.000 địa chỉ. Prod mặc định 120/phút/địa chỉ; Compose nâng 3.000 vì backend thấy IP Nginx: đây là aggregate cap mỗi replica. Không dùng token/UID chưa xác minh làm key. Instance-local, có burst ở ranh giới phút, không phải distributed limiter. Tune theo traffic và dùng edge/WAF. Backend 429 có Retry-After/RATE_LIMITED/request ID; edge 429 có thể HTML mặc định. Probes/static không qua API gate.

Android release chỉ HTTPS/system CA; debug resource override cho emulator/LAN. Release URL guard chặn HTTP, `.invalid`, credential/query/fragment và thiếu `/` cuối. Phải cấu hình staging HTTPS thật trước assembleRelease; APK unsigned không phải store release. Backup rules chặn cloud/device transfer dữ liệu tài khoản; nghiệm thu trên thiết bị/OEM thật.

## Log, health và metrics

Production JSON Logstash stdout. Filter cấp X-Request-ID UUID, MDC requestId; access log chỉ method chuẩn hóa, route_group, status, duration_ms, không URL/query/body/token/email/UID. Unexpected API error log loại exception, không raw message/SQL/cause. Dùng audit DB cho tài chính/quyền. Nginx access log tối giản; error log Nginx/framework có thể chứa context khác: bảo vệ log storage, retention và rà redaction trước public. Không bật TRACE/debug request logging production.

Management prod listener 8081, không host publish. Readiness gồm DB; liveness không phụ thuộc DB để tránh restart loop. Health không lộ components. Public proxy chặn actuator/Swagger/OpenAPI. Prometheus chỉ nhận MONITORING_TOKEN riêng ≥32 ký tự; token ngắn/trống deny. Monitoring token không provision Firebase user hoặc cấp quyền business; Firebase admin token không thay token monitoring.

`ops/monitoring/prometheus.example.yml` + `alerts.yml` dành cho Prometheus private có thể tới backend:8081. Token qua secret file, không URL/Git. Chưa cài Prometheus/Alertmanager trên server thật: cần destination cảnh báo, UI private và test firing/recovery.

Gauges business cache 30 giây, scrape không scan DB. Snapshot chưa có age -1; lỗi query giữ snapshot cũ và tăng error counter. Rule gồm mất scrape, API 5xx >5%, delivery FAILED, khoản chờ hoàn, hold quá hạn, snapshot stale. Refund metric là count, **không phải tuổi từng khoản**. Bổ sung disk/DB/cert-expiry/backup-age ở hạ tầng thật; rule template không có nghĩa monitor đã hoạt động.

## Backup mã hóa

Mục tiêu ban đầu RPO ≤24h (backup hàng ngày), RTO ≤2h sau khi có host/backup/key; đây là mục tiêu **cần đo**, chưa cam kết từ fixture. Chưa có PITR/binlog shipping. DB và uploads không chung transaction: freeze migration/menu uploads hoặc maintenance window để nhất quán ảnh/DB.

Build helper từ Alpine/age package repository chính thức. Private age identity giữ offline/secret manager khác host backup; backup chỉ cần public recipient. Client CNF owner-only, backup user chỉ có quyền dump cần thiết; restore credential chỉ ghi target schema. **Không nhận SQL/backup từ nguồn không tin cậy**: checksum không xác thực người tạo và age public recipient không phải chữ ký người gửi.

```bash
docker build -t golden-leaf-backup-crypto:local ops/backup
# CNF: [client] host=db, user=<backup-user>, password=<secret>
export MYSQL_CNF=/restricted/mysql-backup.cnf
export DOCKER_NETWORK=golden-leaf-production_database
export DB_NAME=datban_db
export BACKUP_RECIPIENT=age1YOUR_PUBLIC_RECIPIENT
export BACKUP_DIR=/restricted/golden-leaf-backups
export UPLOADS_VOLUME=golden-leaf-production_uploads_data
bash ops/scripts/backup.sh
```

Tạo sql.gz.age, tùy chọn uploads.tar.gz.age, mỗi file có SHA256. Dump single-transaction/quick/no-tablespaces không ghi plaintext xuống host. Không DDL đồng thời; không giả định snapshot nhất quán cho nontransactional tables. Pipeline fail thì lượt backup fail, kể cả DB xong nhưng uploads chưa xong.

Copy encrypted files off-host tới destination được chủ hệ thống duyệt, ACL hạn chế, checksum/manifest trong kênh tin cậy. Đề xuất 7 daily + 4 weekly + 3 monthly sau khi đánh giá chính sách nhà hàng. Script **không tự xóa retention, tạo cron hoặc upload dữ liệu khách**. Operator cấu hình lịch/backup-age alert (gợi ý >26h), rotate key và key dự phòng.

## Restore và rollback

DBA tạo schema riêng suffix `_restore_<name>` và credential chỉ được ghi vào schema đó. Script yêu cầu exact confirmation, kiểm checksum + giải mã/gzip toàn bộ trước writes, từ chối DB không rỗng/không tồn tại/tên live. Không DROP/TRUNCATE/reset/promote production tự động.

```bash
export MYSQL_CNF=/restricted/mysql-restore.cnf
export DOCKER_NETWORK=golden-leaf-production_database
export DB_NAME=datban_restore_drill
export RESTORE_CONFIRM=datban_restore_drill
export BACKUP_FILE=/restricted/golden-leaf-backups/your-backup.sql.gz.age
export AGE_IDENTITY=/separate-restricted/location/age-identity
bash ops/scripts/restore.sh
```

SQL restore không nguyên tử toàn dump; thất bại giữa chừng phải giữ target cô lập và DBA tạo schema mới cho lượt tiếp, không ghi đè/drop live. So row counts, tiền DECIMAL, Flyway history, capacity, payments/audit/inbox. Chạy staging trên restored DB với **push worker disabled** để không gửi lại push thật. Không down-migrate V1–V4.

Đặt `PUSH_DELIVERY_ENABLED=false` trên staging dùng DB restore: vẫn xác thực Firebase và giữ inbox, nhưng không chạy job gửi push. Không dùng Firebase project/token thiết bị khách thật để thử delivery trên dữ liệu khôi phục; không mở staging cho người dùng thật.

Uploads archive sau kiểm checksum được giải mã vào **volume mới/rỗng**, container cô lập; không extract lên host/broad directory/volume live. Kiểm filenames/URL với DB trước đổi volume. Script DB không tự restore/promote uploads: operator phải chọn rõ đích và rà archive. Không bỏ qua ảnh vì DB đã restore.

Phát hành/rollback:

1. Chốt revision, required CI xanh, image/dependency scan, UAT staging và approve operator.
2. Backup DB+uploads, ghi SHA/revision/schema; maintenance khi migration không compatible.
3. Set image SHA mới, apply Compose, chờ readiness. Migration fail không nhận traffic.
4. `BASE_URL=https://your-domain bash ops/scripts/smoke.sh`: menu reachable, staff 401, public metrics 404. UAT auth/booking/payment/FCM riêng được phê duyệt.
5. Theo dõi 30 phút lỗi/latency/queue/DB, xác nhận kế toán; giữ image cũ/backup.
6. Rollback image SHA cũ **chỉ nếu schema tương thích**, chạy smoke. Không restore đè DB để quay về: cần maintenance/DBA recovery và reconcile tiền thực nhận sau backup; tiền ngân hàng không rollback cùng DB.

Readonly staging load probe: `BASE_URL=https://staging-domain REQUESTS=100 CONCURRENCY=5 node ops/scripts/load-smoke.mjs`. Bound 1.000 request/20 concurrency; HTTP chỉ localhost. Không bearer/PII/booking/tiền. Fail/429 báo riêng; không chứng nhận tải production.

## Nghiệm thu

Local verify gồm toàn bộ H2/MySQL + hardening/metrics/log + management **HTTP port thật**, Android unit/build/lint. `bash ops/scripts/test-backup-restore.sh` dùng tmpfs DB/network/volume riêng có nhãn, dọn fixture sau test, không dữ liệu nhà hàng. Test fake identity không thay Firebase live.

Kết quả kiểm chứng local ngày 08/10/2026:

- Backend `mvnw verify`: **165 tests**, không failure/error/skip, gồm H2 và MySQL disposable.
- Android `testDebugUnitTest assembleDebug lintDebug`: **10 unit tests đạt**, debug APK build thành công; lint **0 errors, 93 warnings, 7 hints**. Release manifest HTTPS-only/không backup đã kiểm tra; URL HTTP bị build guard từ chối.
- Workflow/actionlint, ShellCheck, Bash syntax, JavaScript syntax và Compose config đạt; backend Docker image build thành công.
- Backup drill đạt: DB DECIMAL và uploads round-trip; từ chối ciphertext hỏng, identity sai, schema live và target không rỗng.
- Container drill đạt: non-root/read-only, uploads writable, metrics private, HTTPS xác minh bằng CA fixture, chặn management public, staff anonymous bị 401 và HSTS có mặt.
- Probe local 100 request/concurrency 5: **0 failures, 0 throttled**, p50 38 ms/p95 148 ms. Số này chỉ mô tả lượt fixture trên máy local, không cam kết tải production.

CI remote cần chạy trên revision đã push và được kiểm tra riêng; kết quả local không thay required checks trên GitHub. Checklist dưới đây là gate vận hành thật, không phải công việc đã tự động hoàn tất bằng fixture.

- [ ] Host/domain/cert renewal, firewall, DB/metrics private.
- [ ] Actions/required checks/review, nguồn artifact, image/dependency scan.
- [ ] Secrets/monitor token/key rotation và quyền đọc.
- [ ] Firebase/admin/staff/bank/bàn nghiệm thu tuần 4–5.
- [ ] Backup off-host theo lịch, backup-age/disk/cert/DB alerts; restore DB+uploads từ backup thật, đo RTO.
- [ ] Prometheus/Alertmanager private, alert đến đúng người trực.
- [ ] Signed HTTPS Android, thiết bị/OEM backup restriction và FCM background.
- [ ] UAT hủy/tiền muộn/hoàn tiền, staging load, schema compatibility/rollback và operator ký nhận.

## Nguồn chuẩn

- [Spring Boot logging](https://docs.spring.io/spring-boot/3.5/reference/features/logging.html), [Actuator](https://docs.spring.io/spring-boot/3.5/reference/actuator/endpoints.html).
- [GitHub Actions secure use](https://docs.github.com/en/actions/reference/security/secure-use), [Nginx limiting](https://nginx.org/en/docs/http/ngx_http_limit_req_module.html).
- [MySQL mysqldump](https://dev.mysql.com/doc/refman/8.0/en/mysqldump.html), [age](https://github.com/FiloSottile/age), [Android network security](https://developer.android.com/privacy-and-security/security-config).
