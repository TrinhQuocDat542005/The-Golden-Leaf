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
| Operations | `notifications`, `audit_logs` | Customer notifications and traceability |

## Important invariants

- Monetary columns use `DECIMAL`, and Java uses `BigDecimal`; floating-point types are not used for persisted money.
- `time_slots` is unique by `(booking_date, slot_label)` and uses optimistic locking through `version`.
- One invoice exists at most once per booking.
- Payment provider references are unique within a provider.
- Favorites and reviews cannot be duplicated for the same user/menu item pair.
- Foreign keys protect booking, menu, invoice and payment relationships.

The executable source of truth is [`V1__initial_production_schema.sql`](../The-Golden-Leaf-server/src/main/resources/db/migration/V1__initial_production_schema.sql).
