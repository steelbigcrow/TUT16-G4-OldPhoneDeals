-- ========================================
-- MySQL Database Creation Script
-- For OldPhoneDeals Project
-- ========================================

-- Create database
CREATE DATABASE IF NOT EXISTS oldphonedeals
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE oldphonedeals;

-- ========================================
-- 1. Users Table
-- ========================================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL COMMENT 'Bcrypt hashed password',
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    is_disabled BOOLEAN NOT NULL DEFAULT FALSE,
    is_ban BOOLEAN NOT NULL DEFAULT FALSE,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    verify_token VARCHAR(255) NULL COMMENT 'Email verification token',
    last_login DATETIME NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_email (email),
    INDEX idx_is_admin (is_admin),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='User accounts and authentication';

-- ========================================
-- 2. Phones Table
-- ========================================
CREATE TABLE IF NOT EXISTS phones (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    brand ENUM('Samsung', 'Apple', 'HTC', 'Huawei', 'Nokia', 'LG', 'Motorola', 'Sony', 'BlackBerry') NOT NULL,
    image VARCHAR(500) NOT NULL COMMENT 'Image file path or URL',
    stock INT UNSIGNED NOT NULL DEFAULT 0,
    seller_id BIGINT UNSIGNED NOT NULL,
    price DECIMAL(10, 2) NOT NULL CHECK (price >= 0),
    is_disabled BOOLEAN NOT NULL DEFAULT FALSE,
    sales_count INT UNSIGNED NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY fk_phones_seller (seller_id)
        REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    INDEX idx_seller (seller_id),
    INDEX idx_brand (brand),
    INDEX idx_is_disabled (is_disabled),
    INDEX idx_sales_count (sales_count DESC),
    INDEX idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Phone product catalog';

-- ========================================
-- 3. Reviews Table
-- ========================================
CREATE TABLE IF NOT EXISTS reviews (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    phone_id BIGINT UNSIGNED NOT NULL,
    reviewer_id BIGINT UNSIGNED NOT NULL,
    rating TINYINT UNSIGNED NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment TEXT NOT NULL,
    is_hidden BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Hidden by admin',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY fk_reviews_phone (phone_id)
        REFERENCES phones(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    FOREIGN KEY fk_reviews_reviewer (reviewer_id)
        REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    INDEX idx_phone (phone_id),
    INDEX idx_reviewer (reviewer_id),
    INDEX idx_created_at (created_at DESC),
    INDEX idx_is_hidden (is_hidden),

    UNIQUE KEY uk_phone_reviewer (phone_id, reviewer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Product reviews and ratings';

-- ========================================
-- 4. Carts Table
-- ========================================
CREATE TABLE IF NOT EXISTS carts (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY fk_carts_user (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='User shopping carts';

-- ========================================
-- 5. Cart Items Table
-- ========================================
CREATE TABLE IF NOT EXISTS cart_items (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    cart_id BIGINT UNSIGNED NOT NULL,
    phone_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(255) NOT NULL COMMENT 'Cached phone title',
    quantity INT UNSIGNED NOT NULL DEFAULT 1 CHECK (quantity > 0),
    price DECIMAL(10, 2) NOT NULL CHECK (price >= 0) COMMENT 'Cached unit price',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY fk_cart_items_cart (cart_id)
        REFERENCES carts(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    FOREIGN KEY fk_cart_items_phone (phone_id)
        REFERENCES phones(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    INDEX idx_cart (cart_id),
    INDEX idx_phone (phone_id),

    UNIQUE KEY uk_cart_phone (cart_id, phone_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Shopping cart items';

-- ========================================
-- 6. Orders Table
-- ========================================
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL CHECK (total_amount >= 0),
    street VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    zip VARCHAR(20) NOT NULL,
    country VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY fk_orders_user (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    INDEX idx_user (user_id),
    INDEX idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Customer purchase orders';

-- ========================================
-- 7. Order Items Table
-- ========================================
CREATE TABLE IF NOT EXISTS order_items (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT UNSIGNED NOT NULL,
    phone_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(255) NOT NULL COMMENT 'Snapshot of phone title',
    quantity INT UNSIGNED NOT NULL DEFAULT 1 CHECK (quantity > 0),
    price DECIMAL(10, 2) NOT NULL CHECK (price >= 0) COMMENT 'Snapshot of unit price',

    FOREIGN KEY fk_order_items_order (order_id)
        REFERENCES orders(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    FOREIGN KEY fk_order_items_phone (phone_id)
        REFERENCES phones(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,

    INDEX idx_order (order_id),
    INDEX idx_phone (phone_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Order line items';

-- ========================================
-- 8. Wishlists Table (Junction Table)
-- ========================================
CREATE TABLE IF NOT EXISTS wishlists (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    phone_id BIGINT UNSIGNED NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY fk_wishlists_user (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    FOREIGN KEY fk_wishlists_phone (phone_id)
        REFERENCES phones(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    INDEX idx_user (user_id),
    INDEX idx_phone (phone_id),
    INDEX idx_created_at (created_at DESC),

    UNIQUE KEY uk_user_phone (user_id, phone_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='User wishlist items';

-- ========================================
-- 9. Admin Logs Table
-- ========================================
CREATE TABLE IF NOT EXISTS admin_logs (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    admin_user_id BIGINT UNSIGNED NOT NULL,
    action VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT UNSIGNED NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY fk_admin_logs_admin (admin_user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,

    INDEX idx_admin_user (admin_user_id),
    INDEX idx_target (target_type, target_id),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at DESC),

    CHECK (action IN (
        'CREATE_USER', 'UPDATE_USER', 'DELETE_USER', 'DISABLE_USER', 'ENABLE_USER',
        'CREATE_PHONE', 'UPDATE_PHONE', 'DELETE_PHONE', 'DISABLE_PHONE', 'ENABLE_PHONE',
        'HIDE_REVIEW', 'SHOW_REVIEW', 'DELETE_REVIEW',
        'EXPORT_ORDERS'
    )),

    CHECK (target_type IN ('User', 'Phone', 'Review', 'Order'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Administrative action audit log';

-- ========================================
-- Views
-- ========================================

-- View: phone_ratings
CREATE OR REPLACE VIEW phone_ratings AS
SELECT
    p.id AS phone_id,
    p.title,
    COUNT(r.id) AS review_count,
    COALESCE(AVG(r.rating), 0) AS average_rating
FROM phones p
LEFT JOIN reviews r ON p.id = r.phone_id AND r.is_hidden = FALSE
GROUP BY p.id, p.title;

-- View: cart_summary
CREATE OR REPLACE VIEW cart_summary AS
SELECT
    c.id AS cart_id,
    c.user_id,
    COUNT(ci.id) AS item_count,
    COALESCE(SUM(ci.quantity), 0) AS total_quantity,
    COALESCE(SUM(ci.quantity * ci.price), 0) AS total_amount
FROM carts c
LEFT JOIN cart_items ci ON c.id = ci.cart_id
GROUP BY c.id, c.user_id;

-- View: user_statistics
CREATE OR REPLACE VIEW user_statistics AS
SELECT
    u.id AS user_id,
    u.email,
    COUNT(DISTINCT o.id) AS order_count,
    COALESCE(SUM(o.total_amount), 0) AS total_spent,
    COUNT(DISTINCT r.id) AS review_count,
    COUNT(DISTINCT w.phone_id) AS wishlist_count
FROM users u
LEFT JOIN orders o ON u.id = o.user_id
LEFT JOIN reviews r ON u.id = r.reviewer_id
LEFT JOIN wishlists w ON u.id = w.user_id
GROUP BY u.id, u.email;

-- ========================================
-- Initial Data Summary
-- ========================================
SELECT '✓ Database and tables created successfully!' AS status;
SELECT 'Run the data conversion script to populate tables' AS next_step;
