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
| `POST` | `/api/ban-slot/dat` | query: `ngay`, `khungGio`, `soLuongKhach` | `TimeSlotResponse` |
| `POST` | `/api/ban-slot/tra` | query: `ngay`, `khungGio`, `soLuongKhach` | `TimeSlotResponse` |
| `POST` | `/api/datban/save` | `BookingCreateRequest` | `BookingResponse` |
| `GET` | `/api/datban/latest` | — | `BookingResponse` |
| `POST` | `/api/giohang/datmon` | `BookingItemRequest[]` | `BookingItemResponse[]` |
| `POST` | `/api/hoadon/create` | `HoaDonRequest` | `InvoiceResponse` |

Menu prices in an order are read from the database by the server. A client-provided price is accepted only for backward-compatible request parsing and is not trusted. Invoice totals must equal table fee plus food total.

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
