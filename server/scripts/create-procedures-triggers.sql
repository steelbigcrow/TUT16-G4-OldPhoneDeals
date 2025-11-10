-- ========================================
-- MySQL Stored Procedures and Triggers
-- For OldPhoneDeals Project
-- ========================================

USE oldphonedeals;

-- ========================================
-- Stored Procedures
-- ========================================

-- Procedure 1: add_to_cart
-- Purpose: Safely add item to cart with quantity update if exists
-- ========================================
DELIMITER //

DROP PROCEDURE IF EXISTS add_to_cart//

CREATE PROCEDURE add_to_cart(
    IN p_user_id BIGINT UNSIGNED,
    IN p_phone_id BIGINT UNSIGNED,
    IN p_quantity INT UNSIGNED
)
BEGIN
    DECLARE v_cart_id BIGINT UNSIGNED;
    DECLARE v_phone_title VARCHAR(255);
    DECLARE v_phone_price DECIMAL(10,2);

    -- Start transaction
    START TRANSACTION;

    -- Get or create cart
    SELECT id INTO v_cart_id FROM carts WHERE user_id = p_user_id;

    IF v_cart_id IS NULL THEN
        INSERT INTO carts (user_id) VALUES (p_user_id);
        SET v_cart_id = LAST_INSERT_ID();
    END IF;

    -- Get phone details
    SELECT title, price INTO v_phone_title, v_phone_price
    FROM phones
    WHERE id = p_phone_id AND is_disabled = FALSE;

    IF v_phone_title IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Phone not found or disabled';
    END IF;

    -- Insert or update cart item
    INSERT INTO cart_items (cart_id, phone_id, title, quantity, price)
    VALUES (v_cart_id, p_phone_id, v_phone_title, p_quantity, v_phone_price)
    ON DUPLICATE KEY UPDATE
        quantity = quantity + p_quantity,
        price = v_phone_price;

    COMMIT;
END//

DELIMITER ;

-- ========================================
-- Procedure 2: create_order_from_cart
-- Purpose: Create order from cart and clear cart
-- ========================================
DELIMITER //

DROP PROCEDURE IF EXISTS create_order_from_cart//

CREATE PROCEDURE create_order_from_cart(
    IN p_user_id BIGINT UNSIGNED,
    IN p_street VARCHAR(255),
    IN p_city VARCHAR(100),
    IN p_state VARCHAR(100),
    IN p_zip VARCHAR(20),
    IN p_country VARCHAR(100),
    OUT p_order_id BIGINT UNSIGNED
)
BEGIN
    DECLARE v_cart_id BIGINT UNSIGNED;
    DECLARE v_total_amount DECIMAL(10,2);

    -- Start transaction
    START TRANSACTION;

    -- Get cart
    SELECT id INTO v_cart_id FROM carts WHERE user_id = p_user_id;

    IF v_cart_id IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cart not found';
    END IF;

    -- Calculate total
    SELECT SUM(quantity * price) INTO v_total_amount
    FROM cart_items
    WHERE cart_id = v_cart_id;

    IF v_total_amount IS NULL OR v_total_amount = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Cart is empty';
    END IF;

    -- Create order
    INSERT INTO orders (user_id, total_amount, street, city, state, zip, country)
    VALUES (p_user_id, v_total_amount, p_street, p_city, p_state, p_zip, p_country);

    SET p_order_id = LAST_INSERT_ID();

    -- Copy cart items to order items
    INSERT INTO order_items (order_id, phone_id, title, quantity, price)
    SELECT p_order_id, phone_id, title, quantity, price
    FROM cart_items
    WHERE cart_id = v_cart_id;

    -- Update sales count and stock
    UPDATE phones p
    INNER JOIN cart_items ci ON p.id = ci.phone_id
    SET p.sales_count = p.sales_count + ci.quantity,
        p.stock = p.stock - ci.quantity
    WHERE ci.cart_id = v_cart_id;

    -- Clear cart
    DELETE FROM cart_items WHERE cart_id = v_cart_id;

    COMMIT;
END//

DELIMITER ;

-- ========================================
-- Procedure 3: calculate_phone_average_rating
-- Purpose: Calculate average rating for a phone
-- ========================================
DELIMITER //

DROP PROCEDURE IF EXISTS calculate_phone_average_rating//

CREATE PROCEDURE calculate_phone_average_rating(
    IN p_phone_id BIGINT UNSIGNED,
    OUT p_average_rating DECIMAL(3,2),
    OUT p_review_count INT
)
BEGIN
    SELECT
        COALESCE(AVG(rating), 0),
        COUNT(id)
    INTO p_average_rating, p_review_count
    FROM reviews
    WHERE phone_id = p_phone_id AND is_hidden = FALSE;
END//

DELIMITER ;

-- ========================================
-- Triggers
-- ========================================

-- Trigger 1: before_cart_items_insert
-- Purpose: Validate stock availability before adding to cart
-- ========================================
DELIMITER //

DROP TRIGGER IF EXISTS before_cart_items_insert//

CREATE TRIGGER before_cart_items_insert
BEFORE INSERT ON cart_items
FOR EACH ROW
BEGIN
    DECLARE v_stock INT UNSIGNED;

    SELECT stock INTO v_stock FROM phones WHERE id = NEW.phone_id;

    IF v_stock < NEW.quantity THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Insufficient stock';
    END IF;
END//

DELIMITER ;

-- ========================================
-- Trigger 2: after_review_insert
-- Purpose: Update phone's updated_at timestamp after review
-- ========================================
DELIMITER //

DROP TRIGGER IF EXISTS after_review_insert//

CREATE TRIGGER after_review_insert
AFTER INSERT ON reviews
FOR EACH ROW
BEGIN
    -- Update phone's updated_at timestamp
    UPDATE phones
    SET updated_at = CURRENT_TIMESTAMP
    WHERE id = NEW.phone_id;
END//

DELIMITER ;

-- ========================================
-- Verification
-- ========================================
SELECT '✓ Stored procedures and triggers created successfully!' AS status;

-- List created procedures
SELECT ROUTINE_NAME, ROUTINE_TYPE
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = 'oldphonedeals'
ORDER BY ROUTINE_TYPE, ROUTINE_NAME;

-- List created triggers
SELECT TRIGGER_NAME, EVENT_MANIPULATION, EVENT_OBJECT_TABLE
FROM information_schema.TRIGGERS
WHERE TRIGGER_SCHEMA = 'oldphonedeals'
ORDER BY EVENT_OBJECT_TABLE, EVENT_MANIPULATION;
