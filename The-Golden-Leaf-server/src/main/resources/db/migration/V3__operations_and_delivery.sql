ALTER TABLE payments ADD COLUMN transfer_reference VARCHAR(128) NULL;
ALTER TABLE payments ADD COLUMN refund_reference VARCHAR(128) NULL;
ALTER TABLE payments ADD CONSTRAINT uk_payment_transfer UNIQUE (transfer_reference);
ALTER TABLE payments ADD CONSTRAINT uk_payment_refund UNIQUE (refund_reference);
ALTER TABLE notifications ADD COLUMN event_key VARCHAR(128) NULL;
ALTER TABLE notifications ADD CONSTRAINT uk_notification_event UNIQUE (event_key);

CREATE TABLE notification_deliveries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    notification_id BIGINT NOT NULL,
    device_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    lease_until TIMESTAMP(6) NULL,
    lease_key VARCHAR(64) NULL,
    last_error VARCHAR(100) NULL,
    CONSTRAINT uk_delivery_notification_device UNIQUE (notification_id, device_id),
    CONSTRAINT fk_delivery_notification FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE,
    CONSTRAINT fk_delivery_device FOREIGN KEY (device_id) REFERENCES device_tokens(id) ON DELETE CASCADE
);
CREATE INDEX idx_delivery_pending ON notification_deliveries(status, next_attempt_at, lease_until);
