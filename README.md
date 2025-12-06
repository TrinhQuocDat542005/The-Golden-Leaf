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
User APIs

POST /api/auth/login

POST /api/auth/register

Menu APIs

GET /api/thucdon

GET /api/mon/{id}

POST /api/danhgia

Booking APIs

POST /api/book

GET /api/book/user/{id}

POST /api/book/cancel/{id}

Table APIs

GET /api/tables

POST /api/tables/assign

POST /api/tables/release

Notification APIs

GET /api/notifications?userId=...

POST /api/notifications/{id}/read
