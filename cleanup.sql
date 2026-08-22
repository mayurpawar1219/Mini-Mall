SET SQL_SAFE_UPDATES = 0;

-- 1. Identify test users
CREATE TEMPORARY TABLE temp_test_users AS
SELECT id FROM users 
WHERE email LIKE '%@t.com' 
   OR email LIKE '%@test.com' 
   OR (first_name = 'T' AND last_name = 'T');

-- 2. Delete test orders and dependencies
CREATE TEMPORARY TABLE temp_test_orders AS
SELECT id FROM orders WHERE user_id IN (SELECT id FROM temp_test_users);

-- Removed pickup_slots delete as it is not dependent on order_id
DELETE FROM return_requests WHERE order_id IN (SELECT id FROM temp_test_orders);
DELETE FROM exchange_requests WHERE order_id IN (SELECT id FROM temp_test_orders);
DELETE FROM order_items WHERE order_id IN (SELECT id FROM temp_test_orders);
DELETE FROM audit_logs WHERE entity_type = 'Order' AND entity_id IN (SELECT HEX(id) FROM temp_test_orders);
DELETE FROM orders WHERE id IN (SELECT id FROM temp_test_orders);

-- 3. Delete test carts and dependencies
CREATE TEMPORARY TABLE temp_test_carts AS
SELECT id FROM carts WHERE user_id IN (SELECT id FROM temp_test_users);

DELETE FROM cart_items WHERE cart_id IN (SELECT id FROM temp_test_carts);
DELETE FROM carts WHERE id IN (SELECT id FROM temp_test_carts);

-- 4. Delete test users
DELETE FROM users WHERE id IN (SELECT id FROM temp_test_users);

-- 5. Identify test products
CREATE TEMPORARY TABLE temp_test_products AS
SELECT id FROM products 
WHERE name LIKE 'Test Prod%' 
   OR name LIKE 'Fail Prod%' 
   OR name LIKE 'Conc Prod%' 
   OR name LIKE 'Exact Prod%' 
   OR name LIKE 'Cap Prod%' 
   OR name LIKE 'Conc Gap Prod%';

-- 6. Delete test products dependencies (inventory, order_items, cart_items)
DELETE FROM inventory WHERE product_id IN (SELECT id FROM temp_test_products);
DELETE FROM cart_items WHERE product_id IN (SELECT id FROM temp_test_products);
DELETE FROM order_items WHERE product_id IN (SELECT id FROM temp_test_products);
DELETE FROM products WHERE id IN (SELECT id FROM temp_test_products);

-- 7. Identify test categories
CREATE TEMPORARY TABLE temp_test_categories AS
SELECT id FROM categories WHERE name LIKE 'TestCat%';

-- 8. Test categories should be deleted, but let's just make sure there are no products left.
-- In case there are, set category_id to NULL first to avoid cascade destruction of legitimate products.
UPDATE products SET category_id = NULL WHERE category_id IN (SELECT id FROM temp_test_categories);
DELETE FROM categories WHERE id IN (SELECT id FROM temp_test_categories);

SET SQL_SAFE_UPDATES = 1;
