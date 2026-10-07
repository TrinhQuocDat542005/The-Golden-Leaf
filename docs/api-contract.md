# REST API contract

The API keeps the existing Vietnamese JSON property names so the Android application remains compatible, while persistence entities are no longer returned directly from REST controllers.

## Documentation

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Documentation is available in local development; production requires authentication. Protected endpoints accept `Authorization: Bearer <Firebase ID token>`. Tokens are verified including revocation, verified email and active account; roles are read from the database on each request.

## Core endpoints

| Method | Path | Request | Response |
|---|---|---|---|
| `GET` | `/api/thucdon` | — | `MenuItemResponse[]` |
| `GET` | `/api/thucdon/{id}` | — | `MenuItemResponse` |
| `GET` | `/api/ban-slot` | — | `TimeSlotResponse[]` |
| `POST` | `/api/ban-slot/dat`, `/api/ban-slot/tra` | legacy parameters | `409 BOOKING_REQUIRED` (no inventory mutation) |
| `POST` | `/api/datban/save` | `BookingCreateRequest`, required `Idempotency-Key` header | `BookingResponse` |
| `GET` | `/api/datban/{id}` | booking ID | `BookingResponse` |
| `POST` | `/api/datban/{id}/confirm` | booking ID | `BookingResponse` |
| `POST` | `/api/datban/{id}/cancel` | booking ID | `BookingResponse` |
| `GET` | `/api/datban/latest` | — | `BookingResponse` |
| `POST` | `/api/giohang/datmon` | `BookingItemRequest[]` | `BookingItemResponse[]` |
| `PUT` | `/api/giohang/{idDat}` | entire `BookingItemRequest[]`, may be empty | `BookingItemResponse[]` |
| `POST` | `/api/hoadon/create` | `HoaDonRequest` | `InvoiceResponse` |

Menu prices and names are read from the database by the server. Client-provided values are accepted only for backward-compatible parsing and are not trusted. Cart writes replace the entire snapshot for one booking, with at most 100 distinct dishes and quantity 1–100 per dish. A retry with unchanged item IDs and quantities preserves the original price snapshot. Cart edits are allowed only during an unexpired hold; an identical confirmed cart can be replayed without writes.

Invoice amounts are checked against the server's configured table fee (default 200,000 VND) and stored cart line totals. Forged amounts return `409 INVALID_INVOICE_TOTAL`. The booking must be confirmed first. Concurrent invoice retries return the same invoice when amounts match. Invoice creation does **not** verify payment; `ngayGioThanhToan` is a legacy JSON field containing the invoice creation timestamp.

## Booking lifecycle and retries

1. Create with a unique `Idempotency-Key` (1–128 characters: letters, digits, `_`, `-`; normalized to lowercase). Use the same key and payload for all retries of one intent. New content needs a new key. Keys are unique across bookings; changed content returns `409 IDEMPOTENCY_CONFLICT`, or a database conflict if two different slots race on the same key.
2. Creation atomically holds `ceil(guestCount / 8)` tables and persists a `HOLDING` booking, with a 15-minute expiry by default. Capacity is aggregate per date/slot; the selected area is a preference, not a physical table assignment.
3. Replace the cart with `PUT /api/giohang/{idDat}` (use `[]` to book without dishes).
4. Confirm via `POST /api/datban/{id}/confirm`. This changes `HOLDING` to `CONFIRMED`, clears the hold deadline and freezes the cart. It confirms the reservation, **not** a payment or staff table assignment.
5. Cancel via `POST /api/datban/{id}/cancel`. `HOLDING`, `CONFIRMED` or `ASSIGNED` transitions to `CANCELLED` and releases reserved capacity/assigned tables once. Paid bookings require a refund; pending payments are cancelled. Expired holds transition to `EXPIRED`; the sweep runs every 60 seconds and creation also reclaims expired holds in its slot.

Confirm and cancel retries are idempotent within their terminal state. Confirm after cancellation/expiry is rejected. Confirming at the exact hold deadline is rejected. Creation accepts the next seven restaurant-local calendar days and only slots that have not started. Restaurant timezone defaults to `Asia/Ho_Chi_Minh`; expiry timestamps are UTC.

`BookingResponse` now also includes `status`, nullable `holdExpiresAt` and `reservedTables`. Mobile should keep the returned `idDat` and read `/api/datban/{id}` for the current flow. `/latest` is retained for older callers and must not be used to associate a cart/invoice with a new booking.

Authenticated booking creation binds the server-verified Firebase UID. Ownership uses that UID, not a supplied email; old rows with no UID use verified email as a compatibility fallback. Local `REQUIRE_AUTH=false` leaves some legacy endpoints unprotected for development, but never grants access to payment, account, device, notification, staff or admin routes.

## Error shape

Validation, not-found and business-rule errors use one JSON shape:

```json
{
  "timestamp": "2026-10-07T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_FAILED",
  "message": "Dữ liệu gửi lên không hợp lệ",
  "path": "/api/datban/save",
  "fieldErrors": {
    "email": "must be a well-formed email address"
  }
}
```

Stable codes currently include `VALIDATION_FAILED`, `MALFORMED_JSON`, `RESOURCE_NOT_FOUND`,
`NOT_ENOUGH_TABLES`, `INVALID_INVOICE_TOTAL`, `DATA_CONFLICT`, `CONCURRENT_UPDATE` and `INTERNAL_ERROR`.

Booking codes also include `INVALID_IDEMPOTENCY_KEY`, `IDEMPOTENCY_CONFLICT`, `INVALID_BOOKING_DATE`,
`INVALID_BOOKING_TIME`, `INVALID_GUEST_COUNT`, `INVALID_BOOKING_STATE`, `BOOKING_HOLD_EXPIRED`,
`BOOKING_REQUIRED`, `CAPACITY_CONFLICT`, `INVALID_CART`, `MENU_ITEM_UNAVAILABLE` and `ACCESS_DENIED`.
Missing headers, invalid query parameters and invalid cart elements return `400 VALIDATION_FAILED`.

## Week 4–5 protected endpoints

CUSTOMER endpoints also accept staff/admin tokens, but still enforce ownership of customer data. STAFF routes accept STAFF or ADMIN; ADMIN routes require ADMIN. Legacy `/nhahang/**` reads require STAFF/ADMIN and writes require ADMIN. All new routes below require bearer authentication except auth sync (which verifies its body token) and public web config.

| Method | Path | Role / purpose |
|---|---|---|
| POST | `/api/auth/sync` | `{idToken}`; verified Firebase identity sync, no client role assignment |
| GET | `/api/auth/me` | Authenticated `{uid,email,roles}` |
| GET | `/api/auth/web-config` | Public `{apiKey}`; Firebase Web API key, never service-account credentials |
| GET | `/api/payments/bookings/{id}/quote` | Owner; `{idDat,tienBan,tienAn,tongTien,currency}`, read-only preview |
| POST / GET | `/api/payments/bookings/{id}` | Owner; create/replay intent / read existing payment |
| GET | `/api/taikhoan/choXacNhan`, `/api/taikhoan/lichSuDonDat` | Owner feed, each up to 100 |
| GET | `/api/dondat/{id}` | Owner detail with item snapshots, status and paymentStatus |
| DELETE | `/api/taikhoan/huyDon/{id}` | Owner cancellation |
| GET | `/api/notifications` | Own last 100 notifications |
| GET | `/api/notifications/unread-count` | Own unread count across the entire feed, `{count}` |
| POST | `/api/notifications/{id}/read` | Own notification, idempotent read flag |
| POST / DELETE | `/api/devices` | Register / unregister own FCM device; JSON `{token}` |
| GET | `/api/staff/bookings?date=YYYY-MM-DD` | STAFF; active queue, optional date, up to 200 |
| GET | `/api/staff/bookings/{id}` | STAFF; booking, items, invoices, payments and table history, including closed orders |
| GET | `/api/staff/tables`, `/api/staff/areas`, `/api/staff/allocations` | STAFF; physical inventory and active allocations (up to 2,000) |
| POST | `/api/staff/bookings/{id}/assign` | STAFF; `{tableIds:[1,2]}`, exact reserved-table count and sufficient seats |
| POST | `/api/staff/bookings/{id}/check-in`, `/complete`, `/cancel` | STAFF; lifecycle actions |
| POST | `/api/staff/bookings/{id}/verify-payment`, `/verify-refund` | STAFF; `{reference,amount}`, exact total and actual bank transaction ID |
| GET | `/api/staff/refunds`, `/api/staff/deliveries` | STAFF; up to 200 pending refunds / recent deliveries |
| POST | `/api/staff/deliveries/{id}/retry` | STAFF; FAILED delivery only, audited reset |
| POST | `/api/admin/tables` | ADMIN; `{areaId,code,capacity}`, capacity 1–80; production inventory units require 8+ seats |
| POST | `/api/admin/inventory/reconcile` | ADMIN; preserve reserved capacity across the current seven-day window |
| POST | `/api/admin/roles` | ADMIN; `{uid,role:"STAFF" or "ADMIN",grant:true or false}` |
| GET | `/api/admin/users`, `/api/admin/audit` | ADMIN; up to 200 users / audit events |
| PUT | `/api/admin/users/{uid}/status` | ADMIN; `{active:true or false}`, protects last active admin |
| GET / POST | `/api/admin/menu` | ADMIN; catalog list / validated menu creation |
| PUT | `/api/admin/menu/{id}` | ADMIN; update, including active flag; existing booking prices stay immutable |
| POST | `/api/admin/menu/image` | ADMIN; multipart `image`, PNG/JPEG ≤5 MB, dimensions ≤4096, returns `{url}` |

Payment response: `{id,bookingId,amount,currency,status,reference,bankName,accountNumber,accountName,paidAt}`. POST computes and freezes the invoice on the server, creates one BANK_TRANSFER intent per booking, snapshots the configured bank destination, and emits a PENDING notification. GET never creates an invoice or payment. Empty bank configuration returns `409 PAYMENT_NOT_CONFIGURED`.

Payment lifecycle: `PENDING → PAID`; cancellation changes PENDING to CANCELLED or PAID to REFUND_REQUIRED; staff records a full outgoing refund to reach REFUNDED. A real transfer arriving after cancellation changes CANCELLED payment to REFUND_REQUIRED, never reopens the booking. Receipt/refund references are normalized uppercase, 4–128 ASCII letters/digits/underscore/hyphen and independently unique. Matching retries are idempotent; changed amounts/references are rejected. The API records bank evidence checked by staff; it does **not** move money or contact a payment gateway.

Booking operations: `CONFIRMED → ASSIGNED → SEATED → COMPLETED`. Check-in requires PAID and the booking's date in the configured restaurant timezone. Completion releases inventory once. SEATED/COMPLETED cannot be customer-cancelled. Area selection remains a preference; staff chooses the actual tables. Production rejects inventory that has not been reconciled with actual active eight-seat-or-larger tables.

Menu create/update JSON: `{tenMon,gia,moTa,anh,nhom,active}`, with groups KHAI_VI/MON_CHINH/TRANG_MIENG, nonnegative DECIMAL-compatible price and safe HTTPS or generated `/uploads/` image path. Uploaded images are re-encoded with UUID filenames; original filenames are never filesystem targets.

Security errors use the same envelope: `401 UNAUTHENTICATED`, `403 ACCESS_DENIED`, `503 AUTH_UNAVAILABLE`. Operations errors include `PAYMENT_NOT_CONFIGURED`, `PAYMENT_CONFLICT`, `AMOUNT_MISMATCH`, `INVALID_TRANSFER_REFERENCE`, `INVALID_PAYMENT_STATE`, `PAYMENT_REQUIRED`, `TABLE_CONFLICT`, `INSUFFICIENT_SEATS`, `INVENTORY_NOT_CONFIGURED`, `CAPACITY_CONFLICT`, `INVALID_CHECKIN_DATE`, `LAST_ADMIN` and `INVALID_DELIVERY_STATE`. Inaccessible customer data is denied; another user's notification read returns not found.
