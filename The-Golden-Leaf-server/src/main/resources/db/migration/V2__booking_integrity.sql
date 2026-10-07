ALTER TABLE bookings ADD COLUMN idempotency_key VARCHAR(128) NULL;
ALTER TABLE bookings ADD COLUMN reserved_tables INT NOT NULL DEFAULT 0;
ALTER TABLE bookings ADD CONSTRAINT uk_bookings_idempotency UNIQUE (idempotency_key);
ALTER TABLE bookings ADD CONSTRAINT ck_bookings_guest_count CHECK (guest_count BETWEEN 1 AND 80);
ALTER TABLE bookings ADD CONSTRAINT ck_bookings_reserved_tables CHECK (reserved_tables >= 0);
ALTER TABLE time_slots ADD CONSTRAINT ck_slot_capacity CHECK (
    initial_table_count >= 0 AND remaining_table_count >= 0 AND remaining_table_count <= initial_table_count
);
CREATE INDEX idx_bookings_expiry ON bookings(status, hold_expires_at);
