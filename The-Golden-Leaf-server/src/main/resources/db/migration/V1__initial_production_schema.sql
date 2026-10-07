CREATE TABLE users (
    uid VARCHAR(128) PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    firebase_provider VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(100) NOT NULL,
    CONSTRAINT uk_roles_code UNIQUE (code)
);

CREATE TABLE user_roles (
    user_uid VARCHAR(128) NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_uid, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_uid) REFERENCES users(uid) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE TABLE menu_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_menu_categories_code UNIQUE (code)
);

CREATE TABLE menu_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id BIGINT NULL,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(12, 2) NOT NULL,
    description TEXT NULL,
    image_url VARCHAR(512) NULL,
    legacy_group VARCHAR(32) NULL,
    featured BOOLEAN NOT NULL DEFAULT FALSE,
    new_item BOOLEAN NOT NULL DEFAULT FALSE,
    discount_percent DECIMAL(5, 2) NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_menu_items_category FOREIGN KEY (category_id) REFERENCES menu_categories(id)
);

CREATE INDEX idx_menu_items_category_active ON menu_items(category_id, active);
CREATE INDEX idx_menu_items_name ON menu_items(name);

CREATE TABLE favorites (
    user_uid VARCHAR(128) NOT NULL,
    menu_item_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (user_uid, menu_item_id),
    CONSTRAINT fk_favorites_user FOREIGN KEY (user_uid) REFERENCES users(uid) ON DELETE CASCADE,
    CONSTRAINT fk_favorites_menu_item FOREIGN KEY (menu_item_id) REFERENCES menu_items(id) ON DELETE CASCADE
);

CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_uid VARCHAR(128) NOT NULL,
    menu_item_id BIGINT NOT NULL,
    rating INT NOT NULL,
    content TEXT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_reviews_user_item UNIQUE (user_uid, menu_item_id),
    CONSTRAINT fk_reviews_user FOREIGN KEY (user_uid) REFERENCES users(uid) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_menu_item FOREIGN KEY (menu_item_id) REFERENCES menu_items(id) ON DELETE CASCADE
);

CREATE INDEX idx_reviews_menu_item_status ON reviews(menu_item_id, status);

CREATE TABLE restaurant_areas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_restaurant_areas_code UNIQUE (code)
);

CREATE TABLE restaurant_tables (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    area_id BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL,
    capacity INT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'AVAILABLE',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_restaurant_tables_code UNIQUE (code),
    CONSTRAINT fk_restaurant_tables_area FOREIGN KEY (area_id) REFERENCES restaurant_areas(id)
);

CREATE INDEX idx_restaurant_tables_area_status ON restaurant_tables(area_id, status, active);

CREATE TABLE time_slots (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_date DATE NOT NULL,
    slot_label VARCHAR(32) NOT NULL,
    start_time TIME NULL,
    end_time TIME NULL,
    initial_table_count INT NOT NULL,
    remaining_table_count INT NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_time_slots_date_label UNIQUE (booking_date, slot_label)
);

CREATE INDEX idx_time_slots_date ON time_slots(booking_date);

CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_uid VARCHAR(128) NULL,
    customer_email VARCHAR(255) NOT NULL,
    customer_name VARCHAR(255) NOT NULL,
    booking_date DATE NOT NULL,
    slot_label VARCHAR(32) NOT NULL,
    guest_count INT NOT NULL,
    note VARCHAR(1000) NULL,
    preferred_area_id BIGINT NULL,
    preferred_area_name VARCHAR(100) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    hold_expires_at TIMESTAMP(6) NULL,
    cancellation_reason VARCHAR(500) NULL,
    cancelled_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_bookings_user FOREIGN KEY (user_uid) REFERENCES users(uid),
    CONSTRAINT fk_bookings_area FOREIGN KEY (preferred_area_id) REFERENCES restaurant_areas(id)
);

CREATE INDEX idx_bookings_user_created ON bookings(user_uid, created_at);
CREATE INDEX idx_bookings_email_created ON bookings(customer_email, created_at);
CREATE INDEX idx_bookings_date_slot_status ON bookings(booking_date, slot_label, status);

CREATE TABLE booking_tables (
    booking_id BIGINT NOT NULL,
    table_id BIGINT NOT NULL,
    assigned_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    released_at TIMESTAMP(6) NULL,
    PRIMARY KEY (booking_id, table_id),
    CONSTRAINT fk_booking_tables_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_tables_table FOREIGN KEY (table_id) REFERENCES restaurant_tables(id)
);

CREATE INDEX idx_booking_tables_table_active ON booking_tables(table_id, released_at);

CREATE TABLE booking_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    menu_item_id BIGINT NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(12, 2) NOT NULL,
    line_total DECIMAL(12, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_booking_items_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE,
    CONSTRAINT fk_booking_items_menu_item FOREIGN KEY (menu_item_id) REFERENCES menu_items(id)
);

CREATE INDEX idx_booking_items_booking ON booking_items(booking_id);

CREATE TABLE invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    table_fee DECIMAL(12, 2) NOT NULL,
    food_total DECIMAL(12, 2) NOT NULL,
    grand_total DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_invoices_booking UNIQUE (booking_id),
    CONSTRAINT fk_invoices_booking FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

CREATE TABLE payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    provider VARCHAR(32) NOT NULL,
    provider_reference VARCHAR(255) NULL,
    amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    paid_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_payments_provider_reference UNIQUE (provider, provider_reference),
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings(id)
);

CREATE INDEX idx_payments_booking_status ON payments(booking_id, status);

CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_uid VARCHAR(128) NOT NULL,
    booking_id BIGINT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'CREATED',
    read_flag BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    read_at TIMESTAMP(6) NULL,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_uid) REFERENCES users(uid) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_booking FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
);

CREATE INDEX idx_notifications_user_read ON notifications(user_uid, read_flag, created_at);

CREATE TABLE device_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_uid VARCHAR(128) NOT NULL,
    token VARCHAR(512) NOT NULL,
    platform VARCHAR(32) NOT NULL DEFAULT 'ANDROID',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uk_device_tokens_token UNIQUE (token),
    CONSTRAINT fk_device_tokens_user FOREIGN KEY (user_uid) REFERENCES users(uid) ON DELETE CASCADE
);

CREATE INDEX idx_device_tokens_user_active ON device_tokens(user_uid, active);

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    actor_uid VARCHAR(128) NULL,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(128) NOT NULL,
    old_value TEXT NULL,
    new_value TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_uid) REFERENCES users(uid)
);

CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id, created_at);

INSERT INTO roles(code, name) VALUES
    ('CUSTOMER', 'Khách hàng'),
    ('STAFF', 'Nhân viên'),
    ('ADMIN', 'Quản trị viên');

INSERT INTO menu_categories(code, name, display_order) VALUES
    ('KHAI_VI', 'Khai vị', 10),
    ('MON_CHINH', 'Món chính', 20),
    ('TRANG_MIENG', 'Tráng miệng', 30);

INSERT INTO restaurant_areas(code, name, description) VALUES
    ('INDOOR', 'Trong nhà', 'Khu vực trong nhà'),
    ('OUTDOOR', 'Ngoài trời', 'Khu vực ngoài trời'),
    ('RIVERSIDE', 'Ven sông', 'Khu vực gần sông hoặc hồ'),
    ('PRIVATE', 'Phòng riêng', 'Khu vực phòng riêng');
