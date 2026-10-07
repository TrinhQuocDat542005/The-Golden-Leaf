# Database schema

The Golden Leaf uses MySQL in development/production and H2 in MySQL mode for automated tests. Flyway is the only component allowed to create or change the schema; Hibernate runs with `ddl-auto=validate`.

## Migration policy

- Migrations live in `The-Golden-Leaf-server/src/main/resources/db/migration`.
- Never edit an applied migration. Add a new `V{n}__description.sql` file instead.
- Deployments run Flyway before Hibernate starts. A migration failure stops the application.
- Timestamps are stored and serialized in UTC.
- A database created by the pre-Flyway prototype must be backed up and migrated or recreated before using this version. `V1` targets a clean schema.

## Main data model

| Domain | Tables | Purpose |
|---|---|---|
| Identity | `users`, `roles`, `user_roles`, `device_tokens` | Firebase identities, authorization and push devices |
| Menu | `menu_categories`, `menu_items`, `favorites`, `reviews` | Menu catalog and customer engagement |
| Restaurant | `restaurant_areas`, `restaurant_tables`, `time_slots` | Physical inventory and per-slot capacity |
| Booking | `bookings`, `booking_tables`, `booking_items` | Booking lifecycle, assigned tables and ordered dishes |
| Billing | `invoices`, `payments` | Immutable invoice totals and payment attempts |
| Operations | `notifications`, `notification_deliveries`, `audit_logs` | Customer inbox, durable per-device push outbox and traceability |

## Important invariants

- Monetary columns use `DECIMAL`, and Java uses `BigDecimal`; floating-point types are not used for persisted money.
- `time_slots` is unique by `(booking_date, slot_label)` and uses optimistic locking through `version`.
- Booking writes use `READ_COMMITTED` transactions and acquire a pessimistic slot lock before a booking lock, so waiting requests read freshly committed cart/invoice data on MySQL as well as H2.
- `V2` adds a unique nullable booking idempotency key, reserved table count, expiry lookup index and guest/capacity check constraints.
- `HOLDING`, `CONFIRMED`, `ASSIGNED` and `SEATED` bookings retain reserved inventory. Expiry, eligible cancellation or completion releases it once; terminal retries never release twice.
- One invoice exists at most once per booking.
- Payment provider references are unique within a provider.
- Favorites and reviews cannot be duplicated for the same user/menu item pair.
- Foreign keys protect booking, menu, invoice and payment relationships.

The executable source of truth is [`V1__initial_production_schema.sql`](../The-Golden-Leaf-server/src/main/resources/db/migration/V1__initial_production_schema.sql).

Booking integrity additions are in [`V2__booking_integrity.sql`](../The-Golden-Leaf-server/src/main/resources/db/migration/V2__booking_integrity.sql).

## Upgrading V1 data

V2 does not infer holds or reserved inventory for existing bookings: they retain their legacy status (`DRAFT` by default), with `reserved_tables=0` and no idempotency key. Reconcile legacy bookings and their slot counts before using them in the new lifecycle. Do not blindly overwrite remaining capacity while active reservations exist. V2 stops if historical data violates its new guest-count or capacity constraints; inspect and reconcile that data first.

Historical slots are retained for lifecycle actions and audit. Initialization only inserts missing slots and refreshes the seven-day window each restaurant-local midnight; it never deletes historical inventory or resets remaining capacity.

## Operations migrations V3 / V4

[V3](../The-Golden-Leaf-server/src/main/resources/db/migration/V3__operations_and_delivery.sql) adds unique nullable `transfer_reference` and `refund_reference` bank evidence to payments, an event key to notifications, and `notification_deliveries`. Each delivery is unique by notification/device; status, attempts, next_attempt_at, lease_until, lease_key and last_error support durable retry and crashed-worker recovery. No network request runs inside the claim/finalization transaction. Deliveries are at-least-once, not guaranteed exactly once.

[V4](../The-Golden-Leaf-server/src/main/resources/db/migration/V4__payment_destination_snapshot.sql) adds receiving bank/account/name to payments. New intents snapshot their destination; later environment changes do not silently redirect existing instructions. Historical null snapshots are not guessed or backfilled from current configuration; reconcile them manually before use.

Booking ownership now populates the existing `bookings.user_uid` using authenticated Firebase identity. Legacy null ownership retains verified-email fallback, not client email authority. Roles and disabled status are database controlled; no migration seeds privileged users, credentials or fictitious physical tables.

Business transitions insert audit/inbox/outbox rows in the same transaction as the booking/payment change. Rollbacks cannot leave a false paid notification. Notifications stay as durable inbox records even when Firebase delivery is disabled or a token fails. Device ownership changes discard old-account pending deliveries; invalid registrations are deactivated.

Physical allocations use `booking_tables.released_at`; slot-first locking serializes competing operations for the same date/time. Production initialization derives capacity from active AVAILABLE tables with at least eight seats in active areas. Reconciliation preserves `initial_capacity - remaining_capacity` and refuses reductions below reservations. Back up and reconcile prototype bookings/inventory before upgrading a live database; never blindly reset counts or edit applied migrations.
