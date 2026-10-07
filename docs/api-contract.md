# REST API contract

The API keeps the existing Vietnamese JSON property names so the Android application remains compatible, while persistence entities are no longer returned directly from REST controllers.

## Documentation

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Both endpoints are public even when Firebase authentication is enabled. Protected business endpoints accept `Authorization: Bearer <Firebase ID token>`.

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
5. Cancel via `POST /api/datban/{id}/cancel`. `HOLDING` or `CONFIRMED` transitions to `CANCELLED` and releases reserved capacity once. Expired holds transition to `EXPIRED`; the sweep runs every 60 seconds and creation also reclaims expired holds in its slot.

Confirm and cancel retries are idempotent within their terminal state. Confirm after cancellation/expiry is rejected. Confirming at the exact hold deadline is rejected. Creation accepts the next seven restaurant-local calendar days and only slots that have not started. Restaurant timezone defaults to `Asia/Ho_Chi_Minh`; expiry timestamps are UTC.

`BookingResponse` now also includes `status`, nullable `holdExpiresAt` and `reservedTables`. Mobile should keep the returned `idDat` and read `/api/datban/{id}` for the current flow. `/latest` is retained for older callers and must not be used to associate a cart/invoice with a new booking.

When Firebase protection is enabled, the authenticated email must own every booking being created, read by ID, confirmed, cancelled, edited or invoiced. Local `REQUIRE_AUTH=false` remains an explicitly unprotected development mode.

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
