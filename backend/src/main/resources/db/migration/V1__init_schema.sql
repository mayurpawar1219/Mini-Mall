-- V1__init_schema.sql

CREATE TABLE users (
    id BINARY(16) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    role VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email)
);

CREATE TABLE categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_name (name)
);

CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category_id BIGINT NOT NULL,
    sku VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_products_sku (sku),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories (id)
);

CREATE TABLE inventory (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    available_quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inventory_product (product_id),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE carts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BINARY(16) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_carts_user (user_id),
    CONSTRAINT fk_carts_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE cart_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    cart_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_cart_items_cart FOREIGN KEY (cart_id) REFERENCES carts (id),
    CONSTRAINT fk_cart_items_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE pickup_slots (
    id BIGINT NOT NULL AUTO_INCREMENT,
    start_time DATETIME(6) NOT NULL,
    end_time DATETIME(6) NOT NULL,
    capacity INT NOT NULL,
    current_bookings INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id)
);

CREATE TABLE orders (
    id BINARY(16) NOT NULL,
    order_number VARCHAR(100) NOT NULL,
    user_id BINARY(16) NOT NULL,
    pickup_slot_id BIGINT,
    status VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    delivery_address VARCHAR(255),
    delivery_city VARCHAR(100),
    delivery_postal_code VARCHAR(20),
    delivery_phone VARCHAR(50),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_number (order_number),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_orders_pickup_slot FOREIGN KEY (pickup_slot_id) REFERENCES pickup_slots (id)
);

CREATE TABLE order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BINARY(16) NOT NULL,
    product_id BIGINT,
    product_name VARCHAR(255) NOT NULL,
    price_at_purchase DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_items_product FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE return_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BINARY(16) NOT NULL,
    order_item_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    resolved_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_return_requests_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_return_requests_order_item FOREIGN KEY (order_item_id) REFERENCES order_items (id)
);

CREATE TABLE exchange_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BINARY(16) NOT NULL,
    original_item_id BIGINT NOT NULL,
    replacement_product_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    resolved_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_exchange_requests_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_exchange_requests_original_item FOREIGN KEY (original_item_id) REFERENCES order_items (id),
    CONSTRAINT fk_exchange_requests_replacement_product FOREIGN KEY (replacement_product_id) REFERENCES products (id)
);

CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    actor_id BINARY(16),
    action VARCHAR(255) NOT NULL,
    entity_type VARCHAR(255) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    details TEXT,
    timestamp DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);
